<#
.SYNOPSIS
    ChargeSound 릴리스 APK 빌드 + 검증 스크립트.

.DESCRIPTION
    스토어 제출 가능한 릴리스 APK 를 만들고, 서명 지문이 OneStore 게시본과
    같은지 확인한 뒤 app\build\ 가 청소되어도 살아남는 dist\ 로 복사합니다.

    app\build\outputs\ 는 Gradle 이 package 단계마다 정리합니다.
    2026-10-17 에 assembleRelease 실패(서명 비밀번호 누락)로 진행 중이던
    출력 APK 가 삭제된 일이 있습니다. 반드시 dist\ 로 빼야 합니다.

.PARAMETER VersionCode
    스토어 제출마다 반드시 증가시켜야 합니다. OneStore / Play 는 이전 값 이하를 거부합니다.

.PARAMETER VersionName
    사용자 노출 버전 문자열.

.EXAMPLE
    $env:STORE_PASSWORD = "..."
    $env:KEY_PASSWORD   = "..."
    .\build-release.ps1 -VersionCode 4 -VersionName 1.3.0
#>
[CmdletBinding()]
param(
    [int]$VersionCode = 3,
    [string]$VersionName = "1.2.0"
)

$ErrorActionPreference = "Stop"

# OneStore 에 게시된 앱의 서명 지문.
# 이 값이 다르면 업데이트가 불가능해집니다(서명 불일치로 설치 거부됨).
$ExpectedFingerprint = "944FFE340E5C90AFC772F11179AC2E7D082673AE6EE9149BF14453D4A6C26A78"

$Root = $PSScriptRoot
$Keystore = Join-Path $Root "my-upload-key.jks"

# ── 사전 점검 ──────────────────────────────────────────────────────
foreach ($pair in @(
    @{ Name = "STORE_PASSWORD"; Value = $env:STORE_PASSWORD },
    @{ Name = "KEY_PASSWORD";   Value = $env:KEY_PASSWORD }
)) {
    if ([string]::IsNullOrWhiteSpace($pair.Value)) {
        throw "$($pair.Name) 환경 변수가 설정되지 않았습니다. 비밀번호를 env 로 전달하세요."
    }
}
if (-not (Test-Path $Keystore)) { throw "서명 키를 찾을 수 없습니다: $Keystore" }

# JDK 26 의 jlink 는 Android 플랫폼 jar 의 빌드 서명을 거부해 빌드를 깨뜨립니다.
# JDK 17~25 전체 JDK 가 필요합니다 (JBR 불가).
$Jdk = Join-Path $env:USERPROFILE ".gradle\jdks\eclipse_adoptium-25-amd64-windows.2"
if (Test-Path $Jdk) { $env:JAVA_HOME = $Jdk }
elseif ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    throw "JAVA_HOME 이 설정되지 않았습니다. JDK 17~25 를 지정하세요 (JDK 26 은 jlink 오류로 실패)."
}

$env:KEYSTORE_PATH = $Keystore
if ([string]::IsNullOrWhiteSpace($env:KEY_ALIAS)) { $env:KEY_ALIAS = "upload" }

# ── 빌드 ──────────────────────────────────────────────────────────
Push-Location $Root
try {
    Write-Host "▶ 빌드 중... versionCode=$VersionCode versionName=$VersionName" -ForegroundColor Cyan
    # 인자를 따옴표로 감싸야 합니다 — 감싸지 않으면 .bat 이 1.2.0 을 .2.0 으로 쪼갭니다.
    & ".\gradlew.bat" ":app:assembleRelease" `
        "-PVERSION_CODE=$VersionCode" "-PVERSION_NAME=$VersionName" `
        --console=plain --no-configuration-cache
    if ($LASTEXITCODE -ne 0) { throw "Gradle 빌드 실패 (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

# ── 산출물 보존 + 검증 ────────────────────────────────────────────
$built = Join-Path $Root "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $built)) { throw "빌드는 성공했지만 APK 가 없습니다: $built" }

$dist = Join-Path $Root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$dest = Join-Path $dist "ChargeSound-$VersionName-vc$VersionCode.apk"
Copy-Item $built $dest -Force

$bt = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\36.0.0"

Write-Host ""
Write-Host "▶ 검증 중..." -ForegroundColor Cyan

# apksigner / aapt2 / zipalign 은 정상 동작에서도 stderr 에 Java 경고를 출력합니다.
# $ErrorActionPreference = "Stop" 상태에서는 stderr 출력이 terminating error 로 오인되므로,
# 네이티브 도구 호출 동안만 완화하고 종료 코드로 판정합니다.
function Invoke-Native {
    param([string]$Exe, [string[]]$Arguments)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $out = & $Exe @Arguments 2>&1
        return [pscustomobject]@{ Output = $out; Code = $LASTEXITCODE }
    } finally { $ErrorActionPreference = $prev }
}

$apksigner = Join-Path $bt "apksigner.bat"
$aapt2     = Join-Path $bt "aapt2.exe"
$zipalign  = Join-Path $bt "zipalign.exe"

$cert = (Invoke-Native $apksigner @("verify", "--print-certs", $dest)).Output
$fingerprint = ($cert | Select-String -Pattern "SHA-256 digest:\s*(\S+)" | Select-Object -First 1).Matches.Groups[1].Value.ToUpper()
$dn = ($cert | Select-String -Pattern "certificate DN:\s*(.+)" | Select-Object -First 1).Matches.Groups[1].Value.Trim()

$badging = (Invoke-Native $aapt2 @("dump", "badging", $dest)).Output
$pkg = ($badging | Select-String -Pattern "^package:\s*name='([^']+)'").Matches.Groups[1].Value
$vc  = ($badging | Select-String -Pattern "versionCode='(\d+)'").Matches.Groups[1].Value
$vn  = ($badging | Select-String -Pattern "versionName='([^']+)'").Matches.Groups[1].Value
$abis = (($badging | Select-String -Pattern "^native-code:").Line -replace "^native-code:\s*","")

Write-Host "  패키지      : $pkg"
Write-Host "  버전        : $vn (versionCode $vc)"
Write-Host "  서명자      : $dn"
Write-Host "  SHA-256     : $fingerprint"
Write-Host "  네이티브    : $abis"
Write-Host ("  크기        : {0:N2} MB" -f ((Get-Item $dest).Length / 1MB))

if ($pkg -ne "com.chargesound.app") {
    throw "패키지명이 다릅니다: '$pkg' — OneStore 게시본은 'com.chargesound.app' 입니다. 다른 앱으로 덮어쓰면 안 됩니다."
}

# 서명 불일치 = OneStore / Galaxy Store 업데이트가 영구 불가
if ($fingerprint -ne $ExpectedFingerprint) {
    Write-Host ""
    Write-Host "✗ 서명 지문 불일치!" -ForegroundColor Red
    Write-Host "  기대: $ExpectedFingerprint" -ForegroundColor Red
    Write-Host "  실제: $fingerprint" -ForegroundColor Red
    throw "다른 키로 빌드되었습니다. 게시된 앱은 업데이트할 수 없게 됩니다."
}
Write-Host "  서명        : OneStore 게시본과 일치 ✓" -ForegroundColor Green

# 16KB 페이지 크기 (Galaxy Store 는 2026-07-01 부터 필수)
$align = Invoke-Native $zipalign @("-c", "-P", "16", "4", $dest)
if ($align.Code -eq 0) { Write-Host "  16KB 정렬   : 통과 ✓" -ForegroundColor Green }
else { Write-Host "  16KB 정렬   : 실패 — Galaxy Store 제출이 거부될 수 있습니다" -ForegroundColor Yellow }

if ($vc -ne "$VersionCode") { Write-Host "  ⚠ versionCode 불일치" -ForegroundColor Yellow }

Write-Host ""
Write-Host "✓ 완료: $dest" -ForegroundColor Green