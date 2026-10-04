param(
    [string]$BaseUrl = 'http://localhost:8080/api/v1',
    [string]$MailpitUrl = 'http://localhost:8025',
    [string]$OutputPath = (Join-Path $PSScriptRoot '../backend/target/infrastructure-results.json')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

function Call-Api([string]$Method, [string]$Path, $Data, [string]$Token = '', [int]$Expected = 200) {
    $headers = @{}
    if ($Token) { $headers.Authorization = 'Bearer ' + $Token }
    if ($Method -eq 'POST' -and -not $Path.StartsWith('/auth/') -and -not $Path.StartsWith('/media/')) {
        $headers['Idempotency-Key'] = [guid]::NewGuid().ToString()
    }
    $parameters = @{ Uri = $BaseUrl + $Path; Method = $Method; Headers = $headers; ContentType = 'application/json'; TimeoutSec = 20; UseBasicParsing = $true }
    if ($null -ne $Data) { $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Data | ConvertTo-Json -Depth 10 -Compress)) }
    $status = 0
    $content = ''
    try {
        $response = Invoke-WebRequest @parameters
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if (-not $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        $content = $_.ErrorDetails.Message
        if (-not $content) {
            $reader = [IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
    }
    if ($status -ne $Expected) { throw "Expected HTTP $Expected for $Method $Path; received $status." }
    $envelope = $content | ConvertFrom-Json
    if (@($envelope.PSObject.Properties).Count -ne 3 -or $envelope.statusCode -ne $status -or -not $envelope.PSObject.Properties['data']) {
        throw 'Invalid HTTP JSON envelope.'
    }
    return $envelope.data
}

function Login-Account([string]$Email, [string]$Password) {
    Call-Api 'POST' '/auth/login' @{ email=$Email; password=$Password; installation_id=[guid]::NewGuid().ToString(); device_name='Infrastructure acceptance' }
}

function Upload-Png([byte[]]$Bytes, [string]$Token) {
    $client = [Net.Http.HttpClient]::new()
    $multipart = [Net.Http.MultipartFormDataContent]::new()
    try {
        $client.DefaultRequestHeaders.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
        $image = [Net.Http.ByteArrayContent]::new($Bytes)
        $image.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
        $multipart.Add($image, 'file', 'acceptance.png')
        $multipart.Add([Net.Http.StringContent]::new('ROOM_PHOTO'), 'purpose')
        $response = $client.PostAsync($BaseUrl + '/media/upload', $multipart).GetAwaiter().GetResult()
        try {
            if ([int]$response.StatusCode -ne 201) { throw 'MinIO upload failed.' }
            $envelope = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            if ($envelope.statusCode -ne 201 -or -not $envelope.data.id) { throw 'Invalid upload envelope.' }
            return $envelope.data
        } finally { $response.Dispose() }
    } finally { $multipart.Dispose(); $client.Dispose() }
}

$email = 'infra-' + [guid]::NewGuid().ToString('N') + '@homely.test'
$password = [guid]::NewGuid().ToString() + '-H0mely!'
$null = Call-Api 'POST' '/auth/register' @{ email=$email; password=$password; full_name='Infrastructure acceptance' } '' 201
$search = Invoke-RestMethod -Uri ($MailpitUrl + '/api/v1/search?query=' + [Uri]::EscapeDataString('to:' + $email)) -TimeoutSec 10
if (-not $search.messages -or $search.messages.Count -lt 1) { throw 'SMTP message did not reach Mailpit.' }
$message = Invoke-RestMethod -Uri ($MailpitUrl + '/api/v1/message/' + $search.messages[0].ID) -TimeoutSec 10
$verification = (($message.Text.Trim() -split '\r?\n')[-1]).Trim()
if (-not $verification) { throw 'Verification token missing from the email.' }
$null = Call-Api 'POST' '/auth/verify-email' @{token=$verification}
$session = Login-Account $email $password
$null = Call-Api 'POST' '/auth/enable-host' $null $session.access_token
$session = Login-Account $email $password
$room = Call-Api 'POST' '/rooms' @{
    unit_code='INFRA_' + [guid]::NewGuid().ToString('N').Substring(0,12)
    room_type='SINGLE_ROOM'; area_m2=25; max_occupants=2; amenity_ids=@()
    address=@{line='Infrastructure acceptance'; province_code='HN'; province_name='Ha Noi'}
    location=@{latitude=21.0285; longitude=105.8542}
} $session.access_token
$png = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
$media = Upload-Png $png $session.access_token
$null = Call-Api 'POST' ('/media/attach/room/' + $room.id) @{media_ids=@($media.id)} $session.access_token
$authorized = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/media/' + $media.id + '/content') -Headers @{Authorization='Bearer ' + $session.access_token} -TimeoutSec 20
if ($authorized.Headers['Content-Type'] -notlike 'image/png*' -or $authorized.RawContentLength -ne $png.Length) { throw 'Stored media content did not match the upload.' }
$downloaded = [IO.MemoryStream]::new()
try {
    $authorized.RawContentStream.Position = 0
    $authorized.RawContentStream.CopyTo($downloaded)
    if ([Convert]::ToBase64String($downloaded.ToArray()) -ne [Convert]::ToBase64String($png)) { throw 'Stored media bytes did not match the upload.' }
} finally { $downloaded.Dispose() }
$null = Call-Api 'GET' ('/media/' + $media.id + '/content') $null '' 404

# Exhaust only this new account's login bucket; do not interfere with demo users.
$limited = $false
for ($attempt = 0; $attempt -lt 12; $attempt++) {
    try {
        $null = Call-Api 'POST' '/auth/login' @{email=$email; password='wrong-password'} '' 401
    } catch {
        # Use a direct request to verify the rate-limit status and Retry-After header.
        try {
            $null = Invoke-WebRequest -UseBasicParsing -Method POST -Uri ($BaseUrl + '/auth/login') -ContentType 'application/json' -Body (@{email=$email;password='wrong-password'} | ConvertTo-Json -Compress)
        } catch {
            if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 429 -and $_.Exception.Response.Headers['Retry-After']) { $limited = $true; break }
            throw
        }
    }
}
if (-not $limited) { throw 'Auth rate limiting did not engage.' }
$null = Call-Api 'POST' '/auth/delete-account' @{current_password=$password} $session.access_token
try {
    $state = Call-Api 'GET' '/auth/delete-account' $null $session.access_token
    if ($state.status -ne 'PENDING' -and $state.status -ne 'PROCESSING') { throw 'Unexpected deletion state.' }
} catch {
    # The job can finish between POST and GET; then the access token is already invalid.
    $null = Call-Api 'GET' '/profile' $null $session.access_token 401
}
$completed = $false
for ($poll = 0; $poll -lt 45; $poll++) {
    try {
        $null = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/profile') -Headers @{Authorization='Bearer ' + $session.access_token} -TimeoutSec 10
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401) { $completed = $true; break }
        if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 403) { throw }
    }
    Start-Sleep -Seconds 1
}
if (-not $completed) { throw 'Account deletion did not complete within the acceptance timeout.' }
$null = Call-Api 'GET' ('/media/' + $media.id + '/content') $null '' 404
$summary = [ordered]@{timestamp=[DateTime]::UtcNow.ToString('o'); smtp_mailpit=$true; minio_upload_read=$true; private_media=$true; auth_rate_limit=$true; account_deletion=$true; pass=$true}
$directory = Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))
[IO.Directory]::CreateDirectory($directory) | Out-Null
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($summary | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
$summary | ConvertTo-Json
