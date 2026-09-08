package org.fleet.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Teaches the app to start from a platform-supplied {@code DATABASE_URL}.
 *
 * <p>Railway, Heroku and most managed Postgres services publish one connection
 * string in URI form:
 *
 * <pre>postgresql://user:password@host:5432/railway</pre>
 *
 * <p>JDBC cannot use that. It wants {@code jdbc:postgresql://host:5432/railway}
 * with the credentials supplied separately, so handing Spring the platform's
 * variable verbatim fails with "Failed to determine suitable jdbc url" — which
 * reads like a missing setting rather than a format mismatch, and is why this
 * has been added and removed from application.properties three times.
 *
 * <p>This runs before the context is built and translates the URI into the three
 * properties Boot actually wants. An explicit {@code spring.datasource.url}
 * always wins: someone who has configured the datasource deliberately does not
 * want it silently replaced.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String[] CANDIDATES = {"DATABASE_URL", "DATABASE_PUBLIC_URL"};

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicit = environment.getProperty("spring.datasource.url");
        if (explicit != null && !explicit.isBlank()) {
            return;
        }

        for (String name : CANDIDATES) {
            String raw = environment.getProperty(name);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Map<String, Object> resolved = translate(raw.trim());
            if (!resolved.isEmpty()) {
                environment.getPropertySources()
                        .addFirst(new MapPropertySource("platform-database-url", resolved));
                // No logger yet — the logging system is not initialised this early.
                System.out.println("[startup] Datasource configured from " + name
                        + " -> " + resolved.get("spring.datasource.url"));
                return;
            }
        }
    }

    private Map<String, Object> translate(String value) {
        Map<String, Object> properties = new HashMap<>();

        // Already a JDBC url: pass it through and let the usual username/password
        // properties apply.
        if (value.startsWith("jdbc:")) {
            properties.put("spring.datasource.url", value);
            return properties;
        }

        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (host == null) {
                return properties;
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(host).append(':').append(port).append('/').append(database);
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbc.append('?').append(uri.getQuery());
            }
            properties.put("spring.datasource.url", jdbc.toString());

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                int separator = userInfo.indexOf(':');
                if (separator >= 0) {
                    properties.put("spring.datasource.username", decode(userInfo.substring(0, separator)));
                    properties.put("spring.datasource.password", decode(userInfo.substring(separator + 1)));
                } else {
                    properties.put("spring.datasource.username", decode(userInfo));
                }
            }
        } catch (Exception e) {
            // A malformed value is not something to guess at. Leaving the map empty
            // lets Boot report its own, clearer, missing-datasource error.
            System.out.println("[startup] Could not parse the database url: " + e.getMessage());
            return new HashMap<>();
        }

        return properties;
    }

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
