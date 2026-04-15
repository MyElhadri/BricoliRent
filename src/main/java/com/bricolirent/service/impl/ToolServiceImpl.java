package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Tool;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ToolService;
import com.bricolirent.service.ToolValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class ToolServiceImpl implements ToolService {

    private ToolRepository toolRepository;

    @PostConstruct
    public void init() {
        this.toolRepository = new ToolRepository();
    }

    @Override
    public List<Tool> getAllTools() {
        return toolRepository.findAll();
    }

    @Override
    public Tool getToolById(Long id) {
        return toolRepository.findById(id).orElse(null);
    }

    @Override
    public List<Tool> getCatalogTools(String keyword, Long categoryId) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return toolRepository.findAll()
                .stream()
                .filter(tool -> Boolean.TRUE.equals(tool.getActive()))
                .filter(tool -> categoryId == null
                        || (tool.getCategory() != null && categoryId.equals(tool.getCategory().getId())))
                .filter(tool -> normalizedKeyword.isBlank()
                        || (tool.getName() != null
                        && tool.getName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .sorted(Comparator
                        .comparing((Tool tool) -> tool.getAvailableQuantity() != null && tool.getAvailableQuantity() > 0)
                        .reversed()
                        .thenComparing(Tool::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public void saveTool(Tool tool) {
        validateToolQuantities(tool);
        toolRepository.save(tool);
    }

    @Override
    public void updateTool(Tool tool) {
        validateToolQuantities(tool);
        toolRepository.update(tool);
    }

    @Override
    public void deleteTool(Tool tool) {
        toolRepository.delete(tool);
    }

    @Override
    public boolean existsByNameAndCategory(String name, Long categoryId, Long excludeToolId) {
        return toolRepository.existsByNameAndCategory(name, categoryId, excludeToolId);
    }

    private void validateToolQuantities(Tool tool) {
        if (tool == null) {
            throw new ToolValidationException("L'outil a enregistrer est invalide.");
        }

        Integer quantiteTotale = tool.getTotalQuantity();
        Integer quantiteDisponible = tool.getAvailableQuantity();

        if (quantiteTotale == null || quantiteDisponible == null) {
            return;
        }

        if (quantiteDisponible > quantiteTotale) {
            throw new ToolValidationException(
                    "La quantite disponible ne peut pas depasser la quantite totale."
            );
        }
    }
}
