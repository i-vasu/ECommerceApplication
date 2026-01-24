package com.app.core.utils;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

@Component
public class ContentSanitizer {

    private final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    /**
     * Sanitize input HTML to prevent XSS.
     * Allows basic formatting (b, i, em, etc.) and links.
     */
    public String sanitize(String input) {
        if (input == null)
            return null;
        return policy.sanitize(input);
    }

    /**
     * Strips all HTML tags from the input.
     */
    public String stripHtml(String input) {
        if (input == null)
            return null;
        // Simple regex or a more robust sanitizer policy
        return input.replaceAll("<[^>]*>", "");
    }
}
