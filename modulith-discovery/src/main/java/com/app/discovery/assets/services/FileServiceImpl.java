package com.app.discovery.assets.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

	@Override
	public String uploadImage(String path, MultipartFile file) throws IOException {

		String originalFileName = file.getOriginalFilename();
		String randomId = UUID.randomUUID().toString();
		String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf('.')));
		Path filePath = Paths.get(path, fileName);

		Files.createDirectories(Paths.get(path));

		Files.copy(file.getInputStream(), filePath);

		return fileName;
	}

	@Override
	public InputStream getResource(String path, String fileName) throws IOException {
		Path filePath = Paths.get(path, fileName);
		return Files.newInputStream(filePath);
	}

}
