$ErrorActionPreference = 'Stop'

$SDK      = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$BT       = Join-Path $SDK 'build-tools\36.0.0'
$PLATFORM = Join-Path $SDK 'platforms\android-37.0\android.jar'
$JBR      = 'C:\Program Files\Android\Android Studio\jbr\bin'
$PROJ     = $PSScriptRoot
$GEN      = Join-Path $PROJ 'gen'
$OBJ      = Join-Path $PROJ 'obj'
$BUILD    = Join-Path $PROJ 'build'
$OUT      = Join-Path $PROJ 'EchoShowLauncher.apk'

New-Item -ItemType Directory -Force -Path $GEN, $OBJ, $BUILD | Out-Null
Remove-Item (Join-Path $BUILD '*') -Recurse -Force -ErrorAction SilentlyContinue

Write-Host '[1/7] Compiling resources...'
& (Join-Path $BT 'aapt2.exe') compile --dir (Join-Path $PROJ 'res') -o (Join-Path $BUILD 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

Write-Host '[2/7] Linking manifest + resources...'
& (Join-Path $BT 'aapt2.exe') link `
    -o (Join-Path $BUILD 'base.apk') `
    -I $PLATFORM `
    --manifest (Join-Path $PROJ 'AndroidManifest.xml') `
    --java $GEN `
    --min-sdk-version 21 `
    --target-sdk-version 30 `
    (Join-Path $BUILD 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

Write-Host '[3/7] Compiling Java sources...'
$srcs = @(Get-ChildItem (Join-Path $PROJ 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$rsrcs = @(Get-ChildItem $GEN -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& (Join-Path $JBR 'javac.exe') --release 8 -encoding UTF-8 -Xlint:-options `
    -cp $PLATFORM -d $OBJ ($srcs + $rsrcs)
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '[4/7] Converting classes to DEX...'
$classes = @(Get-ChildItem $OBJ -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& (Join-Path $BT 'd8.bat') --lib $PLATFORM --min-api 21 --output $BUILD $classes
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '[5/7] Packaging DEX into APK...'
python (Join-Path $PROJ 'add_dex.py') (Join-Path $BUILD 'base.apk') (Join-Path $BUILD 'classes.dex')
if ($LASTEXITCODE -ne 0) { throw 'dex packaging failed' }

Write-Host '[6/7] Aligning APK...'
& (Join-Path $BT 'zipalign.exe') -f -p 4 (Join-Path $BUILD 'base.apk') (Join-Path $BUILD 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host '[7/7] Signing APK...'
$ks = Join-Path $PROJ 'debug.jks'
if (-not (Test-Path $ks)) {
    & (Join-Path $JBR 'keytool.exe') -genkeypair -keystore $ks -alias echo `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass android -keypass android `
        -dname 'CN=Echo Show Launcher, OU=Dev, O=Dev, L=NA, ST=NA, C=US' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}
& (Join-Path $BT 'apksigner.bat') sign `
    --ks $ks --ks-pass pass:android --key-pass pass:android `
    --out $OUT (Join-Path $BUILD 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

Write-Host ""
Write-Host "Built: $OUT"
Get-Item $OUT | Select-Object FullName, Length
