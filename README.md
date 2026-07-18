# DAR Activity Logs

Department of Agrarian Reform Camarines Sur II — web-based system to record, manage, and monitor in-and-out activity logs of documents and land records.

## Project Structure

```
dar_logs/
├── backend/           # PHP 8.1+ API + API server
│   ├── public/        # Document root (index.php + SPA shell)
│   ├── src/           # Modern API (PSR-4, controllers, services, middleware)
│   ├── api/           # Legacy session-based API
│   ├── routes/        # Route definitions
│   ├── config/        # App config, database config
│   ├── includes/      # Shared helpers
│   ├── storage/       # Logs, file cache
│   └── vendor/        # Composer deps
├── frontend/          # React + TypeScript SPA (Vite, Tailwind CSS v4, shadcn/ui)
│   └── src/           # Components, pages, hooks, stores, API client
└── mobile_application/ # Kotlin/Compose Android app
```

## Quick Start

### Backend

```powershell
cd backend
composer install
# Copy .env.example to .env, configure DB
```

### Frontend

```powershell
cd frontend
npm install
npm run dev        # Dev server with API proxy to localhost:8080
npm run build      # Outputs to backend/public/
```

### Mobile

Open `mobile_application/` in Android Studio, sync Gradle, build and run.

## Deployment

Automated scripts at the repo root:

| Script | Purpose |
|--------|---------|
| `.\deploy-laragon.ps1` | Full local dev setup — builds frontend, syncs to Laragon `www/`, imports DB |
| `.\deploy-infinityfree.ps1` | Production deployment to InfinityFree via FTP — builds, uploads, configures |

See `backend/DEPLOY.md` for detailed deployment guides.

## Tech Stack

- **Backend:** PHP 8.1+, MySQL, JWT auth, Composer PSR-4
- **Frontend:** React 19, TypeScript 6, Vite 8, Tailwind CSS v4, shadcn/ui, TanStack Query, Zustand, React Router v7
- **Mobile:** Kotlin, Jetpack Compose

## Default Login

- **Username:** `admin`
- **Password:** `password`
- Change immediately after first login via Manage Accounts.

## License

Internal use — Department of Agrarian Reform Camarines Sur II.
