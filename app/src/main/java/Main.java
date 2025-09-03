package com.example;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.core.credential.TokenRequestContext;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Containerized Java World!");

        // ENV_TYPEが未設定ならdotenv、設定されていればSystem.getenvを使用
        String envType = System.getenv("ENV_TYPE");
        boolean useDotenv = (envType == null || envType.isEmpty());

        String tenantId, clientId, clientSecret, server, database, user;
        if (useDotenv) {
            Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.load();
            tenantId = dotenv.get("AZURE_TENANT_ID");
            clientId = dotenv.get("AZURE_CLIENT_ID");
            clientSecret = dotenv.get("AZURE_CLIENT_SECRET");
            server = dotenv.get("PG_SERVER");
            database = dotenv.get("PG_DATABASE");
            user = dotenv.get("PG_USER");
        } else {
            tenantId = System.getenv("AZURE_TENANT_ID");
            clientId = System.getenv("AZURE_CLIENT_ID");
            clientSecret = System.getenv("AZURE_CLIENT_SECRET");
            server = System.getenv("PG_SERVER");
            database = System.getenv("PG_DATABASE");
            user = System.getenv("PG_USER");
        }

        System.out.println("AZURE_TENANT_ID: " + tenantId);
        System.out.println("AZURE_CLIENT_ID: " + clientId);
        System.out.println("AZURE_CLIENT_SECRET: " + (clientSecret != null ? "[set]" : "[not set]"));
        System.out.println("PG_SERVER: " + server);
        System.out.println("PG_DATABASE: " + database);
        System.out.println("PG_USER: " + user);

        // JDBC URL (Entra認証用)
        String url = String.format("jdbc:postgresql://%s:5432/%s?sslmode=require", server, database);
        System.out.println("JDBC URL: " + url);

        // Azure Identityライブラリでトークン取得
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .tenantId(tenantId)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build();

        TokenRequestContext tokenRequestContext = new TokenRequestContext()
            .addScopes("https://ossrdbms-aad.database.windows.net/.default");
        String accessToken = credential.getToken(tokenRequestContext).block().getToken();
        System.out.println("Access Token: " + (accessToken != null ? accessToken.substring(0, 20) + "..." : "[not acquired]"));

        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", user);
        props.setProperty("password", accessToken);
        props.setProperty("sslmode", "require");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected to Azure PostgreSQL with Entra ID!");
            // ここでDB操作可能
            // コンテナが停止しないように無限ループで待機
            while (true) {
                try {
                    // PostgreSQLのデータベース一覧を表示
                    java.sql.Statement stmt = conn.createStatement();
                    java.sql.ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false;");
                    System.out.println("[Database List]");
                    while (rs.next()) {
                        System.out.println("- " + rs.getString("datname"));
                    }
                    rs.close();
                    stmt.close();
                    Thread.sleep(60000); // 60秒ごとに待機
                } catch (InterruptedException ie) {
                    break;
                } catch (Exception ex) {
                    System.out.println("Failed to list tables:");
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Connection failed:");
            e.printStackTrace();
        }
    }
}
