/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model;

import java.util.UUID;

import dev.metaschema.oscal.lib.model.BackMatter.Resource;
import dev.metaschema.oscal.lib.model.metadata.IBackMatter;
import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class AbstractOscalInstance implements IOscalInstance {

  @Override
  public Resource getResourceByUuid(@NonNull UUID uuid) {
    IBackMatter backMatter = getBackMatter();

    Resource retval = null;
    if (backMatter != null) {
      retval = backMatter.getResourceByUuid(uuid);
    }
    return retval;
  }
}
