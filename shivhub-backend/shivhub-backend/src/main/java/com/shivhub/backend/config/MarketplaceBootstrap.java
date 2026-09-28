package com.shivhub.backend.config;

import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;

/** Idempotent starter catalogue; it only adds missing records and never changes customer listings. */
@Configuration
public class MarketplaceBootstrap {
 @Bean CommandLineRunner seedMarketplace(MarketplaceCategoryRepository categories, MarketplaceSubcategoryRepository subcategories) {
  return args -> {
   Map<String,List<String>> starter = new LinkedHashMap<>();
   starter.put("Mobiles", List.of("Smartphones", "Feature Phones", "Mobile Accessories"));
   starter.put("Bikes", List.of("Motorcycles", "Scooters", "Bicycles"));
   starter.put("Cars", List.of("Cars", "Commercial Vehicles"));
   starter.put("Tractors", List.of("Tractors", "Attachments"));
   starter.put("Cows & Animals", List.of("Cows", "Buffaloes", "Goats & Sheep", "Poultry"));
   starter.put("Farm Produce & Vegetables", List.of("Vegetables", "Fruits", "Grains & Pulses", "Seeds & Fertilizer"));
   starter.put("Farm Equipment", List.of("Irrigation", "Farm Tools", "Implements"));
   starter.put("Electronics", List.of("TVs", "Computers", "Home Appliances"));
   starter.put("Furniture", List.of("Living Room", "Bedroom", "Office Furniture"));
   starter.put("Room & Property Rent", List.of("Room Rent", "House Rent", "Land & Shop"));
   starter.put("Other", List.of());
   for (Map.Entry<String,List<String>> entry : starter.entrySet()) {
    MarketplaceCategory category = categories.findAll().stream().filter(row -> row.getName().equalsIgnoreCase(entry.getKey())).findFirst().orElseGet(() -> { MarketplaceCategory row = new MarketplaceCategory(); row.setName(entry.getKey()); row.setMaxListingDays(10); return categories.save(row); });
    Set<String> existing = new HashSet<>();
    subcategories.findByCategoryIdAndActiveTrueOrderByNameAsc(category.getId()).forEach(row -> existing.add(row.getName().toLowerCase()));
    for (String name : entry.getValue()) if (!existing.contains(name.toLowerCase())) { MarketplaceSubcategory row = new MarketplaceSubcategory(); row.setCategory(category); row.setName(name); subcategories.save(row); }
   }
  };
 }
}
