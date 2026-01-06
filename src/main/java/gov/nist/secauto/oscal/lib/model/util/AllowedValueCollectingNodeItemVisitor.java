/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.util;

import dev.metaschema.core.metapath.DynamicContext;
import dev.metaschema.core.metapath.StaticContext;
import dev.metaschema.core.metapath.item.ISequence;
import dev.metaschema.core.metapath.item.node.AbstractRecursionPreventingNodeItemVisitor;
import dev.metaschema.core.metapath.item.node.IAssemblyInstanceGroupedNodeItem;
import dev.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import dev.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import dev.metaschema.core.metapath.item.node.IFieldNodeItem;
import dev.metaschema.core.metapath.item.node.IFlagNodeItem;
import dev.metaschema.core.metapath.item.node.IModuleNodeItem;
import dev.metaschema.core.metapath.item.node.INodeItemFactory;
import dev.metaschema.core.model.IModule;
import dev.metaschema.core.model.constraint.IAllowedValuesConstraint;
import dev.metaschema.core.model.constraint.ILet;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;

public class AllowedValueCollectingNodeItemVisitor
    extends AbstractRecursionPreventingNodeItemVisitor<DynamicContext, Void> {

  private final Map<IDefinitionNodeItem<?, ?>, NodeItemRecord> nodeItemAnalysis = new LinkedHashMap<>();

  public Collection<NodeItemRecord> getAllowedValueLocations() {
    return nodeItemAnalysis.values();
  }

  public void visit(@NonNull IModule module) {
    DynamicContext context = new DynamicContext(
        StaticContext.builder()
            .defaultModelNamespace(module.getXmlNamespace())
            .build());
    context.disablePredicateEvaluation();

    visit(INodeItemFactory.instance().newModuleNodeItem(module), context);
  }

  public void visit(@NonNull IModuleNodeItem module, @NonNull DynamicContext context) {

    visitMetaschema(module, context);
  }

  private void handleAllowedValuesAtLocation(
      @NonNull IDefinitionNodeItem<?, ?> itemLocation,
      @NonNull DynamicContext context) {
    itemLocation.getDefinition().getAllowedValuesConstraints().stream()
        .forEachOrdered(allowedValues -> {
          ISequence<?> result = allowedValues.getTarget().evaluate(itemLocation, context);
          result.stream().forEachOrdered(target -> {
            assert target != null;
            handleAllowedValues(allowedValues, itemLocation, (IDefinitionNodeItem<?, ?>) target);
          });
        });
  }

  private void handleAllowedValues(
      @NonNull IAllowedValuesConstraint allowedValues,
      @NonNull IDefinitionNodeItem<?, ?> location,
      @NonNull IDefinitionNodeItem<?, ?> target) {
    NodeItemRecord itemRecord = nodeItemAnalysis.get(target);
    if (itemRecord == null) {
      itemRecord = new NodeItemRecord(target);
      nodeItemAnalysis.put(target, itemRecord);
    }

    AllowedValuesRecord allowedValuesRecord = new AllowedValuesRecord(allowedValues, location, target);
    itemRecord.addAllowedValues(allowedValuesRecord);
  }

  @Override
  public Void visitFlag(IFlagNodeItem item, DynamicContext context) {
    assert context != null;
    DynamicContext subContext = handleLetStatements(item, context);
    handleAllowedValuesAtLocation(item, subContext);
    return super.visitFlag(item, subContext);
  }

  @Override
  public Void visitField(IFieldNodeItem item, DynamicContext context) {
    assert context != null;
    DynamicContext subContext = handleLetStatements(item, context);
    handleAllowedValuesAtLocation(item, subContext);
    return super.visitField(item, subContext);
  }

  @Override
  public Void visitAssembly(IAssemblyNodeItem item, DynamicContext context) {
    assert context != null;
    DynamicContext subContext = handleLetStatements(item, context);
    handleAllowedValuesAtLocation(item, subContext);
    return super.visitAssembly(item, subContext);
  }

  private DynamicContext handleLetStatements(IDefinitionNodeItem<?, ?> item, DynamicContext context) {
    assert context != null;
    DynamicContext subContext = context;
    for (ILet let : item.getDefinition().getLetExpressions().values()) {
      ISequence<?> result = let.getValueExpression().evaluate(item,
          subContext).reusable();
      subContext = subContext.bindVariableValue(let.getName(), result);
    }
    return subContext;
  }

  @Override
  public Void visitAssembly(IAssemblyInstanceGroupedNodeItem item, DynamicContext context) {
    return visitAssembly((IAssemblyNodeItem) item, context);
  }

  @Override
  protected Void defaultResult() {
    return null;
  }

  public static final class NodeItemRecord {
    @NonNull
    private final IDefinitionNodeItem<?, ?> item;
    @NonNull
    private final List<AllowedValuesRecord> allowedValues = new LinkedList<>();

    private NodeItemRecord(@NonNull IDefinitionNodeItem<?, ?> item) {
      this.item = item;
    }

    @NonNull
    public IDefinitionNodeItem<?, ?> getItem() {
      return item;
    }

    @NonNull
    public List<AllowedValuesRecord> getAllowedValues() {
      return allowedValues;
    }

    public void addAllowedValues(@NonNull AllowedValuesRecord record) {
      this.allowedValues.add(record);
    }
  }

  public static final class AllowedValuesRecord {
    @NonNull
    private final IAllowedValuesConstraint allowedValues;
    @NonNull
    private final IDefinitionNodeItem<?, ?> location;
    @NonNull
    private final IDefinitionNodeItem<?, ?> target;

    public AllowedValuesRecord(
        @NonNull IAllowedValuesConstraint allowedValues,
        @NonNull IDefinitionNodeItem<?, ?> location,
        @NonNull IDefinitionNodeItem<?, ?> target) {
      this.allowedValues = allowedValues;
      this.location = location;
      this.target = target;
    }

    @NonNull
    public IAllowedValuesConstraint getAllowedValues() {
      return allowedValues;
    }

    @NonNull
    public IDefinitionNodeItem<?, ?> getLocation() {
      return location;
    }

    @NonNull
    public IDefinitionNodeItem<?, ?> getTarget() {
      return target;
    }
  }
}
