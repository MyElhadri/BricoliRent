package com.bricolirent.service;

import com.bricolirent.domain.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(Long id);
    void saveCategory(Category category);
    void updateCategory(Category category);
    void deleteCategory(Category category);
}
