$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'
if (Test-Path -LiteralPath $envFile) { throw '.env already exists; it was not overwritten.' }
function New-RandomSecret([int]$size) {
    $bytes = New-Object byte[] $size
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$contents = @(
    'MYSQL_PASSWORD=' + (New-RandomSecret 24)
    'MYSQL_ROOT_PASSWORD=' + (New-RandomSecret 24)
    'MINIO_ACCESS_KEY=homely-local'
    'MINIO_SECRET_KEY=' + (New-RandomSecret 24)
    'JWT_SECRET=' + (New-RandomSecret 64)
    'DEMO_PASSWORD=' + (New-RandomSecret 18)
    'API_BIND_ADDRESS=127.0.0.1'
)
[IO.File]::WriteAllLines($envFile, $contents, (New-Object Text.UTF8Encoding $false))
Write-Output 'Created Infra/.env. Read DEMO_PASSWORD locally to sign in; do not commit this file.'
