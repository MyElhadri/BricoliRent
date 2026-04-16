package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Tool;
import com.bricolirent.service.ToolService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("toolDetailsBean")
@ViewScoped
public class ToolDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ToolService toolService;

    private Long id;
    private Tool tool;

    public void init() {
        if (id != null) {
            tool = toolService.getToolById(id);
        }
    }

    public boolean isDisponible() {
        return tool != null
                && Boolean.TRUE.equals(tool.getActive())
                && tool.getAvailableQuantity() != null
                && tool.getAvailableQuantity() > 0;
    }

    public String getImageName() {
        if (tool == null || tool.getImagePath() == null || tool.getImagePath().trim().isEmpty()) {
            return "default-tool.jpg";
        }
        return tool.getImagePath().trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }
}
