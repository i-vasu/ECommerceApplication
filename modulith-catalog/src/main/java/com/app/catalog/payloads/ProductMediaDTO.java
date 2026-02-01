package com.app.catalog.payloads;

public record ProductMediaDTO(Long mediaId, String type, String url, int displayOrder, String blurHash) {
}
