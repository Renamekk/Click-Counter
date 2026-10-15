param([switch]$Offline)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Keep SDK discovery and signing local to this checkout; no system-wide changes.
if (-not $env:JAVA_HOME) {
    $javaCommand = Get-Command java.exe -ErrorAction Stop
    $javaSettings = (& $javaCommand.Source -XshowSettings:properties -version 2>&1 | Out-String)
    if ($javaSettings -match 'java\.home\s*=\s*([^\r\n]+)') {
        $env:JAVA_HOME = $Matches[1].Trim()
    }
}
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $keytool)) {
    throw 'A JDK with keytool is required. Set JAVA_HOME to its installation directory.'
}

$sdkDirectory = $env:ANDROID_HOME
if (-not $sdkDirectory) { $sdkDirectory = $env:ANDROID_SDK_ROOT }
if (-not $sdkDirectory) { $sdkDirectory = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path -LiteralPath (Join-Path $sdkDirectory 'platforms\android-36\android.jar'))) {
    throw 'Android SDK Platform 36 is required. Set ANDROID_HOME to the SDK directory.'
}
if (-not (Test-Path -LiteralPath 'local.properties')) {
    Set-Content -LiteralPath 'local.properties' -Encoding ASCII `
        -Value ('sdk.dir=' + $sdkDirectory.Replace('\', '/').Replace(':', '\:'))
}

if (-not (Test-Path -LiteralPath 'signing.properties')) {
    $keystorePath = Join-Path $PSScriptRoot '.signing\click-counter-release.keystore'
    if (Test-Path -LiteralPath $keystorePath) {
        throw 'A signing key already exists. Restore signing.properties instead of replacing the key.'
    }
    New-Item -ItemType Directory -Path '.signing' -Force | Out-Null
    $randomBytes = New-Object byte[] 32
    $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
    $randomGenerator.GetBytes($randomBytes)
    $randomGenerator.Dispose()
    $password = -join ($randomBytes | ForEach-Object { $_.ToString('x2') })
    $env:CLICK_COUNTER_KEY_PASSWORD = $password
    try {
        & $keytool -genkeypair -keystore $keystorePath -storetype PKCS12 `
            -storepass:env CLICK_COUNTER_KEY_PASSWORD -keypass:env CLICK_COUNTER_KEY_PASSWORD `
            -alias click-counter -keyalg RSA -keysize 2048 -validity 10950 `
            -dname 'CN=Click Counter, OU=Local App, O=Click Counter, C=DE' -noprompt
        if ($LASTEXITCODE -ne 0) { throw 'Signing-key generation failed.' }
        @(
            'storeFile=.signing/click-counter-release.keystore'
            "storePassword=$password"
            'keyAlias=click-counter'
            "keyPassword=$password"
        ) | Set-Content -LiteralPath 'signing.properties' -Encoding ASCII
    }
    finally {
        Remove-Item -LiteralPath 'Env:\CLICK_COUNTER_KEY_PASSWORD' -ErrorAction SilentlyContinue
    }
}

$gradleArguments = @('assembleRelease', 'lintRelease', '--no-daemon', '--console', 'plain')
if ($Offline) { $gradleArguments += '--offline' }
& (Join-Path $PSScriptRoot 'gradlew.bat') @gradleArguments
if ($LASTEXITCODE -ne 0) { throw 'Android build or lint failed.' }

$apkSource = Join-Path $PSScriptRoot 'app\build\outputs\apk\release\app-release.apk'
$apkSigner = Join-Path $sdkDirectory 'build-tools\36.0.0\apksigner.bat'
& $apkSigner verify --verbose $apkSource
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }

New-Item -ItemType Directory -Path 'artifacts' -Force | Out-Null
$apkOutput = Join-Path $PSScriptRoot 'artifacts\Click-Counter-1.0.0.apk'
Copy-Item -LiteralPath $apkSource -Destination $apkOutput -Force
$checksum = (Get-FileHash -LiteralPath $apkOutput -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath ($apkOutput + '.sha256') -Encoding ASCII `
    -Value ($checksum + '  ' + [IO.Path]::GetFileName($apkOutput))
Write-Output "APK: $apkOutput"
Write-Output "SHA256: $checksum"
