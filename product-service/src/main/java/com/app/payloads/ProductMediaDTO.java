package com.app.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaDTO {
    private Long mediaId;
    private String type;
    private String url;
    private int displayOrder;
    private String blurHash;
}
