/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.metaschema.core.model.validation.IValidationResult;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.model.ComponentDefinition;
import dev.metaschema.oscal.lib.model.IOscalInstance;

/**
 * Test case for oscal-cli issue #192: Validates that ComponentDefinition
 * implements IOscalInstance, preventing ClassCastException when validating
 * component definitions with import-component-definitions.
 *
 * @see <a href=
 *      "https://github.com/metaschema-framework/oscal-cli/issues/192">Issue
 *      #192</a>
 */
class ComponentDefinitionImportTest {
  private static final Logger LOGGER = LogManager.getLogger(ComponentDefinitionImportTest.class);

  /**
   * Verifies that ComponentDefinition implements IOscalInstance. This is required
   * for the resolve-reference() function to work correctly in
   * ResolveReference.resolveReference() at line 152.
   */
  @Test
  void testComponentDefinitionImplementsIOscalInstance() throws IOException {
    OscalBindingContext bindingContext = OscalBindingContext.newInstance();

    Path basePath = Paths.get("src/test/resources/content/issue-192/base-component-definition.json");
    ComponentDefinition componentDef = bindingContext.loadComponentDefinition(basePath);

    assertInstanceOf(IOscalInstance.class, componentDef,
        "ComponentDefinition must implement IOscalInstance for resolve-reference() to work");

    LOGGER.info("ComponentDefinition correctly implements IOscalInstance");
  }

  /**
   * Verifies that validating a ComponentDefinition with
   * import-component-definitions does not throw a ClassCastException.
   *
   * Previously (issue #192), this would fail with: ClassCastException: class
   * dev.metaschema.oscal.lib.model.ComponentDefinition cannot be cast to class
   * dev.metaschema.oscal.lib.model.IOscalInstance
   */
  @Test
  void testValidateComponentDefinitionWithImport() {
    OscalBindingContext bindingContext = OscalBindingContext.newInstance();

    Path componentDefPath = Paths.get("src/test/resources/content/issue-192/component-definition-with-import.json");

    // Validation should complete without ClassCastException
    IValidationResult validationResult = assertDoesNotThrow(
        () -> bindingContext.validateWithConstraints(componentDefPath.toUri(), null),
        "Validation should not throw ClassCastException");

    assertNotNull(validationResult, "Validation result should not be null");

    if (validationResult.isPassing()) {
      LOGGER.info("Validation passed successfully");
    } else {
      LOGGER.info("Validation completed with {} findings (no ClassCastException)",
          validationResult.getFindings().size());
    }
  }
}
