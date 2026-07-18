# deploy-infinityfree.ps1 - DAR Logs deployment to InfinityFree via FTP
# Usage: .\deploy-infinityfree.ps1

$ErrorActionPreference = "Stop"

$ROOT      = $PSScriptRoot
$SOURCE    = "$ROOT\backend"
$CRED_FILE = "$ROOT\.deploy"

function step($m)    { Write-Host "`n>> $m" -ForegroundColor Cyan }
function ok($m)      { Write-Host "   [OK] $m" -ForegroundColor Green }
function warn($m)    { Write-Host "   [!!] $m" -ForegroundColor Yellow }
function info($m)    { Write-Host "   $m" -ForegroundColor Gray }
function heading($m) { Write-Host "   $m" -ForegroundColor White }

# ---------------------------------------------------------------
# LOAD OR PROMPT FOR CREDENTIALS
# ---------------------------------------------------------------
$FTP_HOST = $FTP_USER = $FTP_PASS = $REMOTE_DIR = ""
$DB_HOST = $DB_NAME = $DB_USER = $DB_PASS = ""

if (Test-Path $CRED_FILE) {
    Get-Content $CRED_FILE | ForEach-Object {
        if ($_ -match '^\s*([A-Z_]+)\s*=\s*(.+)$') {
            Set-Variable -Name $Matches[1] -Value $Matches[2].Trim() -Scope Script
        }
    }
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host "  DAR Activity Logs - InfinityFree Deployment       " -ForegroundColor White
Write-Host "==================================================" -ForegroundColor DarkCyan

if (-not $FTP_HOST) { $FTP_HOST = Read-Host "FTP host (e.g. ftpupload.net)" }
if (-not $FTP_USER) { $FTP_USER = Read-Host "FTP username (from panel -> FTP Details)" }
if (-not $FTP_PASS) {
    $ss = Read-Host "FTP password" -AsSecureString
    $FTP_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($ss))
}
if (-not $REMOTE_DIR) { $REMOTE_DIR = "/htdocs" }
if (-not $DB_HOST)    { $DB_HOST = Read-Host "MySQL host (from panel -> Databases, e.g. sql123.epizy.com)" }
if (-not $DB_NAME)    { $DB_NAME = Read-Host "MySQL database name (e.g. epiz_XXXXX_dar_logs)" }
if (-not $DB_USER)    { $DB_USER = Read-Host "MySQL username (e.g. epiz_XXXXX)" }
if (-not $DB_PASS)    {
    $ss2 = Read-Host "MySQL password" -AsSecureString
    $DB_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($ss2))
}

$CRED = New-Object System.Net.NetworkCredential($FTP_USER, $FTP_PASS)
$FTP_BASE = "ftp://${FTP_HOST}:21"

# ---------------------------------------------------------------
# CHANGE TRACKING
# ---------------------------------------------------------------
$DEPLOY_HEAD = "$ROOT\.deploy-head"
$CURRENT_HEAD = git -C $ROOT rev-parse HEAD 2>$null
$LAST_DEPLOYED_HEAD = if (Test-Path $DEPLOY_HEAD) { Get-Content $DEPLOY_HEAD -Raw | ForEach-Object { $_.Trim() } } else { "" }

$IS_FIRST_DEPLOY = (-not $LAST_DEPLOYED_HEAD)

if ($IS_FIRST_DEPLOY) {
    warn "First deployment detected — uploading all backend files"
    $CHANGED_FILES = @("__FULL_DEPLOY__")
} elseif ($CURRENT_HEAD -eq $LAST_DEPLOYED_HEAD) {
    warn "No changes since last deployment (HEAD: $($CURRENT_HEAD.Substring(0,7)))"
    $skip = Read-Host "   Force full deploy anyway? (y/N)"
    if ($skip -eq 'y') {
        $CHANGED_FILES = @("__FULL_DEPLOY__")
    } else {
        ok "Nothing to deploy."
        pause; exit 0
    }
} else {
    $diffOutput = git -C $ROOT diff --name-status $LAST_DEPLOYED_HEAD HEAD -- backend/ 2>&1
    $CHANGED_FILES = @($diffOutput | Where-Object { $_ -ne '' })
    $changedCount = $CHANGED_FILES.Count
    if ($changedCount -eq 0) {
        warn "No backend changes detected. Only frontend will be uploaded."
    } else {
        ok "Backend files changed since last deploy ($($LAST_DEPLOYED_HEAD.Substring(0,7)) -> $($CURRENT_HEAD.Substring(0,7))):"
        $CHANGED_FILES | ForEach-Object { info "  $_" }
    }
}

# Offer to save credentials
if (-not (Test-Path $CRED_FILE)) {
    $save = Read-Host "`n   Save credentials to .deploy file for future deploys? (Y/n)"
    if ($save -ne 'n') {
        $content = @"
FTP_HOST=$FTP_HOST
FTP_USER=$FTP_USER
FTP_PASS=$FTP_PASS
REMOTE_DIR=$REMOTE_DIR
DB_HOST=$DB_HOST
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASS=$DB_PASS
"@
        [IO.File]::WriteAllText($CRED_FILE, $content)
        ok "Saved to .deploy (this file is gitignored - never committed)"
    }
}

# ---------------------------------------------------------------
# FTP HELPERS
# ---------------------------------------------------------------
function FtpMkdir($path) {
    try {
        $r = [Net.FtpWebRequest]::Create("${FTP_BASE}${path}")
        $r.Method = [Net.WebRequestMethods+Ftp]::MakeDirectory
        $r.Credentials = $CRED; $r.Timeout = 5000
        $r.GetResponse().Close()
    } catch {}
}
function FtpUpload($localFile, $remotePath) {
    try {
        $fileName = (Split-Path $localFile -Leaf)
        $totalBytes = (Get-Item $localFile).Length
        $r = [Net.FtpWebRequest]::Create("${FTP_BASE}${remotePath}")
        $r.Method = [Net.WebRequestMethods+Ftp]::UploadFile
        $r.Credentials = $CRED; $r.Timeout = 300000
        $r.UseBinary = $true; $r.KeepAlive = $false
        $r.ContentLength = $totalBytes
        $s = $r.GetRequestStream()
        $fs = [IO.File]::OpenRead($localFile)
        $buffer = New-Object byte[] 8192
        $uploaded = 0L; $startTime = Get-Date
        while (($read = $fs.Read($buffer, 0, $buffer.Length)) -gt 0) {
            $s.Write($buffer, 0, $read)
            $uploaded += $read
            $elapsed = ((Get-Date) - $startTime).TotalSeconds
            if ($elapsed -gt 0) {
                $pct = [math]::Min(100, [math]::Round(($uploaded / $totalBytes) * 100, 1))
                $speedKB = [math]::Round(($uploaded / $elapsed) / 1024, 1)
                $line = "   $fileName  $pct%  ${speedKB} KB/s"
                Write-Host "`r$line" -NoNewline -ForegroundColor Gray
                [Console]::Write($(' ' * [Math]::Max(0, 60 - $line.Length)))
            }
        }
        $fs.Close(); $fs.Dispose()
        $s.Close(); $s.Dispose()
        $r.GetResponse().Close()
        Write-Host "`r   $fileName  100%  [OK]                                     " -ForegroundColor Green
        return $true
    } catch { Write-Host "`r   $fileName  FAIL: $_" -ForegroundColor Red; return $false }
}
function FtpDelete($path) {
    try {
        $r = [Net.FtpWebRequest]::Create("${FTP_BASE}${path}")
        $r.Method = [Net.WebRequestMethods+Ftp]::DeleteFile
        $r.Credentials = $CRED; $r.Timeout = 5000
        $r.GetResponse().Close()
    } catch {}
}
function FtpListDir($path) {
    try {
        $r = [Net.FtpWebRequest]::Create("${FTP_BASE}${path}")
        $r.Method = [Net.WebRequestMethods+Ftp]::ListDirectoryDetails
        $r.Credentials = $CRED; $r.Timeout = 10000
        $sr = New-Object IO.StreamReader($r.GetResponse().GetResponseStream())
        $raw = $sr.ReadToEnd(); $sr.Close(); $r.GetResponse().Close()
        return ($raw.Trim() -split '\r?\n') | Where-Object { $_ -ne '' }
    } catch { return @() }
}
function FtpCleanDir($path) {
    Write-Host "   Cleaning $path..."
    $items = FtpListDir $path
    foreach ($line in $items) {
        if ($line -match '^d') {
            $name = ($line -split '\s+')[-1]
            if ($name -eq '.' -or $name -eq '..') { continue }
            FtpCleanDir "$path/$name"
            try {
                $r = [Net.FtpWebRequest]::Create("${FTP_BASE}$path/$name")
                $r.Method = [Net.WebRequestMethods+Ftp]::RemoveDirectory
                $r.Credentials = $CRED; $r.Timeout = 5000
                $r.GetResponse().Close()
            } catch {}
        } else {
            $name = ($line -split '\s+')[-1]
            FtpDelete "$path/$name"
        }
    }
}
function FtpUploadDir($localDir, $remoteDir) {
    $localDir = $localDir.TrimEnd('\')
    $remoteDir = $remoteDir.TrimEnd('/')
    FtpMkdir $remoteDir
    $files = Get-ChildItem $localDir
    $totalFiles = @($files).Count; $n = 0
    $files | ForEach-Object {
        $lp = $_.FullName; $rp = "$remoteDir/$($_.Name)" -replace '\\','/'
        if ($_.PSIsContainer) {
            FtpUploadDir $lp $rp
        } else {
            $n++
            Write-Host "   [$n/$totalFiles]" -NoNewline -ForegroundColor DarkGray
            $null = FtpUpload $lp $rp
        }
    }
}

# ---------------------------------------------------------------
# 1. PREPARE PRODUCTION .env
# ---------------------------------------------------------------
step "Preparing .env for production"

$prodEnv = @"
DB_HOST=$DB_HOST
DB_PORT=3306
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASS=$DB_PASS
DB_CHARSET=utf8mb4

APP_ENV=production
APP_DEBUG=false

JWT_SECRET=dar_logs_jwt_secret_key_change_in_production_2026
JWT_EXPIRY=86400

AUDIT_LOG_CAPACITY=100
NOTIFICATION_CAP=50

CACHE_ENABLED=true
CACHE_TTL_STATS=30
CACHE_TTL_USERS=120
"@

$prodEnvFile = "$SOURCE\.env.production"
[IO.File]::WriteAllText($prodEnvFile, $prodEnv)
ok "Created .env.production with InfinityFree DB credentials"

info "DB credentials being deployed:"
info "  Host: $DB_HOST"
info "  Name: $DB_NAME"
info "  User: $DB_USER"

# ---------------------------------------------------------------
# 2. INSTALL COMPOSER DEPENDENCIES
# ---------------------------------------------------------------
step "[2/4] Installing Composer dependencies"
$composer = Get-Command composer -ErrorAction SilentlyContinue
if ($composer) {
    pushd $SOURCE
    try {
        composer install --no-dev --optimize-autoloader 2>&1 | ForEach-Object { info $_ }
        ok "Dependencies installed"
    } catch {
        warn "Composer had errors - run 'composer install' manually"
    } finally { popd }
} else {
    warn "Composer not found - you'll need to run 'composer install' before deploying"
    info "Download from: https://getcomposer.org/download/"
}

# ---------------------------------------------------------------
# 3. ENSURE STORAGE DIRECTORIES
# ---------------------------------------------------------------
$storageDirs = @("$SOURCE\storage\logs", "$SOURCE\storage\cache")
foreach ($d in $storageDirs) {
    if (-not (Test-Path $d)) {
        New-Item -ItemType Directory -Path $d -Force | Out-Null
    }
}

# ---------------------------------------------------------------
# 4. TEST FTP CONNECTION
# ---------------------------------------------------------------
step "Testing FTP connection to $FTP_HOST"
try {
    $r = [Net.FtpWebRequest]::Create("${FTP_BASE}/")
    $r.Method = [Net.WebRequestMethods+Ftp]::ListDirectory
    $r.Credentials = $CRED; $r.Timeout = 15000
    $r.GetResponse().Close()
    ok "Connected successfully"
} catch {
    Write-Host "   [ERROR] Cannot connect: $_" -ForegroundColor Red
    pause; exit 1
}

function FtpEnsureDir($remotePath) {
    $parts = $remotePath.Trim('/').Split('/',[StringSplitOptions]::RemoveEmptyEntries)
    $current = ""
    foreach ($part in $parts[0..($parts.Count - 2)]) {
        $current += "/$part"
        FtpMkdir $current
    }
}
function FtpUploadSingle($localFile, $remotePath) {
    $dir = Split-Path $remotePath -Parent
    FtpEnsureDir $dir
    FtpUpload $localFile $remotePath
}

# ---------------------------------------------------------------
# UPLOAD LOGIC — FULL vs INCREMENTAL
# ---------------------------------------------------------------
$IS_INCREMENTAL = ($CHANGED_FILES.Count -gt 0 -and $CHANGED_FILES[0] -ne "__FULL_DEPLOY__")

if (-not $IS_INCREMENTAL) {
    # --- FULL DEPLOY ---
    step "Cleaning remote /htdocs"
    FtpCleanDir $REMOTE_DIR
    ok "Remote directory cleaned"

    step "Uploading root files"
    $rootFiles = @(
        '.htaccess', '.user.ini', 'bootstrap.php', 'composer.json', 'composer.lock',
        'README.md', 'database.sql'
    )
    $totalRoot = $rootFiles.Count; $i = 0
    foreach ($f in $rootFiles) {
        $i++
        $src = "$SOURCE\$f"
        if (Test-Path $src) {
            Write-Host "   [$i/$totalRoot]" -NoNewline -ForegroundColor DarkGray
            FtpUpload $src "$REMOTE_DIR/$f"
        }
    }

    FtpUpload $prodEnvFile "$REMOTE_DIR/.env"
    info "  UP: .env (production)"
    Remove-Item $prodEnvFile -ErrorAction SilentlyContinue
    ok "Root files uploaded"

    $dirs = 'api','config','includes','routes','src','storage','vendor'
    foreach ($d in $dirs) {
        $src = "$SOURCE\$d"
        if (Test-Path $src) {
            step "Uploading $d/"
            FtpUploadDir $src "$REMOTE_DIR/$d"
        }
    }

    step "Uploading public/ contents to /htdocs"
    if (Test-Path "$SOURCE\public") {
        FtpUploadDir "$SOURCE\public" $REMOTE_DIR
        ok "Public files deployed to web root"
    }

    FtpMkdir "$REMOTE_DIR/_setup"
    FtpUpload "$SOURCE\database.sql" "$REMOTE_DIR/_setup/database.sql"
    info "Database.sql also uploaded to /_setup/database.sql"

} else {
    # --- INCREMENTAL DEPLOY ---
    step "Incremental upload — only changed files"

    $uploadedCount = 0
    $frontendLoaded = $false

    foreach ($line in $CHANGED_FILES) {
        if ($line -eq '') { continue }
        $status = $line[0]
        $file = $line.Substring(1).Trim()
        # Map backend/path -> path (strip 'backend/')
        if ($file -match '^backend/(.+)$') {
            $relative = $Matches[1]
        } else {
            continue
        }

        if ($status -eq 'D') {
            # Deleted file — remove from remote
            info "  DELETE $relative"
            FtpDelete "$REMOTE_DIR/$relative"
            continue
        }

        $localPath = "$ROOT\$file"
        if (-not (Test-Path $localPath)) {
            info "  SKIP (deleted locally): $relative"
            continue
        }

        # public/ files go directly to /htdocs, not /htdocs/public/
        if ($relative -match '^public/(.+)$') {
            $remotePath = "$REMOTE_DIR/$($Matches[1])"
        } else {
            $remotePath = "$REMOTE_DIR/$relative"
        }

        info "  UPLOAD $relative"
        FtpUploadSingle $localPath $remotePath
        $uploadedCount++

        # Force full frontend rebuild if any frontend source changed
        if ($relative -match '^public/assets/') {
            $frontendLoaded = $true
        }
    }

    # Always upload .env (it may have changed even if not in git)
    FtpEnsureDir $REMOTE_DIR
    FtpUpload $prodEnvFile "$REMOTE_DIR/.env"
    info "  UP: .env (production)"
    Remove-Item $prodEnvFile -ErrorAction SilentlyContinue

    # Database SQL — upload if changed
    if ($CHANGED_FILES | Where-Object { $_ -match 'backend/database\.sql' } | Select-Object -First 1) {
        FtpMkdir "$REMOTE_DIR/_setup"
        FtpUpload "$SOURCE\database.sql" "$REMOTE_DIR/_setup/database.sql"
        info "  UP: database.sql"
    }

    ok "Incremental upload complete ($uploadedCount files)"
}

# ---------------------------------------------------------------
# FRONTEND (always full upload)
# ---------------------------------------------------------------
step "Building & uploading frontend"
$frontendDir = "$SOURCE\..\frontend"
if (Test-Path "$frontendDir\package.json") {
    $previousLoc = Get-Location

    # Build
    Set-Location $frontendDir
    try {
        info "Installing frontend dependencies..."
        npm install 2>&1 | Out-Null
        info "Building..."
        $buildOutput = npm run build 2>&1 | Out-String
        info "  build complete"
    } catch {
        warn "Frontend build failed"
    }

    Set-Location $previousLoc

    # Upload frontend assets (always full sync of public/)
    if (Test-Path "$SOURCE\public\assets") {
        info "Uploading SPA assets..."
        FtpEnsureDir "$REMOTE_DIR/assets"
        FtpUploadDir "$SOURCE\public\assets" "$REMOTE_DIR/assets"
        ok "Frontend assets uploaded"
    }

    if (Test-Path "$SOURCE\public\index.html") {
        FtpUploadSingle "$SOURCE\public\index.html" "$REMOTE_DIR/index.html"
        ok "index.html uploaded"
    }

    if (Test-Path "$SOURCE\public\vite.svg") {
        FtpUploadSingle "$SOURCE\public\vite.svg" "$REMOTE_DIR/vite.svg"
    }
} else {
    warn "frontend/ directory not found — skipping frontend"
}

# ---------------------------------------------------------------
# SAVE DEPLOY HEAD
# ---------------------------------------------------------------
if ($CURRENT_HEAD) {
    [IO.File]::WriteAllText($DEPLOY_HEAD, $CURRENT_HEAD)
    info "Saved deploy marker: $($CURRENT_HEAD.Substring(0,7))"
}

# ---------------------------------------------------------------
# DONE - POST-DEPLOY INSTRUCTIONS
# ---------------------------------------------------------------
Write-Host ""
Write-Host "==================================================" -ForegroundColor DarkCyan
Write-Host "  UPLOAD COMPLETE" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor DarkCyan

Write-Host ""
heading "NOW DO THESE STEPS ON INFINITYFREE:"
Write-Host ""

Write-Host "  [1] Import the database:" -ForegroundColor Yellow
Write-Host "      a. Go to InfinityFree control panel -> Databases -> phpMyAdmin"
Write-Host "      b. Select your database ($DB_NAME)"
Write-Host "      c. Click 'Import' tab at the top"
Write-Host "      d. Click 'Choose File' -> select dar_logs\database.sql from your PC"
Write-Host "      e. Scroll down, click 'Import' (leave all settings as default)"
Write-Host "      f. You should see 'Import has been successfully finished'"
Write-Host ""

Write-Host "  [2] Verify the site works:" -ForegroundColor Yellow
Write-Host "      a. Open your InfinityFree domain URL"
Write-Host "      b. You should see the React login page"
Write-Host "      c. Admin: admin / password (CHANGE IMMEDIATELY)"
Write-Host ""

Write-Host "  [3] Change the admin password:" -ForegroundColor Yellow
Write-Host "      a. Log in as admin"
Write-Host "      b. Go to Manage Accounts -> click your username"
Write-Host "      c. Set a strong password"
Write-Host ""

Write-Host "  [4] Secure your site:" -ForegroundColor Yellow
Write-Host "      a. InfinityFree panel -> SSL/TLS -> enable free SSL"
Write-Host "      b. Delete the _setup/ folder via FTP when done"
Write-Host ""

Write-Host "  If login shows 500 error, check:" -ForegroundColor Gray
Write-Host "    - .env has correct DB_HOST/DB_NAME/DB_USER/DB_PASS" -ForegroundColor Gray
Write-Host "    - database.sql was imported via phpMyAdmin" -ForegroundColor Gray
Write-Host "    - vendor/ folder exists on server (composer install was run)" -ForegroundColor Gray
Write-Host ""

pause
