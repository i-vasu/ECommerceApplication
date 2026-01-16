package com.app;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.app")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule services_should_only_be_accessed_by_controllers_or_other_services = classes().that()
            .resideInAPackage("..services..")
            .should().onlyBeAccessed().byAnyPackage("..controllers..", "..services..", "..config..", "..security..");

    @ArchTest
    static final ArchRule repositories_should_only_be_accessed_by_services = classes().that()
            .resideInAPackage("..repositories..")
            .should().onlyBeAccessed().byAnyPackage("..services..", "..repositories.."); // Repos can access other repos
                                                                                         // if needed

    @ArchTest
    static final ArchRule no_cycles_between_slices = classes().that().resideInAPackage("com.app..").should()
            .beFreeOfCycles();
}
