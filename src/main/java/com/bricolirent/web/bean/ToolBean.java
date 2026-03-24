package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Tool;
import com.bricolirent.service.ToolService;
import com.bricolirent.service.impl.ToolServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean for tool catalog operations.
 */
@Named("toolBean")
@RequestScoped
public class ToolBean implements Serializable {

    private List<Tool> tools;
    private Tool selectedTool;

    private final ToolService toolService = new ToolServiceImpl();

    @PostConstruct
    public void init() {
        tools = toolService.getAvailableTools();
    }

    // ==================== Getters & Setters ====================

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public Tool getSelectedTool() {
        return selectedTool;
    }

    public void setSelectedTool(Tool selectedTool) {
        this.selectedTool = selectedTool;
    }
}
