package org.fleet.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Existing uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/")
                .setCachePeriod(0);

        // QR codes directory
        registry.addResourceHandler("/uploads/qr-codes/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/qr-codes/")
                .setCachePeriod(0);
    }
}