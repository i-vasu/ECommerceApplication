package com.app.search.services;

import com.app.search.entities.CustomDesign;
import com.app.search.repositories.CustomDesignRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomDesignService {

    private final CustomDesignRepo designRepo;

    @Transactional
    public CustomDesign saveDesign(CustomDesign design) {
        if (design.getCreatedAt() == null) {
            design.setCreatedAt(LocalDateTime.now());
        }
        return designRepo.save(design);
    }

    @Transactional(readOnly = true)
    public List<CustomDesign> getUserDesigns(Long userId) {
        return designRepo.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public CustomDesign getDesign(Long designId) {
        return designRepo.findById(designId)
                .orElseThrow(() -> new RuntimeException("Design not found"));
    }
}
