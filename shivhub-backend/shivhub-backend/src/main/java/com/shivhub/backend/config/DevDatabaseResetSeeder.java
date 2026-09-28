package com.shivhub.backend.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.ProductImage;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ImageType;
import com.shivhub.backend.enums.ProductSource;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.ProductImageRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.SubCategoryRepository;
import com.shivhub.backend.repository.UserRepository;

/*
 * =========================================================
 * DEV DATABASE RESET + TEST DATA SEEDER
 * =========================================================
 *
 * This runner is intentionally OFF by default.
 *
 * To clean the current MySQL database and seed test data:
 *
 *   ./mvnw.cmd spring-boot:run ^
 *     -Dspring-boot.run.arguments="--shivhub.dev.reset-database=true --shivhub.dev.seed-products=true"
 *
 * It creates:
 *   - demo admin / seller / customer users
 *   - all required categories and subcategories
 *   - 100 approved active products in every subcategory
 *
 * Never enable this on production data.
 * =========================================================
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevDatabaseResetSeeder implements ApplicationRunner {

    private static final int PRODUCTS_PER_SUBCATEGORY = 100;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Value("${shivhub.dev.reset-database:false}")
    private boolean resetDatabase;

    @Value("${shivhub.dev.seed-products:false}")
    private boolean seedProducts;

    public DevDatabaseResetSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository) {

        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!resetDatabase && !seedProducts) {
            return;
        }

        if (resetDatabase) {
            truncateAllTablesInCurrentSchema();
        }

        User seller = createUsers();
        createCategories();

        if (seedProducts) {
            seedApprovedProducts(seller);
        }
    }

    private void truncateAllTablesInCurrentSchema() {

        String schemaName =
                jdbcTemplate.queryForObject(
                        "SELECT DATABASE()",
                        String.class
                );

        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalStateException(
                    "Cannot reset database because no current MySQL schema is selected."
            );
        }

        List<String> tableNames =
                jdbcTemplate.queryForList(
                        """
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = ?
                          AND table_type = 'BASE TABLE'
                        """,
                        String.class,
                        schemaName
                );

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        for (String tableName : tableNames) {
            jdbcTemplate.execute(
                    "TRUNCATE TABLE `" + tableName.replace("`", "``") + "`"
            );
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private User createUsers() {

        createUser(
                "ShivHub Admin",
                "shivhub007@gmail.com",
                "9000000001",
                Role.ADMIN,
                "ShivHub Admin"
        );

        User seller =
                createUser(
                        "Demo Seller",
                        "seller@shivhub.test",
                        "9000000002",
                        Role.SELLER,
                        "Demo Mobile Shopee"
                );

        createUser(
                "Demo Customer",
                "customer@shivhub.test",
                "9000000003",
                Role.CUSTOMER,
                null
        );

        return seller;
    }

    private User createUser(
            String name,
            String email,
            String mobile,
            Role role,
            String businessName) {

        return userRepository
                .findByEmail(email)
                .orElseGet(() -> {

                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setMobile(mobile);
                    user.setPassword(passwordEncoder.encode("Test@12345"));
                    user.setRole(role);
                    user.setStatus(UserStatus.APPROVED);
                    user.setEnabled(true);
                    user.setBusinessName(businessName);
                    user.setLegalBusinessName(businessName);
                    user.setBusinessAddress("Demo test address, replace before launch");
                    user.setBusinessCity("Demo City");
                    user.setBusinessState("Maharashtra");
                    user.setBusinessPincode("000000");
                    user.setGstin("");
                    user.setInvoicePrefix(role == Role.SELLER ? "DEMO" : null);

                    return userRepository.save(user);
                });
    }

    private void createCategories() {

        createCategory("Mobiles", List.of("Smartphones", "Feature Phones", "Tablets"));
        createCategory("Electronics", List.of("Smart Watches", "Headphones", "Earbuds", "Mobile Covers", "Tempered Glass", "Chargers", "Power Banks", "Cables", "Bluetooth Speakers"));
        createCategory("Computers", List.of("Laptops", "Desktops", "Monitors", "Keyboard", "Mouse", "Pendrives", "Computer Accessories"));
        createCategory("TVs & Entertainment", List.of("LED TVs", "Smart TVs", "TV Accessories", "Speakers"));
        createCategory("Men's Fashion", List.of("Shirts", "T-Shirts", "Jeans", "Trousers", "Shoes", "Accessories"));
        createCategory("Women's Fashion", List.of("Sarees", "Kurtis", "Dresses", "Tops", "Footwear", "Accessories"));
        createCategory("Kids", List.of("Boys Clothing", "Girls Clothing", "Kids Footwear", "Toys", "School Accessories"));
        createCategory("Home & Essentials", List.of("Kitchen", "Home Appliances", "Cleaning", "Storage", "Lighting", "Home Decor", "Daily Essentials"));
        createCategory("Beauty & Personal Care", List.of("Skincare", "Hair Care", "Grooming", "Personal Care"));
        createCategory("Grocery & Daily Needs", List.of("Food", "Beverages", "Household Items", "Daily Needs"));
        createCategory("Automobile", List.of("Car Accessories", "Bike Accessories", "Car Care", "Other Accessories"));
        createCategory("Offers & Deals", List.of("Today's Deals", "Best Sellers", "New Arrivals"));
    }

    private void createCategory(
            String categoryName,
            List<String> subCategoryNames) {

        Category category =
                categoryRepository
                        .findByNameIgnoreCase(categoryName)
                        .orElseGet(() -> {
                            Category created = new Category();
                            created.setName(categoryName);
                            created.setActive(true);
                            return categoryRepository.save(created);
                        });

        for (String subCategoryName : subCategoryNames) {
            subCategoryRepository
                    .findByNameIgnoreCaseAndCategory(subCategoryName, category)
                    .orElseGet(() -> {
                        SubCategory subCategory = new SubCategory();
                        subCategory.setName(subCategoryName);
                        subCategory.setActive(true);
                        subCategory.setCategory(category);
                        return subCategoryRepository.save(subCategory);
                    });
        }
    }

    private void seedApprovedProducts(User seller) {

        if (!resetDatabase && productRepository.count() > 0) {
            return;
        }

        List<Category> categories = categoryRepository.findByActiveTrue();
        List<Product> batch = new ArrayList<>();

        for (Category category : categories) {
            List<SubCategory> subCategories =
                    subCategoryRepository.findByCategoryAndActiveTrue(category);

            for (SubCategory subCategory : subCategories) {
                for (int index = 1; index <= PRODUCTS_PER_SUBCATEGORY; index++) {
                    batch.add(
                            createProduct(
                                    seller,
                                    category,
                                    subCategory,
                                    index
                            )
                    );

                    if (batch.size() >= 500) {
                        saveProductsWithImages(batch);
                        batch.clear();
                    }
                }
            }
        }

        saveProductsWithImages(batch);
    }

    private Product createProduct(
            User seller,
            Category category,
            SubCategory subCategory,
            int index) {

        Product product = new Product();
        String productType = productTypeFor(category, subCategory);
        String brand = brandFor(index, productType);
        String variant = variantFor(index, productType);

        product.setName(brand + " " + subCategory.getName() + " " + variant);
        product.setBrand(brand);
        product.setModel(subCategory.getName() + " Model " + index);
        product.setModelNumber(category.getName().substring(0, Math.min(3, category.getName().length())).toUpperCase() + "-" + subCategory.getId() + "-" + index);
        product.setRam(productType.equals("NEW_MOBILE") ? ramFor(index) : "");
        product.setStorage(productType.equals("NEW_MOBILE") ? storageFor(index) : "");
        product.setColorOptions(colorFor(index));
        product.setHsnCode(hsnFor(productType));
        product.setGstRate(gstFor(productType));
        product.setSellingPriceIncludesGst(true);
        product.setSellerSku("DEMO-" + category.getId() + "-" + subCategory.getId() + "-" + index);
        product.setProductType(productType);
        product.setPhysicalCondition("NEW");
        product.setPurchaseTaxTreatment("REGULAR_GST");
        product.setSaleTaxTreatment("REGULAR_GST");
        product.setAccessoryType(productType.equals("ACCESSORY") ? subCategory.getName() : null);
        product.setCompatibility(productType.equals("ACCESSORY") ? "Universal / demo compatible models" : null);
        product.setWarrantyDetails("Demo warranty details for testing only");
        product.setPackageContents("Main unit, demo box contents");
        product.setTaxTreatmentBasis("Demo regular GST test product");
        product.setReorderThreshold(5);
        product.setSerialTrackingRequired(productType.equals("ACCESSORY") && index % 5 == 0);
        product.setSpecificationDetails(specificationFor(productType, category, subCategory, index));
        product.setShortHighlights("Demo product for testing\nApproved catalogue item\nGST-inclusive price");
        product.setDescription("Demo test product generated for ShivHub catalogue testing. Replace before production.");
        product.setPrice(priceFor(category, index));
        product.setOfferPercentage(index % 10 == 0 ? BigDecimal.valueOf(5) : BigDecimal.ZERO);
        product.setStock(20 + (index % 30));
        product.setReservedStock(0);
        product.setCategory(category.getName());
        product.setCategoryEntity(category);
        product.setSubCategory(subCategory);
        product.setImageUrl(imageUrl(category, subCategory, index, 1));
        product.setActive(true);
        product.setApprovalStatus(ProductStatus.APPROVED);
        product.setSource(ProductSource.SELLER);
        product.setAdminReview("Demo seeded approved product");
        product.setSeller(seller);

        return product;
    }

    private void saveProductsWithImages(List<Product> products) {

        if (products.isEmpty()) {
            return;
        }

        List<Product> savedProducts = productRepository.saveAll(products);
        List<ProductImage> images = new ArrayList<>();

        for (Product product : savedProducts) {
            for (int order = 1; order <= 5; order++) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageType(ImageType.PRODUCT);
                image.setDisplayOrder(order);
                image.setImageUrl(imageUrl(
                        product.getCategoryEntity(),
                        product.getSubCategory(),
                        product.getId().intValue(),
                        order
                ));
                images.add(image);
            }
        }

        productImageRepository.saveAll(images);
    }

    private String productTypeFor(Category category, SubCategory subCategory) {

        String categoryName = category.getName().toLowerCase();
        String subCategoryName = subCategory.getName().toLowerCase();

        if (categoryName.contains("mobile") || subCategoryName.contains("phone") || subCategoryName.contains("tablet")) {
            return "NEW_MOBILE";
        }

        if (categoryName.contains("electronics") || subCategoryName.contains("accessor") || subCategoryName.contains("charger") || subCategoryName.contains("cable") || subCategoryName.contains("cover") || subCategoryName.contains("glass")) {
            return "ACCESSORY";
        }

        return "GENERAL";
    }

    private String brandFor(int index, String productType) {

        List<String> mobileBrands = List.of("Samsung", "Apple", "Vivo", "Oppo", "Realme", "OnePlus", "Motorola", "Xiaomi");
        List<String> accessoryBrands = List.of("Boat", "Portronics", "Ambrane", "Samsung", "Mi", "Zebronics", "Anker", "Noise");
        List<String> generalBrands = List.of("ShivHub", "UrbanPro", "Nova", "Prime", "DailyPro", "MaxStyle", "HomeEase", "ValueMart");

        List<String> brands =
                productType.equals("NEW_MOBILE")
                        ? mobileBrands
                        : productType.equals("ACCESSORY")
                                ? accessoryBrands
                                : generalBrands;

        return brands.get(index % brands.size());
    }

    private String variantFor(int index, String productType) {

        if (productType.equals("NEW_MOBILE")) {
            return ramFor(index) + " " + storageFor(index) + " " + colorFor(index);
        }

        return "Demo Variant " + String.format("%03d", index);
    }

    private String ramFor(int index) {
        return List.of("4 GB", "6 GB", "8 GB", "12 GB").get(index % 4);
    }

    private String storageFor(int index) {
        return List.of("64 GB", "128 GB", "256 GB", "512 GB").get(index % 4);
    }

    private String colorFor(int index) {
        return List.of("Midnight Black", "Glory White", "Sky Blue", "Sunset Gold", "Forest Green").get(index % 5);
    }

    private String hsnFor(String productType) {
        if (productType.equals("NEW_MOBILE")) {
            return "85171300";
        }
        if (productType.equals("ACCESSORY")) {
            return "85177990";
        }
        return "999999";
    }

    private BigDecimal gstFor(String productType) {
        return productType.equals("GENERAL") ? BigDecimal.valueOf(12) : BigDecimal.valueOf(18);
    }

    private BigDecimal priceFor(Category category, int index) {

        String name = category.getName().toLowerCase();
        int base =
                name.contains("mobile")
                        ? 7999
                        : name.contains("computer")
                                ? 14999
                                : name.contains("tv")
                                        ? 11999
                                        : name.contains("fashion")
                                                ? 499
                                                : 299;

        return BigDecimal.valueOf(base + (index * 137L));
    }

    private String specificationFor(
            String productType,
            Category category,
            SubCategory subCategory,
            int index) {

        if (productType.equals("NEW_MOBILE")) {
            return """
                    Display: 6.5 inch FHD+ demo panel
                    Processor: Demo chipset %d
                    RAM: %s
                    Storage: %s
                    Rear camera: Demo dual camera
                    Front camera: Demo selfie camera
                    Battery: 5000 mAh
                    Connectivity: 4G/5G demo support
                    """.formatted(index, ramFor(index), storageFor(index));
        }

        if (productType.equals("ACCESSORY")) {
            return """
                    Accessory category: %s
                    Compatibility: Universal demo compatibility
                    Material/spec: Demo specification set %d
                    Warranty: Demo warranty only
                    """.formatted(subCategory.getName(), index);
        }

        return """
                Category: %s
                Subcategory: %s
                Demo specification set: %d
                """.formatted(category.getName(), subCategory.getName(), index);
    }

    private String imageUrl(
            Category category,
            SubCategory subCategory,
            int index,
            int order) {

        String text =
                (subCategory.getName() + " " + index + "-" + order)
                        .replace(" ", "+")
                        .replace("&", "and");

        String color =
                switch (order) {
                    case 1 -> "0B1220";
                    case 2 -> "1D4ED8";
                    case 3 -> "FF8A1F";
                    case 4 -> "0E947B";
                    default -> "64748B";
                };

        return "https://placehold.co/900x900/" + color + "/FFFFFF/png?text=" + text;
    }
}
