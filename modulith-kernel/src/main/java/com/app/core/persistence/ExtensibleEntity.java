package com.app.core.persistence;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Extensible Entity Base.
 * Mimics Broadleaf's 'ExtensionHandler' pattern.
 * Uses Postgres JSONB to allow dynamic attributes without schema changes.
 */
@MappedSuperclass
public abstract class ExtensibleEntity implements Serializable {

    @Column(name = "extension_attributes", columnDefinition = "jsonb")
    private Map<String, Object> extensionAttributes = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtensionAttributes() {
        return extensionAttributes;
    }

    @JsonAnySetter
    public void setExtensionAttribute(String key, Object value) {
        if (extensionAttributes == null) {
            extensionAttributes = new HashMap<>();
        }
        extensionAttributes.put(key, value);
    }
}
