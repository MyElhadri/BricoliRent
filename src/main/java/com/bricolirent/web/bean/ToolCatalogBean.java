package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Category;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.service.CategoryService;
import com.bricolirent.service.ToolService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("toolCatalogBean")
@ViewScoped
public class ToolCatalogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ToolService toolService;

    @Inject
    private CategoryService categoryService;

    private List<Tool> tools;
    private List<Category> categories;
    private String keyword;
    private Long selectedCategoryId;

    @PostConstruct
    public void init() {
        categories = categoryService.getAllCategories();
        search();
    }

    public void search() {
        tools = toolService.getCatalogTools(keyword, selectedCategoryId);
    }

    public void resetFilters() {
        keyword = null;
        selectedCategoryId = null;
        search();
    }

    public boolean isDisponible(Tool tool) {
        return tool != null
                && Boolean.TRUE.equals(tool.getActive())
                && tool.getAvailableQuantity() != null
                && tool.getAvailableQuantity() > 0;
    }

    public boolean disponible(Tool tool) {
        return isDisponible(tool);
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
}
