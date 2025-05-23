/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.profile.resolver;

import gov.nist.secauto.metaschema.core.metapath.DynamicContext;
import gov.nist.secauto.metaschema.core.metapath.IDocumentLoader;
import gov.nist.secauto.metaschema.core.metapath.IMetapathExpression;
import gov.nist.secauto.metaschema.core.metapath.StaticContext;
import gov.nist.secauto.metaschema.core.metapath.format.IPathFormatter;
import gov.nist.secauto.metaschema.core.metapath.function.FunctionUtils;
import gov.nist.secauto.metaschema.core.metapath.item.IItem;
import gov.nist.secauto.metaschema.core.metapath.item.ISequence;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDocumentNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.metapath.item.node.IRootAssemblyNodeItem;
import gov.nist.secauto.metaschema.core.model.IBoundObject;
import gov.nist.secauto.metaschema.core.qname.IEnhancedQName;
import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.metaschema.databind.io.BindingException;
import gov.nist.secauto.metaschema.databind.io.DeserializationFeature;
import gov.nist.secauto.metaschema.databind.io.IBoundLoader;
import gov.nist.secauto.metaschema.databind.model.IBoundDefinitionModelAssembly;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.OscalModelConstants;
import gov.nist.secauto.oscal.lib.OscalUtils;
import gov.nist.secauto.oscal.lib.model.BackMatter;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Base64;
import gov.nist.secauto.oscal.lib.model.BackMatter.Resource.Rlink;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.Merge;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.Metadata.Location;
import gov.nist.secauto.oscal.lib.model.Metadata.Party;
import gov.nist.secauto.oscal.lib.model.Metadata.Role;
import gov.nist.secauto.oscal.lib.model.Modify;
import gov.nist.secauto.oscal.lib.model.Modify.ProfileSetParameter;
import gov.nist.secauto.oscal.lib.model.Parameter;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.ProfileImport;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.metadata.AbstractLink;
import gov.nist.secauto.oscal.lib.model.metadata.AbstractProperty;
import gov.nist.secauto.oscal.lib.profile.resolver.alter.AddVisitor;
import gov.nist.secauto.oscal.lib.profile.resolver.alter.RemoveVisitor;
import gov.nist.secauto.oscal.lib.profile.resolver.merge.FlatteningStructuringVisitor;
import gov.nist.secauto.oscal.lib.profile.resolver.selection.Import;
import gov.nist.secauto.oscal.lib.profile.resolver.selection.ImportCycleException;
import gov.nist.secauto.oscal.lib.profile.resolver.support.BasicIndexer;
import gov.nist.secauto.oscal.lib.profile.resolver.support.ControlIndexingVisitor;
import gov.nist.secauto.oscal.lib.profile.resolver.support.IEntityItem;
import gov.nist.secauto.oscal.lib.profile.resolver.support.IEntityItem.ItemType;
import gov.nist.secauto.oscal.lib.profile.resolver.support.IIndexer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class ProfileResolver {

  public enum StructuringDirective {
    FLAT,
    AS_IS,
    CUSTOM;
  }

  private static final Logger LOGGER = LogManager.getLogger(ProfileResolver.class);
  @NonNull
  private static final IEnhancedQName IMPORT_QNAME = IEnhancedQName.of(OscalModelConstants.NS_OSCAL, "import");

  @NonNull
  private static final IMetapathExpression METAPATH_SET_PARAMETER
      = IMetapathExpression.compile("modify/set-parameter",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);
  @NonNull
  private static final IMetapathExpression METAPATH_ALTER
      = IMetapathExpression.compile("modify/alter",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);
  @NonNull
  private static final IMetapathExpression METAPATH_ALTER_REMOVE
      = IMetapathExpression.compile("remove",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);
  @NonNull
  private static final IMetapathExpression METAPATH_ALTER_ADD
      = IMetapathExpression.compile("add",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);
  @NonNull
  private static final IMetapathExpression CATALOG_OR_PROFILE
      = IMetapathExpression.compile("/(catalog|profile)",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);
  @NonNull
  private static final IMetapathExpression CATALOG
      = IMetapathExpression.compile("/catalog",
          OscalBindingContext.OSCAL_STATIC_METAPATH_CONTEXT);

  @NonNull
  private final DynamicContext dynamicContext;
  @NonNull
  private final UriResolver uriResolver;

  public ProfileResolver() {
    this(newDynamicContext(), (uri, source) -> source.resolve(uri));
  }

  public ProfileResolver(
      @NonNull DynamicContext dynamicContext,
      @NonNull UriResolver uriResolver) {
    this.dynamicContext = dynamicContext;
    this.uriResolver = uriResolver;
  }

  @NonNull
  private static DynamicContext newDynamicContext() {
    IBoundLoader loader = OscalBindingContext.instance().newBoundLoader();
    loader.disableFeature(DeserializationFeature.DESERIALIZE_VALIDATE_CONSTRAINTS);

    DynamicContext retval = new DynamicContext(StaticContext.builder()
        .defaultModelNamespace(OscalModelConstants.NS_OSCAL)
        .build());
    retval.setDocumentLoader(loader);
    return retval;
  }

  /**
   * Gets the configured loader or creates a new default loader if no loader was
   * configured.
   *
   * @return the bound loader
   * @since 5.0.0
   */
  @NonNull
  public IDocumentLoader getDocumentLoader() {
    return getDynamicContext().getDocumentLoader();
  }

  @NonNull
  public DynamicContext getDynamicContext() {
    return dynamicContext;
  }

  @Nullable
  private static IRootAssemblyNodeItem getRoot(
      @NonNull IDocumentNodeItem document,
      @NonNull IMetapathExpression rootPath) {
    ISequence<?> result = rootPath.evaluate(document);
    IItem item = result.getFirstItem(false);

    return item == null ? null : FunctionUtils.asType(item);
  }

  @NonNull
  public IDocumentNodeItem resolve(@NonNull URL url)
      throws URISyntaxException, IOException, ProfileResolutionException {
    IDocumentLoader loader = getDocumentLoader();
    IDocumentNodeItem catalogOrProfile = loader.loadAsNodeItem(url);
    return resolve(catalogOrProfile, new Stack<>());
  }

  @NonNull
  public IDocumentNodeItem resolve(@NonNull File file) throws IOException, ProfileResolutionException {
    return resolve(ObjectUtils.notNull(file.toPath()));
  }

  @NonNull
  public IDocumentNodeItem resolve(@NonNull Path path) throws IOException, ProfileResolutionException {
    IDocumentLoader loader = getDocumentLoader();
    IDocumentNodeItem catalogOrProfile = loader.loadAsNodeItem(path);
    return resolve(catalogOrProfile, new Stack<>());
  }

  @NonNull
  public IDocumentNodeItem resolve(
      @NonNull IDocumentNodeItem profileOrCatalogDocument)
      throws IOException, ProfileResolutionException {
    return resolve(profileOrCatalogDocument, new Stack<>());
  }

  @NonNull
  public IDocumentNodeItem resolve(
      @NonNull IDocumentNodeItem profileOrCatalogDocument,
      @NonNull Stack<URI> importHistory)
      throws IOException, ProfileResolutionException {
    IRootAssemblyNodeItem profileOrCatalog = getRoot(
        profileOrCatalogDocument,
        CATALOG_OR_PROFILE);
    if (profileOrCatalog == null) {
      throw new ProfileResolutionException(
          String.format("The provided document '%s' does not contain a catalog or profile.",
              profileOrCatalogDocument.getDocumentUri()));
    }
    return resolve(profileOrCatalog, importHistory);
  }

  @NonNull
  public IDocumentNodeItem resolve(
      @NonNull IRootAssemblyNodeItem profileOrCatalog,
      @NonNull Stack<URI> importHistory)
      throws IOException, ProfileResolutionException {
    Object profileObject = profileOrCatalog.getValue();

    IDocumentNodeItem retval;
    if (profileObject instanceof Catalog) {
      // already a catalog
      retval = profileOrCatalog.getParentNodeItem();
    } else {
      // must be a profile
      retval = resolveProfile(profileOrCatalog, importHistory);
    }
    return retval;
  }

  /**
   * Resolve the profile to a catalog.
   *
   * @param profileItem
   *          a {@link IDocumentNodeItem} containing the profile to resolve
   * @param importHistory
   *          the import stack for cycle detection
   * @return the resolved profile
   * @throws IOException
   *           if an error occurred while loading the profile or an import
   * @throws ProfileResolutionException
   *           if an error occurred while resolving the profile
   */
  @NonNull
  protected IDocumentNodeItem resolveProfile(
      @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull Stack<URI> importHistory) throws IOException, ProfileResolutionException {
    Catalog resolvedCatalog = new Catalog();

    generateMetadata(resolvedCatalog, profileItem);

    IIndexer index = resolveImports(resolvedCatalog, profileItem, importHistory);
    handleReferences(resolvedCatalog, profileItem, index);
    handleMerge(resolvedCatalog, profileItem, index);
    handleModify(resolvedCatalog, profileItem);

    return INodeItemFactory.instance().newDocumentNodeItem(
        ObjectUtils.requireNonNull(
            (IBoundDefinitionModelAssembly) OscalBindingContext.instance().getBoundDefinitionForClass(Catalog.class)),
        ObjectUtils.requireNonNull(profileItem.getBaseUri()),
        resolvedCatalog);
  }

  @NonNull
  private static Profile toProfile(@NonNull IRootAssemblyNodeItem profileItem) {
    Object object = profileItem.getValue();
    assert object != null;

    return (Profile) object;
  }

  private void generateMetadata(
      @NonNull Catalog resolvedCatalog,
      @NonNull IRootAssemblyNodeItem profileItem) {
    resolvedCatalog.setUuid(UUID.randomUUID());

    Profile profile = toProfile(profileItem);
    Metadata profileMetadata = profile.getMetadata();

    Metadata resolvedMetadata = new Metadata();
    resolvedMetadata.setTitle(profileMetadata.getTitle());

    if (profileMetadata.getVersion() != null) {
      resolvedMetadata.setVersion(profileMetadata.getVersion());
    }

    // metadata.setOscalVersion(OscalUtils.OSCAL_VERSION);
    resolvedMetadata.setOscalVersion(profileMetadata.getOscalVersion());

    resolvedMetadata.setLastModified(ZonedDateTime.now(ZoneOffset.UTC));

    resolvedMetadata.addProp(AbstractProperty.builder("resolution-tool").value("libOSCAL-Java").build());

    URI documentUri = profileItem.getParentNodeItem().getDocumentUri();
    URI profileUri = ObjectUtils.requireNonNull(uriResolver.resolve(documentUri, documentUri));
    resolvedMetadata.addLink(AbstractLink.builder(profileUri)
        .relation("source-profile")
        .build());

    resolvedCatalog.setMetadata(resolvedMetadata);
  }

  @NonNull
  private IIndexer resolveImports(
      @NonNull Catalog resolvedCatalog,
      @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull Stack<URI> importHistory)
      throws IOException, ProfileResolutionException {

    // first verify there is at least one import
    @SuppressWarnings("unchecked")
    List<IAssemblyNodeItem> profileImports
        = (List<IAssemblyNodeItem>) profileItem.getModelItemsByName(IMPORT_QNAME);
    if (profileImports.isEmpty()) {
      throw new ProfileResolutionException(String.format("Profile '%s' has no imports", profileItem.getBaseUri()));
    }

    // now process each import
    IIndexer retval = new BasicIndexer();
    for (IAssemblyNodeItem profileImportItem : profileImports) {
      IIndexer result = resolveImport(
          ObjectUtils.notNull(profileImportItem),
          profileItem,
          importHistory,
          resolvedCatalog);
      retval.append(result);
    }
    return retval;
  }

  @NonNull
  protected IIndexer resolveImport(
      @NonNull IAssemblyNodeItem profileImportItem,
      @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull Stack<URI> importHistory,
      @NonNull Catalog resolvedCatalog) throws IOException, ProfileResolutionException {
    ProfileImport profileImport = ObjectUtils.requireNonNull((ProfileImport) profileImportItem.getValue());

    URI importUri = profileImport.getHref();
    if (importUri == null) {
      throw new ProfileResolutionException("profileImport.getHref() must return a non-null URI");
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.atDebug().log("resolving profile import '{}'", importUri);
    }

    IDocumentNodeItem importedDocument = getImport(importUri, profileItem);
    URI importedUri = importedDocument.getDocumentUri();
    assert importedUri != null; // always non-null

    // Import import = Import.
    // InputSource source = newImportSource(importUri, profileItem);
    // URI sourceUri = ObjectUtils.notNull(URI.create(source.getSystemId()));

    // check for import cycle
    try {
      requireNonCycle(
          importedUri,
          importHistory);
    } catch (ImportCycleException ex) {
      throw new IOException(ex);
    }

    // track the import in the import history
    importHistory.push(importedUri);
    try {
      IDocumentNodeItem importedCatalog = resolve(importedDocument, importHistory);

      // Create a defensive deep copy of the document and associated values, since we
      // will be making
      // changes to the data.
      try {
        IRootAssemblyNodeItem importedCatalogRoot = ObjectUtils.requireNonNull(getRoot(importedCatalog, CATALOG));
        Catalog catalogCopy = (Catalog) OscalBindingContext.instance().deepCopy(
            ObjectUtils.requireNonNull((IBoundObject) importedCatalogRoot.getValue()),
            null);

        importedCatalog = INodeItemFactory.instance().newDocumentNodeItem(
            importedCatalogRoot.getDefinition(),
            ObjectUtils.requireNonNull(importedCatalog.getDocumentUri()),
            catalogCopy);

        return new Import(profileItem, profileImportItem).resolve(importedCatalog, resolvedCatalog, uriResolver);
      } catch (BindingException ex) {
        throw new IOException(ex);
      }
    } finally {
      // pop the resolved catalog from the import history
      URI poppedUri = ObjectUtils.notNull(importHistory.pop());
      assert importedUri.equals(poppedUri);
    }
  }

  private IDocumentNodeItem getImport(
      @NonNull URI importUri,
      @NonNull IRootAssemblyNodeItem importingProfile) throws IOException {

    URI importingDocumentUri = ObjectUtils.requireNonNull(importingProfile.getParentNodeItem().getDocumentUri());

    IDocumentNodeItem retval;
    if (OscalUtils.isInternalReference(importUri)) {
      // handle internal reference
      String uuid = OscalUtils.internalReferenceFragmentToId(importUri);

      Profile profile = INodeItem.toValue(importingProfile);
      Resource resource = profile.getResourceByUuid(ObjectUtils.notNull(UUID.fromString(uuid)));
      if (resource == null) {
        throw new IOException(
            String.format("unable to find the resource identified by '%s' used in profile import", importUri));
      }

      retval = getImport(resource, importingDocumentUri);
    } else {
      URI uri = importingDocumentUri.resolve(importUri);
      assert uri != null;

      retval = getDynamicContext().getDocumentLoader().loadAsNodeItem(uri);
    }
    return retval;
  }

  @Nullable
  private IDocumentNodeItem getImport(
      @NonNull Resource resource,
      @NonNull URI baseUri) throws IOException {

    IDocumentLoader loader = getDynamicContext().getDocumentLoader();

    IDocumentNodeItem retval = null;
    // first try base64 data
    Base64 base64 = resource.getBase64();
    ByteBuffer buffer = base64 == null ? null : base64.getValue();
    if (buffer != null) {
      URI resourceUri = baseUri.resolve("#" + resource.getUuid());
      assert resourceUri != null;
      retval = loader.loadAsNodeItem(resourceUri);
    }

    if (retval == null) {
      Rlink rlink = OscalUtils.findMatchingRLink(resource, null);
      URI uri = rlink == null ? null : rlink.getHref();

      if (uri == null) {
        throw new IOException(
            String.format("Unable to determine URI for resource '%s'.", resource.getUuid()));
      }

      uri = baseUri.resolve(uri);
      assert uri != null;
      try {
        retval = loader.loadAsNodeItem(uri);
      } catch (IOException ex) {
        throw new IOException(String.format("Unable to load resource '%s'.", uri), ex);
      }
    }
    return retval;
  }

  private static void requireNonCycle(@NonNull URI uri, @NonNull Stack<URI> importHistory)
      throws ImportCycleException {
    List<URI> cycle = checkCycle(uri, importHistory);
    if (!cycle.isEmpty()) {
      throw new ImportCycleException(String.format("Importing resource '%s' would result in the import cycle: %s", uri,
          cycle.stream().map(URI::toString).collect(Collectors.joining(" -> ", " -> ", ""))));
    }
  }

  @NonNull
  private static List<URI> checkCycle(@NonNull URI uri, @NonNull Stack<URI> importHistory) {
    int index = importHistory.indexOf(uri);

    List<URI> retval;
    if (index == -1) {
      retval = CollectionUtil.emptyList();
    } else {
      retval = CollectionUtil.unmodifiableList(
          ObjectUtils.notNull(importHistory.subList(0, index + 1)));
    }
    return retval;
  }

  // TODO: move this to an abstract method on profile
  private static StructuringDirective getStructuringDirective(Profile profile) {
    Merge merge = profile.getMerge();

    StructuringDirective retval;
    if (merge == null) {
      retval = StructuringDirective.FLAT;
    } else if (merge.getAsIs() != null && merge.getAsIs()) {
      retval = StructuringDirective.AS_IS;
    } else if (merge.getCustom() != null) {
      retval = StructuringDirective.CUSTOM;
    } else {
      retval = StructuringDirective.FLAT;
    }
    return retval;
  }

  protected void handleMerge(
      @NonNull Catalog resolvedCatalog,
      @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull IIndexer importIndex) {
    // handle combine

    // handle structuring
    switch (getStructuringDirective(toProfile(profileItem))) {
    case AS_IS:
      // do nothing
      break;
    case CUSTOM:
      throw new UnsupportedOperationException("custom structuring");
    case FLAT:
    default:
      structureFlat(resolvedCatalog, profileItem, importIndex);
      break;
    }

  }

  protected void structureFlat(@NonNull Catalog resolvedCatalog, @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull IIndexer importIndex) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("applying flat structuring directive");
    }

    // {
    // // rebuild an index
    // IDocumentNodeItem resolvedCatalogItem =
    // DefaultNodeItemFactory.instance().newDocumentNodeItem(
    // new RootAssemblyDefinition(
    // ObjectUtils.notNull(
    // (IAssemblyClassBinding)
    // OscalBindingContext.instance().getClassBinding(Catalog.class))),
    // resolvedCatalog,
    // profileDocument.getBaseUri());
    //
    // // FIXME: need to find a better way to create an index that doesn't auto
    // select groups
    // IIndexer indexer = new BasicIndexer();
    // ControlSelectionVisitor selectionVisitor
    // = new ControlSelectionVisitor(IControlFilter.ALWAYS_MATCH, indexer);
    // selectionVisitor.visitCatalog(resolvedCatalogItem);
    // }

    // rebuild the document, since the paths have changed
    IDocumentNodeItem resolvedCatalogItem = INodeItemFactory.instance().newDocumentNodeItem(
        ObjectUtils.requireNonNull(
            (IBoundDefinitionModelAssembly) OscalBindingContext.instance().getBoundDefinitionForClass(Catalog.class)),
        ObjectUtils.requireNonNull(profileItem.getBaseUri()),
        resolvedCatalog);

    new FlatteningStructuringVisitor(uriResolver).visitCatalog(resolvedCatalogItem, importIndex);
  }

  @SuppressWarnings("PMD.ExceptionAsFlowControl") // ok
  protected void handleModify(@NonNull Catalog resolvedCatalog, @NonNull IRootAssemblyNodeItem profileItem)
      throws ProfileResolutionException {
    IDocumentNodeItem resolvedCatalogDocument = INodeItemFactory.instance().newDocumentNodeItem(
        ObjectUtils.requireNonNull(
            (IBoundDefinitionModelAssembly) OscalBindingContext.instance().getBoundDefinitionForClass(Catalog.class)),
        ObjectUtils.requireNonNull(profileItem.getBaseUri()),
        resolvedCatalog);

    try {
      IIndexer indexer = new BasicIndexer();
      ControlIndexingVisitor visitor = new ControlIndexingVisitor(
          ObjectUtils.notNull(EnumSet.of(IEntityItem.ItemType.CONTROL, IEntityItem.ItemType.PARAMETER)));
      visitor.visitCatalog(resolvedCatalogDocument, indexer);

      METAPATH_SET_PARAMETER.evaluate(profileItem)
          .forEach(item -> {
            IAssemblyNodeItem setParameter = (IAssemblyNodeItem) item;
            try {
              handleSetParameter(setParameter, indexer);
            } catch (ProfileResolutionEvaluationException ex) {
              throw new ProfileResolutionEvaluationException(
                  String.format("Unable to apply the set-parameter at '%s'. %s",
                      setParameter.toPath(IPathFormatter.METAPATH_PATH_FORMATER),
                      ex.getLocalizedMessage()),
                  ex);
            }
          });

      METAPATH_ALTER.evaluate(profileItem)
          .forEach(item -> {
            handleAlter((IAssemblyNodeItem) item, indexer);
          });
    } catch (ProfileResolutionEvaluationException ex) {
      throw new ProfileResolutionException(ex.getLocalizedMessage(), ex);
    }
  }

  protected void handleSetParameter(IAssemblyNodeItem item, IIndexer indexer) {
    ProfileSetParameter setParameter = ObjectUtils.requireNonNull((Modify.ProfileSetParameter) item.getValue());
    String paramId = ObjectUtils.requireNonNull(setParameter.getParamId());
    IEntityItem entity = indexer.getEntity(IEntityItem.ItemType.PARAMETER, paramId, false);
    if (entity == null) {
      throw new ProfileResolutionEvaluationException(
          String.format(
              "The parameter '%s' does not exist in the resolved catalog.",
              paramId));
    }

    Parameter param = entity.getInstanceValue();

    // apply the set parameter values
    param.setClazz(ModifyPhaseUtils.mergeItem(param.getClazz(), setParameter.getClazz()));
    param.setProps(ModifyPhaseUtils.merge(param.getProps(), setParameter.getProps(),
        ModifyPhaseUtils.identifierKey(Property::getUuid)));
    param.setLinks(ModifyPhaseUtils.merge(param.getLinks(), setParameter.getLinks(), ModifyPhaseUtils.identityKey()));
    param.setLabel(ModifyPhaseUtils.mergeItem(param.getLabel(), setParameter.getLabel()));
    param.setUsage(ModifyPhaseUtils.mergeItem(param.getUsage(), setParameter.getUsage()));
    param.setConstraints(
        ModifyPhaseUtils.merge(param.getConstraints(), setParameter.getConstraints(), ModifyPhaseUtils.identityKey()));
    param.setGuidelines(
        ModifyPhaseUtils.merge(param.getGuidelines(), setParameter.getGuidelines(), ModifyPhaseUtils.identityKey()));
    param.setValues(new LinkedList<>(setParameter.getValues()));
    param.setSelect(setParameter.getSelect());
  }

  @SuppressWarnings("PMD.ExceptionAsFlowControl")
  protected void handleAlter(IAssemblyNodeItem item, IIndexer indexer) {
    Modify.Alter alter = ObjectUtils.requireNonNull((Modify.Alter) item.getValue());
    String controlId = ObjectUtils.requireNonNull(alter.getControlId());
    IEntityItem entity = indexer.getEntity(IEntityItem.ItemType.CONTROL, controlId, false);
    if (entity == null) {
      throw new ProfileResolutionEvaluationException(
          String.format(
              "Unable to apply the alter targeting control '%s' at '%s'."
                  + " The control does not exist in the resolved catalog.",
              controlId,
              item.toPath(IPathFormatter.METAPATH_PATH_FORMATER)));
    }
    Control control = entity.getInstanceValue();

    METAPATH_ALTER_REMOVE.evaluate(item)
        .forEach(nodeItem -> {
          INodeItem removeItem = (INodeItem) nodeItem;
          Modify.Alter.Remove remove = ObjectUtils.notNull((Modify.Alter.Remove) removeItem.getValue());

          try {
            if (!RemoveVisitor.remove(
                control,
                remove.getByName(),
                remove.getByClass(),
                remove.getById(),
                remove.getByNs(),
                RemoveVisitor.TargetType.forFieldName(remove.getByItemName()))) {
              throw new ProfileResolutionEvaluationException(
                  String.format("The remove did not match a valid target"));
            }
          } catch (ProfileResolutionEvaluationException ex) {
            throw new ProfileResolutionEvaluationException(
                String.format("Unable to apply the remove targeting control '%s' at '%s'. %s",
                    control.getId(),
                    removeItem.toPath(IPathFormatter.METAPATH_PATH_FORMATER),
                    ex.getLocalizedMessage()),
                ex);
          }
        });
    METAPATH_ALTER_ADD.evaluate(item)
        .forEach(nodeItem -> {
          INodeItem addItem = (INodeItem) nodeItem;
          Modify.Alter.Add add = ObjectUtils.notNull((Modify.Alter.Add) addItem.getValue());
          String byId = add.getById();
          try {
            if (!AddVisitor.add(
                control,
                AddVisitor.Position.forName(add.getPosition()),
                byId,
                add.getTitle(),
                CollectionUtil.listOrEmpty(add.getParams()),
                CollectionUtil.listOrEmpty(add.getProps()),
                CollectionUtil.listOrEmpty(add.getLinks()),
                CollectionUtil.listOrEmpty(add.getParts()))) {

              throw new ProfileResolutionEvaluationException(
                  String.format("The add did not match a valid target"));
            }
          } catch (ProfileResolutionEvaluationException ex) {
            throw new ProfileResolutionEvaluationException(
                String.format("Unable to apply the add targeting control '%s'%s at '%s'. %s",
                    control.getId(),
                    byId == null ? "" : String.format(" having by-id '%s'", byId),
                    addItem.toPath(IPathFormatter.METAPATH_PATH_FORMATER),
                    ex.getLocalizedMessage()),
                ex);
          }
        });
  }

  private static void handleReferences(@NonNull Catalog resolvedCatalog, @NonNull IRootAssemblyNodeItem profileItem,
      @NonNull IIndexer index) {

    BasicIndexer profileIndex = new BasicIndexer();

    new ControlIndexingVisitor(ObjectUtils.notNull(EnumSet.allOf(ItemType.class)))
        .visitProfile(profileItem, profileIndex);

    // copy roles, parties, and locations with prop name:keep and any referenced
    Metadata resolvedMetadata = resolvedCatalog.getMetadata();
    resolvedMetadata.setRoles(
        IIndexer.filterDistinct(
            ObjectUtils.notNull(CollectionUtil.listOrEmpty(resolvedMetadata.getRoles()).stream()),
            profileIndex.getEntitiesByItemType(IEntityItem.ItemType.ROLE),
            Role::getId)
            .collect(Collectors.toCollection(LinkedList::new)));
    resolvedMetadata.setParties(
        IIndexer.filterDistinct(
            ObjectUtils.notNull(CollectionUtil.listOrEmpty(resolvedMetadata.getParties()).stream()),
            profileIndex.getEntitiesByItemType(IEntityItem.ItemType.PARTY),
            Party::getUuid)
            .collect(Collectors.toCollection(LinkedList::new)));
    resolvedMetadata.setLocations(
        IIndexer.filterDistinct(
            ObjectUtils.notNull(CollectionUtil.listOrEmpty(resolvedMetadata.getLocations()).stream()),
            profileIndex.getEntitiesByItemType(IEntityItem.ItemType.LOCATION),
            Location::getUuid)
            .collect(Collectors.toCollection(LinkedList::new)));

    // copy resources
    BackMatter resolvedBackMatter = resolvedCatalog.getBackMatter();
    List<Resource> resolvedResources = resolvedBackMatter == null ? CollectionUtil.emptyList()
        : CollectionUtil.listOrEmpty(resolvedBackMatter.getResources());

    List<Resource> resources = IIndexer.filterDistinct(
        ObjectUtils.notNull(resolvedResources.stream()),
        profileIndex.getEntitiesByItemType(IEntityItem.ItemType.RESOURCE),
        Resource::getUuid)
        .collect(Collectors.toCollection(LinkedList::new));

    if (!resources.isEmpty()) {
      if (resolvedBackMatter == null) {
        resolvedBackMatter = new BackMatter();
        resolvedCatalog.setBackMatter(resolvedBackMatter);
      }

      resolvedBackMatter.setResources(resources);
    }

    index.append(profileIndex);
  }

  @FunctionalInterface
  public interface UriResolver {
    @NonNull
    URI resolve(@NonNull URI uri, @NonNull URI source);
  }
}
