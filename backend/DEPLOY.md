# DAR Activity Logs — Deployment Guide

## Prerequisites

- [PHP 8.1+](https://windows.php.net/download) (add to PATH)
- [Composer](https://getcomposer.org/download/) (add to PATH)
- [Laragon](https://laragon.org/download/) (for local dev)
- InfinityFree hosting account with a domain and MySQL database created

---

## Local Deployment (Laragon)

### Quick Deploy

```powershell
..\deploy-laragon.ps1
```

The script handles everything automatically: validates `.env`, installs dependencies, syncs files, creates the database, and imports the schema.

### Manual Steps

If the script can't find MySQL automatically, do this manually:

#### 1. Install dependencies

```powershell
cd C:\Users\James\Downloads\dar_logs\backend
composer install --no-dev --optimize-autoloader
```

#### 2. Copy files to Laragon

Copy the entire `backend` folder (excluding `.git`, `.idea`, `.vscode`, `.venv`, `node_modules`) into `C:\laragon\www\`.

Final path: `C:\laragon\www\backend\`

#### 3. Ensure `.env` has local values

```
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
```

#### 4. Create the database

**Option A — phpMyAdmin:**
1. Open http://localhost/phpmyadmin
2. Click **New** on the left sidebar
3. Name: `dar_logs`, Collation: `utf8mb4_general_ci` → **Create**
4. Select the new database → **Import** tab
5. Choose `database.sql` → **Go**

**Option B — MySQL CLI (Laragon Terminal):**
```sql
CREATE DATABASE IF NOT EXISTS dar_logs CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE dar_logs;
SOURCE C:/laragon/www/backend/database.sql;
```

#### 5. Verify

Open http://localhost/backend/ and log in with:
- **Username:** `admin`
- **Password:** `password`

---

## Production Deployment (InfinityFree)

### Quick Deploy

1. Create a `.deploy` file in the project root with your credentials:
```
FTP_HOST=ftpupload.net
FTP_USER=epiz_XXXXX
FTP_PASS=your-ftp-password
REMOTE_DIR=/htdocs
DB_HOST=sqlXXX.epizy.com
DB_NAME=epiz_XXXXX_dar_logs
DB_USER=epiz_XXXXX
DB_PASS=your-db-password
```

2. Run the deploy script:
```powershell
..\deploy-infinityfree.ps1
```

The script will: generate a production `.env`, install dependencies, clean the remote server, upload all files, and give you post-deploy instructions.

### Manual Steps

If you prefer manual deployment:

#### 1. Get your InfinityFree credentials

From the InfinityFree control panel:
- **FTP details:** Account → FTP Details → FTP hostname, username, password
- **Database details:** Account → Databases → Host, Name, Username, Password

#### 2. Install dependencies

```powershell
cd C:\Users\James\Downloads\dar_logs\backend
composer install --no-dev --optimize-autoloader
```

#### 3. Create production `.env`

```
DB_HOST=sqlXXX.epizy.com
DB_PORT=3306
DB_NAME=epiz_XXXXX_dar_logs
DB_USER=epiz_XXXXX
DB_PASS=your-actual-db-password
DB_CHARSET=utf8mb4

APP_ENV=production
APP_DEBUG=false

JWT_SECRET=generate-a-random-64-character-string-here
JWT_EXPIRY=86400

AUDIT_LOG_CAPACITY=100
NOTIFICATION_CAP=50

CACHE_ENABLED=true
CACHE_TTL_STATS=30
CACHE_TTL_USERS=120
```

#### 4. Upload files via FTP (FileZilla)

Open FileZilla, connect with your InfinityFree FTP credentials, and upload these folders and files to `/htdocs`:

**Root files to upload:**
```
.htaccess
.user.ini
bootstrap.php
composer.json
composer.lock
dashboard.php
admin.php
archive.php
completed.php
index.php
my_work_logs.php
notifications.php
pending_records.php
.env           (the production one you just created)
database.sql
```

**Directories to upload** (upload the folder itself, including all contents):
```
api/
assets/
config/
includes/
public/
routes/
src/
storage/
vendor/
```

Make sure `storage/logs/` and `storage/cache/` directories exist on the server (create empty folders if needed).

#### 5. Import the database

1. Go to InfinityFree control panel → **Databases** → **phpMyAdmin**
2. On the left, click your database name (`epiz_XXXXX_dar_logs`)
3. Click the **Import** tab at the top
4. Click **Choose File** → select `database.sql` from your PC
5. Scroll down, click **Import** (leave all settings as default — Format: SQL, SQL compatibility: NONE)
6. You should see: *"Import has been successfully finished, X queries executed."*

This creates the `users` table and inserts the default admin account.

#### 6. Verify

Open your InfinityFree domain (e.g. `https://yoursite.epizy.com`) and log in:

| Field    | Value      |
|----------|------------|
| Username | `admin`    |
| Password | `password` |

**Immediately change the admin password:** Log in → Manage Accounts → click your username → set a strong password.

---

## Default Accounts

The `database.sql` file creates one admin user:

| Username | Password (bcrypt hash) | Role  |
|----------|------------------------|-------|
| admin    | `password`             | admin |

To reset the admin password, run this in phpMyAdmin (SQL tab) or MySQL CLI:

```sql
UPDATE users
SET password = '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
WHERE username = 'admin';
```

---

## Troubleshooting

### 500 Internal Server Error on login

This is almost always a database connection issue.

1. Check `.env` on the server has the correct InfinityFree DB credentials
2. Verify the database exists and was imported via phpMyAdmin
3. Check that `vendor/` folder exists on the server (Composer dependencies)
4. InfinityFree may override `display_errors` — check `error_log` files in `storage/logs/`

### "Authentication required" on API calls

The session path may not be writable. InfinityFree uses a shared session save path.
Ensure `session_start()` can write — check your `.user.ini` has `session.use_strict_mode = 1`.

### Database connection refused

InfinityFree MySQL hostnames look like `sql123.epizy.com`, NOT `localhost`.
Get the exact hostname from: InfinityFree panel → Databases → your database → "MySQL Host".

### Files not showing after upload

- InfinityFree uses `/htdocs` as the document root — make sure files are uploaded there, not to `/`
- Check `.htaccess` was uploaded (it blocks access to sensitive files)
- Clear your browser cache
