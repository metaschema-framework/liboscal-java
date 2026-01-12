/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.catalog;

import java.util.List;

import dev.metaschema.oscal.lib.model.ControlPart;

public interface ICatalogGroup extends IGroupContainer {
  List<ControlPart> getParts();
}
