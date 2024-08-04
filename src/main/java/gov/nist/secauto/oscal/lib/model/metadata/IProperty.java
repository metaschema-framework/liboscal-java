/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.metadata;

import java.net.URI;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface IProperty {
  @SuppressWarnings("null")
  @NonNull
  URI OSCAL_NAMESPACE = URI.create("http://csrc.nist.gov/ns/oscal");
  @SuppressWarnings("null")
  @NonNull
  URI RMF_NAMESPACE = URI.create("http://csrc.nist.gov/ns/rmf");

  String getName();

  URI getNs();

  boolean isNamespaceEqual(@NonNull URI namespace);
}
