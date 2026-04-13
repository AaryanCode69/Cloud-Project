param(
    [string]$ResourceGroup = "cloud-project-rg",
    [string]$AcrName = "apnacloudproject",
    [string]$Namespace = "cloud-project",
    [string]$ImageTag = "",
    [switch]$SkipHealthChecks = $false
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

if ([string]::IsNullOrWhiteSpace($ImageTag)) {
    $ImageTag = (Get-Date -Format "yyyyMMddHHmmss")
}

Write-Host "==> Using image tag: $ImageTag"

$services = @(
    @{ Name = "user-service"; Path = "user-service"; Deployment = "user-service" },
    @{ Name = "product-service"; Path = "product-service"; Deployment = "product-service" },
    @{ Name = "inventory-service"; Path = "inventory-service"; Deployment = "inventory-service" },
    @{ Name = "order-service"; Path = "order-service"; Deployment = "order-service" }
)

Write-Host "==> Validating Azure authentication"
az account show --output table | Out-Host

foreach ($svc in $services) {
    $image = "$AcrName.azurecr.io/$($svc.Name):$ImageTag"
    Write-Host "==> ACR build: $image"
    az acr build --registry $AcrName --image "$($svc.Name):$ImageTag" "$($svc.Path)" --output table | Out-Host
}

Write-Host "==> Updating AKS deployments to new image tag"
foreach ($svc in $services) {
    $image = "$AcrName.azurecr.io/$($svc.Name):$ImageTag"
    kubectl set image "deployment/$($svc.Deployment)" "$($svc.Deployment)=$image" -n $Namespace | Out-Host
}

Write-Host "==> Waiting for rollout"
foreach ($svc in $services) {
    kubectl rollout status "deployment/$($svc.Deployment)" -n $Namespace --timeout=300s | Out-Host
}

if (-not $SkipHealthChecks) {
    Write-Host "==> Running ingress health checks"
    $ip = kubectl get ingress -n $Namespace -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}'
    if ([string]::IsNullOrWhiteSpace($ip)) {
        throw "Ingress IP not found in namespace '$Namespace'."
    }

    $healthPaths = @(
        "/user/actuator/health",
        "/product/actuator/health",
        "/inventory/actuator/health",
        "/order/actuator/health"
    )

    foreach ($path in $healthPaths) {
        $url = "http://$ip$path"
        $statusCode = curl.exe -s -o $null -w "%{http_code}" $url
        if ($statusCode -ne "200") {
            throw "Health check failed for $url (HTTP $statusCode)"
        }
        Write-Host "Healthy: $url"
    }
}

Write-Host ""
Write-Host "==> Redeploy completed successfully"
Write-Host "Image Tag: $ImageTag"
Write-Host "Namespace: $Namespace"
