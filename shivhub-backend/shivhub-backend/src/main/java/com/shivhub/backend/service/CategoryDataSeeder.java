package com.shivhub.backend.service;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.entity.Category;
import com.shivhub.backend.entity.SubCategory;
import com.shivhub.backend.repository.CategoryRepository;
import com.shivhub.backend.repository.SubCategoryRepository;

@Component
public class CategoryDataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private final SubCategoryRepository subCategoryRepository;

    public CategoryDataSeeder(
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository) {

        this.categoryRepository = categoryRepository;

        this.subCategoryRepository =
                subCategoryRepository;
    }


    @Override
    @Transactional
    public void run(String... args) {

        createCategory(
                "Mobiles",
                List.of(
                        "Smartphones",
                        "Feature Phones",
                        "Tablets"
                )
        );


        createCategory(
                "Electronics",
                List.of(
                        "Smart Watches",
                        "Headphones",
                        "Earbuds",
                        "Mobile Covers",
                        "Tempered Glass",
                        "Chargers",
                        "Power Banks",
                        "Cables",
                        "Bluetooth Speakers"
                )
        );


        createCategory(
                "Computers",
                List.of(
                        "Laptops",
                        "Desktops",
                        "Monitors",
                        "Keyboard",
                        "Mouse",
                        "Pendrives",
                        "Computer Accessories"
                )
        );


        createCategory(
                "TVs & Entertainment",
                List.of(
                        "LED TVs",
                        "Smart TVs",
                        "TV Accessories",
                        "Speakers"
                )
        );


        createCategory(
                "Men's Fashion",
                List.of(
                        "Shirts",
                        "T-Shirts",
                        "Jeans",
                        "Trousers",
                        "Shoes",
                        "Accessories"
                )
        );


        createCategory(
                "Women's Fashion",
                List.of(
                        "Sarees",
                        "Kurtis",
                        "Dresses",
                        "Tops",
                        "Footwear",
                        "Accessories"
                )
        );


        createCategory(
                "Kids",
                List.of(
                        "Boys Clothing",
                        "Girls Clothing",
                        "Kids Footwear",
                        "Toys",
                        "School Accessories"
                )
        );


        createCategory(
                "Home & Essentials",
                List.of(
                        "Kitchen",
                        "Home Appliances",
                        "Cleaning",
                        "Storage",
                        "Lighting",
                        "Home Decor",
                        "Daily Essentials"
                )
        );


        createCategory(
                "Beauty & Personal Care",
                List.of(
                        "Skincare",
                        "Hair Care",
                        "Grooming",
                        "Personal Care"
                )
        );


        createCategory(
                "Grocery & Daily Needs",
                List.of(
                        "Food",
                        "Beverages",
                        "Household Items",
                        "Daily Needs"
                )
        );


        createCategory(
                "Automobile",
                List.of(
                        "Car Accessories",
                        "Bike Accessories",
                        "Car Care",
                        "Other Accessories"
                )
        );


        createCategory(
                "Offers & Deals",
                List.of(
                        "Today's Deals",
                        "Best Sellers",
                        "New Arrivals"
                )
        );
    }


    private void createCategory(
            String categoryName,
            List<String> subCategoryNames) {


        Category category =
                categoryRepository
                        .findByNameIgnoreCase(categoryName)
                        .orElseGet(() -> {

                            Category newCategory =
                                    new Category();

                            newCategory.setName(
                                    categoryName
                            );

                            newCategory.setActive(true);

                            return categoryRepository
                                    .save(newCategory);
                        });


        for (String subCategoryName :
                subCategoryNames) {


            if (
                subCategoryRepository
                    .findByNameIgnoreCaseAndCategory(
                            subCategoryName,
                            category
                    )
                    .isEmpty()
            ) {

                SubCategory subCategory =
                        new SubCategory();

                subCategory.setName(
                        subCategoryName
                );

                subCategory.setActive(true);

                subCategory.setCategory(
                        category
                );

                subCategoryRepository.save(
                        subCategory
                );
            }
        }
    }
}