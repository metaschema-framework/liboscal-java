/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.metadata;

import java.util.List;
import java.util.UUID;

import dev.metaschema.oscal.lib.model.Metadata.Location;
import dev.metaschema.oscal.lib.model.Metadata.Party;
import dev.metaschema.oscal.lib.model.Metadata.Role;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public interface IMetadata {
  @Nullable
  Party getPartyByUuid(@NonNull UUID uuid);

  List<Role> getRoles();

  List<Location> getLocations();

  List<Party> getParties();
}
