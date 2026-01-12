/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control;

import java.util.List;
import java.util.stream.Stream;

import dev.metaschema.oscal.lib.model.ParameterSelection;
import dev.metaschema.oscal.lib.model.Property;
import edu.umd.cs.findbugs.annotations.NonNull;

public interface IParameter {
  List<Property> getProps();

  ParameterSelection getSelect();

  @NonNull
  Stream<String> getParameterReferences();
}
