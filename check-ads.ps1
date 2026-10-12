#Requires -Version 5.1
<#
.SYNOPSIS
    AdMob 광고가 왜 안 뜨는지 진단합니다.

.DESCRIPTION
    debug APK(테스트 ID)와 release APK(실 ID)의 차이를 로그로 확인합니다.
    같은 기기·같은 네트워크에서 테스트 광고는 떴는데 실 광고가 안 뜨면,
    원인은 코드가 아니라 AdMob 서버 쪽(앱 준비 상태)입니다.

.PARAMETER Package
    진단할 패키지명 (기본: com.chargesound.app)

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\check-ads.ps1
#>

param(
    [string]$Package = "com.chargesound.app",
    [int]$Seconds = 40
)

$ErrorActionPreference = "Continue"

# ── 0. adb 찾기 (PATH에 없으면 Android SDK에서 탐색) ────────────────
function Resolve-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe") }
    if ($env:ANDROID_SDK_ROOT) { $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe") }
    $local = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    $candidates += $local

    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

$Adb = Resolve-Adb
if (-not $Adb) {
    Write-Host "adb.exe 를 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "ANDROID_HOME 을 설정하거나, platform-tools 를 PATH 에 추가해 주세요." -ForegroundColor Red
    exit 1
}

function Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Adb { & $Adb @args }

Step "0. adb"
Write-Host "  $Adb"

# ── 1. 기기 연결 확인 ──────────────────────────────────────────────
Step "1. 기기 확인"
$devices = Adb devices 2>&1
$devices | ForEach-Object { Write-Host "  $_" }

$deviceLines = @($devices | Select-String -Pattern "\tdevice$")
if ($deviceLines.Count -eq 0) {
    Write-Host "`n기기가 연결 안 됐습니다. USB 케이블을 꽂고 USB 디버깅을 켜주세요." -ForegroundColor Red
    exit 1
}

# ── 2. 설치된 앱 확인 ──────────────────────────────────────────────
Step "2. 설치된 앱"
Adb shell pm list packages 2>&1 |
    Where-Object { $_ -match "chargesound|aistudio" } |
    ForEach-Object { Write-Host "  $_" }

$installed = Adb shell pm list packages 2>&1 |
    Where-Object { $_ -match "package:$([regex]::Escape($Package))" }

if (-not $installed) {
    Write-Host "`n[$Package] 이(가) 설치되어 있지 않습니다." -ForegroundColor Yellow
}

# ── 3. 매니페스트의 APPLICATION_ID 확인 ───────────────────────────
Step "3. 설치된 앱 정보"
Adb shell dumpsys package $Package 2>&1 |
    Select-String -Pattern "versionName|versionCode|firstInstallTime|lastUpdateTime" |
    ForEach-Object { Write-Host "  $($_.Line.Trim())" }

# ── 4. 로그 수집 ───────────────────────────────────────────────────
Step "4. 광고 관련 로그 수집 ($Seconds 초간)"
Write-Host "  지금 release APK 를 실행해서 배너/네이티브 광고 영역이 있는"
Write-Host "  메인 화면을 띄워두세요. 여기서 기다립니다."
Write-Host ""

# 스트리밍 대신 dump 모드 사용: 스트리밍은 프로세스가 안 끝나 멈출 수 있습니다.
& $Adb logcat -c 2>&1 | Out-Null
Start-Sleep -Seconds $Seconds
$log = @(& $Adb logcat -d -v time 2>&1)

$logPath = Join-Path $env:TEMP "admob_log.txt"
$log | Out-File $logPath -Encoding UTF8
Write-Host "  로그 $log.Length 줄 저장됨 → $logPath"

# ── 5. 로그 분석 ───────────────────────────────────────────────────
Step "5. 분석 결과"

$patterns = [ordered]@{
    "SDK 초기화"            = "MobileAds initialized|Google Mobile Ads SDK was initialized"
    "광고 로드 성공"        = "Ad loaded|onAdLoaded|loadAd: Ad"
    "광고 로드 실패"        = "onAdFailedToLoad|ad failed to load|Failed to load ad"
    "채움 없음(no fill)"    = "no fill|No ad config|returned no ads"
    "앱 미준비/게재제한"   = "not eligible|no eligible|limited ad serving|app readiness"
    "네트워크 오류"         = "NetworkError|SocketTimeout|Unable to resolve host"
}

foreach ($key in $patterns.Keys) {
    $hits = @($log | Select-String -Pattern $patterns[$key])
    $mark = if ($hits.Count -gt 0) { "FOUND($($hits.Count))" } else { "-" }
    Write-Host ("  {0,-22} {1}" -f $key, $mark)
    if ($hits.Count -gt 0 -and $hits.Count -le 5) {
        $hits | ForEach-Object { Write-Host "      $($_.Line.Trim())" -ForegroundColor DarkGray }
    }
}

# ── 6. 결론 ────────────────────────────────────────────────────────
Write-Host ""
$initOk = @($log | Select-String -Pattern "MobileAds initialized|Google Mobile Ads SDK was initialized").Count -gt 0
$failLines = @($log | Select-String -Pattern "onAdFailedToLoad|Failed to load ad|ad failed to load|no fill|returned no ads")

Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray
if (-not $initOk) {
    Write-Host "진단: SDK가 초기화되지 않았습니다." -ForegroundColor Yellow
    Write-Host "  → 네트워크 차단 또는 AD_ID 권한 문제. 코드/설정을 점검해야 합니다." -ForegroundColor Yellow
}
elseif ($failLines.Count -gt 0) {
    Write-Host "진단: SDK는 정상 동작하나 AdMob 서버가 광고를 주지 않았습니다." -ForegroundColor Yellow
    Write-Host "  → 요청은 서버에 도달했습니다. 채움(fill) 단계에서 걸린 것입니다." -ForegroundColor Yellow
    Write-Host "  → 원인은 AdMob 계정/앱 준비 상태입니다. 스토어 연결 없이는 해결되지 않습니다." -ForegroundColor Yellow
}
else {
    Write-Host "진단: 광고 관련 로그가 없습니다." -ForegroundColor Yellow
    Write-Host "  → 앱을 실행한 뒤 다시 시도하거나, 설치된 패키지명이 다른지 확인하세요." -ForegroundColor Yellow
}
Write-Host "─────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "전체 로그: $env:TEMP\admob_log.txt"