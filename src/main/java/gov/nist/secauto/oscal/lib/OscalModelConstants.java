/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib;

import gov.nist.secauto.metaschema.core.util.ObjectUtils;

import java.net.URI;

import javax.xml.namespace.QName;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class OscalModelConstants {

  @NonNull
  public static final String NS_OSCAL = "http://csrc.nist.gov/ns/oscal/1.0";
  @NonNull
  public static final URI NS_URI_OSCAL = ObjectUtils.notNull(URI.create(NS_OSCAL));
  @NonNull
  public static final QName QNAME_METADATA = new QName(NS_OSCAL, "metadata");
  @NonNull
  public static final QName QNAME_BACK_MATTER = new QName(NS_OSCAL, "back-matter");
  @NonNull
  public static final QName QNAME_PROFILE = new QName(NS_OSCAL, "profile");
  @NonNull
  public static final QName QNAME_IMPORT = new QName(NS_OSCAL, "import");
  @NonNull
  public static final QName QNAME_TITLE = new QName(NS_OSCAL, "title");
  @NonNull
  public static final QName QNAME_PROP = new QName(NS_OSCAL, "prop");
  @NonNull
  public static final QName QNAME_LINK = new QName(NS_OSCAL, "link");
  @NonNull
  public static final QName QNAME_CITATION = new QName(NS_OSCAL, "citation");
  @NonNull
  public static final QName QNAME_TEXT = new QName(NS_OSCAL, "text");
  @NonNull
  public static final QName QNAME_PROSE = new QName(NS_OSCAL, "prose");
  @NonNull
  public static final QName QNAME_PARAM = new QName(NS_OSCAL, "param");
  @NonNull
  public static final QName QNAME_ROLE = new QName(NS_OSCAL, "role");
  @NonNull
  public static final QName QNAME_LOCATION = new QName(NS_OSCAL, "location");
  @NonNull
  public static final QName QNAME_PARTY = new QName(NS_OSCAL, "party");
  @NonNull
  public static final QName QNAME_GROUP = new QName(NS_OSCAL, "group");
  @NonNull
  public static final QName QNAME_CONTROL = new QName(NS_OSCAL, "control");

  private OscalModelConstants() {
    // disable construction
  }
}
