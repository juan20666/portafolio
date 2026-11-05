package dev.juanpablocorrea.portfolio.contact;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guards the dependency rule of hexagonal / clean architecture:
 * dependencies point inwards (infrastructure → application → domain), never outwards.
 * If someone imports Spring into the domain, the build fails.
 */
class ArchitectureTest {

    private static final String ROOT = "dev.juanpablocorrea.portfolio.contact";

    private JavaClasses production;

    @BeforeEach
    void importProductionClasses() {
        production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void theDomainDependsOnNothingOutsideItself() {
        noClasses().that().resideInAPackage("..contact.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..contact.application..",
                        "..contact.infrastructure..",
                        "org.springframework..",
                        "jakarta..",
                        "com.fasterxml..",
                        "tools.jackson..",
                        "org.slf4j..")
                .because("business rules must not know about frameworks or delivery mechanisms")
                .check(production);
    }

    @Test
    void theApplicationLayerDependsOnlyOnTheDomain() {
        noClasses().that().resideInAPackage("..contact.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..contact.infrastructure..",
                        "org.springframework..",
                        "jakarta..",
                        "com.fasterxml..",
                        "tools.jackson..")
                .because("use cases are orchestrated through ports, not through concrete technology")
                .check(production);
    }

    @Test
    void inboundAndOutboundAdaptersDoNotKnowEachOther() {
        noClasses().that().resideInAPackage("..infrastructure.adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.out..")
                .because("a driving adapter must go through the application layer")
                .check(production);

        noClasses().that().resideInAPackage("..infrastructure.adapter.out..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.adapter.in..")
                .check(production);
    }

    @Test
    void adaptersAreConfiguredFromOutsideNotTheOtherWayRound() {
        noClasses().that().resideInAPackage("..infrastructure.adapter..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.config..")
                .because("the composition root wires adapters; adapters must not reach back into it")
                .check(production);
    }
}
