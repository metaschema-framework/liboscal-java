/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.metapath.function.library;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.function.FunctionUtils;
import gov.nist.secauto.metaschema.core.metapath.function.IArgument;
import gov.nist.secauto.metaschema.core.metapath.function.IFunction;
import gov.nist.secauto.metaschema.core.metapath.function.InvalidArgumentFunctionException;
import gov.nist.secauto.metaschema.core.metapath.function.UnidentifiedFunctionError;
import gov.nist.secauto.metaschema.core.metapath.function.library.FnRoot;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IAnyUriItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IStringItem;
import gov.nist.secauto.metaschema.core.metapath.item.atomic.IUuidItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.oscal.lib.OscalModelConstants;
import gov.nist.secauto.oscal.lib.OscalUtils;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.IOscalInstance;

import java.net.URI;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Supports resolving a link to a backmatter resource.
 */
public final class ResolveReference {
  private static final String NAME = "resolve-reference";

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG = IFunction.builder()
      .name(NAME)
      .namespace(OscalModelConstants.NS_OSCAL)
      .argument(IArgument.builder()
          .name("uri")
          .type(IAnyUriItem.type())
          .zeroOrOne()
          .build())
      .focusDependent()
      .contextDependent()
      .deterministic()
      .returnZeroOrOne()
      .returnOne()
      .functionHandler(ResolveReference::executeOneArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_TWO_ARGS = IFunction.builder()
      .name(NAME)
      .namespace(OscalModelConstants.NS_OSCAL)
      .argument(IArgument.builder()
          .name("uri")
          .type(IAnyUriItem.type())
          .zeroOrOne()
          .build())
      .argument(IArgument.builder()
          .name("mediaType")
          .type(IStringItem.type())
          .zeroOrOne()
          .build())
      .focusIndependent()
      .contextDependent()
      .deterministic()
      .returnType(IAnyUriItem.type())
      .returnZeroOrOne()
      .functionHandler(ResolveReference::executeTwoArg)
      .build();

  private ResolveReference() {
    // disable construction
  }

  @SuppressWarnings({ "unused",
      "PMD.OnlyOneReturn" // readability
  })
  @NonNull
  private static ISequence<?> executeOneArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {
    IAnyUriItem uri = FunctionUtils.asTypeOrNull(ObjectUtils.requireNonNull(arguments.get(0).getFirstItem(true)));

    if (uri == null) {
      return ISequence.empty();
    }

    INodeItem node = checkForNodeItem(focus);
    return ISequence.of(resolveReference(uri, null, node));
  }

  @SuppressWarnings({ "unused",
      "PMD.OnlyOneReturn" // readability
  })
  @NonNull
  private static ISequence<?> executeTwoArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {
    IAnyUriItem uri = FunctionUtils.asTypeOrNull(ObjectUtils.requireNonNull(arguments.get(0).getFirstItem(true)));

    if (uri == null) {
      return ISequence.empty();
    }

    // this function is focus dependent, so the focus must be non null
    assert focus != null;

    IStringItem mediaType = FunctionUtils.asTypeOrNull(ObjectUtils.requireNonNull(arguments.get(1).getFirstItem(true)));
    INodeItem node = checkForNodeItem(focus);
    return ISequence.of(resolveReference(uri, mediaType, node));
  }

  /**
   * Ensure the provided item is a node item.
   *
   * @param item
   *          the item to check
   * @return the item as a node item
   * @throws InvalidArgumentFunctionException
   *           with code
   *           {@link InvalidArgumentFunctionException#INVALID_ARGUMENT_TYPE} if
   *           the item is not a node item
   */
  private static INodeItem checkForNodeItem(@NonNull IItem item) {
    if (!(item instanceof INodeItem)) {
      // this is expected to be a node
      throw new InvalidArgumentFunctionException(
          InvalidArgumentFunctionException.INVALID_ARGUMENT_TYPE,
          String.format("Item of type '%s' is not a node item.", item.getClass().getName()));
    }
    return (INodeItem) item;
  }

  @NonNull
  public static IAnyUriItem resolveReference(
      @NonNull IAnyUriItem reference,
      @Nullable IStringItem mediaType,
      @NonNull INodeItem focusedItem) {
    INodeItem root = FnRoot.fnRoot(focusedItem);
    IOscalInstance oscalInstance = (IOscalInstance) INodeItem.toValue(root);

    String fragment = reference.asUri().getFragment();

    return fragment == null
        ? reference
        : IAnyUriItem.valueOf(resolveReference(
            fragment,
            mediaType == null ? null : mediaType.asString(),
            oscalInstance));
  }

  @NonNull
  public static URI resolveReference(
      @NonNull String reference,
      @Nullable String mediaType,
      @NonNull IOscalInstance oscalInstance) {
    Resource resource = oscalInstance.getResourceByUuid(IUuidItem.valueOf(reference).asUuid());
    if (resource == null) {
      throw new UnidentifiedFunctionError(
          String.format("A backmatter resource with the id '%s' does not exist.", reference));
    }

    Rlink rLink = OscalUtils.findMatchingRLink(resource, mediaType);
    if (rLink == null) {
      throw new UnidentifiedFunctionError(
          String.format("The backmatter resource '%s' does not have an rlink entry.", reference));
    }
    URI retval = rLink.getHref();
    if (retval == null) {
      throw new UnidentifiedFunctionError(
          String.format("The backmatter resource '%s' has an rlink with a null href value.", reference));
    }
    return retval;
  }
}
