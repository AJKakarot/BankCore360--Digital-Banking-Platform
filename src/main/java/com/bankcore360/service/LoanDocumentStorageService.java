package com.bankcore360.service;

import com.bankcore360.entity.LoanDocumentType;
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
 * Service managing loan document uploads to Cloudinary cloud storage
 * with automatic fallback to secure private filesystem storage.
 */
@Service
public class LoanDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(LoanDocumentStorageService.class);

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Path storageLocation;
    private final Cloudinary cloudinary;

    public LoanDocumentStorageService(
            @Value("${LOAN_DOCUMENT_STORAGE_DIR:storage/private/loan-documents}") String storageDir,
            @Autowired(required = false) Cloudinary cloudinary
    ) {
        this.cloudinary = cloudinary;
        Path targetPath;
        try {
            targetPath = Paths.get(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(targetPath);
        } catch (Exception ex) {
            log.warn("Could not initialize secure loan document storage directory [{}]: {}. Falling back to system temp.", storageDir, ex.getMessage());
            try {
                targetPath = Paths.get(System.getProperty("java.io.tmpdir"), "bankcore360", "loan-documents").toAbsolutePath().normalize();
                Files.createDirectories(targetPath);
            } catch (Exception innerEx) {
                log.warn("Could not create fallback temp directory: {}. Using root temp.", innerEx.getMessage());
                targetPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
            }
        }
        this.storageLocation = targetPath;
    }

    public String storeFile(MultipartFile file, String applicationReference, LoanDocumentType documentType) {
        validateFile(file);

        // Upload to Cloudinary if client configured
        if (cloudinary != null) {
            try {
                String cleanRef = applicationReference.replace('-', '_');
                String publicId = "loan_" + cleanRef + "_" + documentType.name().toLowerCase() + "_" + UUID.randomUUID();

                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "bankcore360/loan-documents",
                        "resource_type", "auto",
                        "public_id", publicId
                ));

                String secureUrl = (String) uploadResult.get("secure_url");
                if (StringUtils.hasText(secureUrl)) {
                    log.info("Loan document [{}] successfully uploaded to Cloudinary: {}", documentType, secureUrl);
                    return secureUrl;
                }
            } catch (Exception ex) {
                log.warn("Cloudinary upload failed ({}). Falling back to secure local filesystem.", ex.getMessage());
            }
        }

        return storeLocally(file, applicationReference, documentType);
    }

    private String storeLocally(MultipartFile file, String applicationReference, LoanDocumentType documentType) {
        String originalFilename = file.getOriginalFilename();
        String cleanOriginalName = originalFilename != null
                ? StringUtils.cleanPath(originalFilename)
                : "document";

        String extension = "";
        int dotIndex = cleanOriginalName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < cleanOriginalName.length() - 1) {
            extension = cleanOriginalName.substring(dotIndex);
        }

        String secureFilename = "loan_" + applicationReference.replace('-', '_')
                + "_" + documentType.name().toLowerCase()
                + "_" + UUID.randomUUID() + extension;

        Path targetLocation = this.storageLocation.resolve(secureFilename).normalize();
        if (!targetLocation.startsWith(this.storageLocation)) {
            throw new InvalidDocumentException("Invalid path sequence detected in file storage");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to securely store loan document file", ex);
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
            throw new InvalidDocumentException("Unsupported file type. Only PDF, JPEG, and PNG are permitted");
        }
    }

    public Resource loadFileAsResource(String storageReference) {
        if (storageReference != null && (storageReference.startsWith("http://") || storageReference.startsWith("https://"))) {
            try {
                Resource resource = new UrlResource(URI.create(storageReference));
                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            } catch (Exception ex) {
                log.warn("Could not load Cloudinary URL as local resource stream: {}", ex.getMessage());
            }
            try {
                return new UrlResource(URI.create(storageReference));
            } catch (MalformedURLException ex) {
                throw new InvalidDocumentException("Malformed Cloudinary document URL: " + storageReference, ex);
            }
        }

        try {
            Path filePath = this.storageLocation.resolve(storageReference).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new InvalidDocumentException("Cannot access file outside secure storage");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new InvalidDocumentException("File not found or not readable: " + storageReference);
            }
        } catch (MalformedURLException ex) {
            throw new InvalidDocumentException("File path is malformed: " + storageReference, ex);
        }
    }
}
