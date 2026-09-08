package org.fleet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${UPLOAD_DIR:${user.dir}/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = ensureDirectoryExists(uploadDir);
        if (uploadPath == null) {
            logger.warn("Upload directory is not available, skipping /uploads/** resource handler registration");
            return;
        }

        String uploadLocation = "file:" + uploadPath.toAbsolutePath() + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(0);

        Path qrCodesPath = ensureDirectoryExists(uploadPath.resolve("qr-codes").toString());
        if (qrCodesPath != null) {
            registry.addResourceHandler("/uploads/qr-codes/**")
                    .addResourceLocations("file:" + qrCodesPath.toAbsolutePath() + "/")
                    .setCachePeriod(0);
        }
    }

    private Path ensureDirectoryExists(String directory) {
        try {
            Path path = Paths.get(directory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            if (!Files.isWritable(path) && !Files.isReadable(path)) {
                logger.warn("Directory {} is neither readable nor writable", path);
                return null;
            }
            return path;
        } catch (IOException | RuntimeException e) {
            logger.error("Could not create or access directory: {}", directory, e);
            return null;
        }
    }
}