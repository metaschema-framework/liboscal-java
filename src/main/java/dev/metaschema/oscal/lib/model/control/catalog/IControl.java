/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.catalog;

import java.util.List;

import dev.metaschema.oscal.lib.model.Control;
import dev.metaschema.oscal.lib.model.ControlPart;

public interface IControl extends IControlContainer {

  String getId();

  List<ControlPart> getParts();

  Control getParentControl();

  void setParentControl(Control parent);
}
