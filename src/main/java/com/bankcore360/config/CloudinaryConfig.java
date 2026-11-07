package com.bankcore360.config;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration providing a Cloudinary client instance for permanent
 * cloud storage of KYC and Loan documents.
 *
 * If Cloudinary credentials are not present in the runtime environment,
 * the bean evaluates to null, triggering the fail-safe local storage engine.
 */
@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.url:${CLOUDINARY_URL:}}")
    private String cloudinaryUrl;

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        try {
            if (StringUtils.hasText(cloudinaryUrl)) {
                log.info("Cloudinary client initialized using CLOUDINARY_URL configuration.");
                return new Cloudinary(cloudinaryUrl.trim());
            }

            if (StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
                log.info("Cloudinary client initialized for cloudName: {}", cloudName.trim());
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", cloudName.trim());
                config.put("api_key", apiKey.trim());
                config.put("api_secret", apiSecret.trim());
                return new Cloudinary(config);
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize Cloudinary client ({}). Falling back to local storage.", ex.getMessage());
            return null;
        }

        log.info("Cloudinary credentials not detected in environment. Using secure local filesystem storage fallback.");
        return null;
    }
}

