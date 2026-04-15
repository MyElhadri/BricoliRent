package com.bricolirent.service;

import com.bricolirent.domain.entity.Tool;
import java.util.List;

public interface ToolService {
    List<Tool> getAllTools();

    Tool getToolById(Long id);

    List<Tool> getCatalogTools(String keyword, Long categoryId);

    void saveTool(Tool tool);

    void updateTool(Tool tool);

    void deleteTool(Tool tool);

    boolean existsByNameAndCategory(String name, Long categoryId, Long excludeToolId);
}
