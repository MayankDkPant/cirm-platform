package com.cirm.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architectural safety rules for modular boundaries.
 *
 * Purpose:
 * - Prevent cross-module repository coupling.
 * - Keep repository usage internal to each module.
 */
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.cxp.platform");

    @Test
    void externalModulesMustNotDependOnAiRepository() {
        noClasses()
                .that().resideOutsideOfPackage("com.cxp.platform.ai..")
                .should().dependOnClassesThat().resideInAPackage("com.cxp.platform.ai.repository..");
    }
}
