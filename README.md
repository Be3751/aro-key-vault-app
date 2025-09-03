

Key Vault にシークレットを格納する。
## aro-key-vault

Small demo that reads secrets from Azure Key Vault via the Secrets Store CSI driver and injects them into a Java app.

Features
- Uses Secrets Store CSI driver + SecretProviderClass to sync Key Vault secrets into a Kubernetes Secret
- Image entrypoint reads files from `/mnt/secrets-store` and exports them as environment variables
- Local development supported via an embedded `.env` (not for production)

Warning
- Do not commit real secrets. This repository should never contain production credentials.

Quick start (local)

1. Build the image:

```bash
cd app
docker build -t aro-key-vault-app:local .
```

2. Run locally (uses `./app/.env`):

```bash
docker run --rm -v /mnt/secrets-store:/mnt/secrets-store aro-key-vault-app:local
```

Quick start (Kubernetes)

1. Ensure Secrets Store CSI driver and provider are installed on the cluster.
2. Create `SecretProviderClass` (example: `secret-provider.yaml`) pointing at your Key Vault.
3. Apply the pod manifest: `oc apply -f app.yaml` (or `kubectl apply -f app.yaml`).

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

CI / ACR push (example)

```bash
# tag
docker tag aro-key-vault-app:local <ACR_HOST>/aro-key-vault-app:latest
# login then push
docker push <ACR_HOST>/aro-key-vault-app:latest
```

Development notes
- The container entrypoint (`app/entrypoint.sh`) will read allowed files from `/mnt/secrets-store` and export them as environment variables (hyphens converted to underscores). In local runs the application falls back to `.env` using dotenv.

License
- MIT
