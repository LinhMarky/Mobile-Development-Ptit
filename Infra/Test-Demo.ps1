param([string]$BaseUrl = 'http://localhost:8080/api/v1')
$ErrorActionPreference = 'Stop'
if (-not $env:DEMO_PASSWORD) { throw 'Set DEMO_PASSWORD before running this script.' }
function Invoke-Api([string]$Method, [string]$Path, $Data, [string]$Token, [string]$Key = '') {
    $headers = @{}
    if ($Token) { $headers.Authorization = 'Bearer ' + $Token }
    if ($Method -eq 'POST' -and -not $Path.StartsWith('/auth/')) {
        if (-not $Key) { $Key = [guid]::NewGuid().ToString() }
        $headers['Idempotency-Key'] = $Key
    }
    $params = @{ Uri = $BaseUrl + $Path; Method = $Method; Headers = $headers; ContentType = 'application/json'; TimeoutSec = 20 }
    if ($null -ne $Data) { $params.Body = [Text.Encoding]::UTF8.GetBytes(($Data | ConvertTo-Json -Depth 12 -Compress)) }
    $envelope = Invoke-RestMethod @params
    if ($null -eq $envelope) { return $null }
    if (-not $envelope.PSObject.Properties['statusCode'] -or -not $envelope.PSObject.Properties['message'] -or -not $envelope.PSObject.Properties['data']) {
        throw 'API response must contain statusCode, message and data.'
    }
    if (@($envelope.PSObject.Properties).Count -ne 3 -or $envelope.statusCode -lt 200 -or $envelope.statusCode -ge 300) {
        throw 'Invalid success envelope.'
    }
    $envelope.data
}
function Login([string]$Email) {
    Invoke-Api 'POST' '/auth/login' @{email=$Email;password=$env:DEMO_PASSWORD;installation_id=[guid]::NewGuid().ToString();device_name='HTTP acceptance'} ''
}
function Assert-Status([int]$Expected, [scriptblock]$Request) {
    try { & $Request | Out-Null } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq $Expected) {
            $rawError = $_.ErrorDetails.Message
            if (-not $rawError) {
                $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
                try { $rawError = $reader.ReadToEnd() } finally { $reader.Dispose() }
            }
            $errorEnvelope = $rawError | ConvertFrom-Json
            if (@($errorEnvelope.PSObject.Properties).Count -ne 3 -or $errorEnvelope.statusCode -ne $Expected -or $errorEnvelope.data.status -ne $Expected -or -not $errorEnvelope.data.code -or -not $errorEnvelope.data.trace_id) {
                throw 'Invalid error envelope.'
            }
            return
        }
        throw
    }
    throw "Expected HTTP $Expected but request succeeded."
}
$tenant = Login 'tenant@homely.test'
$hostUser = Login 'host@homely.test'
$admin = Login 'admin@homely.test'
$tenantToken = $tenant.access_token
$hostToken = $hostUser.access_token
$adminToken = $admin.access_token
$room = Invoke-Api 'POST' '/rooms' @{
    unit_code = 'HTTP_' + [guid]::NewGuid().ToString('N').Substring(0,12).ToUpper()
    room_type='SINGLE_ROOM';area_m2=25;max_occupants=2;amenity_ids=@()
    address=@{line='Phong kiem thu HTTP';province_code='HN';province_name='Ha Noi';ward_code='HA_DONG';ward_name='Ha Dong'}
    location=@{latitude=20.98;longitude=105.79}
} $hostToken
$listing = Invoke-Api 'POST' ('/listings/room/' + $room.id) @{title='HTTP acceptance room';description='Repeatable demo acceptance';rent_vnd='3000000';deposit_vnd='1000000'} $hostToken
Assert-Status 404 { Invoke-Api 'GET' ('/listings/' + $listing.id) $null '' }
Invoke-Api 'POST' ('/listings/' + $listing.id + '/submit') $null $hostToken | Out-Null
Assert-Status 403 { Invoke-Api 'POST' ('/admin/listings/' + $listing.id + '/approve') $null $tenantToken }
Invoke-Api 'POST' ('/admin/listings/' + $listing.id + '/approve') $null $adminToken | Out-Null
$public = Invoke-Api 'GET' ('/listings/' + $listing.id) $null ''
if ($public.rent_vnd -isnot [string]) { throw 'Money must serialize as a string.' }
$key = [guid]::NewGuid().ToString()
$request = @{room_id=$room.id;occupant_count=1}
$booking = Invoke-Api 'POST' '/bookings' $request $tenantToken $key
$replayed = Invoke-Api 'POST' '/bookings' $request $tenantToken $key
if ($booking.id -ne $replayed.id) { throw 'Booking idempotency failed.' }
Assert-Status 409 { Invoke-Api 'POST' '/bookings' @{room_id=$room.id;occupant_count=2} $tenantToken $key }
Invoke-Api 'POST' ('/bookings/' + $booking.id + '/approve') $null $hostToken | Out-Null
Assert-Status 409 { Invoke-Api 'POST' ('/bookings/' + $booking.id + '/confirm-deposit') $null $tenantToken }
$payment = Invoke-Api 'POST' '/payments' @{booking_id=$booking.id} $tenantToken
Assert-Status 404 { Invoke-Api 'POST' ('/payments/' + $payment.id + '/simulate') $null $hostToken }
Invoke-Api 'POST' ('/payments/' + $payment.id + '/simulate') $null $tenantToken | Out-Null
$confirmed = Invoke-Api 'GET' ('/bookings/' + $booking.id) $null $tenantToken
if ($confirmed.status -ne 'CONFIRMED') { throw 'Payment did not confirm booking.' }
Invoke-Api 'POST' ('/bookings/' + $booking.id + '/handover-tenant') $null $tenantToken | Out-Null
$completed = Invoke-Api 'POST' ('/bookings/' + $booking.id + '/handover-host') $null $hostToken
if ($completed.status -ne 'COMPLETED') { throw 'Handover did not complete booking.' }
$inbox = Invoke-Api 'GET' '/notifications?size=50' $null $tenantToken
if ($inbox.total_elements -lt 1) { throw 'Booking notifications are missing.' }
$rotated = Invoke-Api 'POST' '/auth/refresh' @{refresh_token=$tenant.refresh_token} ''
Assert-Status 400 { Invoke-Api 'POST' '/auth/refresh' @{refresh_token=$tenant.refresh_token} '' }
Invoke-Api 'POST' '/auth/logout' @{refresh_token=$rotated.refresh_token} $rotated.access_token | Out-Null
Assert-Status 400 { Invoke-Api 'POST' '/auth/refresh' @{refresh_token=$rotated.refresh_token} '' }
Write-Output ('PASS: auth rotation/logout, catalog moderation/privacy, money DTO, booking replay/conflict, deposit guard, sandbox payment, handover, inbox. Booking ID: ' + $booking.id)
