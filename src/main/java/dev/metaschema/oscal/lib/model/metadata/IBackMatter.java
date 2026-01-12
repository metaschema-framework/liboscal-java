/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.metadata;

import java.util.List;
import java.util.UUID;

import dev.metaschema.oscal.lib.model.BackMatter.Resource;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public interface IBackMatter {
  List<Resource> getResources();

  @Nullable
  Resource getResourceByUuid(@NonNull UUID uuid);
}
