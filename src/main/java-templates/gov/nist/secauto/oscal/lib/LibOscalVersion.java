/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib;

import gov.nist.secauto.metaschema.core.util.IVersionInfo;

public final class LibOscalVersion implements IVersionInfo {
  public static final String NAME = "liboscal-java";
  public static final String BUILD_VERSION = "${project.version}";
  public static final String BUILD_TIMESTAMP = "${timestamp}";
  public static final String COMMIT = "@git.commit.id.abbrev@";
  public static final String BRANCH = "@git.branch@";
  public static final String CLOSEST_TAG = "@git.closest.tag.name@";
  public static final String ORIGIN = "@git.remote.origin.url@";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getVersion() {
    return CLOSEST_TAG;
  }

  @Override
  public String getBuildTimestamp() {
    return BUILD_TIMESTAMP;
  }

  @Override
  public String getGitOriginUrl() {
    return ORIGIN;
  }

  @Override
  public String getGitCommit() {
    return COMMIT;
  }

  @Override
  public String getGitBranch() {
    return BRANCH;
  }

  @Override
  public String getGitClosestTag() {
    return CLOSEST_TAG;
  }
}
