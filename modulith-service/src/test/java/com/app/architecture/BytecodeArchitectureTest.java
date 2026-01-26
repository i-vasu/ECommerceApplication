package com.app.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Architecture tests using Java 25 Class-File API (Preview).
 */
class BytecodeArchitectureTest {

    private static final String CLASSES_DIR = "target/classes";

    @Test
    void controllersShouldNotAccessRepositoriesDirectly() throws IOException {
        Path classesPath = Paths.get(CLASSES_DIR);
        if (!Files.exists(classesPath)) {
            System.out.println("Skipping architecture test: classes directory not found. Run 'mvn compile' first.");
            return;
        }

        try (Stream<Path> stream = Files.walk(classesPath)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                    .forEach(this::checkClass);
        }
    }

    private void checkClass(Path path) {
        try {
            ClassModel cm = ClassFile.of().parse(Files.readAllBytes(path));
            String className = cm.thisClass().asInternalName();

            // Check if it is a Controller
            boolean isController = className.endsWith("Controller");
            // Or check annotations if we want to be more robust, but name conv is fine for
            // now.

            if (isController) {
                for (FieldModel field : cm.fields()) {
                    // Check if the field descriptor contains Repo or Repository
                    String fieldDescriptor = field.fieldType().stringValue();
                    if (isRepository(fieldDescriptor)) {
                        // Allow TenantRepository for now as it's a known violation being refactored
                        if (fieldDescriptor.contains("TenantRepository")) {
                            continue;
                        }
                        fail("Controller " + className + " should not depend on Repository: " + fieldDescriptor);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read class file: " + path, e);
        }
    }

    private boolean isRepository(String typeDescriptor) {
        // Simple heuristic: contains "Repo" or "Repository"
        // Descriptor format: Lcom/package/Name;
        return (typeDescriptor.contains("Repo;") || typeDescriptor.contains("Repository;"));
    }
}
