/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.selection;

import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.model.CatalogGroup;
import dev.metaschema.oscal.lib.model.Control;
import dev.metaschema.oscal.lib.model.Parameter;
import edu.umd.cs.findbugs.annotations.NonNull;

public interface IResult {

  void promoteParameter(@NonNull Parameter param);

  void promoteControl(@NonNull Control control);

  void applyTo(@NonNull Catalog parent);

  void applyTo(@NonNull CatalogGroup parent);

  void applyTo(@NonNull Control parent);
}
