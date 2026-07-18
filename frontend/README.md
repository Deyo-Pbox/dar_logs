# DAR Activity Logs — Frontend

React 19 SPA with TypeScript 6, Vite 8, Tailwind CSS v4, and shadcn/ui.

## Stack

- React 19 + TypeScript 6
- Vite 8 (build tool)
- Tailwind CSS v4 (styling)
- shadcn/ui (components)
- TanStack Query (server state)
- Zustand (client state, auth store)
- React Router v7 (hash-based routing)
- Axios (API client)

## Setup

```bash
npm install
npm run dev      # Dev server on localhost:5173, proxies /api -> localhost:8080
npm run build    # TypeScript check + Vite build -> ../backend/public/
```

## Project Structure

```
frontend/
├── src/
│   ├── api/          # API client (axios), endpoint modules
│   ├── components/   # Reusable UI + layout components
│   ├── hooks/        # Data hooks (TanStack Query wrappers)
│   ├── lib/          # Utilities (JWT decode, etc.)
│   ├── pages/        # Route pages
│   └── stores/       # Zustand stores (auth)
├── public/           # Static assets
└── vite.config.ts    # Vite config (base: './', outDir: ../backend/public/)
```

## Auth Flow

- Login via `/api/v1/auth/login` → JWT token stored in Zustand (`persist` → localStorage)
- Axios interceptor attaches `Authorization: Bearer` header
- 401 responses trigger logout + redirect to `#/login`
- `ProtectedRoute` guards authenticated routes
- `AdminRoute` guards admin-only routes
