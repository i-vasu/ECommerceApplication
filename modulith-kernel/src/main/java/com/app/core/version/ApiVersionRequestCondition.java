package com.app.core.version;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("application/vnd\\.vaabhi\\.v(\\d+)\\+json");
    private final int apiVersion;

    public ApiVersionRequestCondition(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        return new ApiVersionRequestCondition(other.apiVersion);
    }

    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null) {
            return apiVersion == 1 ? this : null;
        }

        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(acceptHeader);
        if (matcher.find()) {
            int version = Integer.parseInt(matcher.group(1));
            if (version == apiVersion) {
                return this;
            }
        }

        // Default to v1 if no version found and this is v1
        if (!matcher.find() && apiVersion == 1) {
            return this;
        }

        return null;
    }

    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        return Integer.compare(other.apiVersion, this.apiVersion);
    }

    public int getApiVersion() {
        return apiVersion;
    }
}
