package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Category;
import com.bricolirent.service.CategoryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("categoryAdminBean")
@ViewScoped
public class CategoryAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CategoryService categoryService;

    private List<Category> categories;
    private Category currentCategory;

    @PostConstruct
    public void init() {
        currentCategory = new Category();
        refreshList();
    }

    private void refreshList() {
        categories = categoryService.getAllCategories();
    }

    public void prepareNew() {
        currentCategory = new Category();
    }

    public void save() {
        try {
            if (currentCategory.getId() == null) {
                categoryService.saveCategory(currentCategory);
                addMessage(FacesMessage.SEVERITY_INFO, "Succès", "Catégorie ajoutée.");
            } else {
                categoryService.updateCategory(currentCategory);
                addMessage(FacesMessage.SEVERITY_INFO, "Succès", "Catégorie mise à jour.");
            }
            refreshList();
            prepareNew();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'opération a échoué.");
        }
    }

    public void edit(Category category) {
        this.currentCategory = category;
    }

    public void delete(Category category) {
        try {
            categoryService.deleteCategory(category);
            refreshList();
            addMessage(FacesMessage.SEVERITY_INFO, "Succès", "Catégorie supprimée.");
        } catch (IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Une erreur technique est survenue.");
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public Category getCurrentCategory() { return currentCategory; }
    public void setCurrentCategory(Category currentCategory) { this.currentCategory = currentCategory; }
}
