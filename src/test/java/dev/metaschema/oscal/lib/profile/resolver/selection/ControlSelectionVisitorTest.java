/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.metaschema.core.metapath.item.node.IDocumentNodeItem;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.oscal.lib.model.CatalogGroup;
import dev.metaschema.oscal.lib.model.Control;
import dev.metaschema.oscal.lib.model.control.catalog.IControlContainer;
import dev.metaschema.oscal.lib.profile.resolver.TestUtil;
import dev.metaschema.oscal.lib.profile.resolver.support.BasicIndexer;
import dev.metaschema.oscal.lib.profile.resolver.support.IEntityItem;
import dev.metaschema.oscal.lib.profile.resolver.support.IIndexer;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ControlSelectionVisitorTest {

  @Test
  void test() {
    // setup the imported catalog
    IDocumentNodeItem importedCatalogDocumentItem = TestUtil.newImportedCatalog();

    IControlFilter filter = IControlFilter.newInstance(
        IControlSelectionFilter.ALL_MATCH,
        IControlSelectionFilter.matchIds("control2", "control5", "control7"));

    IIndexer indexer = new BasicIndexer();
    IControlSelectionState state = new ControlSelectionState(indexer, filter);
    ControlSelectionVisitor.instance().visitCatalog(importedCatalogDocumentItem, state);

    Set<String> selected = Stream.concat(
        indexer.getEntitiesByItemType(IEntityItem.ItemType.GROUP).stream(),
        indexer.getEntitiesByItemType(IEntityItem.ItemType.CONTROL).stream())
        .filter(entry -> indexer.isSelected(ObjectUtils.notNull(entry)))
        .map(entry -> {
          IControlContainer container = entry.getInstanceValue();
          String id;
          if (container instanceof Control) {
            id = ((Control) container).getId();
          } else if (container instanceof CatalogGroup) {
            id = ((CatalogGroup) container).getId();
          } else {
            throw new UnsupportedOperationException(
                String.format("Invalid container type: %s", container.getClass().getName()));
          }
          return id;
        })
        .collect(Collectors.toSet());
    assertEquals(Set.of("control1", "control3", "control4", "control6", "control8", "group1", "group2"), selected);
  }
}
