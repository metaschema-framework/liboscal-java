/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.selection;

import dev.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import dev.metaschema.core.metapath.item.node.IDocumentNodeItem;
import dev.metaschema.core.metapath.item.node.INodeItemFactory;
import dev.metaschema.core.metapath.item.node.IRootAssemblyNodeItem;
import dev.metaschema.core.util.CollectionUtil;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.databind.model.IBoundDefinitionModelAssembly;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.OscalModelConstants;
import dev.metaschema.oscal.lib.model.Catalog;
import dev.metaschema.oscal.lib.model.IncludeAll;
import dev.metaschema.oscal.lib.model.Profile;
import dev.metaschema.oscal.lib.model.ProfileImport;
import dev.metaschema.oscal.lib.model.control.catalog.AbstractControl;
import dev.metaschema.oscal.lib.model.control.profile.AbstractProfileSelectControlById;
import dev.metaschema.oscal.lib.profile.resolver.ProfileResolutionException;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.NonNull;

class ImportTest {

  @NonNull
  private static IDocumentNodeItem newImportedCatalog() {

    // setup the imported catalog
    Catalog importedCatalog = new Catalog();

    importedCatalog.addControl(AbstractControl.builder("control1")
        .title("Control 1")
        .build());
    importedCatalog.addControl(AbstractControl.builder("control2")
        .title("Control 2")
        .build());

    return INodeItemFactory.instance().newDocumentNodeItem(
        ObjectUtils.requireNonNull(
            (IBoundDefinitionModelAssembly) OscalBindingContext.instance().getBoundDefinitionForClass(Catalog.class)),
        ObjectUtils.notNull(Paths.get(System.getProperty("user.dir")).toUri()),
        importedCatalog);
  }

  @SuppressWarnings("null")
  @Test
  void test() throws ProfileResolutionException {
    URI cwd = Paths.get(System.getProperty("user.dir")).toUri();

    ProfileImport profileImport = new ProfileImport();
    profileImport.setIncludeAll(new IncludeAll());
    profileImport.setExcludeControls(Collections.singletonList(
        AbstractProfileSelectControlById.builder()
            .withId("control1")
            .build()));
    profileImport.setHref(cwd);

    // setup the profile
    Profile profile = new Profile();

    profile.addImport(profileImport);

    IDocumentNodeItem profileDocumentItem = INodeItemFactory.instance().newDocumentNodeItem(
        ObjectUtils.requireNonNull(
            (IBoundDefinitionModelAssembly) OscalBindingContext.instance().getBoundDefinitionForClass(Profile.class)),
        cwd,
        profile);
    // setup the imported catalog
    IDocumentNodeItem importedCatalogDocumentItem = newImportedCatalog();

    // setup the resolved catalog
    Catalog resolvedCatalog = new Catalog();
    for (IRootAssemblyNodeItem profileRootItem : CollectionUtil
        .toIterable(profileDocumentItem.getModelItemsByName(OscalModelConstants.QNAME_PROFILE).stream()
            .map(rootItem -> (IRootAssemblyNodeItem) rootItem))) {
      for (IAssemblyNodeItem importItem : CollectionUtil.toIterable(
          profileRootItem.getModelItemsByName(OscalModelConstants.QNAME_IMPORT).stream()
              .map(item -> (IAssemblyNodeItem) item))) {

        Import catalogImport = new Import(profileRootItem, importItem);
        catalogImport.resolve(
            importedCatalogDocumentItem,
            resolvedCatalog,
            (uri, src) -> importedCatalogDocumentItem.getBaseUri().resolve(uri));
      }

    }

  }
}
