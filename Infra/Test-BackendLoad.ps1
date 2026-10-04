param(
    [string]$BaseUrl = 'http://localhost:8080/api/v1',
    [ValidateRange(1, 32)][int]$Concurrency = 4,
    [ValidateRange(1, 1000)][int]$RequestsPerWorker = 25,
    [int]$MaxP95Ms = 5000,
    [string]$OutputPath = (Join-Path $PSScriptRoot '../backend/target/backend-load-results.json')
)
$ErrorActionPreference = 'Stop'
$paths = @('/listings?size=20', '/listings?query=room&size=20', '/listings?min_rent_vnd=1000000&max_rent_vnd=5000000&size=20')
$pool = [RunspaceFactory]::CreateRunspacePool(1, $Concurrency)
$pool.Open()
$tasks = @()
$worker = {
    param($BaseUrl, $Paths, $Count)
    for ($index = 0; $index -lt $Count; $index++) {
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $status = 0
        $valid = $false
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $Paths[$index % $Paths.Count]) -TimeoutSec 20
            $status = [int]$response.StatusCode
            $body = $response.Content | ConvertFrom-Json
            $valid = $status -eq 200 -and $body.statusCode -eq 200 -and @($body.PSObject.Properties).Count -eq 3 -and $null -ne $body.data
        } catch {
            if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        } finally { $watch.Stop() }
        [pscustomobject]@{ latency_ms = $watch.ElapsedMilliseconds; status = $status; valid = $valid }
    }
}
try {
    for ($index = 0; $index -lt $Concurrency; $index++) {
        $shell = [PowerShell]::Create()
        $shell.RunspacePool = $pool
        $null = $shell.AddScript($worker.ToString()).AddArgument($BaseUrl).AddArgument($paths).AddArgument($RequestsPerWorker)
        $tasks += [pscustomobject]@{ shell = $shell; handle = $shell.BeginInvoke() }
    }
    $results = @()
    foreach ($task in $tasks) { $results += @($task.shell.EndInvoke($task.handle)) }
    $latencies = @($results.latency_ms | Sort-Object)
    $p95 = $latencies[[Math]::Max(0, [int][Math]::Ceiling($latencies.Count * 0.95) - 1)]
    $failures = @($results | Where-Object { -not $_.valid }).Count
    $summary = [ordered]@{
        timestamp = [DateTime]::UtcNow.ToString('o')
        base_url = $BaseUrl
        concurrency = $Concurrency
        requests = $results.Count
        failures = $failures
        p95_ms = $p95
        max_ms = $latencies[-1]
        pass = $failures -eq 0 -and $results.Count -eq ($Concurrency * $RequestsPerWorker) -and $p95 -le $MaxP95Ms
    }
    $directory = Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($summary | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
    $summary | ConvertTo-Json
    if (-not $summary.pass) { throw 'Load check failed; inspect the generated report.' }
} finally {
    foreach ($task in $tasks) { $task.shell.Dispose() }
    $pool.Close()
    $pool.Dispose()
}
