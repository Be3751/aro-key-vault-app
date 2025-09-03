#!/bin/bash
set -euo pipefail
MOUNT_DIR="${SECRETS_MOUNT_PATH:-/mnt/secrets-store}"
ALLOW_LIST=("ENV-TYPE" "AZURE-TENANT-ID" "AZURE-CLIENT-ID" "AZURE-CLIENT-SECRET" "PG-SERVER" "PG-DATABASE" "PG-USER")
for f in "${ALLOW_LIST[@]}"; do
  if [ -f "${MOUNT_DIR}/${f}" ]; then
    env_name=$(echo "$f" | tr '[:lower:]-' '[:upper:]_')
    value=$(tr -d '\r\n' < "${MOUNT_DIR}/${f}")
    export "${env_name}=${value}"
  fi
done
exec "$@"
