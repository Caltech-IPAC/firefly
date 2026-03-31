# Firefly Helm Chart

Helm chart for deploying [Firefly](https://github.com/Caltech-IPAC/firefly), a web application for astronomical data visualization.

## Chart Structure

```
helm/
├── Chart.yaml
├── values.yaml         # defaults
└── env/
    ├── dev.yaml        # sample dev overrides
    └── prod.yaml       # sample prod overrides
```

## Install

**From GHCR:**

List available versions:
```bash
gh api /orgs/Caltech-IPAC/packages/container/firefly-chart%2Ffirefly/versions \
  --jq '.[].metadata.container.tags'
```

Inspect a specific version:
```bash
helm show chart oci://ghcr.io/caltech-ipac/firefly-chart/firefly --version <chart-version>
```

Install:
```bash
helm upgrade --install firefly oci://ghcr.io/caltech-ipac/firefly-chart/firefly \
  -n my-namespace \
  --create-namespace \
  --version <chart-version> \
  -f env/dev.yaml \
  --set ingress.host=my.example.com
```
> Omit `--version` to use the latest published chart.

**From local chart:**
```bash
helm update --install firefly ./helm \
  -n my-namespace \
  --create-namespace \
  -f env/dev.yaml
  --set ingress.host=my.example.com \
```

**Uninstall:**
```bash
helm -n my-namespace uninstall firefly
```

## Configuration

### Image
| Key | Default | Description |
|-----|---------|-------------|
| `image.repository` | `ghcr.io/caltech-ipac/firefly` | Image repository |
| `image.tag` | appVersion in Chart.yaml | Image tag |
| `image.pullPolicy` | `IfNotPresent` | Image pull policy |

### Scaling
| Key | Default | Description |
|-----|---------|-------------|
| `replicaCount` | `1` | Number of replicas. When > 1, Redis is automatically deployed and session affinity annotations are added to the ingress based on `ingress.className` |

### Ingress
| Key | Default | Description |
|-----|---------|-------------|
| `ingress.enabled` | `true` | Enable ingress |
| `ingress.className` | `""` | Ingress class; omit to use cluster default. Supported values: `nginx`, `traefik`, `haproxy` |
| `ingress.host` | `firefly.example.com` | **Required override.** Hostname depends on where the app is deployed and must be set explicitly via `-f env/<env>.yaml` or `--set ingress.host=<host>` |
| `ingress.annotations` | `{}` | Additional annotations; merged with auto-generated affinity annotations |
| `ingress.tlsSecretName` | `""` | TLS secret name; when set, TLS is enabled |

### Session Affinity
Session affinity is required when `replicaCount > 1`. If `ingress.className` is set,
the appropriate cookie-based affinity annotations are automatically injected. Supported
values: `nginx`, `traefik`, `haproxy`. For other ingress controllers, set the annotations
manually via `ingress.annotations`.

### Persistence
All volumes default to `emptyDir`. Each can be overridden with a new PVC or an existing one.
The examples below are samples — actual values for `storageClass`, `size`, and `existingClaim`
depend on your Kubernetes setup:

```yaml
persistence:
  workDir:
    pvc:
      size: 10Gi
      storageClass: standard
      accessMode: ReadWriteOnce
  logsDir:
    existingClaim: my-logs-pvc
  sharedWorkDir:
    pvc:
      size: 10Gi
      storageClass: efs
      accessMode: ReadWriteMany  # required for multi-replica
```

### Admin Password
```yaml
# plain text
adminPassword:
  value: mypassword

# from a Kubernetes secret
adminPassword:
  secretName: my-secret
  secretKey: password
```

### Redis
Automatically deployed when `replicaCount > 1`. Consult `values.yaml` for configuration options including image, resources, and persistence.

### Security Context
Optional. Shared by both Firefly and Redis deployments. When not set, containers run as the default user defined in the image (e.g. `tomcat(91)` for Firefly).
```yaml
securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
```
