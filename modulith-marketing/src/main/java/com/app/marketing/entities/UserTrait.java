package com.app.marketing.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_traits")
@Data
@NoArgsConstructor
public class UserTrait {

    @Id
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> traits;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserTrait(String email, Map<String, Object> traits) {
        this.email = email;
        this.traits = traits;
        this.updatedAt = LocalDateTime.now();
    }
}
