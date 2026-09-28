package com.shivhub.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;


/*
 * =========================================================
 * FILE STORAGE SERVICE
 * =========================================================
 *
 * Handles:
 *
 * 1. Product image upload
 * 2. Purchase / distributor invoice upload
 *
 * Files are stored on server.
 * Database stores only the file path.
 *
 * =========================================================
 */

@Service
public class FileStorageService {


    /*
     * =========================================================
     * PRODUCT UPLOAD DIRECTORY
     * =========================================================
     */

    private final Path productUploadDirectory =
            Paths.get("uploads/products");


    /*
     * =========================================================
     * PURCHASE INVOICE DIRECTORY
     * =========================================================
     */

    private final Path purchaseUploadDirectory =
            Paths.get("uploads/purchases");

    private final Path profileUploadDirectory =
            Paths.get("uploads/profile");

    private final Path campaignUploadDirectory =
            Paths.get("uploads/campaigns");

    private final Path afterSalesUploadDirectory = Paths.get("uploads/after-sales");
    private final Path marketplaceUploadDirectory = Paths.get("uploads/marketplace");


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public FileStorageService() {

        try {

            Files.createDirectories(
                    productUploadDirectory
            );

            Files.createDirectories(
                    purchaseUploadDirectory
            );

            Files.createDirectories(
                    profileUploadDirectory
            );

            Files.createDirectories(
                    campaignUploadDirectory
            );
            Files.createDirectories(afterSalesUploadDirectory);
            Files.createDirectories(marketplaceUploadDirectory);

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Could not create upload directories",
                    exception
            );
        }
    }


    /*
     * =========================================================
     * STORE PRODUCT IMAGE
     * =========================================================
     *
     * Example:
     *
     * uploads/products/17/uuid.png
     *
     * =========================================================
     */

    public String storeProductImage(
            Long productId,
            MultipartFile file) {


        /*
         * Validate file.
         */

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Uploaded image is empty"
            );
        }


        /*
         * Product-specific directory.
         */

        Path productDirectory =
                productUploadDirectory.resolve(
                        String.valueOf(productId)
                );


        try {

            Files.createDirectories(
                    productDirectory
            );


            /*
             * Get original filename.
             */

            String originalFilename =
                    file.getOriginalFilename();


            String extension = "";


            /*
             * Extract extension.
             */

            if (originalFilename != null
                    && originalFilename.contains(".")) {

                extension =
                        originalFilename.substring(
                                originalFilename.lastIndexOf(".")
                        ).toLowerCase();
            }


            /*
             * Validate image type.
             */

            if (!extension.equals(".jpg")
                    && !extension.equals(".jpeg")
                    && !extension.equals(".png")
                    && !extension.equals(".webp")) {

                throw new RuntimeException(
                        "Only JPG, JPEG, PNG and WEBP images are allowed"
                );
            }


            /*
             * Generate unique filename.
             */

            String uniqueFilename =
                    UUID.randomUUID()
                            .toString()
                    + extension;


            /*
             * Final path.
             */

            Path targetPath =
                    productDirectory.resolve(
                            uniqueFilename
                    );


            /*
             * Save file.
             */

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            /*
             * Return database path.
             */

            return targetPath
                    .toString()
                    .replace("\\", "/");


        } catch (IOException exception) {

            throw new RuntimeException(
                    "Could not store product image",
                    exception
            );
        }
    }


    /*
     * =========================================================
     * STORE PURCHASE INVOICE
     * =========================================================
     *
     * Stores original distributor bill.
     *
     * Example:
     *
     * uploads/purchases/4/uuid.pdf
     *
     * Supported:
     *
     * PDF
     * JPG
     * JPEG
     * PNG
     *
     * =========================================================
     */

    public String storePurchaseInvoice(
            Long purchaseId,
            MultipartFile file) {


        /*
         * =====================================================
         * VALIDATE FILE
         * =====================================================
         */

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Purchase invoice file is empty"
            );
        }


        /*
         * =====================================================
         * GET ORIGINAL FILENAME
         * =====================================================
         */

        String originalFilename =
                file.getOriginalFilename();


        String extension = "";


        /*
         * =====================================================
         * EXTRACT EXTENSION
         * =====================================================
         */

        if (originalFilename != null
                && originalFilename.contains(".")) {

            extension =
                    originalFilename.substring(
                            originalFilename.lastIndexOf(".")
                    ).toLowerCase();
        }


        /*
         * =====================================================
         * VALIDATE FILE TYPE
         * =====================================================
         */

        if (!extension.equals(".pdf")
                && !extension.equals(".jpg")
                && !extension.equals(".jpeg")
                && !extension.equals(".png")) {

            throw new RuntimeException(
                    "Only PDF, JPG, JPEG and PNG invoice files are allowed"
            );
        }


        /*
         * =====================================================
         * PURCHASE-SPECIFIC DIRECTORY
         * =====================================================
         *
         * Example:
         *
         * uploads/purchases/4/
         *
         * =====================================================
         */

        Path purchaseDirectory =
                purchaseUploadDirectory.resolve(
                        String.valueOf(purchaseId)
                );


        try {

            /*
             * Create directory.
             */

            Files.createDirectories(
                    purchaseDirectory
            );


            /*
             * =================================================
             * GENERATE UNIQUE FILE NAME
             * =================================================
             */

            String uniqueFilename =
                    UUID.randomUUID()
                            .toString()
                    + extension;


            /*
             * =================================================
             * TARGET PATH
             * =================================================
             */

            Path targetPath =
                    purchaseDirectory.resolve(
                            uniqueFilename
                    );


            /*
             * =================================================
             * SAVE FILE
             * =================================================
             */

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            /*
             * =================================================
             * RETURN RELATIVE PATH
             * =================================================
             *
             * Example:
             *
             * uploads/purchases/4/
             * abc123.pdf
             *
             * =================================================
             */

            return targetPath
                    .toString()
                    .replace("\\", "/");


        } catch (IOException exception) {

            throw new RuntimeException(
                    "Could not store purchase invoice",
                    exception
            );
        }
    }

    public String storeCustomerProfilePhoto(Long customerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Profile photo file is empty");
        }

        if (file.getSize() > 2L * 1024L * 1024L) {
            throw new RuntimeException("Profile photo must be 2 MB or smaller");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!extension.equals(".jpg")
                && !extension.equals(".jpeg")
                && !extension.equals(".png")
                && !extension.equals(".webp")) {
            throw new RuntimeException("Only JPG, JPEG, PNG and WEBP profile photos are allowed");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new RuntimeException("Profile photo must be an image file");
        }

        Path customerDirectory = profileUploadDirectory.resolve(String.valueOf(customerId));
        try {
            Files.createDirectories(customerDirectory);
            String uniqueFilename = UUID.randomUUID() + extension;
            Path targetPath = customerDirectory.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/" + targetPath.toString().replace("\\", "/");
        } catch (IOException exception) {
            throw new RuntimeException("Could not store profile photo", exception);
        }
    }

    /** Stores an administrator-provided campaign banner and returns its public URL. */
    public String storeCampaignBanner(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Campaign banner file is empty");
        }
        if (file.getSize() > 5L * 1024L * 1024L) {
            throw new RuntimeException("Campaign banner must be 5 MB or smaller");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!extension.equals(".jpg") && !extension.equals(".jpeg")
                && !extension.equals(".png") && !extension.equals(".webp")) {
            throw new RuntimeException("Only JPG, JPEG, PNG and WEBP campaign banners are allowed");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new RuntimeException("Campaign banner must be an image file");
        }

        try {
            Files.createDirectories(campaignUploadDirectory);
            Path targetPath = campaignUploadDirectory.resolve(UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/" + targetPath.toString().replace("\\", "/");
        } catch (IOException exception) {
            throw new RuntimeException("Could not store campaign banner", exception);
        }
    }

    /** Evidence is type/size validated before it becomes visible through a protected after-sales request. */
    public String storeAfterSalesAttachment(Long requestId, MultipartFile file) {
        if (requestId == null || file == null || file.isEmpty()) throw new IllegalArgumentException("After-sales evidence file is required");
        if (file.getSize() > 25L * 1024L * 1024L) throw new IllegalArgumentException("Evidence file must be 25 MB or smaller");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.lastIndexOf('.') < 0 ? "" : original.substring(original.lastIndexOf('.')).toLowerCase();
        boolean allowed = extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png") || extension.equals(".webp")
                || extension.equals(".pdf") || extension.equals(".mp4") || extension.equals(".mov");
        if (!allowed) throw new IllegalArgumentException("Only JPG, PNG, WEBP, PDF, MP4 and MOV evidence files are allowed");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!(contentType.startsWith("image/") || contentType.equals("application/pdf") || contentType.startsWith("video/"))) throw new IllegalArgumentException("Evidence file type is invalid");
        try {
            Path directory = afterSalesUploadDirectory.resolve(String.valueOf(requestId)); Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + extension); Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/" + target.toString().replace("\\", "/");
        } catch (IOException exception) { throw new RuntimeException("Could not store after-sales evidence", exception); }
    }

    public Resource loadAfterSalesAttachment(String storedPath) {
        if (storedPath == null || !storedPath.replace("\\", "/").startsWith("/uploads/after-sales/")) throw new IllegalArgumentException("Invalid after-sales attachment path");
        Path root = afterSalesUploadDirectory.toAbsolutePath().normalize();
        Path target = Paths.get(storedPath.substring(1)).toAbsolutePath().normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) throw new IllegalArgumentException("After-sales attachment not found");
        return new FileSystemResource(target);
    }

    /** Stores a customer-owned photo before a marketplace listing is submitted. */
    public String storeMarketplaceImage(Long customerId, MultipartFile file) {
        if (customerId == null || file == null || file.isEmpty()) throw new IllegalArgumentException("Marketplace image is required");
        if (file.getSize() > 5L * 1024L * 1024L) throw new IllegalArgumentException("Marketplace image must be 5 MB or smaller");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.lastIndexOf('.') < 0 ? "" : original.substring(original.lastIndexOf('.')).toLowerCase();
        if (!(extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png") || extension.equals(".webp"))) throw new IllegalArgumentException("Only JPG, JPEG, PNG and WEBP marketplace images are allowed");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!type.startsWith("image/")) throw new IllegalArgumentException("Marketplace upload must be an image");
        try {
            Path directory = marketplaceUploadDirectory.resolve(String.valueOf(customerId));
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/" + target.toString().replace("\\", "/");
        } catch (IOException exception) { throw new RuntimeException("Could not store marketplace image", exception); }
    }
}
