/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control;

import java.util.List;

public interface IControlSelection {

  String getWithChildControls();

  List<String> getWithIds();

  List<? extends IControlMatching> getMatching();

}
