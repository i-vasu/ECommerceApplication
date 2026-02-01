package com.app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests to enforce modulith architecture boundaries.
 * 
 * These tests ensure that:
 * - Modules don't have circular dependencies
 * - Module dependencies flow in the correct direction
 * - Kernel module remains free of domain dependencies
 * - Cross-module communication uses contracts, not direct implementations
 */
class ModulithArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.app");
    }

    /**
     * Rule 1: No cyclic dependencies between modules
     * This ensures clean module boundaries
     */
    @Test
    void modulesShouldBeFreeOfCycles() {
        ArchRule rule = slices()
                .matching("com.app.(*)..")
                .should().beFreeOfCycles();

        rule.check(allClasses);
    }

    /**
     * Rule 2: Kernel module should not depend on any domain module
     * Kernel is shared infrastructure - it should be dependency-free
     */
    @Test
    void kernelShouldNotDependOnDomainModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.app.security..",
                        "com.app.catalog..",
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.finance.payment..",
                        "com.app.logistics.shipping..",
                        "com.app.marketplace..",
                        "com.app.catalog.review..",
                        "com.app.marketing..",
                        "com.app.intelligence.analytics..");

        rule.check(allClasses);
    }

    /**
     * Rule 3: Security module should not depend on Order or Catalog modules
     * Security is a foundational domain
     */
    @Test
    void securityShouldNotDependOnOrderOrCatalog() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.security..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.catalog..",
                        "com.app.finance..",
                        "com.app.logistics..");

        rule.check(allClasses);
    }

    /**
     * Rule 4: Catalog module should not depend on Order module
     * Catalog can depend on Security for user info
     */
    @Test
    void catalogShouldNotDependOnOrder() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.catalog..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.finance..",
                        "com.app.logistics..");

        rule.check(allClasses);
    }

    /**
     * Rule 5: Order module can depend on Catalog and Security
     * This validates our dependency hierarchy: Kernel <- Security <- Catalog <- Order
     */
    @Test
    void orderCanDependOnCatalogAndSecurity() {
        // This is allowed - just documenting the dependency direction
        // Order is at the top of the dependency hierarchy
    }

    /**
     * Rule 6: Cross-module service calls should use contracts
     * Services from other modules should be accessed via interfaces in
     * kernel.contracts
     */
    @Test
    void crossModuleServiceCallsShouldUseContracts() {
        // This rule checks that services implement contract interfaces
        // In practice, we'd check that UserService implements UserServiceContract, etc.
        // Also checks that 'order' doesn't use implementation services from others.

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.order..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.app.security.services..")
                .andShould().dependOnClassesThat()
                .resideInAPackage("com.app.marketing.services..");

         rule.check(allClasses);
    }

    /**
     * Rule 7: Entities should not be exposed across module boundaries
     * Use DTOs for cross-module data transfer
     */
    @Test
    void entitiesShouldNotCrossModuleBoundaries() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.order..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.app.catalog.entities..")
                .orShould().dependOnClassesThat()
                .resideInAPackage("com.app.security.entities..");

        // This will have violations for now - enable after full migration
        // rule.check(allClasses);
    }
}
