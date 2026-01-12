/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.selection;

import dev.metaschema.core.metapath.item.node.IModelNodeItem;
import dev.metaschema.oscal.lib.profile.resolver.support.IIndexer;
import edu.umd.cs.findbugs.annotations.NonNull;

public interface IControlSelectionState {
  @NonNull
  IIndexer getIndex();

  boolean isSelected(@NonNull IModelNodeItem<?, ?> item);
}
