package com.shivhub.backend.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.ProductStatus;
import com.shivhub.backend.enums.Role;
import com.shivhub.backend.enums.UserStatus;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.UserRepository;

/**
 * Adds a small approved catalogue for every customer-dashboard category.
 * It is idempotent: restarting the backend never creates duplicate products.
 */
@Configuration
public class CustomerCatalogueSeeder {

    @Bean
    CommandLineRunner seedCustomerCatalogue(
            UserRepository users,
            ProductRepository products,
            @Value("${shivhub.demo.catalogue-seed:false}") boolean seedDemoCatalogue) {
        return args -> {
            if (!seedDemoCatalogue) return;
            List<User> sellers = users.findByRoleAndStatus(Role.SELLER, UserStatus.APPROVED);
            if (sellers.isEmpty()) return; // A product must always belong to a real approved seller.
            User seller = sellers.get(0);

            List<CatalogueItem> catalogue = List.of(
                new CatalogueItem("Nova X5 Smartphone", "Mobiles", "Fast 5G smartphone with a vibrant edge-to-edge display.", "24999", 18, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("AeroBook Pro 14", "Laptops", "Slim everyday laptop built for work, study and streaming.", "58999", 12, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("Vision 4K Smart TV", "Televisions", "Cinematic 4K display with smart entertainment built in.", "42999", 8, "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("Pulse Wireless Headphones", "Audio", "Comfortable wireless audio with rich sound and long battery life.", "3499", 35, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("Orbit Smart Watch", "Smart Watches", "Fitness, calls and notifications on your wrist.", "4999", 24, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("Focus Mini Camera", "Cameras", "Compact camera for crisp everyday memories.", "18999", 10, "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("PowerHub USB-C Kit", "Accessories", "Essential charging and connectivity kit for every desk.", "1299", 60, "https://images.unsplash.com/photo-1587033411391-5d9e51cce126?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("BrewMate Coffee Maker", "Home Appliances", "Simple morning coffee with a compact modern design.", "6499", 15, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80"),
                new CatalogueItem("GlowDesk LED Light", "Electronics", "Adjustable LED lighting for your study or workspace.", "2199", 40, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80")
            );

            for (CatalogueItem item : catalogue) {
                boolean exists = products.findAll().stream().anyMatch(product -> product.getName().equalsIgnoreCase(item.name));
                if (exists) continue;
                Product product = new Product();
                product.setName(item.name);
                product.setCategory(item.category);
                product.setDescription(item.description);
                product.setPrice(new BigDecimal(item.price));
                product.setStock(item.stock);
                product.setImageUrl(item.imageUrl);
                product.setSeller(seller);
                product.setActive(true);
                product.setApprovalStatus(ProductStatus.APPROVED);
                product.setAdminReview("Approved catalogue item.");
                product.setReviewedAt(LocalDateTime.now());
                products.save(product);
            }
        };
    }

    private record CatalogueItem(String name, String category, String description, String price, int stock, String imageUrl) { }
}
