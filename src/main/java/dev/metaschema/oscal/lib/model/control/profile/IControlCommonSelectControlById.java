/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.profile;

import java.util.List;

import dev.metaschema.oscal.lib.model.ProfileMatching;
import dev.metaschema.oscal.lib.model.control.IControlSelection;

public interface IControlCommonSelectControlById extends IControlSelection {

  @Override
  List<ProfileMatching> getMatching();

}
