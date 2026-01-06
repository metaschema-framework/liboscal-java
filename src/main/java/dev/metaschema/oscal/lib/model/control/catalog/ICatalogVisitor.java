/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.control.catalog;

import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.model.CatalogGroup;
import dev.metaschema.oscal.lib.model.Control;
import dev.metaschema.oscal.lib.model.Parameter;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface ICatalogVisitor<RESULT, CONTEXT> {
  /**
   * Visit the provided {@code catalog}.
   *
   * @param catalog
   *          the bound catalog object
   * @param context
   *          the visitor context
   * @return a meaningful result from visiting the object
   */
  RESULT visitCatalog(@NonNull Catalog catalog, CONTEXT context);

  /**
   * Visit the provided {@code group}.
   *
   * @param group
   *          the bound group object
   * @param context
   *          the visitor context
   * @return a meaningful result from visiting the object
   */
  RESULT visitGroup(@NonNull CatalogGroup group, CONTEXT context);

  /**
   * Visit the provided {@code control}.
   *
   * @param control
   *          the bound control object
   * @param context
   *          the visitor context
   * @return a meaningful result from visiting the object
   */
  RESULT visitControl(@NonNull Control control, CONTEXT context);

  /**
   * Visit the provided {@code parameter}.
   *
   * @param parameter
   *          the bound parameter object
   * @param context
   *          the visitor context
   * @return a meaningful result from visiting the object
   */
  RESULT visitParameter(@NonNull Parameter parameter, CONTEXT context);
}
