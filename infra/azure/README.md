# Useful Azure Services Bootstrap

This folder contains optional automation to provision useful Azure services for this project without changing service code paths.

## What this provisions

- Virtual Network + app subnet
- Network Watcher (enabled in selected region)
- Key Vault (RBAC enabled)
- Azure App Configuration (Free)
- Azure Blob Storage account + `cloud-project-assets` container
- Azure Static Web App (Free)
- Static Public IP for AKS ingress/load balancer use
- Optional: AKS OIDC + Workload Identity + Key Vault CSI add-on enablement

## Script

Run:

```powershell
pwsh ./infra/azure/deploy-useful-services.ps1
```

Optional parameters:

```powershell
pwsh ./infra/azure/deploy-useful-services.ps1 `
  -ResourceGroup cloud-project-rg `
  -Location centralindia `
  -Prefix cloudproject `
  -AksName cloud-project-aks `
  -AksNamespace cloud-project `
  -EnableAksKeyVaultIntegration
```

To skip AKS Key Vault integration:

```powershell
pwsh ./infra/azure/deploy-useful-services.ps1 -EnableAksKeyVaultIntegration:$false
```

## Redeploy Without Local Docker

Use ACR cloud builds and rolling updates when Docker daemon is unavailable locally:

```powershell
pwsh ./infra/azure/redeploy-via-acr-build.ps1 `
  -ResourceGroup cloud-project-rg `
  -AcrName apnacloudproject `
  -Namespace cloud-project
```

Optional parameters:

```powershell
# Set a custom immutable image tag
pwsh ./infra/azure/redeploy-via-acr-build.ps1 -ImageTag 202604130101

# Skip ingress health checks (not recommended)
pwsh ./infra/azure/redeploy-via-acr-build.ps1 -SkipHealthChecks
```

## Important

- The script requires Azure CLI auth and ARM connectivity.
- It does not change running workloads directly.
- `SecretProviderClass` in `k8s/keyvault/` requires the Key Vault CSI CRDs to exist in the cluster.
- After provisioning and enabling the AKS add-on, apply the Key Vault manifests in `k8s/keyvault/`.
- `redeploy-via-acr-build.ps1` requires Azure CLI + ACR access + AKS kubectl context.
