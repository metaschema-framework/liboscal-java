/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model;

import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public interface IOscalInstance {
  UUID getUuid();

  Metadata getMetadata();

  BackMatter getBackMatter();

  /**
   * Lookup a backmatter resource by its UUID value.
   *
   * @param id
   *          the uuid value
   * @return the resource or {@code null} if no resource matched the UUID
   */
  @Nullable
  Resource getResourceByUuid(@NonNull UUID id);
}
