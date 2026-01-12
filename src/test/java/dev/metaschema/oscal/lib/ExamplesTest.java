/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.metaschema.core.metapath.DynamicContext;
import dev.metaschema.core.metapath.item.node.IDocumentNodeItem;
import dev.metaschema.core.metapath.item.node.INodeItem;
import dev.metaschema.core.model.constraint.ConstraintValidationException;
import dev.metaschema.core.model.constraint.DefaultConstraintValidator;
import dev.metaschema.core.model.constraint.FindingCollectingConstraintValidationHandler;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.databind.io.DeserializationFeature;
import dev.metaschema.databind.io.Format;
import dev.metaschema.databind.io.IBoundLoader;
import dev.metaschema.databind.io.ISerializer;
import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.profile.resolver.ProfileResolutionException;
import dev.metaschema.oscal.lib.profile.resolver.ProfileResolver;

class ExamplesTest {

  @Test
  void simpleLoadAndSave() throws IOException {
    // Initialize the Module framework
    OscalBindingContext bindingContext = OscalBindingContext.instance(); // manages the Module model
    IBoundLoader loader = bindingContext.newBoundLoader(); // supports loading OSCAL documents

    // load an OSCAL catalog
    Catalog catalog = loader.load(
        ObjectUtils.requireNonNull(Paths.get("src/test/resources/content/test-catalog.xml"))); // load the catalog
    assertNotNull(catalog);

    // Create a serializer which can be used to write multiple catalogs
    ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.YAML, Catalog.class);

    // create the output directory
    Path outDir = Paths.get("target/generated-test-resources");
    Files.createDirectories(outDir);

    // serialize the catalog as yaml
    serializer.serialize(catalog, ObjectUtils.notNull(outDir.resolve("test-catalog.yaml")));
  }

  @Disabled
  @Test
  void testConstraintValidation()
      throws MalformedURLException, IOException, URISyntaxException, ProfileResolutionException,
      ConstraintValidationException {
    // Initialize the Module framework
    OscalBindingContext bindingContext = OscalBindingContext.instance(); // manages the Module model
    IBoundLoader loader = bindingContext.newBoundLoader(); // supports loading OSCAL documents
    loader.disableFeature(DeserializationFeature.DESERIALIZE_VALIDATE_CONSTRAINTS);

    IDocumentNodeItem nodeItem = loader.loadAsNodeItem(new URL(
        "https://raw.githubusercontent.com/Rene2mt/fedramp-automation/a692b9385d8fbcacbb1d3e3d0b0d7e3c45a205d0/src/content/baselines/rev5/xml/FedRAMP_rev5_HIGH-baseline_profile.xml"));

    DynamicContext dynamicContext = new DynamicContext(nodeItem.getStaticContext());
    dynamicContext.setDocumentLoader(loader);
    FindingCollectingConstraintValidationHandler handler = new FindingCollectingConstraintValidationHandler();
    DefaultConstraintValidator validator = new DefaultConstraintValidator(handler);

    validator.validate(nodeItem, dynamicContext);
    validator.finalizeValidation(dynamicContext);

    assertTrue(handler.isPassing());

    IDocumentNodeItem resolvedCatalog = new ProfileResolver().resolve(nodeItem);

    // Create a serializer which can be used to write multiple catalogs
    ISerializer<Catalog> serializer = bindingContext.newSerializer(Format.YAML, Catalog.class);
    // serialize the catalog as yaml
    @SuppressWarnings("resource") // not owned
    OutputStream os = ObjectUtils.notNull(System.out);

    serializer.serialize((Catalog) INodeItem.toValue(resolvedCatalog), os);
  }
}
