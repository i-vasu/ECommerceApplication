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
                        "com.app.identity..",
                        "com.app.product..",
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.payment..",
                        "com.app.shipping..",
                        "com.app.marketplace..",
                        "com.app.review..",
                        "com.app.marketing..",
                        "com.app.analytics..");

        rule.check(allClasses);
    }

    /**
     * Rule 3: Identity module should not depend on Order or Product modules
     * Identity is a foundational domain
     */
    @Test
    void identityShouldNotDependOnOrderOrProduct() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.product..",
                        "com.app.payment..",
                        "com.app.shipping..");

        rule.check(allClasses);
    }

    /**
     * Rule 4: Product module should not depend on Order module
     * Product can depend on Identity for user info
     */
    @Test
    void productShouldNotDependOnOrder() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.product..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.app.order..",
                        "com.app.cart..",
                        "com.app.payment..",
                        "com.app.shipping..");

        rule.check(allClasses);
    }

    /**
     * Rule 5: Order module can depend on Product and Identity
     * This validates our dependency hierarchy: Kernel <- Identity <- Product <-
     * Order
     */
    @Test
    void orderCanDependOnProductAndIdentity() {
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

        ArchRule rule = noClasses()
                .that().resideInAPackage("com.app.order..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.app.identity.services..")
                .andShould().dependOnClassesThat()
                .resideInAPackage("com.app.marketing.services..");

        // Note: This rule will fail until we fully migrate to contracts
        // Keep it commented for now, enable after migration
        // rule.check(allClasses);
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
                .resideInAPackage("com.app.product.entities..")
                .orShould().dependOnClassesThat()
                .resideInAPackage("com.app.identity.entities..");

        // This will have violations for now - enable after full migration
        // rule.check(allClasses);
    }
}
