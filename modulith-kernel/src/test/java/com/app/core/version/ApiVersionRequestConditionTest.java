package com.app.core.version;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ApiVersionRequestConditionTest {

    @Test
    void testGetMatchingCondition_NoAcceptHeader_DefaultToV1() {
        ApiVersionRequestCondition conditionV1 = new ApiVersionRequestCondition(1);
        ApiVersionRequestCondition conditionV2 = new ApiVersionRequestCondition(2);
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNotNull(conditionV1.getMatchingCondition(request));
        assertNull(conditionV2.getMatchingCondition(request));
    }

    @Test
    void testGetMatchingCondition_WithMatchingVersion() {
        ApiVersionRequestCondition conditionV1 = new ApiVersionRequestCondition(1);
        ApiVersionRequestCondition conditionV2 = new ApiVersionRequestCondition(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/vnd.vaabhi.v2+json");

        assertNull(conditionV1.getMatchingCondition(request));
        assertNotNull(conditionV2.getMatchingCondition(request));
    }

    @Test
    void testGetMatchingCondition_WithNonMatchingVersion() {
        ApiVersionRequestCondition conditionV1 = new ApiVersionRequestCondition(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/vnd.vaabhi.v3+json");

        assertNull(conditionV1.getMatchingCondition(request));
    }

    @Test
    void testGetMatchingCondition_WithGenericAcceptHeader_DefaultToV1() {
        ApiVersionRequestCondition conditionV1 = new ApiVersionRequestCondition(1);
        ApiVersionRequestCondition conditionV2 = new ApiVersionRequestCondition(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");

        assertNotNull(conditionV1.getMatchingCondition(request));
        assertNull(conditionV2.getMatchingCondition(request));
    }
}
