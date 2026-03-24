package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Tool;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ToolService;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ToolService.
 */
public class ToolServiceImpl implements ToolService {

    private final ToolRepository toolRepository = new ToolRepository();

    @Override
    public List<Tool> getAllTools() {
        return toolRepository.findAll();
    }

    @Override
    public Optional<Tool> getToolById(Long id) {
        return toolRepository.findById(id);
    }

    @Override
    public List<Tool> getToolsByCategory(Long categoryId) {
        return toolRepository.findByCategory(categoryId);
    }

    @Override
    public List<Tool> getAvailableTools() {
        return toolRepository.findAvailable();
    }

    @Override
    public void addTool(Tool tool) {
        toolRepository.save(tool);
    }

    @Override
    public void updateTool(Tool tool) {
        toolRepository.update(tool);
    }

    @Override
    public void deleteTool(Tool tool) {
        toolRepository.delete(tool);
    }
}
