package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Category;
import com.bricolirent.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean for category operations.
 */
@Named("categoryBean")
@RequestScoped
public class CategoryBean implements Serializable {

    private List<Category> categories;
    private Category selectedCategory;

    private final CategoryRepository categoryRepository = new CategoryRepository();

    @PostConstruct
    public void init() {
        categories = categoryRepository.findAll();
    }

    // ==================== Getters & Setters ====================

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Category selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
}
