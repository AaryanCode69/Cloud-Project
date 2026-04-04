# Cloud-Project Azure Deployment — Progress & Context

## Goal

Deploy the Cloud-Project microservices to AKS and verify that **Azure SQL** and **Azure Cosmos DB** are working as the backing databases.

---

## What Was Done

### 1. Code Changes — Application Properties & Azure SQL Profile

The original `application.properties` files for all 4 services only had PostgreSQL config and were missing:

- **Feature flag bindings** — The Java code reads `feature.cosmos-catalog-enabled`, `feature.cosmos-cart-enabled`, etc. via `@Value` and `@ConditionalOnProperty`, but the properties files never mapped these to the `FEATURE_*` env vars injected by Kubernetes.
- **Cosmos DB property mappings** — `CosmosCatalogConfig` and `CosmosCartConfig` read `cosmos.endpoint`, `cosmos.key`, `cosmos.database`, `cosmos.catalog-container`, `cosmos.cart-container`, but these were never mapped to the `AZURE_COSMOS_*` env vars.
- **Azure SQL Spring profile** — The K8s manifests set `SPRING_PROFILES_ACTIVE=azure-sql`, but no `application-azure-sql.properties` file existed, so Spring would still use the PostgreSQL driver and dialect against an Azure SQL Server (which would crash).

#### Files Created

| File | Purpose |
|------|---------|
| `user-service/src/main/resources/application-azure-sql.properties` | Overrides driver to `SQLServerDriver` and dialect to `SQLServerDialect` |
| `inventory-service/src/main/resources/application-azure-sql.properties` | Same for inventory-service |
| `order-service/src/main/resources/application-azure-sql.properties` | Same for order-service |

#### Files Modified

| File | Changes |
|------|---------|
| `user-service/src/main/resources/application.properties` | Added feature flag bindings |
| `inventory-service/src/main/resources/application.properties` | Added feature flag bindings |
| `product-service/src/main/resources/application.properties` | Added feature flags + `cosmos.*` property mappings |
| `order-service/src/main/resources/application.properties` | Added feature flags + `cosmos.*` property mappings |

---

### 2. Azure Resource Provisioning

#### ✅ Azure SQL Server — Successfully Created

- **Server:** `cloud-project-sql-pavit.database.windows.net`
- **Location:** `centralindia`
- **Admin:** `dbadmin` / `P@ssw0rdCloud2026!`
- **Databases created:** `userdb`, `inventorydb`, `orderdb` (all Basic tier)
- **Firewall:** `AllowAzureServices` rule added (0.0.0.0 → 0.0.0.0) so AKS pods can connect

#### ❌ Azure Cosmos DB — BLOCKED by Azure Policy

Tried creating Cosmos DB in **every allowed region** (`centralindia`, `koreacentral`, `eastasia`, `uaenorth`, `eastus`, `southcentralus`) and with every option (`serverless`, `free-tier`, default provisioned). **All attempts failed** with the same policy error:

> *"Resource 'cloudproject-cosmos-pavit' was disallowed by Azure: This policy maintains a set of best available regions where your subscription can deploy resources."*

**Root cause:** The "Azure for Students" subscription has a restrictive Azure Policy (`Allowed resource deployment regions`) that blocks `Microsoft.DocumentDB` (Cosmos DB) resource creation entirely. This is a subscription-level restriction — not a code issue. The code is correctly written to support Cosmos DB; it just can't be provisioned on this subscription.

#### ✅ AKS Cluster — Started

- `cloud-project-aks` was in `Stopped` state → successfully started
- `kubectl` credentials refreshed via `az aks get-credentials`

---

### 3. Kubernetes Manifest Updates

| File | Changes |
|------|---------|
| `k8s/azure-sql-secret.yaml` | Updated with real SQL Server FQDN + credentials |
| `k8s/user-service.yaml` | Image tag bumped to `v2` |
| `k8s/inventory-service.yaml` | Image tag bumped to `v2` |
| `k8s/product-service.yaml` | Image → `v2`, removed `azure-cosmos-secret` refs, disabled Cosmos catalog, using in-cluster PostgreSQL |
| `k8s/order-service.yaml` | Image → `v2`, removed `azure-cosmos-secret` refs, disabled Cosmos cart, orders still use Azure SQL |

---

### 4. Docker Images — Built & Pushed

All 4 images built locally with Docker Desktop and pushed to ACR:

- `apnacloudproject.azurecr.io/user-service:v2` ✅
- `apnacloudproject.azurecr.io/product-service:v2` ✅
- `apnacloudproject.azurecr.io/inventory-service:v2` ✅
- `apnacloudproject.azurecr.io/order-service:v2` ✅

---

## What's Left To Do

- [ ] Deploy all K8s manifests to AKS (`kubectl apply` — namespace, secrets, postgres, services, ingress)
- [ ] Wait for pods to reach `Running` state
- [ ] Get the public Ingress IP
- [ ] Verify health endpoints (`/actuator/health`) for all 4 services
- [ ] Test Azure SQL connectivity by creating users/orders via the API
- [ ] Test in-cluster PostgreSQL connectivity for product-service

---

## Why Cosmos DB Isn't Working

> **⚠️ This is NOT a code bug.**

The Azure for Students subscription has a built-in Azure Policy that blocks `Microsoft.DocumentDB` (Cosmos DB) resource creation across ALL regions. The code is fully wired to support Cosmos DB — it just needs a subscription that allows Cosmos DB provisioning (e.g., a Pay-As-You-Go or Enterprise subscription).

**To enable Cosmos DB later:**

1. Use a subscription that allows `Microsoft.DocumentDB` resources
2. Run:
   ```bash
   az cosmosdb create --name <name> --resource-group cloud-project-rg \
     --locations regionName=centralindia --default-consistency-level Session
   ```
3. Get the endpoint + key and update `k8s/azure-cosmos-secret.yaml`
4. Flip `FEATURE_COSMOS_CATALOG_ENABLED` and `FEATURE_COSMOS_CART_ENABLED` to `"true"` in the K8s manifests
5. Redeploy — the code will auto-create the `catalog` and `cart` containers on startup

---

## Cost Management — Stop & Start Commands

### What Costs Money?

| Resource                         | While Running     | While AKS is Stopped | How to Stop Billing    |
|----------------------------------|-------------------|----------------------|------------------------|
| **AKS Node VMs**                 | ~₹5-8/hr per node | **Free** ✅           | `az aks stop`          |
| **Managed Disk** (PostgreSQL 5Gi)| ~₹15/month        | ~₹15/month           | Delete resource group  |
| **Public IP** (Ingress LB)       | ~₹250/month       | ~₹250/month          | Delete resource group  |
| **Container Registry (ACR)**     | ~₹350/month       | ~₹350/month          | Delete resource group  |
| **Azure SQL (3 × Basic)**        | ~₹350/month each  | ~₹350/month each     | Delete databases       |
| **AKS Control Plane**            | Free (Basic tier)  | Free                 | —                      |

> 💡 **Tip:** The **node VMs are the biggest cost**. Stopping the AKS cluster eliminates ~90% of the VM bill.

---

### Stop Temporarily (Recommended when not in use)

This stops the AKS node VMs to save costs. Your data, images, and configuration are preserved.

```bash
# Stop the AKS cluster (takes 2-5 minutes)
az aks stop --resource-group cloud-project-rg --name cloud-project-aks
```

> After this, all pods stop and the public IP becomes unreachable.

---

### Start Again

```bash
# Start the AKS cluster (takes 3-5 minutes)
az aks start --resource-group cloud-project-rg --name cloud-project-aks

# Refresh kubectl credentials
az aks get-credentials --resource-group cloud-project-rg --name cloud-project-aks --overwrite-existing

# Verify everything is back
kubectl get pods -n cloud-project
kubectl get ingress -n cloud-project
```

> ⚠️ The public IP may change after a stop/start cycle. Check with `kubectl get svc -n ingress-nginx`.

---

### Delete Everything (Stop ALL Billing)

```bash
# ⚠️ IRREVERSIBLE — deletes AKS, ACR, SQL Server, disks, everything
az group delete --name cloud-project-rg --yes --no-wait
```
