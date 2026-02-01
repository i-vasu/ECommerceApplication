package com.app.service;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.app");

    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..service..", "..services..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..controllers..");

        rule.check(importedClasses);
    }

    @Test
    void persistenceLayerShouldNotDependOnServiceLayer() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..repository..", "..repositories..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..services..");

        rule.check(importedClasses);
    }
}
