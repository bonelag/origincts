param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$SdkRoot = $env:ANDROID_HOME
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (!$JavaHome -or !(Test-Path (Join-Path $JavaHome 'bin/java.exe'))) {
        throw 'Pass -JavaHome with a JDK 21 directory.'
    }
    if (!$SdkRoot -or !(Test-Path (Join-Path $SdkRoot 'platforms/android-36/android.jar'))) {
        throw 'Pass -SdkRoot with an Android SDK containing platform 36.'
    }
    $env:JAVA_HOME = (Resolve-Path $JavaHome).Path
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-cache'
    $sdkPath = (Resolve-Path $SdkRoot).Path.Replace('\','/').Replace(':','\:')
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'local.properties'), "sdk.dir=$sdkPath" + [Environment]::NewLine)
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') tools/VerifyApk.java app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'Native APK packaging verification failed' }
    New-Item -ItemType Directory -Force dist | Out-Null
    $metadata = Get-Content 'app/build/outputs/apk/debug/output-metadata.json' -Raw | ConvertFrom-Json
    $apkName = "OriginCTS-$($metadata.elements[0].versionName)-universal-debug.apk"
    Copy-Item app/build/outputs/apk/debug/app-debug.apk (Join-Path dist $apkName)
    $hash = (Get-FileHash (Join-Path dist $apkName) -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksums = Join-Path $PSScriptRoot 'dist/SHA256SUMS.txt'
    $previous = if (Test-Path $checksums) { @(Get-Content $checksums | Where-Object { $_ -notmatch "\s+$([regex]::Escape($apkName))$" }) } else { @() }
    [IO.File]::WriteAllLines($checksums, @($previous) + "$hash  $apkName")
} finally { Pop-Location }
