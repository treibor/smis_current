$ErrorActionPreference = 'Stop'
# First run Maven dependency:build-classpath -Dmdep.outputFile=target/security-classpath.txt.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$sources = @()
foreach ($dependency in (Get-Content target/security-classpath.txt -Raw).Trim().Split(';')) {
    $sourceJar = $dependency -replace '\.jar$', '-sources.jar'
    if (!(Test-Path -LiteralPath $sourceJar)) { continue }
    $archive = [IO.Compression.ZipFile]::OpenRead($sourceJar)
    try {
        foreach ($entry in $archive.Entries) {
            if (!$entry.FullName.EndsWith('.java')) { continue }
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $source = $reader.ReadToEnd() } finally { $reader.Dispose() }
            if ($source -match 'executeJs|executeJavaScript|callJsFunction|NativeFunction|\.eval') {
                $sources += @{jar=[IO.Path]::GetFileName($sourceJar); file=$entry.FullName; source=$source}
            }
        }
    } finally { $archive.Dispose() }
}
[IO.File]::WriteAllText((Join-Path (Get-Location) 'target/csp-source-inventory.json'), (ConvertTo-Json -InputObject $sources -Depth 4))
