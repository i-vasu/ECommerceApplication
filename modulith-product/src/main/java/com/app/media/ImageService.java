package com.app.media;

import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import io.trbl.blurhash.BlurHash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance image processing service using vips-ffm
 * Uses libvips for fast, memory-efficient image operations
 */
@Service
public class ImageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageService.class);

    /**
     * Generate a BlurHash placeholder for an image
     * 
     * @param imageStream Input image stream
     * @return BlurHash string (4x3 components)
     */
    public CompletableFuture<String> generateBlurHash(InputStream imageStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (imageStream) {
                BufferedImage image = ImageIO.read(imageStream);
                if (image == null) {
                    log.warn("Could not read image for BlurHash generation");
                    return null;
                }
                // Generate 4x3 blurhash (good balance of size vs quality)
                return BlurHash.encode(image, 4, 3);
            } catch (IOException e) {
                log.error("Failed to generate BlurHash", e);
                return null;
            }
        });
    }

    /**
     * Resize an image using libvips (high-performance)
     * 
     * @param imageBytes  Input image bytes
     * @param targetWidth Target width in pixels
     * @return Resized image as JPEG bytes
     */
    public CompletableFuture<byte[]> resizeImage(byte[] imageBytes, int targetWidth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Write input to temp file (vips works best with files)
                Path inputPath = Files.createTempFile("vips_input_", ".jpg");
                Path outputPath = Files.createTempFile("vips_output_", ".jpg");

                try {
                    Files.write(inputPath, imageBytes);

                    // Use vips-ffm for high-performance resizing
                    Vips.run(arena -> {
                        VImage thumbnail = VImage.thumbnail(
                                arena,
                                inputPath.toAbsolutePath().toString(),
                                targetWidth,
                                VipsOption.Boolean("auto-rotate", true));

                        int width = thumbnail.getWidth();
                        int height = thumbnail.getHeight();
                        log.debug("Created thumbnail: {}x{}", width, height);

                        thumbnail.writeToFile(outputPath.toAbsolutePath().toString());
                    });

                    return Files.readAllBytes(outputPath);

                } finally {
                    // Clean up temp files
                    Files.deleteIfExists(inputPath);
                    Files.deleteIfExists(outputPath);
                }

            } catch (Exception e) {
                log.error("Vips resize failed", e);
                throw new RuntimeException("Image resize failed", e);
            }
        });
    }

    /**
     * Generate multiple sizes of an image for responsive serving
     * 
     * @param imageBytes Original image bytes
     * @param sizes      Array of target widths
     * @return Array of resized images as JPEG bytes
     */
    public CompletableFuture<byte[][]> generateResponsiveSizes(byte[] imageBytes, int... sizes) {
        return CompletableFuture.supplyAsync(() -> {
            byte[][] results = new byte[sizes.length][];

            try {
                // Write input to temp file
                Path inputPath = Files.createTempFile("vips_input_", ".jpg");

                try {
                    Files.write(inputPath, imageBytes);

                    for (int i = 0; i < sizes.length; i++) {
                        int targetWidth = sizes[i];
                        Path outputPath = Files.createTempFile("vips_output_" + targetWidth + "_", ".jpg");

                        try {
                            Vips.run(arena -> {
                                VImage thumbnail = VImage.thumbnail(
                                        arena,
                                        inputPath.toAbsolutePath().toString(),
                                        targetWidth,
                                        VipsOption.Boolean("auto-rotate", true));
                                thumbnail.writeToFile(outputPath.toAbsolutePath().toString());
                            });

                            results[i] = Files.readAllBytes(outputPath);
                        } finally {
                            Files.deleteIfExists(outputPath);
                        }
                    }

                } finally {
                    Files.deleteIfExists(inputPath);
                }

                log.info("Generated {} responsive image sizes using libvips", sizes.length);
                return results;

            } catch (Exception e) {
                log.error("Failed to generate responsive sizes", e);
                throw new RuntimeException("Failed to generate responsive sizes", e);
            }
        });
    }

    /**
     * Convert image to WebP format for better compression
     * 
     * @param imageBytes Input image bytes
     * @param quality    WebP quality (0-100)
     * @return WebP image bytes
     */
    public CompletableFuture<byte[]> convertToWebP(byte[] imageBytes, int quality) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path inputPath = Files.createTempFile("vips_input_", ".jpg");
                Path outputPath = Files.createTempFile("vips_output_", ".webp");

                try {
                    Files.write(inputPath, imageBytes);

                    Vips.run(arena -> {
                        VImage image = VImage.newFromFile(
                                arena,
                                inputPath.toAbsolutePath().toString());
                        image.writeToFile(
                                outputPath.toAbsolutePath().toString(),
                                VipsOption.Int("Q", quality));
                    });

                    return Files.readAllBytes(outputPath);

                } finally {
                    Files.deleteIfExists(inputPath);
                    Files.deleteIfExists(outputPath);
                }

            } catch (Exception e) {
                log.error("WebP conversion failed", e);
                throw new RuntimeException("WebP conversion failed", e);
            }
        });
    }
}
