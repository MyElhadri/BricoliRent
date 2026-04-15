package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Category;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.service.CategoryService;
import com.bricolirent.service.ToolService;
import com.bricolirent.service.ToolValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("toolAdminBean")
@ViewScoped
public class ToolAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ToolService toolService;

    @Inject
    private CategoryService categoryService;

    private List<Tool> tools;
    private List<Category> categories;
    private Tool currentTool;
    private Long selectedCategoryId;

    @PostConstruct
    public void init() {
        prepareNew();
        refreshLists();
    }

    private void refreshLists() {
        tools = toolService.getAllTools();
        categories = categoryService.getAllCategories();
    }

    public void prepareNew() {
        currentTool = new Tool();
        currentTool.setActive(true);
        selectedCategoryId = null;
    }

    public void save() {
        try {
            if (selectedCategoryId != null) {
                Category cat = categoryService.getCategoryById(selectedCategoryId);
                currentTool.setCategory(cat);
            }

            if (currentTool.getCategory() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Veuillez selectionner une categorie valide.");
                return;
            }

            boolean exists = toolService.existsByNameAndCategory(
                    currentTool.getName(),
                    selectedCategoryId,
                    currentTool.getId()
            );
            if (exists) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Un outil avec le meme nom existe deja dans cette categorie.");
                return;
            }

            if (currentTool.getTotalQuantity() != null && currentTool.getAvailableQuantity() == null) {
                currentTool.setAvailableQuantity(currentTool.getTotalQuantity());
            }

            if (currentTool.getId() == null) {
                toolService.saveTool(currentTool);
                addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Outil ajoute.");
            } else {
                toolService.updateTool(currentTool);
                addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Outil mis a jour.");
            }

            refreshLists();
            prepareNew();
        } catch (ToolValidationException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "L'operation a echoue.");
        }
    }

    public void edit(Tool tool) {
        this.currentTool = copyTool(tool);
        if (tool.getCategory() != null) {
            this.selectedCategoryId = tool.getCategory().getId();
        }
    }

    public void toggleActive(Tool tool) {
        try {
            tool.setActive(!tool.getActive());
            toolService.updateTool(tool);
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Statut de l'outil modifie avec succes.");
            refreshLists();
        } catch (ToolValidationException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Impossible de changer le statut.");
        }
    }

    public void delete(Tool tool) {
        try {
            toolService.deleteTool(tool);

            if (currentTool != null && currentTool.getId() != null && currentTool.getId().equals(tool.getId())) {
                prepareNew();
            }

            refreshLists();
            addMessage(FacesMessage.SEVERITY_INFO, "Succes", "Outil supprime avec succes.");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur", "Impossible de supprimer l'outil.");
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    private Tool copyTool(Tool source) {
        Tool copy = new Tool();
        copy.setId(source.getId());
        copy.setCategory(source.getCategory());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setPricePerDay(source.getPricePerDay());
        copy.setDepositAmount(source.getDepositAmount());
        copy.setTotalQuantity(source.getTotalQuantity());
        copy.setAvailableQuantity(source.getAvailableQuantity());
        copy.setActive(source.getActive());
        return copy;
    }

    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }

    public Tool getCurrentTool() { return currentTool; }
    public void setCurrentTool(Tool currentTool) { this.currentTool = currentTool; }

    public Long getSelectedCategoryId() { return selectedCategoryId; }
    public void setSelectedCategoryId(Long selectedCategoryId) { this.selectedCategoryId = selectedCategoryId; }
}
