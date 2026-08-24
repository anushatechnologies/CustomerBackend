package com.example.project;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.repository.CategoryRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SubcategoryRepository;

@SpringBootApplication
public class CustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }

    @Bean
    CommandLineRunner seedCatalog(CategoryRepository categoryRepository,
                                   SubcategoryRepository subcategoryRepository,
                                   ProductRepository productRepository) {
        return args -> {
            List<Category> categories = seedCategories(categoryRepository);
            List<Subcategory> subcategories = seedSubcategories(subcategoryRepository, categories);
            seedProducts(productRepository, subcategories);
        };
    }

    private List<Category> seedCategories(CategoryRepository repository) {
        List<Category> categories = repository.findAll();
        if (!categories.isEmpty()) {
            return categories;
        }

        return repository.saveAll(List.of(
                category("Fruits", "fruits", "https://example.com/images/fruits.jpg", 1),
                category("Vegetables", "vegetables", "https://example.com/images/vegetables.jpg", 2),
                category("Dairy", "dairy", "https://example.com/images/dairy.jpg", 3),
                category("Bakery", "bakery", "https://example.com/images/bakery.jpg", 4),
                category("Beverages", "beverages", "https://example.com/images/beverages.jpg", 5)
        ));
    }

    private List<Subcategory> seedSubcategories(SubcategoryRepository repository,
                                                List<Category> categories) {
        List<Subcategory> subcategories = repository.findAll();
        if (!subcategories.isEmpty()) {
            return subcategories;
        }

        return repository.saveAll(List.of(
                subcategory(categories.get(0), "Apples", "apples", 1),
                subcategory(categories.get(1), "Leafy Greens", "leafy-greens", 2),
                subcategory(categories.get(2), "Milk", "milk", 3),
                subcategory(categories.get(3), "Bread", "bread", 4),
                subcategory(categories.get(4), "Juices", "juices", 5)
        ));
    }

    private void seedProducts(ProductRepository repository,
                              List<Subcategory> subcategories) {
        if (!repository.findAll().isEmpty()) {
            return;
        }

        repository.saveAll(List.of(
                product(subcategories.get(0), "Red Apples", "Fresh red apples", "3.99", 50, "kg"),
                product(subcategories.get(1), "Spinach", "Fresh baby spinach", "2.49", 30, "bunch"),
                product(subcategories.get(2), "Whole Milk", "Pasteurized whole milk", "1.89", 25, "liter"),
                product(subcategories.get(3), "Wheat Bread", "Freshly baked wheat bread", "2.99", 20, "loaf"),
                product(subcategories.get(4), "Orange Juice", "Natural orange juice", "3.49", 40, "liter")
        ));
    }

    private Category category(String name, String slug, String imageUrl, int sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setImageUrl(imageUrl);
        category.setActive(true);
        category.setSortOrder(sortOrder);
        return category;
    }

    private Subcategory subcategory(Category category, String name, String slug, int sortOrder) {
        Subcategory subcategory = new Subcategory();
        subcategory.setCategory(category);
        subcategory.setName(name);
        subcategory.setSlug(slug);
        subcategory.setImageUrl("https://example.com/images/" + slug + ".jpg");
        subcategory.setActive(true);
        subcategory.setSortOrder(sortOrder);
        return subcategory;
    }

    private Product product(Subcategory subcategory, String title, String description,
                            String price, int stockQty, String unit) {
        Product product = new Product();
        product.setSubcategory(subcategory);
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQty(stockQty);
        product.setUnit(unit);
        product.setImageUrl("https://example.com/images/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        product.setActive(true);
        return product;
    }
}
