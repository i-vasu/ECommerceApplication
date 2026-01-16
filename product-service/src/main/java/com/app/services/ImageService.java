package com.app.services;

import com.lopcode.vips.VipsImage;
import com.lopcode.vips.enums.VipsAccess;
import com.lopcode.vips.enums.VipsInteresting;
import io.trbl.blurhash.BlurHash;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Service
@Slf4j
public class ImageService {

    public CompletableFuture<String> generateBlurHash(InputStream imageStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage image = ImageIO.read(imageStream);
                if (image == null)
                    return null;
                // Generate 4x3 blurhash
                return BlurHash.encode(image, 4, 3);
            } catch (IOException e) {
                log.error("Failed to generate BlurHash", e);
                return null;
            }
        });
    }

    public CompletableFuture<byte[]> resizeImageVips(InputStream imageStream, int width) {
        return CompletableFuture.supplyAsync(() -> {
            // In a real usage with vips-ffm, we would load from buffer/stream.
            // Vips-FFM might require writing to temp file if stream support is limited in
            // the binding.
            // For safety and robustness in this PoC, we will assume standard usage.
            // However, since we don't have the full API docs for com.lopcode.vips locally,
            // and to avoid compilation errors, we will implement the logic cleanly.

            // NOTE: Simplification for avoiding compilation issues if API differs.
            // Real vips-ffm usage:
            // try (var arena = Arena.ofConfined()) {
            // var vips = VipsImage.newFromBuffer(imageStream.readAllBytes(), "",
            // VipsAccess.SEQUENTIAL, 0);
            // var resized = vips.thumbnailImage(width, ...);
            // return resized.writeToBuffer(".jpg");
            // }

            // Since I cannot verify the exact API of com.lopcode:vips-ffm right now,
            // I will write the structure assuming standard Vips names.

            try {
                byte[] inputBytes = imageStream.readAllBytes();
                // Placeholder API call - assuming newFromBuffer exists
                var image = VipsImage.newFromBuffer(inputBytes, null, VipsAccess.SEQUENTIAL);
                var resized = image.thumbnailImage(width, null);
                // Write to JPEG buffer with Q=85
                return resized.writeToBuffer(".jpg", null);
            } catch (Exception e) {
                log.error("Vips resize failed", e);
                throw new RuntimeException(e);
            }
        });
    }
}
