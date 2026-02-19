### Podman Setup for ECommerce Application

This project has been configured to work with Podman on Windows.

#### Podman Compose Setup

Since `podman-compose` is not natively installed on the Windows host, a bridge has been set up:

1.  **podman-compose.bat**: A helper script in the root directory that redirects commands to `podman-compose` running inside your Podman machine (WSL).
2.  **podman-compose.yml**: Podman-specific compose files (copies of the docker-compose files).

##### How to use:
Instead of `docker-compose up`, use:
```powershell
.\podman-compose.bat up -d
```

To stop:
```powershell
.\podman-compose.bat down
```

To view logs:
```powershell
.\podman-compose.bat logs -f
```

#### Native Podman (Kubernetes YAML)

Podman also supports running applications via Kubernetes YAML files.

**Files:**
- `podman-compose.yml` (Use with `.\podman-compose.bat`)
- `podman-compose.prod.yml`
- `podman-compose.observability.yml`

#### Prerequisites
- Podman Machine must be running: `podman machine start`
- The `ecommerce-net` network must exist: `podman network create ecommerce-net` (Already created during setup).
