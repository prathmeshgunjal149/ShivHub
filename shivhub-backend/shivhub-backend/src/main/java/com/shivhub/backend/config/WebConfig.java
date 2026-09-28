package com.shivhub.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/*
 * =========================================================
 * WebConfig
 * =========================================================
 *
 * This class configures how Spring Boot serves static files.
 *
 * Our uploaded product images are stored outside the
 * normal "src/main/resources/static" folder.
 *
 * Location:
 *
 * uploads/products/
 *
 *
 * Example actual file:
 *
 * uploads/products/1/abc123.jpg
 *
 *
 * We want the browser to access it using:
 *
 * http://localhost:8080/uploads/products/1/abc123.jpg
 *
 *
 * This configuration creates that mapping.
 *
 * =========================================================
 */

@Configuration
public class WebConfig implements WebMvcConfigurer {


    /*
     * =========================================================
     * ADD RESOURCE HANDLERS
     * =========================================================
     *
     * This method tells Spring where to find files when
     * a browser requests a URL.
     * =========================================================
     */

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {


        /*
         * -----------------------------------------------------
         * PRODUCT IMAGE MAPPING
         * -----------------------------------------------------
         *
         * Browser request:
         *
         * /uploads/products/1/image.jpg
         *
         * will be mapped to:
         *
         * file:uploads/products/1/image.jpg
         *
         *
         * "file:" means the file exists on the server's
         * filesystem.
         * -----------------------------------------------------
         */

        registry.addResourceHandler(
                "/uploads/products/**"
        ).addResourceLocations(
                "file:uploads/products/"
        );

        registry.addResourceHandler(
                "/uploads/profile/**"
        ).addResourceLocations(
                "file:uploads/profile/"
        );

        registry.addResourceHandler(
                "/uploads/campaigns/**"
        ).addResourceLocations(
                "file:uploads/campaigns/"
        );

        registry.addResourceHandler(
                "/uploads/marketplace/**"
        ).addResourceLocations(
                "file:uploads/marketplace/"
        );
    }
}
