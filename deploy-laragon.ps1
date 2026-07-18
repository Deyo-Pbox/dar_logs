# deploy-laragon.ps1 - DAR Logs full deployment to Laragon
# Usage: .\deploy-laragon.ps1

$ErrorActionPreference = "Stop"

$PROJECT   = "backend"
$LARAGON   = "C:\laragon\www"
$ROOT      = $PSScriptRoot
$SOURCE    = "$ROOT\backend"
$DEST      = "$LARAGON\$PROJECT"
$SQL_FILE  = "$SOURCE\database.sql"
$DB_NAME   = "dar_logs"

function step($m)    { Write-Host "`n>> $m" -ForegroundColor Cyan }
function ok($m)      { Write-Host "   [OK] $m" -ForegroundColor Green }
function warn($m)    { Write-Host "   [!!] $m" -ForegroundColor Yellow }
function info($m)    { Write-Host "   $m" -ForegroundColor Gray }
function heading($m) { Write-Host "   $m" -ForegroundColor White }

Write-Host ""
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host "  DAR Activity Logs - Laragon Deployment" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host ""

$deployStart = Get-Date

# ---------------------------------------------------------------
# CHANGE TRACKING
# ---------------------------------------------------------------
$DEPLOY_HEAD = "$ROOT\.deploy-head"
$CURRENT_HEAD = git -C $ROOT rev-parse HEAD 2>$null
$LAST_DEPLOYED_HEAD = if (Test-Path $DEPLOY_HEAD) { Get-Content $DEPLOY_HEAD -Raw | ForEach-Object { $_.Trim() } } else { "" }

if ($LAST_DEPLOYED_HEAD -and $CURRENT_HEAD -eq $LAST_DEPLOYED_HEAD) {
    ok "No changes since last deployment (HEAD: $($CURRENT_HEAD.Substring(0,7))) — deploy not needed."
    pause; exit 0
}

# ---------------------------------------------------------------
# 1. PREREQUISITES
# ---------------------------------------------------------------
step "[1/7] Checking prerequisites"

if (-not (Test-Path $LARAGON)) {
    Write-Host "   [ERROR] Laragon not found at $LARAGON" -ForegroundColor Red
    Write-Host "   Install from https://laragon.org/download/" -ForegroundColor Yellow
    pause; exit 1
}
ok "Laragon: $LARAGON"

$php = Get-Command php -ErrorAction SilentlyContinue
if ($php) { ok "PHP: $($php.Source)" } else { warn "PHP not in PATH" }

$composer = Get-Command composer -ErrorAction SilentlyContinue
if ($composer) { ok "Composer: $($composer.Source)" } else { warn "Composer not in PATH" }

$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) { ok "Node: v$(node -v)" } else { warn "Node not in PATH" }

$mysql = Get-ChildItem "C:\laragon\bin\mysql" -Recurse -Filter "mysql.exe" -ErrorAction SilentlyContinue |
         Select-Object -First 1 -ExpandProperty FullName
if ($mysql) { ok "MySQL: $mysql" } else { warn "MySQL not found - DB import will be skipped" }

# ---------------------------------------------------------------
# 2. BUILD FRONTEND
# ---------------------------------------------------------------
step "[2/7] Building React frontend"
$frontendDir = "$ROOT\frontend"
if (Test-Path "$frontendDir\package.json") {
    pushd $frontendDir
    try {
        info "Installing dependencies..."
        npm install 2>&1 | Out-Null
        info "Running TypeScript check + Vite build..."
        $buildResult = cmd /c "npm run build 2>&1"
        $totalSize = 0
        $buildResult -split '\n' | ForEach-Object {
            $line = $_.Trim()
            if ($line -match '\.(js|css|html)\s+([\d.]+)\s*kB') {
                $size = [double]$Matches[2]
                $totalSize += $size
                $file = ($line -split '\s+')[0] -replace '.*\\',''
                info "  $file ($size kB)"
            }
        }
        $count = @(Get-ChildItem "$SOURCE\public\assets" -File -ErrorAction SilentlyContinue).Count
        ok "Frontend built - $count assets, $([math]::Round($totalSize,1)) kB total -> backend/public/"
    }
    catch {
        warn "Frontend build failed - try 'npm run build' in frontend/ manually"
    }
    finally {
        popd
    }
}
else {
    warn "frontend/ not found - skipping"
}

# ---------------------------------------------------------------
# 3. PHP DEPENDENCIES
# ---------------------------------------------------------------
step "[3/7] Installing PHP dependencies"
if ($composer) {
    pushd $SOURCE
    try {
        $prevEAP = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        composer install --no-dev --optimize-autoloader 2>&1 | ForEach-Object { info $_ }
        $ErrorActionPreference = $prevEAP
        ok "Composer dependencies ready"
    }
    catch {
        warn "Composer had errors - try running composer install manually"
    }
    finally {
        popd
    }
}
else {
    warn "Composer not found - run 'composer install' in backend/ manually"
}

# ---------------------------------------------------------------
# 4. CONFIGURE .env
# ---------------------------------------------------------------
step "[4/7] Configuring environment"
$envFile = "$SOURCE\.env"

if (-not (Test-Path $envFile)) {
    Copy-Item "$SOURCE\.env.example" $envFile
    info "Created .env from .env.example"
}

$envContent = Get-Content $envFile -Raw
if ($envContent -match 'sql.*\.epizy\.com' -or $envContent -match 'epiz_') {
    warn "Detected production InfinityFree DB values in .env"
    $fix = Read-Host "   Override with local dev values? (Y/n)"
    if ($fix -ne 'n') {
        $localEnv = @'
DB_HOST=localhost
DB_PORT=3306
DB_NAME=dar_logs
DB_USER=root
DB_PASS=
DB_CHARSET=utf8mb4

APP_ENV=development
APP_DEBUG=true

JWT_SECRET=dar_logs_jwt_secret_key_change_in_production_2026
JWT_EXPIRY=86400

AUDIT_LOG_CAPACITY=100
NOTIFICATION_CAP=50

CACHE_ENABLED=true
CACHE_TTL_STATS=30
CACHE_TTL_USERS=120
'@
        [IO.File]::WriteAllText($envFile, $localEnv)
        ok ".env set to local dev values"
    }
    else {
        warn "Keeping current .env (may not connect to local MySQL)"
    }
}
else {
    ok ".env looks like local dev"
}

# ---------------------------------------------------------------
# 5. DEPLOY TO LARAGON
# ---------------------------------------------------------------
step "[5/7] Deploying to $DEST"

# Clean destination
if (Test-Path $DEST) {
    info "Cleaning previous deployment..."
    Get-ChildItem $DEST -Directory | ForEach-Object {
        if ($_.Name -ne 'storage') {
            Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    Get-ChildItem $DEST -File | ForEach-Object {
        Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Test-Path $DEST)) {
    New-Item -ItemType Directory -Path $DEST -Force | Out-Null
}

@('storage\logs', 'storage\cache') | ForEach-Object {
    $p = "$DEST\$_"
    if (-not (Test-Path $p)) { New-Item -ItemType Directory -Path $p -Force | Out-Null }
}

# Sync with robocopy
$excludeDirs  = '.git','.idea','.vscode','.venv','node_modules','.commandcode','storage\cache'
$excludeFiles = '*.ps1','.deploy'
info "Syncing backend/ files..."
$syncResult = & robocopy $SOURCE $DEST /E /NFL /NDL /XD $excludeDirs /XF $excludeFiles 2>&1

if ($LASTEXITCODE -ge 8) {
    Write-Host "   [ERROR] File sync failed (exit code $LASTEXITCODE)" -ForegroundColor Red
    pause; exit 1
}

$fileMatch = ($syncResult | Select-String 'Files\s*:\s+(\d+)').Matches
$fileCount = if ($fileMatch) { $fileMatch.Groups[1].Value } else { '?' }
$dirMatch  = ($syncResult | Select-String 'Dirs\s*:\s+(\d+)').Matches
$dirCount  = if ($dirMatch) { $dirMatch.Groups[1].Value } else { '?' }

ok "Deployed - $fileCount files, $dirCount directories"

# ---------------------------------------------------------------
# 6. DATABASE
# ---------------------------------------------------------------
step "[6/7] Database setup"
$import = Read-Host "   Import database.sql into '$DB_NAME'? (Y/n)"
if ($import -ne 'n') {
    if ($mysql) {
        info "Creating database '$DB_NAME' if not exists..."
        "CREATE DATABASE IF NOT EXISTS ``$DB_NAME`` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" |
            & $mysql -u root 2>&1 | ForEach-Object { info $_ }

        info "Importing database.sql..."
        $prevEAP = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        Get-Content $SQL_FILE | & $mysql -f -u root $DB_NAME 2>&1 | Out-Null
        $ErrorActionPreference = $prevEAP
        ok "Database '$DB_NAME' ready"
    }
    else {
        warn "MySQL not found. Import manually via http://localhost/phpmyadmin:"
        info "  1. Create database '$DB_NAME' (utf8mb4_general_ci)"
        info "  2. Select the database, Import, Choose database.sql, Go"
    }
}
else {
    warn "Skipped. Import manually if this is a fresh install."
}

# ---------------------------------------------------------------
# 7. VERIFY
# ---------------------------------------------------------------
step "[7/7] Verifying deployment"

# PHP syntax check
if ($php) {
    $broken = @()
    Get-ChildItem $DEST -Recurse -Filter "*.php" | Where-Object { $_.FullName -notmatch '\\vendor\\' } | ForEach-Object {
        $result = & php -l $_.FullName 2>&1
        if ($result -notmatch 'No syntax errors') { $broken += $_.Name }
    }
    if ($broken.Count -eq 0) {
        ok "PHP syntax: all clean"
    }
    else {
        warn "PHP syntax errors in: $($broken -join ', ')"
    }
}

# Critical files present
$checkFiles = @(
    "$DEST\bootstrap.php",
    "$DEST\public\index.php",
    "$DEST\public\.htaccess",
    "$DEST\public\index.html",
    "$DEST\vendor\autoload.php",
    "$DEST\.env"
)
$missing = @()
foreach ($f in $checkFiles) {
    if (-not (Test-Path $f)) { $missing += Split-Path $f -Leaf }
}
if ($missing.Count -eq 0) {
    ok "Critical files: all present"
}
else {
    warn "Missing: $($missing -join ', ')"
}

# Assets
$assetDir = "$DEST\public\assets"
if (Test-Path $assetDir) {
    $assetCount = @(Get-ChildItem $assetDir -File).Count
    ok "SPA assets: $assetCount files"
}
else {
    warn "No SPA assets - run 'npm run build' in frontend/"
}

# Document root
info "Document root must be: $DEST\public\"

# ---------------------------------------------------------------
# SAVE DEPLOY HEAD
# ---------------------------------------------------------------
if ($CURRENT_HEAD) {
    [IO.File]::WriteAllText($DEPLOY_HEAD, $CURRENT_HEAD)
    info "Saved deploy marker: $($CURRENT_HEAD.Substring(0,7))"
}

# ---------------------------------------------------------------
# DONE
# ---------------------------------------------------------------
$elapsed = [math]::Round(((Get-Date) - $deployStart).TotalSeconds, 1)

Write-Host ""
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host "  DEPLOYMENT COMPLETE  (${elapsed}s)" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host ""

heading "NEXT STEPS:"
Write-Host ""
Write-Host "  [1] Set Laragon document root:" -ForegroundColor Yellow
Write-Host "      Laragon -> Menu -> Preferences -> General"
Write-Host "      Document Root: C:\laragon\www\backend\public\"
Write-Host "      Then click 'Restart'"
Write-Host ""
Write-Host "  [2] Open in browser:" -ForegroundColor Yellow
Write-Host "      http://localhost/"
Write-Host ""
Write-Host "  [3] Log in and change password:" -ForegroundColor Yellow
Write-Host "      Username: admin" -ForegroundColor White
Write-Host "      Password: password" -ForegroundColor White
Write-Host "      -> Manage Accounts -> click admin -> set strong password"
Write-Host ""

Write-Host "  Reset admin password (MySQL CLI):" -ForegroundColor Gray
Write-Host "    UPDATE users SET password = '$2y`$10`$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' WHERE username = 'admin';" -ForegroundColor Gray
Write-Host ""

pause
