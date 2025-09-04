# aro-key-vault-app
## Overview
Small demo that reads secrets from Azure Key Vault via the Secrets Store CSI driver and injects them into a Java app.

Features
- Uses Secrets Store CSI driver + SecretProviderClass to sync Key Vault secrets into a Kubernetes Secret
- Image entrypoint reads files from `/mnt/secrets-store` and exports them as environment variables
- Local development supported via an embedded `.env` (not for production)

## Quick start
### local

1. Build the image:

```bash
cd app
docker build -t aro-key-vault-app:local .
```

2. Run locally (uses `./app/.env`):

```bash
docker run --rm -v /mnt/secrets-store:/mnt/secrets-store aro-key-vault-app:local
```

### Azure

1. CI / ACR push (example)

```bash
# tag
docker tag aro-key-vault-app:local <ACR_HOST>/aro-key-vault-app:latest
# login then push
docker push <ACR_HOST>/aro-key-vault-app:latest
```

Recommended sequence for ACR push and Key Vault setup

Prerequisites
- Install and login with Azure CLI: `az login`
- Have access to an Azure Container Registry (ACR) and an Azure Key Vault

1) Build, tag and push image to ACR (local Docker)

```bash
# build locally
cd app
docker build -t aro-key-vault-app:latest .

# tag for your ACR (replace <ACR_LOGIN_SERVER> e.g. myacr.azurecr.io)
docker tag aro-key-vault-app:latest <ACR_LOGIN_SERVER>/aro-key-vault-app:latest

# login to ACR (option A: az acr login)
az acr login --name <ACR_NAME>

# push
docker push <ACR_LOGIN_SERVER>/aro-key-vault-app:latest
```

Alternatively build in cloud using ACR Tasks:

```bash
az acr build --registry <ACR_NAME> --image aro-key-vault-app:latest ./app
```

2) (Optional) Create a service principal for CI to push to ACR and access Key Vault

```bash
# create SP and allow pushing images to ACR
az ad sp create-for-rbac --name "ci-acr-sp" --role AcrPush --scopes /subscriptions/<SUBSCRIPTION_ID>/resourceGroups/<RG>/providers/Microsoft.ContainerRegistry/registries/<ACR_NAME>

# the command prints appId (CLIENT_ID) and password (CLIENT_SECRET)
```

3) Grant the service principal access to Key Vault secrets (so it can read or the pod can use SP credentials)

```bash
# grant get/list on secrets to the SP
az keyvault set-policy --name <KV_NAME> --spn <CLIENT_ID> --secret-permissions get list
```

4) Set Key Vault secrets (example)

```bash
az keyvault secret set --vault-name <KV_NAME> --name ENV-TYPE --value "dev"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-TENANT-ID --value "<TENANT_ID>"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-CLIENT-ID --value "<CLIENT_ID>"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-CLIENT-SECRET --value "<CLIENT_SECRET>"
az keyvault secret set --vault-name <KV_NAME> --name PG-SERVER --value "<PG_HOST>"
az keyvault secret set --vault-name <KV_NAME> --name PG-DATABASE --value "<DB_NAME>"
az keyvault secret set --vault-name <KV_NAME> --name PG-USER --value "<DB_USER>"
```

5) Update `secret-provider.yaml` (if using service principal auth) with `tenantId`, and ensure `keyvaultName` matches `<KV_NAME>`; then apply manifests:

```bash
kubectl apply -f secret-provider.yaml
kubectl apply -f app.yaml
```

Notes
- Use Azure Managed Identities where possible instead of storing client secrets.
- For automation (CI), store sensitive values in CI secrets (GitHub Actions Secrets, Azure DevOps library) and inject them during the pipeline — do not hardcode secrets in repository files.


Set Key Vault secrets (example):

```bash
az keyvault secret set --vault-name <KV_NAME> --name ENV-TYPE --value "dev"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-TENANT-ID --value "<tenant-id>"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-CLIENT-ID --value "<client-id>"
az keyvault secret set --vault-name <KV_NAME> --name AZURE-CLIENT-SECRET --value "<client-secret>"
az keyvault secret set --vault-name <KV_NAME> --name PG-SERVER --value "<pg-host>"
az keyvault secret set --vault-name <KV_NAME> --name PG-DATABASE --value "<db-name>"
az keyvault secret set --vault-name <KV_NAME> --name PG-USER --value "<db-user>"
```

1. Ensure Secrets Store CSI driver and provider are installed on the cluster.
2. Create `SecretProviderClass` (example: `secret-provider.yaml`) pointing at your Key Vault.
3. Apply the pod manifest: `oc apply -f app.yaml` (or `kubectl apply -f app.yaml`).

## Development notes
- The container entrypoint (`app/entrypoint.sh`) will read allowed files from `/mnt/secrets-store` and export them as environment variables (hyphens converted to underscores). In local runs the application falls back to `.env` using dotenv.

## License
- MIT
