# Deploying Cloud-Project on Azure Kubernetes Service (AKS)

## Prerequisites

- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) installed
- [kubectl](https://kubernetes.io/docs/tasks/tools/) installed
- [Docker](https://www.docker.com/) installed
- An active Azure subscription

---

## Step 1 — Create Azure Resources

```bash
# Login
az login

# Create resource group
az group create --name cloud-project-rg --location southindia

# Create container registry (name must be globally unique, alphanumeric only)
az acr create --resource-group cloud-project-rg --name apnacloudproject --sku Basic

# Create AKS cluster attached to the registry
az aks create --resource-group cloud-project-rg --name cloud-project-aks --node-count 1 --attach-acr apnacloudproject --generate-ssh-keys --location centralindia

# Connect kubectl to the cluster
az aks get-credentials --resource-group cloud-project-rg --name cloud-project-aks
```



---

## Step 2 — Build & Push Docker Images

```bash
# Login to ACR
az acr login --name apnacloudproject

# Build and push all services (run from project root)
docker build -t apnacloudproject.azurecr.io/user-service:v1      ./user-service
docker build -t apnacloudproject.azurecr.io/product-service:v1   ./product-service
docker build -t apnacloudproject.azurecr.io/inventory-service:v1 ./inventory-service
docker build -t apnacloudproject.azurecr.io/order-service:v1     ./order-service

docker push apnacloudproject.azurecr.io/user-service:v1
docker push apnacloudproject.azurecr.io/product-service:v1
docker push apnacloudproject.azurecr.io/inventory-service:v1
docker push apnacloudproject.azurecr.io/order-service:v1
```

---

## Step 3 — Update Image Names in Manifests

All service YAML files under `k8s/` are already configured to use `apnacloudproject.azurecr.io`:

- `k8s/user-service.yaml`
- `k8s/product-service.yaml`
- `k8s/inventory-service.yaml`
- `k8s/order-service.yaml`

---

## Step 4 — Deploy to AKS

```bash
# Namespace and secrets first
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres-secret.yaml

# Database
kubectl apply -f k8s/postgres.yaml

# Wait for PostgreSQL to be ready
kubectl wait --namespace cloud-project --for=condition=ready pod -l app=postgres --timeout=120s

# Microservices
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/product-service.yaml
kubectl apply -f k8s/inventory-service.yaml
kubectl apply -f k8s/order-service.yaml
```

---

## Step 5 — Expose Services to the Internet (NGINX Ingress)

This step sets up a single public IP that routes traffic to all your microservices via path-based routing. This is the URL you share with your frontend developer.

### 5.1 — Install NGINX Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/cloud/deploy.yaml
```

Wait for the controller to get an external IP (takes 1–2 minutes):

```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller --watch
```

Once `EXTERNAL-IP` shows an IP address (not `<pending>`), press `Ctrl+C` to stop watching.

### 5.2 — Deploy the Ingress Resource

```bash
kubectl apply -f k8s/ingress.yaml
```

### 5.3 — Get Your Public URL

```bash
kubectl get ingress -n cloud-project
```

The `ADDRESS` column shows your public IP. Your frontend developer can now use:

| Service           | Public URL                                    |
|-------------------|-----------------------------------------------|
| User Service      | `http://<EXTERNAL-IP>/user/api/users`         |
| Product Service   | `http://<EXTERNAL-IP>/product/api/products`   |
| Inventory Service | `http://<EXTERNAL-IP>/inventory/api/inventory`|
| Order Service     | `http://<EXTERNAL-IP>/order/api/orders`       |

> 💡 **Tip:** The ingress uses path-based routing. Requests to `/user/...` go to `user-service`, `/product/...` go to `product-service`, and so on. The path prefix is stripped before forwarding to the service.

---

## Step 6 — Verify

```bash
# Check all pods are running
kubectl get pods -n cloud-project

# Check services
kubectl get svc -n cloud-project

# Check ingress
kubectl get ingress -n cloud-project

# Quick smoke test using the public IP
curl http://<EXTERNAL-IP>/user/actuator/health
curl http://<EXTERNAL-IP>/product/actuator/health
curl http://<EXTERNAL-IP>/inventory/actuator/health
curl http://<EXTERNAL-IP>/order/actuator/health

# View logs if something is wrong
kubectl logs -f deployment/user-service -n cloud-project
```

---

## Step 7 — Testing

### How to Test

**1. Check all pods are running:**

```bash
kubectl get pods -n cloud-project
```

All pods should show `STATUS: Running` and `READY: 1/1`. If a pod shows `CrashLoopBackOff` or `Error`, check its logs:

```bash
kubectl logs <pod-name> -n cloud-project
```

**2. Test via the public Ingress URL (no port-forward needed):**

```bash
# Health checks
curl http://<EXTERNAL-IP>/user/actuator/health
curl http://<EXTERNAL-IP>/product/actuator/health
curl http://<EXTERNAL-IP>/inventory/actuator/health
curl http://<EXTERNAL-IP>/order/actuator/health
```

Each should return `{"status":"UP"}`.

**3. Test API endpoints:**

```bash
# Example: Create a user
curl -X POST http://<EXTERNAL-IP>/user/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com"}'

# Example: Get all products
curl http://<EXTERNAL-IP>/product/api/products
```

**4. (Alternative) Test via port-forward if Ingress is not ready:**

```bash
kubectl port-forward svc/user-service 8081:8081 -n cloud-project
# Then: curl http://localhost:8081/actuator/health
```

### When to Stop Testing

- ✅ All 4 service pods show `Running` status
- ✅ All health endpoints return `{"status":"UP"}` via the public URL
- ✅ You can successfully create/read data through the API endpoints
- ✅ PostgreSQL pod is running and services can connect to it
- ✅ Ingress has an external IP assigned

If all the above checks pass, your deployment is working correctly.

---

## Step 8 — Cost Management (Stop / Restart / Delete)

### What Costs Money?

| Resource | While Running | While AKS is Stopped | How to Stop Billing |
|---|---|---|---|
| **AKS Node VMs** | ~₹5-8/hr per node | **Free** ✅ | `az aks stop` |
| **Managed Disk** (PostgreSQL 5Gi) | ~₹15/month | ~₹15/month (disk stays) | Delete resource group |
| **Public IP** (Ingress LoadBalancer) | ~₹250/month | ~₹250/month (IP reserved) | Delete resource group |
| **Container Registry (ACR)** | ~₹350/month (Basic) | ~₹350/month | Delete resource group |
| **AKS Control Plane** | Free (Basic tier) | Free | — |

> 💡 **Tip:** The **node VMs are the biggest cost**. Stopping the AKS cluster eliminates ~90% of the bill. The remaining charges (disk, IP, ACR) are small.

---

### Option 1 — Stop Temporarily (Recommended for Daily Use)

This stops the VMs to save costs. Your data, images, and configuration are preserved.

**Stop the cluster:**

```bash
az aks stop --resource-group cloud-project-rg --name cloud-project-aks
```

> ⏱️ This takes 2–5 minutes. After this, all pods stop and the public IP becomes unreachable.

**Restart the cluster later:**

```bash
az aks start --resource-group cloud-project-rg --name cloud-project-aks
```

> ⏱️ This takes 3–5 minutes. After restart, all your deployments, services, and ingress come back automatically — your data in PostgreSQL is preserved because it uses a persistent volume.

**Verify after restart:**

```bash
kubectl get pods -n cloud-project
kubectl get ingress -n cloud-project
```

All pods should return to `Running` and ingress should show the same external IP.

> ⚠️ **Note:** The public IP may change after a stop/start cycle. If it does, run `kubectl get svc -n ingress-nginx` to get the new IP and update your frontend dev.

---

### Option 2 — Delete Services Only (Keep the Cluster)

If you want to remove all workloads but keep the AKS cluster available for future deployments:

```bash
kubectl delete -f k8s/ingress.yaml
kubectl delete -f k8s/order-service.yaml
kubectl delete -f k8s/inventory-service.yaml
kubectl delete -f k8s/product-service.yaml
kubectl delete -f k8s/user-service.yaml
kubectl delete -f k8s/postgres.yaml
kubectl delete -f k8s/postgres-secret.yaml
kubectl delete -f k8s/namespace.yaml
```

To redeploy later, run Steps 4 and 5 again.

> ⚠️ **Warning:** This deletes the PostgreSQL pod and its data. If you want to keep the data, just use `az aks stop` instead.

---

### Option 3 — Delete Everything (Stop All Billing)

To permanently delete **all** Azure resources and stop **all** billing:

```bash
az group delete --name cloud-project-rg --yes --no-wait
```

> 🚨 **Caution:** This deletes the resource group, AKS cluster, container registry, persistent disks, and all data. **This action is irreversible.** You will need to redo all steps from Step 1 to deploy again.
