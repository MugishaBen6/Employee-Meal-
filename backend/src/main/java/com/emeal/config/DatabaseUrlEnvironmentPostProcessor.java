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
 *
 * Supports standard URI formats (e.g. postgresql://user:pass@host/db?sslmode=require)
 * as well as standard JDBC formats (e.g. jdbc:postgresql://host/db?sslmode=require).
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
                    if (environment.getProperty("DB_USERNAME") == null && environment.getProperty("spring.datasource.username") == null) {
                        customProperties.put("spring.datasource.username", parts[0]);
                    }
                    if (environment.getProperty("DB_PASSWORD") == null && environment.getProperty("spring.datasource.password") == null) {
                        customProperties.put("spring.datasource.password", parts[1]);
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "/employee_meal_db";
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                } else {
                    jdbcUrl += "?sslmode=require";
                }

                customProperties.put("spring.datasource.url", jdbcUrl);
                customProperties.put("DB_URL", jdbcUrl);
            } catch (Exception e) {
                String jdbcUrl = url.startsWith("jdbc:") ? url : "jdbc:" + url;
                customProperties.put("spring.datasource.url", jdbcUrl);
                customProperties.put("DB_URL", jdbcUrl);
            }
        } else if (url.startsWith("jdbc:postgresql://") || url.startsWith("jdbc:h2:")) {
            customProperties.put("spring.datasource.url", url);
            customProperties.put("DB_URL", url);
        }

        if (!customProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlProperties", customProperties));
        }
    }
}
