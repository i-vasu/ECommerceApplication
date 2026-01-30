package com.app.discovery.domain.controllers;

import com.app.discovery.domain.entities.CustomDesign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import reactor.core.publisher.Flux;

@Tag(name = "Custom Design", description = "Endpoints for AI-generated designs and virtual try-on")
@SecurityRequirement(name = "E-Commerce Application")
public interface CustomDesignApi {

        @Operation(summary = "Save a custom design", description = "Saves a generated or customized design for a user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Design saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid design data")
        })
        @PostMapping("/public")
        ResponseEntity<CustomDesign> saveDesign(@RequestBody CustomDesign design);

        @Operation(summary = "Get user designs", description = "Retrieves all custom designs saved by a user")
        @ApiResponse(responseCode = "200", description = "List of designs retrieved")
        @GetMapping("/public/user/{userId}")
        ResponseEntity<List<CustomDesign>> getUserDesigns(@PathVariable Long userId);

        @Operation(summary = "Get a specific design", description = "Retrieves a design by its ID")
        @ApiResponse(responseCode = "200", description = "Design found")
        @ApiResponse(responseCode = "404", description = "Design not found")
        @GetMapping("/public/{designId}")
        ResponseEntity<CustomDesign> getDesign(@PathVariable Long designId);

        @Operation(summary = "Generate Saree Design", description = "Generates a saree design using AI based on a prompt")
        @ApiResponse(responseCode = "200", description = "Design generated successfully")
        @PostMapping("/public/generate")
        ResponseEntity<String> generateDesign(@RequestBody String prompt);

        @Operation(summary = "Stream Saree Design Generation", description = "Streams the generation process of a saree design")
        @GetMapping(value = "/public/generate/stream", produces = "text/event-stream")
        Flux<String> generateDesignStream(@RequestParam String prompt);

        @Operation(summary = "Virtual Try-On", description = "Performs virtual try-on using a user photo and saree image")
        @ApiResponse(responseCode = "200", description = "Try-on result URL generated")
        @PostMapping("/public/tryon")
        ResponseEntity<String> virtualTryOn(
                        @Parameter(description = "Map containing userPhotoUrl and sareeImageUrl") @RequestBody java.util.Map<String, String> request);
}
