package com.app.media;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ImageService {

    public byte[] convertToWebP(MultipartFile file) throws IOException {
        return ImmutableImage.loader().fromStream(file.getInputStream())
                .bytes(WebpWriter.DEFAULT);
    }
}
