/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.metapath.function.library;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import dev.metaschema.core.metapath.DynamicContext;
import dev.metaschema.core.metapath.MetapathConstants;
import dev.metaschema.core.metapath.function.DocumentFunctionException;
import dev.metaschema.core.metapath.function.FunctionUtils;
import dev.metaschema.core.metapath.function.IArgument;
import dev.metaschema.core.metapath.function.IFunction;
import dev.metaschema.core.metapath.function.library.FnDoc;
import dev.metaschema.core.metapath.function.library.FnResolveUri;
import dev.metaschema.core.metapath.item.IItem;
import dev.metaschema.core.metapath.item.ISequence;
import dev.metaschema.core.metapath.item.atomic.IAnyUriItem;
import dev.metaschema.core.metapath.item.node.IDocumentNodeItem;
import dev.metaschema.core.metapath.item.node.INodeItem;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.oscal.lib.OscalModelConstants;
import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.profile.resolver.ProfileResolutionException;
import dev.metaschema.oscal.lib.profile.resolver.ProfileResolver;
import edu.umd.cs.findbugs.annotations.NonNull;

public final class ResolveProfile {

  @NonNull
  static final IFunction SIGNATURE_NO_ARG = IFunction.builder()
      .name("resolve-profile")
      .namespace(OscalModelConstants.NS_OSCAL)
      .returnType(INodeItem.type())
      .focusDependent()
      .contextDependent()
      .deterministic()
      .returnOne()
      .functionHandler(ResolveProfile::executeNoArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG = IFunction.builder()
      .name("resolve-profile")
      .namespace(OscalModelConstants.NS_OSCAL)
      .argument(IArgument.builder()
          .name("profile")
          .type(INodeItem.type())
          .zeroOrOne()
          .build())
      .focusIndependent()
      .contextDependent()
      .deterministic()
      .returnType(INodeItem.type())
      .returnOne()
      .functionHandler(ResolveProfile::executeOneArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_NO_ARG_METAPATH = IFunction.builder()
      .name("resolve-profile")
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS)
      .returnType(INodeItem.type())
      .focusDependent()
      .contextDependent()
      .deterministic()
      .returnOne()
      .functionHandler(ResolveProfile::executeNoArg)
      .build();

  @NonNull
  static final IFunction SIGNATURE_ONE_ARG_METAPATH = IFunction.builder()
      .name("resolve-profile")
      .namespace(MetapathConstants.NS_METAPATH_FUNCTIONS)
      .argument(IArgument.builder()
          .name("profile")
          .type(INodeItem.type())
          .zeroOrOne()
          .build())
      .focusIndependent()
      .contextDependent()
      .deterministic()
      .returnType(INodeItem.type())
      .returnOne()
      .functionHandler(ResolveProfile::executeOneArg)
      .build();

  private ResolveProfile() {
    // disable construction
  }

  @SuppressWarnings({ "unused",
      "PMD.OnlyOneReturn" // readability
  })
  @NonNull
  public static ISequence<?> executeNoArg(
      @NonNull IFunction function,
      @NonNull List<ISequence<?>> arguments,
      @NonNull DynamicContext dynamicContext,
      IItem focus) {

    if (focus == null) {
      return ISequence.empty();
    }
    return ISequence.of(resolveProfile(FunctionUtils.asType(focus), dynamicContext));
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
    ISequence<? extends IDocumentNodeItem> arg = FunctionUtils.asType(
        ObjectUtils.notNull(arguments.get(0)));

    IItem item = arg.getFirstItem(true);
    if (item == null) {
      return ISequence.empty();
    }

    return ISequence.of(resolveProfile(FunctionUtils.asType(item), dynamicContext));
  }

  @NonNull
  public static IDocumentNodeItem resolveProfile(
      @NonNull IDocumentNodeItem document,
      @NonNull DynamicContext dynamicContext) {

    // make this work with unresolved fragments
    URI documentUri = document.getBaseUri();
    String fragment = documentUri.getFragment();

    IDocumentNodeItem profile;
    if (fragment == null) {
      profile = document;
    } else {
      IAnyUriItem referenceUri = ResolveReference.resolveReference(IAnyUriItem.valueOf(documentUri), null, document);
      IAnyUriItem resolvedUri = FnResolveUri.fnResolveUri(referenceUri, null, dynamicContext);
      profile = FnDoc.fnDoc(resolvedUri, dynamicContext);
    }

    Object profileObject = INodeItem.toValue(profile);

    IDocumentNodeItem retval;
    if (profileObject instanceof Catalog) {
      retval = profile;
    } else {
      // this is a profile
      ProfileResolver resolver
          = new ProfileResolver(dynamicContext, (uri, source) -> profile.getDocumentUri().resolve(uri));
      try {
        retval = resolver.resolve(profile);
      } catch (IOException | ProfileResolutionException ex) {
        throw new DocumentFunctionException(
            DocumentFunctionException.ERROR_RETRIEVING_RESOURCE,
            String.format("Unable to resolve profile '%s'. %s",
                profile.getBaseUri(),
                ex.getLocalizedMessage()),
            ex);
      }
    }
    return retval;
  }
}
