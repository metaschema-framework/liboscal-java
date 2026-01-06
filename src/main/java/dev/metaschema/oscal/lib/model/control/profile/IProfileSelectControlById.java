/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.profile;

import dev.metaschema.oscal.lib.model.ProfileMatching;

import java.util.List;

public interface IProfileSelectControlById {

  String getWithChildControls();

  List<String> getWithIds();

  List<ProfileMatching> getMatching();

}
