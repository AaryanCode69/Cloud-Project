param(
    [string]$ResourceGroup = "cloud-project-rg",
    [string]$Location = "centralindia",
    [string]$Prefix = "cloudproject",
    [string]$AksName = "cloud-project-aks",
    [string]$AksNamespace = "cloud-project",
    [string]$StaticWebAppLocation = "centralindia",
    [switch]$EnableAksKeyVaultIntegration = $true
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Write-Host "==> Verifying Azure authentication"
az account show --output table | Out-Host

Write-Host "==> Ensuring required Azure CLI extensions are available"
az extension add --name staticwebapp --only-show-errors | Out-Null

# Deterministic names with random suffix to avoid collisions.
$rand = -join ((48..57) + (97..122) | Get-Random -Count 6 | ForEach-Object {[char]$_})
$kvName = ("{0}kv{1}" -f $Prefix, $rand).ToLower()
$appConfigName = ("{0}appcfg{1}" -f $Prefix, $rand).ToLower()
$storageAccountName = ("{0}st{1}" -f $Prefix, $rand).ToLower()
$vnetName = "${Prefix}-vnet"
$aksSubnetName = "aks-subnet"
$appSubnetName = "app-subnet"
$publicIpName = "${Prefix}-ingress-pip"
$staticWebAppName = "${Prefix}-swa-${rand}"

Write-Host "==> Creating/validating resource group"
az group create --name $ResourceGroup --location $Location --output table | Out-Host

Write-Host "==> Creating VNet and subnets"
az network vnet create --resource-group $ResourceGroup --name $vnetName --location $Location --address-prefixes 10.20.0.0/16 --subnet-name $aksSubnetName --subnet-prefixes 10.20.1.0/24 --output table | Out-Host
az network vnet subnet create --resource-group $ResourceGroup --vnet-name $vnetName --name $appSubnetName --address-prefixes 10.20.2.0/24 --output table | Out-Host

Write-Host "==> Ensuring Network Watcher is enabled"
az network watcher configure --resource-group $ResourceGroup --locations $Location --enabled true --output table | Out-Host

Write-Host "==> Creating Key Vault"
az keyvault create --resource-group $ResourceGroup --name $kvName --location $Location --enable-rbac-authorization true --retention-days 90 --output table | Out-Host

Write-Host "==> Creating App Configuration"
az appconfig create --resource-group $ResourceGroup --name $appConfigName --location $Location --sku Free --output table | Out-Host

Write-Host "==> Creating Blob Storage account and container"
az storage account create --resource-group $ResourceGroup --name $storageAccountName --location $Location --sku Standard_LRS --kind StorageV2 --allow-blob-public-access false --https-only true --min-tls-version TLS1_2 --output table | Out-Host
az storage container create --account-name $storageAccountName --name cloud-project-assets --auth-mode login --public-access off --output table | Out-Host

Write-Host "==> Creating Static Web App (GitHub-less bootstrap)"
az staticwebapp create --name $staticWebAppName --resource-group $ResourceGroup --location $StaticWebAppLocation --sku Free --output table | Out-Host

Write-Host "==> Creating static ingress public IP for AKS load balancer"
az network public-ip create --resource-group $ResourceGroup --name $publicIpName --sku Standard --allocation-method Static --output table | Out-Host

if ($EnableAksKeyVaultIntegration -and -not [string]::IsNullOrWhiteSpace($AksName)) {
    Write-Host "==> Checking AKS cluster for Key Vault integration"
    $aksId = az aks show --resource-group $ResourceGroup --name $AksName --query id -o tsv 2>$null
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($aksId)) {
        Write-Host "==> Enabling OIDC issuer + Workload Identity on AKS"
        az aks update --resource-group $ResourceGroup --name $AksName --enable-oidc-issuer --enable-workload-identity --output table | Out-Host

        $kvAddonEnabled = az aks show --resource-group $ResourceGroup --name $AksName --query addonProfiles.azureKeyvaultSecretsProvider.enabled -o tsv
        if ($kvAddonEnabled -ne "true") {
            Write-Host "==> Enabling Key Vault CSI add-on on AKS"
            az aks enable-addons --resource-group $ResourceGroup --name $AksName --addons azure-keyvault-secrets-provider --output table | Out-Host
        } else {
            Write-Host "==> Key Vault CSI add-on is already enabled"
        }

        $oidcIssuer = az aks show --resource-group $ResourceGroup --name $AksName --query oidcIssuerProfile.issuerUrl -o tsv
        Write-Host "AKS OIDC Issuer: $oidcIssuer"
    } else {
        Write-Warning "AKS cluster '$AksName' not found in resource group '$ResourceGroup'; skipping AKS Key Vault integration steps."
    }
}

$publicIp = az network public-ip show --resource-group $ResourceGroup --name $publicIpName --query ipAddress -o tsv
$tenantId = az account show --query tenantId -o tsv
$blobEndpoint = az storage account show --name $storageAccountName --resource-group $ResourceGroup --query primaryEndpoints.blob -o tsv
$appCfgEndpoint = az appconfig show --name $appConfigName --resource-group $ResourceGroup --query endpoint -o tsv

Write-Host ""
Write-Host "==> Provisioning summary"
Write-Host "Key Vault Name: $kvName"
Write-Host "App Config Name: $appConfigName"
Write-Host "App Config Endpoint: $appCfgEndpoint"
Write-Host "Storage Account: $storageAccountName"
Write-Host "Blob Endpoint: $blobEndpoint"
Write-Host "Static Web App: $staticWebAppName"
Write-Host "Ingress Public IP: $publicIp"
Write-Host "Tenant ID: $tenantId"

Write-Host ""
Write-Host "==> Next steps"
Write-Host "1) Populate Key Vault secrets expected by k8s/keyvault/secretproviderclass.yaml"
Write-Host "2) Enable AKS Key Vault CSI + Workload Identity add-ons if not already enabled"
Write-Host "3) Apply k8s/keyvault manifests"
Write-Host "4) Point frontend Static Web App API base URL to your ingress IP: http://$publicIp"
Write-Host "5) If Key Vault CSI was enabled, apply k8s/keyvault/workload-identity-serviceaccount.yaml and k8s/keyvault/secretproviderclass.yaml"
