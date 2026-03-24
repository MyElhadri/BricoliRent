package com.bricolirent.service;

import com.bricolirent.domain.entity.Tool;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for tool management.
 */
public interface ToolService {

    List<Tool> getAllTools();

    Optional<Tool> getToolById(Long id);

    List<Tool> getToolsByCategory(Long categoryId);

    List<Tool> getAvailableTools();

    void addTool(Tool tool);

    void updateTool(Tool tool);

    void deleteTool(Tool tool);
}
