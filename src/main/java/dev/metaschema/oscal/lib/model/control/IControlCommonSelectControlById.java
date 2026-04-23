/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control;

import java.util.List;

import dev.metaschema.oscal.lib.model.ProfileMatching;

public interface IControlCommonSelectControlById extends IControlSelection {

  @Override
  List<ProfileMatching> getMatching();

}
