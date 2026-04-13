[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$tempDir = Join-Path $PSScriptRoot 'temp'
$outDir = Join-Path $workspaceRoot 'docs\risk-data'
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = Get-ChildItem 'C:\Users\szh\Desktop' -Filter '*.docx'
Write-Output "Found $($files.Count) docx files:"
foreach ($f in $files) {
    Write-Output "  Size=$($f.Length) Name=$($f.Name)"
}

$targetFile = $null
foreach ($f in $files) {
    if ($f.Length -eq 54705) {
        $targetFile = $f
        break
    }
}

if ($targetFile -eq $null) {
    Write-Output "Target file not found by size 54705"
    exit 1
}

$tempPath = Join-Path $tempDir 'risk_tips_temp3.docx'
Copy-Item -LiteralPath $targetFile.FullName -Destination $tempPath -Force
Write-Output "Copied: Size=$($targetFile.Length)"

Add-Type -AssemblyName 'System.IO.Compression.FileSystem'
$zip = [System.IO.Compression.ZipFile]::OpenRead($tempPath)
$entry = $zip.GetEntry('word/document.xml')
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
$xmlContent = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()

$xml = [xml]$xmlContent
$ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')

$paragraphs = $xml.SelectNodes('//w:p', $ns)
$count = 0
$output = [System.Text.StringBuilder]::new()
foreach ($p in $paragraphs) {
    $texts = $p.SelectNodes('.//w:t', $ns)
    $line = ''
    foreach ($t in $texts) {
        $line += $t.InnerText
    }
    if ($line.Trim()) {
        [void]$output.AppendLine("$count`t$line")
        $count++
    }
}

$outFile = Join-Path $outDir 'risk_data_3.txt'
[System.IO.File]::WriteAllText($outFile, $output.ToString(), [System.Text.Encoding]::UTF8)
Write-Output "Total paragraphs: $count"
Write-Output "Saved to: $outFile"

Remove-Item $tempPath -Force -ErrorAction SilentlyContinue
