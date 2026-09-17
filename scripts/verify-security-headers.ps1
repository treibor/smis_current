param([Parameter(Mandatory=$true)][string]$BaseUrl)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$base = $BaseUrl.TrimEnd('/')

function Check-Response([string]$Path, [string]$Method, [int]$ExpectedStatus) {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$base$Path")
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    try {
        if ([int]$response.StatusCode -ne $ExpectedStatus) { throw "$Method $Path returned $([int]$response.StatusCode), expected $ExpectedStatus" }
        $expected = @{
            'Referrer-Policy' = 'strict-origin-when-cross-origin'
            'X-Content-Type-Options' = 'nosniff'
            'X-Frame-Options' = 'DENY'
            'X-XSS-Protection' = '0'
            'Permissions-Policy' = 'geolocation=(self), microphone=()'
        }
        foreach ($name in $expected.Keys) {
            if (!$response.Headers.Contains($name)) { throw "Missing $name on $Path" }
            $values = @($response.Headers.GetValues($name))
            if ($values.Count -ne 1 -or $values[0] -ne $expected[$name]) { throw "Incorrect or duplicate $name on $Path" }
        }
        foreach ($name in @('Expect-CT', 'Content-Security-Policy-Report-Only')) {
            if ($response.Headers.Contains($name)) { throw "Unexpected $name on $Path" }
        }
        $policies = @($response.Headers.GetValues('Content-Security-Policy'))
        if ($policies.Count -ne 1) { throw "Expected exactly one CSP on $Path" }
        $directives = @($policies[0].Split(';') | ForEach-Object { $_.Trim() })
        foreach ($required in @("default-src 'self'", "base-uri 'self'", "object-src 'none'", "frame-ancestors 'none'", "style-src 'self' 'unsafe-inline'")) {
            if ($directives -notcontains $required) { throw "Missing required CSP directive on $Path" }
        }
        $scriptPolicy = @($directives | Where-Object { $_.StartsWith('script-src ') })
        if ($scriptPolicy.Count -ne 1 -or $scriptPolicy[0].Contains("'unsafe-inline'") -or $scriptPolicy[0].Contains("'unsafe-eval'")) { throw "Unsafe or missing script policy on $Path" }
        if ($Path -eq '/login' -and $directives -notcontains "require-trusted-types-for 'script'") { throw 'Trusted Types enforcement missing on bootstrap HTML' }
        if ($response.Headers.Contains('Set-Cookie')) {
            foreach ($cookie in $response.Headers.GetValues('Set-Cookie')) {
                if ($cookie.StartsWith('SameSite=')) { throw 'Fake session cookie found' }
            }
        }
        if ($base.StartsWith('https://')) {
            if (@($response.Headers.GetValues('Strict-Transport-Security'))[0] -ne 'max-age=31536000') { throw 'Unexpected HSTS' }
        } elseif ($response.Headers.Contains('Strict-Transport-Security')) { throw 'HSTS appeared on HTTP' }
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $cache = if ($response.Headers.Contains('Cache-Control')) { $response.Headers.GetValues('Cache-Control') -join ', ' } else { '(absent)' }
        if ($Path.StartsWith('/VAADIN/build/') -and $ExpectedStatus -eq 200 -and $cache -match 'no-store') {
            throw 'Successful Vaadin build resource unexpectedly uses no-store'
        }
        Write-Host "PASS $Method $Path status=$ExpectedStatus cache=$cache"
        return $body
    } finally { $response.Dispose(); $request.Dispose() }
}

try {
    $bootstrap = Check-Response '/login' 'GET' 200
    if ($bootstrap -notmatch 'VAADIN/build/([^"''<> ]+\.js)') { throw 'No production Vaadin bootstrap bundle found' }
    $bundle = '/VAADIN/build/' + $Matches[1]
    $null = Check-Response $bundle 'GET' 200
    $null = Check-Response $bundle 'HEAD' 200
    $null = Check-Response '/images/plant.png' 'GET' 200
    $null = Check-Response '/images/plant.png' 'HEAD' 200
    $null = Check-Response '/VAADIN/build/security-header-missing-00000000.js' 'HEAD' 404
} finally { $client.Dispose(); $handler.Dispose() }
