package com.emeal.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically normalizes database connection environment variables (DB_URL, DATABASE_URL, SPRING_DATASOURCE_URL)
 * to ensure seamless compatibility with cloud PostgreSQL providers (Neon, Render, Supabase, Railway).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("DB_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("DB_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            System.out.println("================================================================================");
            System.out.println("[DatabaseConfig] NOTICE: No cloud DB_URL or DATABASE_URL detected.");
            System.out.println("[DatabaseConfig] Using default local connection: jdbc:postgresql://localhost:5432/employee_meal_db");
            System.out.println("[DatabaseConfig] If running on Render/Cloud, please add DB_URL in the Environment Variables tab.");
            System.out.println("================================================================================");
            return;
        }

        String url = rawUrl.trim();
        Map<String, Object> customProperties = new HashMap<>();

        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            try {
                URI uri = new URI(url);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    String dbUser = environment.getProperty("DB_USERNAME");
                    if (dbUser == null || dbUser.isBlank() || dbUser.equals("postgres")) {
                        customProperties.put("spring.datasource.username", parts[0]);
                        customProperties.put("DB_USERNAME", parts[0]);
                    }
                    String dbPass = environment.getProperty("DB_PASSWORD");
                    if (dbPass == null || dbPass.isBlank() || dbPass.equals("postgres")) {
                        customProperties.put("spring.datasource.password", parts[1]);
                        customProperties.put("DB_PASSWORD", parts[1]);
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null && !uri.getPath().isBlank() ? uri.getPath() : "/neondb";
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                } else {
                    jdbcUrl += "?sslmode=require";
                }

                customProperties.put("spring.datasource.url", jdbcUrl);
                customProperties.put("DB_URL", jdbcUrl);
                System.out.println("[DatabaseConfig] Normalized postgres:// URI to JDBC URL: " + jdbcUrl);
            } catch (Exception e) {
                String jdbcUrl = url.startsWith("jdbc:") ? url : "jdbc:" + url;
                customProperties.put("spring.datasource.url", jdbcUrl);
                customProperties.put("DB_URL", jdbcUrl);
                System.out.println("[DatabaseConfig] Formatted JDBC URL: " + jdbcUrl);
            }
        } else if (url.startsWith("jdbc:postgresql://") || url.startsWith("jdbc:h2:")) {
            customProperties.put("spring.datasource.url", url);
            customProperties.put("DB_URL", url);
            System.out.println("[DatabaseConfig] Using configured JDBC URL: " + url);
        }

        if (!customProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlProperties", customProperties));
        }
    }
}
