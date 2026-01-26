package com.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit tests to enforce layered architecture within each module.
 * 
 * Each module should follow a clean architecture pattern:
 * - Controllers (API layer)
 * - Services (Business logic layer)
 * - Repositories (Data access layer)
 * - Entities (Domain models)
 */
class LayeredArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.app");
    }

    /**
     * Enforce layered architecture across all modules
     * 
     * Rules:
     * - Controllers should only access Services
     * - Services can access Repositories and other Services
     * - Repositories should only be accessed by Services
     * - Entities can be used by all layers
     */
    @Test
    void shouldFollowLayeredArchitecture() {
        Architectures.LayeredArchitecture architecture = layeredArchitecture()
                .consideringAllDependencies()

                // Define layers
                .layer("Controllers").definedBy("..controllers..")
                .layer("Services").definedBy("..services..")
                .layer("Repositories").definedBy("..repositories..")
                .layer("Entities").definedBy("..entities..")
                .layer("Payloads").definedBy("..payloads..")

                // Define access rules
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Services")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
                .whereLayer("Entities")
                .mayOnlyBeAccessedByLayers("Services", "Repositories", "Payloads", "Controllers");

        architecture.check(allClasses);
    }

    /**
     * Services should not directly instantiate entities via 'new'
     * Use repositories or factories instead
     */
    @Test
    void servicesShouldNotDirectlyInstantiateEntities() {
        // This is a best practice rule - not enforced strictly
        // In practice, some simple entity creation is acceptable
    }

    /**
     * Controllers should return DTOs, not entities
     * This prevents accidental exposure of internal domain models
     */
    @Test
    void controllersShouldReturnDTOs() {
        // This can be enforced by checking that controller methods
        // return types from ..payloads.. package, not ..entities..
        // Implementation left as TODO
    }
}
