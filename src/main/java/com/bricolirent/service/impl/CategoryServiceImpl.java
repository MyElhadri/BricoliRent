package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Category;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.repository.CategoryRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;
    private ToolRepository toolRepository;

    @PostConstruct
    public void init() {
        this.categoryRepository = new CategoryRepository();
        this.toolRepository = new ToolRepository();
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public void saveCategory(Category category) {
        categoryRepository.save(category);
    }

    @Override
    public void updateCategory(Category category) {
        categoryRepository.update(category);
    }

    @Override
    public void deleteCategory(Category category) {
        // Validation avant suppression
        List<Tool> linkedTools = toolRepository.findByCategory(category.getName());
        if (linkedTools != null && !linkedTools.isEmpty()) {
            throw new IllegalStateException("Vous ne pouvez pas la supprimer, supprimez d'abord tout outil de cette catégorie");
        }
        
        categoryRepository.delete(category);
    }
}
