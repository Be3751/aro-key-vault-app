gradle build

# aro-key-vault-app ビルド・実行手順

## 前提
- Java 17 (Eclipse Temurin)
- Docker
- Gradle 8.x (wrapper同梱、Shadowプラグインとの互換性のため)

## 1. ビルド（fat jar作成）

```bash
cd app
./gradlew --no-daemon shadowJar
```

- `build/libs/app.jar` が生成されます（依存込みjar）。

## 2. Dockerイメージ作成

```bash
docker build -t aro-key-vault-app .
```

- `aro-key-vault-app:latest` イメージが作成されます。

## 3. ローカル実行（.env使用）

```bash
docker run --rm -e ENV_TYPE=local -v "$PWD/.env:/app/.env" aro-key-vault-app
```

- `.env` ファイルを `/app/.env` としてマウントします。
- ローカル環境では `.env` から環境変数を取得します。

## 4. クラウド実行（環境変数使用）

- クラウド環境では `.env` を使わず、サービス側で環境変数（AZURE_TENANT_ID など）を設定してください。
- `ENV_TYPE` を `local` 以外にすれば自動で System.getenv() 取得に切り替わります。

## 5. よくあるトラブル
- `.env` ファイルがない場合は環境変数で取得してください。
- Gradle 9.xではShadowプラグインが動作しません。必ず8.x系を使用してください。

---

何か問題があれば `docker logs` や `./gradlew --stacktrace` で詳細を確認してください。
