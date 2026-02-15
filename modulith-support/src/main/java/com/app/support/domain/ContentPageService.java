package com.app.support.domain;

import com.app.support.entities.ContentPage;
import com.app.support.repositories.ContentPageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContentPageService {

    private final ContentPageRepo contentPageRepo;

    public List<ContentPage> getAllPages() {
        return contentPageRepo.findAll();
    }

    public Optional<ContentPage> getPageBySlug(String slug) {
        return contentPageRepo.findBySlug(slug);
    }

    public ContentPage createPage(ContentPage page) {
        return contentPageRepo.save(page);
    }

    public ContentPage updatePage(Long id, ContentPage updated) {
        ContentPage existing = contentPageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Page not found"));
        
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setPublished(updated.isPublished());
        existing.setSlug(updated.getSlug());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return contentPageRepo.save(existing);
    }

    public void deletePage(Long id) {
        contentPageRepo.deleteById(id);
    }
}
