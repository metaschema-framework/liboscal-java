/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.policy;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import dev.metaschema.core.metapath.item.node.IDocumentNodeItem;
import dev.metaschema.core.metapath.item.node.INodeItem;
import dev.metaschema.databind.io.Format;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.profile.resolver.TestUtil;
import dev.metaschema.oscal.lib.profile.resolver.selection.ControlSelectionState;
import dev.metaschema.oscal.lib.profile.resolver.selection.ControlSelectionVisitor;
import dev.metaschema.oscal.lib.profile.resolver.selection.IControlFilter;
import dev.metaschema.oscal.lib.profile.resolver.selection.IControlSelectionFilter;
import dev.metaschema.oscal.lib.profile.resolver.selection.IControlSelectionState;
import dev.metaschema.oscal.lib.profile.resolver.support.IIdentifierMapper;
import dev.metaschema.oscal.lib.profile.resolver.support.IIndexer;
import dev.metaschema.oscal.lib.profile.resolver.support.ReassignmentIndexer;

class ReferenceCountingVisitorTest {
  @SuppressWarnings("null")
  @Test
  void test() throws IOException {
    // setup the imported catalog
    IDocumentNodeItem importedCatalogDocumentItem = TestUtil.newImportedCatalog();

    // setup the selection visitor
    IControlFilter filter = IControlFilter.newInstance(
        IControlSelectionFilter.ALL_MATCH,
        IControlSelectionFilter.matchIds("control2", "control5", "control7"));
    IIdentifierMapper mapper = TestUtil.UUID_CONCAT_ID_MAPPER;
    IIndexer indexer = new ReassignmentIndexer(mapper);
    IControlSelectionState state = new ControlSelectionState(indexer, filter);

    // process selections
    ControlSelectionVisitor.instance().visitCatalog(importedCatalogDocumentItem, state);

    IIndexer.logIndex(indexer, Level.DEBUG);

    // setup reference counting
    ReferenceCountingVisitor.instance()
        .visitCatalog(
            importedCatalogDocumentItem,
            indexer,
            (uri, src) -> importedCatalogDocumentItem.getBaseUri().resolve(uri));

    IIndexer.logIndex(indexer, Level.DEBUG);

    OscalBindingContext.instance()
        .newSerializer(Format.YAML, Catalog.class)
        .serialize(
            (Catalog) INodeItem.toValue(importedCatalogDocumentItem),
            System.out);
  }

}
