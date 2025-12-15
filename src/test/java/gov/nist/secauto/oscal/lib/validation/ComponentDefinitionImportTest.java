/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.nist.secauto.metaschema.core.model.validation.IValidationResult;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.ComponentDefinition;
import gov.nist.secauto.oscal.lib.model.IOscalInstance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test case to reproduce oscal-cli issue #192: ClassCastException when
 * validating ComponentDefinition with import-component-definitions.
 *
 * @see <a href=
 *      "https://github.com/metaschema-framework/oscal-cli/issues/192">Issue
 *      #192</a>
 */
class ComponentDefinitionImportTest {
  private static final Logger LOGGER = LogManager.getLogger(ComponentDefinitionImportTest.class);

  /**
   * Demonstrates that ComponentDefinition does NOT implement IOscalInstance,
   * which is the root cause of the ClassCastException in ResolveReference.
   */
  @Test
  void testComponentDefinitionDoesNotImplementIOscalInstance() throws IOException {
    OscalBindingContext bindingContext = OscalBindingContext.newInstance();

    // Load the base component definition
    Path basePath = Paths.get("src/test/resources/content/issue-192/base-component-definition.json");
    ComponentDefinition componentDef = bindingContext.loadComponentDefinition(basePath);

    // Verify that ComponentDefinition does NOT implement IOscalInstance
    // This is the root cause of the bug - the cast in
    // ResolveReference.resolveReference() line 152
    boolean implementsIOscalInstance = componentDef instanceof IOscalInstance;

    LOGGER.info("ComponentDefinition implements IOscalInstance: {}", implementsIOscalInstance);

    // This assertion documents the bug: ComponentDefinition SHOULD implement
    // IOscalInstance
    // but currently does NOT
    if (implementsIOscalInstance) {
      LOGGER.info("Bug appears to be fixed - ComponentDefinition now implements IOscalInstance");
    } else {
      LOGGER.warn("Bug confirmed: ComponentDefinition does NOT implement IOscalInstance");
      LOGGER.warn("This causes ClassCastException in ResolveReference.resolveReference() at line 152");
    }

    // Asserting false to document the current broken state
    // When the bug is fixed, this test should be updated to assertTrue
    assertTrue(!implementsIOscalInstance,
        "This test documents the bug: ComponentDefinition should implement IOscalInstance but doesn't");
  }

  /**
   * Reproduces the ClassCastException from oscal-cli issue #192.
   *
   * When validating a ComponentDefinition with import-component-definitions that
   * uses a fragment reference (href="#uuid"), the constraint evaluation calls
   * resolve-reference() which attempts to cast the root ComponentDefinition to
   * IOscalInstance, causing:
   *
   * ClassCastException: class
   * gov.nist.secauto.oscal.lib.model.ComponentDefinition cannot be cast to class
   * gov.nist.secauto.oscal.lib.model.IOscalInstance
   */
  @Test
  void testValidateComponentDefinitionWithImportCausesClassCastException() {
    OscalBindingContext bindingContext = OscalBindingContext.newInstance();

    Path componentDefPath = Paths.get("src/test/resources/content/issue-192/component-definition-with-import.json");

    try {
      // This validation should trigger the constraint that uses resolve-reference()
      // The constraint is defined in ComponentDefinition's @ValueConstraints
      // annotation:
      // "import-component-definition !
      // recurse-depth('doc(resolve-uri(Q{...}resolve-reference(@href)))/component-definition')"
      IValidationResult validationResult = bindingContext.validateWithConstraints(
          componentDefPath.toUri(),
          null);

      // If we get here without exception, check if validation passed or had expected
      // findings
      if (validationResult.isPassing()) {
        LOGGER.info("Validation passed - bug may be fixed or constraint not triggered");
      } else {
        LOGGER.info("Validation completed with findings (no ClassCastException)");
      }

    } catch (ClassCastException e) {
      // This is the expected behavior when the bug is present
      LOGGER.error("Bug reproduced: ClassCastException during validation", e);

      // Verify it's the specific cast error from ResolveReference
      assertTrue(e.getMessage().contains("ComponentDefinition")
          && e.getMessage().contains("IOscalInstance"),
          "Expected ClassCastException from ComponentDefinition to IOscalInstance cast");

      // Fail the test to indicate the bug exists
      fail("Issue #192 reproduced: " + e.getMessage());

    } catch (Exception e) {
      // Log any other exception for debugging
      LOGGER.error("Unexpected exception during validation", e);

      // Check if the root cause is the ClassCastException
      Throwable cause = e;
      while (cause != null) {
        if (cause instanceof ClassCastException) {
          ClassCastException cce = (ClassCastException) cause;
          if (cce.getMessage() != null
              && cce.getMessage().contains("ComponentDefinition")
              && cce.getMessage().contains("IOscalInstance")) {
            LOGGER.error("Bug reproduced (wrapped): ClassCastException found in cause chain", cce);
            fail("Issue #192 reproduced (wrapped exception): " + cce.getMessage());
          }
        }
        cause = cause.getCause();
      }

      // Re-throw if it's not the expected ClassCastException
      throw new RuntimeException("Unexpected exception during validation", e);
    }
  }
}
