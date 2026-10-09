param([string]$Path = "keystore_b64.txt")
$b = [IO.File]::ReadAllText($Path).Trim()
Write-Host ("length = " + $b.Length)
if ($b.Length -ne 5816) { Write-Host "FAIL: wrong length (expect 5816)"; exit 1 }
try {
  $d = [Convert]::FromBase64String($b)
  $o = [IO.File]::ReadAllBytes("my-upload-key.jks")
  if ($d.Length -ne $o.Length) { Write-Host "FAIL: byte count mismatch"; exit 1 }
  $same = $true
  for ($i=0; $i -lt $d.Length; $i++) { if ($d[$i] -ne $o[$i]) { $same = $false; break } }
  if (-not $same) { Write-Host "FAIL: content mismatch"; exit 1 }
  Write-Host "OK - copy the entire file content into the secret field"
} catch { Write-Host ("FAIL: decode error - " + $_.Exception.Message); exit 1 }