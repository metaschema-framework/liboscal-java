/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import dev.metaschema.core.datatype.markup.MarkupMultiline;
import dev.metaschema.core.datatype.markup.flexmark.InsertAnchorExtension;
import dev.metaschema.core.datatype.markup.flexmark.InsertAnchorExtension.InsertAnchorNode;
import dev.metaschema.oscal.lib.model.ControlPart;
import edu.umd.cs.findbugs.annotations.NonNull;

public interface IPart {
  MarkupMultiline getProse();

  List<ControlPart> getParts();

  @NonNull
  Stream<InsertAnchorExtension.InsertAnchorNode> getInserts(@NonNull Predicate<InsertAnchorNode> filter);
}
