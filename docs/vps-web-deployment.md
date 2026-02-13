# VPS Web Deployment Setup

How the KMP web app is hosted on `https://vokab.alirezaiyan.com` alongside the backend API.

## VPS Overview

- **IP**: `148.230.109.213`
- **Access**: `ssh root@148.230.109.213`
- **Specs**: 1 CPU, 3.8GB RAM, 48GB disk
- **OS**: Ubuntu with systemd

## Directory Structure

```
/var/www/vokab/
├── vokab.server/       # Spring Boot backend (runs in Docker)
└── kmp/                # KMP WasmJs web app (static files)
    ├── index.html
    ├── composeApp.js
    ├── *.wasm           # Kotlin/Wasm binaries (~16MB total)
    ├── sql-wasm.wasm    # SQLite for browser
    └── composeResources/  # fonts, strings, drawables
```

## What's Running on the VPS

### 1. Caddy (Web Server / Reverse Proxy)

The VPS uses **Caddy**, not nginx, as the web server. Caddy listens on ports 80 and 443.

```bash
# Check what's listening on port 80
ss -tlnp | grep ':80 '
# Output: caddy (pid=541402)
```

Caddy was chosen over nginx because it **automatically provisions and renews SSL certificates** via Let's Encrypt. No `certbot` setup needed.

### 2. Docker Containers

The backend runs as two Docker containers:

```bash
docker ps
# vokab-server   → Spring Boot app on 127.0.0.1:8080
# vokab-postgres → PostgreSQL database
```

These are defined in `/var/www/vokab/vokab.server/docker-compose.yml`.

## The Key Change: Caddy Configuration

### Before (API only)

The original `/etc/caddy/Caddyfile` proxied everything to the backend:

```
vokab.alirezaiyan.com {
  encode zstd gzip
  reverse_proxy 127.0.0.1:8080
}
```

Every request to `vokab.alirezaiyan.com` went to Spring Boot. There was no web app.

### After (Web App + API)

```
vokab.alirezaiyan.com {
  encode zstd gzip

  # API requests -> Spring Boot backend (Docker :8080)
  handle /api/* {
    reverse_proxy 127.0.0.1:8080
  }

  # Web app -> static files with SPA fallback
  handle {
    root * /var/www/vokab/kmp
    try_files {path} /index.html
    file_server
  }
}
```

### How This Works

Caddy processes `handle` blocks **in order of specificity**. More specific path matchers win:

1. **`handle /api/*`** — Any request starting with `/api/` gets proxied to the Spring Boot backend on port 8080. This covers all backend endpoints (`/api/v1/auth`, `/api/v1/words`, `/api/v1/collections`, etc.)

2. **`handle`** (catch-all) — Everything else serves static files from `/var/www/vokab/kmp/`:
   - `root * /var/www/vokab/kmp` — Sets the filesystem root
   - `try_files {path} /index.html` — Try to serve the exact file requested. If it doesn't exist, serve `index.html` instead. This is the **SPA fallback** — critical because the Compose app handles its own routing client-side
   - `file_server` — Actually serves the files

3. **`encode zstd gzip`** — Compresses responses (zstd preferred, gzip fallback). Significant for the large `.wasm` and `.js` files.

### Why SPA Fallback Matters

The KMP web app is a Single Page Application. When a user navigates to `/settings` or `/collections`, there's no actual `settings/index.html` file on disk. The browser needs `index.html` which loads `composeApp.js` which loads the Wasm binaries, and then Compose handles routing in-memory.

Without `try_files {path} /index.html`, refreshing the page on any route other than `/` would return a 404.

## Caddy Commands

```bash
# Validate config without applying
caddy validate --config /etc/caddy/Caddyfile

# Reload config (zero-downtime)
systemctl reload caddy

# Check status
systemctl status caddy

# View logs
journalctl -u caddy -f
```

## How Deployment Works

Since the VPS is too small for Gradle builds (Kotlin/Wasm compilation needs significant memory), we build locally and transfer the output:

```
Local machine                          VPS
┌─────────────────┐     rsync     ┌──────────────────┐
│ gradlew          │  ────────>   │ /var/www/vokab/   │
│ wasmJsBrowser    │              │   kmp/            │
│ Distribution     │              │                   │
│                  │              │ Caddy serves      │
│ build/dist/      │              │ these files       │
│ wasmJs/          │              │ at root (/)       │
│ productionExe/   │              │                   │
└─────────────────┘              └──────────────────┘
```

The `scripts/deploy-web.sh` automates this:

```bash
./scripts/deploy-web.sh           # build + deploy
./scripts/deploy-web.sh --deploy-only  # skip build, just rsync
```

## Caddy vs Nginx

This VPS had an nginx config file at `/etc/nginx/sites-available/vokab.alirezaiyan.com` but nginx wasn't actually running — Caddy was. Key differences:

| | Caddy | Nginx |
|---|---|---|
| SSL | Automatic (built-in ACME/Let's Encrypt) | Manual (`certbot --nginx`) |
| Config | Caddyfile (minimal, declarative) | nginx.conf (verbose) |
| Reload | `systemctl reload caddy` | `systemctl reload nginx` |
| Default | HTTPS with auto-redirect from HTTP | HTTP only unless configured |

## Verifying It Works

```bash
# Web app serves index.html
curl -s -o /dev/null -w "%{http_code}" https://vokab.alirezaiyan.com/
# -> 200

# API still reaches backend
curl -s -o /dev/null -w "%{http_code}" https://vokab.alirezaiyan.com/api/v1/health
# -> 200

# SPA fallback works (unknown path returns index.html, not 404)
curl -s -o /dev/null -w "%{http_code}" https://vokab.alirezaiyan.com/settings
# -> 200
```
