package com.app.search.controllers;

import com.app.product.payloads.ProductDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "Visual Search", description = "Endpoints for visual search and 'Shop the Look'")
@SecurityRequirement(name = "E-Commerce Application")
public interface VisualSearchApi {

    @Operation(summary = "Search Products by Image", description = "Finds products similar to the uploaded image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Similar products found"),
            @ApiResponse(responseCode = "400", description = "Invalid image input"),
            @ApiResponse(responseCode = "404", description = "No similar products found")
    })
    @PostMapping(value = { "/visual", "/shop-the-look" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<List<ProductDTO>> searchByImage(
            @Parameter(description = "Image file to search for", required = true) @RequestPart("image") MultipartFile image);
}
