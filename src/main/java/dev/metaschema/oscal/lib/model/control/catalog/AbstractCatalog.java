/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.catalog;

import java.util.stream.Stream;

import dev.metaschema.core.util.CollectionUtil;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.oscal.lib.model.AbstractOscalInstance;
import dev.metaschema.oscal.lib.model.control.AbstractParameter;
import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class AbstractCatalog
    extends AbstractOscalInstance
    implements ICatalog {

  @NonNull
  @Override
  public Stream<String> getReferencedParameterIds() {
    // get parameters referenced by the control's parameters
    return ObjectUtils.notNull(
        CollectionUtil.listOrEmpty(getParams()).stream()
            .flatMap(ObjectUtils::filterNull)
            .flatMap(AbstractParameter::getParameterReferences)
            .distinct());
  }
}
