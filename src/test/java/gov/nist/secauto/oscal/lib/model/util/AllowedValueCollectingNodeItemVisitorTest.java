
package gov.nist.secauto.oscal.lib.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nist.secauto.metaschema.core.model.MetaschemaException;
import gov.nist.secauto.metaschema.core.model.constraint.ExternalConstraintsModulePostProcessor;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.core.model.xml.IXmlMetaschemaModule;
import gov.nist.secauto.metaschema.core.model.xml.ModuleLoader;
import gov.nist.secauto.metaschema.core.model.xml.XmlMetaConstraintLoader;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.model.metaschema.IBindingMetaschemaModule;
import gov.nist.secauto.metaschema.databind.model.metaschema.IBindingModuleLoader;
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
    List<IConstraintSet> constraintSet = new XmlMetaConstraintLoader().load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-metaschema-meta-constraints.xml").toUri()));
    ExternalConstraintsModulePostProcessor postProcessor = new ExternalConstraintsModulePostProcessor(constraintSet);
    ModuleLoader loader = new ModuleLoader(CollectionUtil.singletonList(postProcessor));
    IXmlMetaschemaModule module = loader.load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-example.xml").toUri()));
    AllowedValueCollectingNodeItemVisitor walker = new AllowedValueCollectingNodeItemVisitor();
    walker.visit(module);
    Collection<NodeItemRecord> allowedValuesByTarget = ObjectUtils.notNull(walker.getAllowedValueLocations());
    assertEquals(1, allowedValuesByTarget.size());
  }

  @Test
  void testAllowedValuesMissingVariableBindings2() throws MetaschemaException, IOException {
    List<IConstraintSet> constraintSet = new XmlMetaConstraintLoader().load(ObjectUtils.requireNonNull(
        Paths.get("src/test/resources/content/issue-112/computer-metaschema-meta-constraints.xml").toUri()));

    Path genDir = Paths.get("target/dynamic-bindings/allowed-values");
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
