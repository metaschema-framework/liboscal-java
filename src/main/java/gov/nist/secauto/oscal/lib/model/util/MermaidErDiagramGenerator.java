/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.util;

import gov.nist.secauto.metaschema.core.metapath.item.node.AbstractRecursionPreventingNodeItemVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyInstanceGroupedNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IFieldNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IModelNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IModuleNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IModelDefinition;
import gov.nist.secauto.metaschema.core.model.INamedModelInstance;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.databind.model.IBoundModule;

import java.io.IOException;
import java.io.PrintWriter;

import edu.umd.cs.findbugs.annotations.NonNull;

public class MermaidErDiagramGenerator
    extends AbstractRecursionPreventingNodeItemVisitor<PrintWriter, Void> {

  private enum Relationship {
    ZERO_OR_ONE("|o", "o|"),
    ONE("||", "||"),
    ZERO_OR_MORE("}o", "o{"),
    ONE_OR_MORE("}|", "|{");

    @NonNull
    private final String left;
    @NonNull
    private final String right;

    Relationship(@NonNull String left, @NonNull String right) {
      this.left = left;
      this.right = right;
    }

    @NonNull
    public String getLeft() {
      return left;
    }

    @NonNull
    public String getRight() {
      return right;
    }

    @NonNull
    public static Relationship toRelationship(int minOccurs, int maxOccurs) {
      return minOccurs < 1
          ? maxOccurs == 1
              ? ZERO_OR_ONE
              : ZERO_OR_MORE
          : maxOccurs == 1
              ? ONE
              : ONE_OR_MORE;
    }

    private String generate() {
      return getLeft() + "--" + getRight();
    }
  }

  @Override
  public Void visitField(IFieldNodeItem item, PrintWriter writer) {
    processNodeItem(item, writer);
    return super.visitField(item, writer);
  }

  @Override
  public Void visitAssembly(IAssemblyNodeItem item, PrintWriter writer) {
    processNodeItem(item, writer);
    return super.visitAssembly(item, writer);
  }

  @Override
  public Void visitAssembly(IAssemblyInstanceGroupedNodeItem item, PrintWriter writer) {
    processNodeItem(item, writer);
    return super.visitAssembly(item, writer);
  }

  @Override
  protected Void defaultResult() {
    return null;
  }

  private void processNodeItem(@NonNull IModelNodeItem<?, ?> item, PrintWriter writer) {
    IAssemblyNodeItem left = item.getParentContentNodeItem();

    IAssemblyDefinition leftDefinition = left.getDefinition();
    INamedModelInstance rightInstance = item.getInstance();
    IModelDefinition rightDefinition = item.getDefinition();

    Relationship relationship
        = Relationship.toRelationship(rightInstance.getMinOccurs(), rightInstance.getMaxOccurs());

    writer.format("  %s %s %s : %s%n",
        leftDefinition.getEffectiveName(),
        relationship.generate(),
        rightDefinition.getEffectiveName(),
        rightInstance.getEffectiveName());
  }

  public void generate(@NonNull IBoundModule module, @NonNull PrintWriter writer) throws IOException {
    IModuleNodeItem moduleItem = INodeItemFactory.instance().newModuleNodeItem(module);

    for (IAssemblyDefinition root : module.getExportedRootAssemblyDefinitions()) {
      for (IModelNodeItem<?, ?> rootItem : moduleItem.getModelItemsByName(root.getDefinitionQName())) {
        IAssemblyDefinition definition = (IAssemblyDefinition) rootItem.getDefinition();

        writer.println("erDiagram");
        writer.format("  module ||--|| %s: \"%s\"%n", definition.getEffectiveName(), definition.getRootName());

        for (IModelNodeItem<?, ?> modelItem : CollectionUtil.toIterable(rootItem.modelItems())) {
          modelItem.accept(this, writer);
        }
      }
    }
  }
}
