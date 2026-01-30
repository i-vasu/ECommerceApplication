package com.app.catalog.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ProductMediaDTO(Long mediaId, String type, String url, int displayOrder, String blurHash) {
}
