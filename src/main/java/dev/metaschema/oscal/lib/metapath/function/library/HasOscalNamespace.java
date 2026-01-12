/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.metapath.function.library;

import java.net.URI;
import java.util.List;

import dev.metaschema.core.metapath.DynamicContext;
import dev.metaschema.core.metapath.MetapathConstants;
import dev.metaschema.core.metapath.function.FunctionUtils;
import dev.metaschema.core.metapath.function.IArgument;
import dev.metaschema.core.metapath.function.IFunction;
import dev.metaschema.core.metapath.function.InvalidTypeFunctionException;
import dev.metaschema.core.metapath.item.IItem;
import dev.metaschema.core.metapath.item.ISequence;
import dev.metaschema.core.metapath.item.atomic.IAnyUriItem;
import dev.metaschema.core.metapath.item.atomic.IBooleanItem;
import dev.metaschema.core.metapath.item.atomic.IStringItem;
import dev.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import dev.metaschema.core.metapath.item.node.IFieldNodeItem;
import dev.metaschema.core.metapath.item.node.IFlagNodeItem;
import dev.metaschema.core.metapath.item.node.IModelNodeItem;
import dev.metaschema.core.metapath.type.InvalidTypeMetapathException;
import dev.metaschema.core.model.IFlagInstance;
import dev.metaschema.core.model.IModelDefinition;
import dev.metaschema.core.qname.IEnhancedQName;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.oscal.lib.OscalModelConstants;
import dev.metaschema.oscal.lib.model.metadata.IProperty;
import edu.umd.cs.findbugs.annotations.NonNull;

public final class HasOscalNamespace {
  @NonNull
  private static final IEnhancedQName NS_FLAG_QNAME = IEnhancedQName.of("ns");
  @NonNull
  static final IFunction SIGNATURE_ONE_ARG = IFunction.builder()
      .name("has-oscal-namespace")
      .namespace(OscalModelConstants.NS_OSCAL)
      .argument(IArgument.builder()
          .name("namespace")
          .type(IStringItem.type())
          .oneOrMore()
          .build())
      .allowUnboundedArity(true)
      .returnType(IBooleanItem.type())
      .focusDependent()
      .contextIndependent()
      .deterministic()
      .returnOne()
      .functionHandler(HasOscalNamespace::executeOneArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_TWO_ARGS = IFunction.builder()
      .name("has-oscal-namespace")
      .namespace(OscalModelConstants.NS_OSCAL)
      .argument(IArgument.builder()
          .name("propOrPart")
          .type(IAssemblyNodeItem.type())
          .one()
          .build())
      .argument(IArgument.builder()
          .name("namespace")
          .type(IStringItem.type())
          .oneOrMore()
          .build())
      .allowUnboundedArity(true)
      .focusIndependent()
      .contextIndependent()
      .deterministic()
      .returnType(IBooleanItem.type())
      .returnOne()
      .functionHandler(HasOscalNamespace::executeTwoArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG_METAPATH = IFunction.builder()
      .name("has-oscal-namespace")
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS)
      .argument(IArgument.builder()
          .name("namespace")
          .type(IStringItem.type())
          .oneOrMore()
          .build())
      .allowUnboundedArity(true)
      .returnType(IBooleanItem.type())
      .focusDependent()
      .contextIndependent()
      .deterministic()
      .returnOne()
      .functionHandler(HasOscalNamespace::executeOneArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_TWO_ARGS_METAPATH = IFunction.builder()
      .name("has-oscal-namespace")
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS)
      .argument(IArgument.builder()
          .name("propOrPart")
          .type(IAssemblyNodeItem.type())
          .one()
          .build())
      .argument(IArgument.builder()
          .name("namespace")
          .type(IStringItem.type())
          .oneOrMore()
          .build())
      .allowUnboundedArity(true)
      .focusIndependent()
      .contextIndependent()
      .deterministic()
      .returnType(IBooleanItem.type())
      .returnOne()
      .functionHandler(HasOscalNamespace::executeTwoArg)
      .build();

  private HasOscalNamespace() {
    // disable construction
  }

  @SuppressWarnings({ "unused",
      "PMD.OnlyOneReturn" // readability
  })
  @NonNull
  public static ISequence<?> executeOneArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {
    assert arguments.size() == 1;
    ISequence<? extends IStringItem> namespaceArgs = FunctionUtils.asType(
        ObjectUtils.notNull(arguments.get(0)));

    if (namespaceArgs.isEmpty()) {
      return ISequence.empty();
    }

    // Support both assembly and field nodes
    IModelNodeItem<?, ?> node;
    if (focus instanceof IAssemblyNodeItem) {
      node = (IAssemblyNodeItem) focus;
    } else if (focus instanceof IFieldNodeItem) {
      node = (IFieldNodeItem) focus;
    } else {
      throw new InvalidTypeMetapathException(
          focus,
          String.format("Expected an assembly or field node, but got '%s'",
              focus == null ? "null" : focus.getClass().getName()));
    }
    return ISequence.of(hasNamespace(node, namespaceArgs));
  }

  @SuppressWarnings({ "unused",
      "PMD.OnlyOneReturn" // readability
  })
  @NonNull
  public static ISequence<?> executeTwoArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {
    assert arguments.size() == 2;

    ISequence<? extends IStringItem> namespaceArgs = FunctionUtils.asType(
        ObjectUtils.notNull(arguments.get(1)));
    if (namespaceArgs.isEmpty()) {
      return ISequence.empty();
    }

    ISequence<? extends IAssemblyNodeItem> nodeSequence = FunctionUtils.asType(
        ObjectUtils.notNull(arguments.get(0)));

    // always not null, since the first item is required
    IAssemblyNodeItem node = FunctionUtils.asType(ObjectUtils.requireNonNull(nodeSequence.getFirstItem(true)));
    return ISequence.of(hasNamespace(node, namespaceArgs));
  }

  @SuppressWarnings("PMD.LinguisticNaming") // false positive
  @NonNull
  public static IBooleanItem hasNamespace(
      @NonNull IModelNodeItem<?, ?> propOrPart,
      @NonNull ISequence<? extends IStringItem> namespaces) {
    Object propOrPartObject = propOrPart.getValue();
    if (propOrPartObject == null) {
      throw new InvalidTypeFunctionException(InvalidTypeFunctionException.NODE_HAS_NO_TYPED_VALUE, propOrPart);
    }

    URI nodeNamespace = null;
    // get the "ns" flag value
    IFlagNodeItem ns = propOrPart.getFlagByName(NS_FLAG_QNAME);
    if (ns == null) {
      // check if the node actually has a "ns" flag
      IModelDefinition definition = propOrPart.getDefinition();
      IFlagInstance flag = definition.getFlagInstanceByName(NS_FLAG_QNAME.getIndexPosition());
      if (flag == null) {
        // Node doesn't support namespaces, so it can't match any namespace
        return IBooleanItem.FALSE;
      }

      Object defaultValue = flag.getDefinition().getDefaultValue();
      if (defaultValue != null) {
        nodeNamespace = IAnyUriItem.valueOf(ObjectUtils.notNull(defaultValue.toString())).asUri();
      }
    } else {
      nodeNamespace = IAnyUriItem.cast(ObjectUtils.notNull(ns.toAtomicItem())).asUri();
    }

    String nodeNamespaceString = IProperty.normalizeNamespace(nodeNamespace).toString();
    return IBooleanItem.valueOf(namespaces.stream()
        .map(node -> nodeNamespaceString.equals(node.asString()))
        .anyMatch(bool -> bool));
  }
}
