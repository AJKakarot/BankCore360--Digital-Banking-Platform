package com.bankcore360.service;

import com.bankcore360.entity.KycDocumentType;
import com.bankcore360.exception.InvalidDocumentException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing KYC document storage using Cloudinary cloud storage
 * with fallback to local private storage when Cloudinary is not configured.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Path storageLocation;
    private final Cloudinary cloudinary;

    public FileStorageService(
            @Value("${KYC_DOCUMENT_STORAGE_DIR:storage/private/kyc-documents}") String storageDir,
            @Autowired(required = false) Cloudinary cloudinary
    ) {
        this.cloudinary = cloudinary;
        this.storageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize secure private storage directory: " + this.storageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file, Long customerId, KycDocumentType documentType) {
        validateFile(file);

        if (cloudinary != null) {
            try {
                String publicId = "kyc_cust_" + customerId + "_" + documentType.name().toLowerCase() + "_" + UUID.randomUUID();
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "bankcore360/kyc-documents",
                        "resource_type", "auto",
                        "public_id", publicId
                ));

                String secureUrl = (String) uploadResult.get("secure_url");
                if (StringUtils.hasText(secureUrl)) {
                    log.info("KYC document [{}] for customer {} successfully uploaded to Cloudinary: {}", documentType, customerId, secureUrl);
                    return secureUrl;
                }
            } catch (Exception ex) {
                log.warn("Cloudinary KYC upload failed ({}). Falling back to secure local filesystem.", ex.getMessage());
            }
        }

        return storeLocally(file, customerId, documentType);
    }

    private String storeLocally(MultipartFile file, Long customerId, KycDocumentType documentType) {
        String originalFilename = file.getOriginalFilename();
        String cleanOriginalName = originalFilename != null
                ? StringUtils.cleanPath(originalFilename)
                : "document";

        // Extract extension safely
        String extension = "";
        int dotIndex = cleanOriginalName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < cleanOriginalName.length() - 1) {
            extension = cleanOriginalName.substring(dotIndex);
        }

        // Prevent path traversal by generating a secure random filename
        String secureFilename = "cust_" + customerId + "_" + documentType.name().toLowerCase()
                + "_" + UUID.randomUUID() + extension;

        Path targetLocation = this.storageLocation.resolve(secureFilename).normalize();
        if (!targetLocation.startsWith(this.storageLocation)) {
            throw new InvalidDocumentException("Invalid path sequence detected in file storage");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to securely store KYC document file", ex);
        }

        return secureFilename;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidDocumentException("File size exceeds maximum allowed limit of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidDocumentException("Unsupported file type. Only PDF, JPEG, and PNG are allowed");
        }
    }

    public Resource loadFileAsResource(String secureFilename) {
        if (secureFilename != null && (secureFilename.startsWith("http://") || secureFilename.startsWith("https://"))) {
            try {
                Resource resource = new UrlResource(URI.create(secureFilename));
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            } catch (Exception ex) {
                log.warn("Could not load Cloudinary URL as local resource stream: {}", ex.getMessage());
            }
            try {
                return new UrlResource(URI.create(secureFilename));
            } catch (MalformedURLException ex) {
                throw new InvalidDocumentException("Malformed Cloudinary document URL: " + secureFilename, ex);
            }
        }

        try {
            Path filePath = this.storageLocation.resolve(secureFilename).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new InvalidDocumentException("Cannot access file outside secure storage");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new InvalidDocumentException("File not found or not readable: " + secureFilename);
            }
        } catch (MalformedURLException ex) {
            throw new InvalidDocumentException("File path is malformed: " + secureFilename);
        }
    }

    public Path getStorageLocation() {
        return storageLocation;
    }
}
