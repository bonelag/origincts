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
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:lintRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    $apk = 'app/build/outputs/apk/release/app-release.apk'
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') tools/VerifyApk.java $apk
    if ($LASTEXITCODE -ne 0) { throw 'Native APK packaging verification failed' }
    & (Join-Path $SdkRoot 'build-tools/36.0.0/apksigner.bat') verify $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
    Write-Host "Release APK: $(Resolve-Path $apk)"
} finally { Pop-Location }