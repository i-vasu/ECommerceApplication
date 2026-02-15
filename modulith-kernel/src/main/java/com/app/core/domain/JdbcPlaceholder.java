package com.app.core.domain;

import org.springframework.data.annotation.Id;

/**
 * Placeholder entity for JDBC-only repositories that don't map to a specific aggregate root.
 */
@org.springframework.data.relational.core.mapping.Table("jdbc_placeholder")
public class JdbcPlaceholder {
    @Id
    private Long id;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
