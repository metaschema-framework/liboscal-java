/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.metaschema.core.model.MetaschemaException;
import dev.metaschema.core.model.constraint.IConstraintSet;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.databind.IBindingContext;
import dev.metaschema.databind.model.metaschema.BindingConstraintLoader;
import dev.metaschema.databind.model.metaschema.IBindingMetaschemaModule;
import dev.metaschema.databind.model.metaschema.IBindingModuleLoader;
import gov.nist.secauto.oscal.lib.model.util.AllowedValueCollectingNodeItemVisitor.NodeItemRecord;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

class AllowedValueCollectingNodeItemVisitorTest {

  @Test
  void testAllowedValuesMissingVariableBindings() throws MetaschemaException, IOException {
    IBindingContext bindingContext = IBindingContext.builder().build();

    List<IConstraintSet> constraintSet = new BindingConstraintLoader(bindingContext).load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-metaschema-meta-constraints.xml").toUri()));

    Path genDir = Paths.get("target/dynamic-bindings/allowed-values-1");
    Files.createDirectories(genDir);

    IBindingContext bindingContextWithConstraints = IBindingContext.builder()
        .constraintSet(constraintSet)
        .compilePath(genDir)
        .build();

    IBindingModuleLoader loader = bindingContextWithConstraints.newModuleLoader();
    IBindingMetaschemaModule module = loader.load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-example.xml").toUri()));
    AllowedValueCollectingNodeItemVisitor walker = new AllowedValueCollectingNodeItemVisitor();
    walker.visit(module);
    Collection<NodeItemRecord> allowedValuesByTarget = ObjectUtils.notNull(walker.getAllowedValueLocations());
    assertEquals(1, allowedValuesByTarget.size());
  }

  @Test
  void testAllowedValuesMissingVariableBindings2() throws MetaschemaException, IOException {
    IBindingContext initialBindingContext = IBindingContext.builder().build();

    List<IConstraintSet> constraintSet = new BindingConstraintLoader(initialBindingContext).load(ObjectUtils
        .requireNonNull(Paths.get("src/test/resources/content/issue-112/computer-metaschema-meta-constraints.xml")
            .toUri()));

    Path genDir = Paths.get("target/dynamic-bindings/allowed-values-2");
    Files.createDirectories(genDir);

    IBindingContext bindingContext = IBindingContext.builder()
        .constraintSet(constraintSet)
        .compilePath(genDir)
        .build();

    IBindingModuleLoader loader = bindingContext.newModuleLoader();
    IBindingMetaschemaModule module = loader.load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-example.xml").toUri()));
    AllowedValueCollectingNodeItemVisitor walker = new AllowedValueCollectingNodeItemVisitor();
    walker.visit(module);
    Collection<NodeItemRecord> allowedValuesByTarget = ObjectUtils.notNull(walker.getAllowedValueLocations());
    assertEquals(1, allowedValuesByTarget.size());
  }
}
