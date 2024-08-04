/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib;

import gov.nist.secauto.metaschema.core.metapath.StaticContext;
import gov.nist.secauto.metaschema.core.model.IModuleLoader;
import gov.nist.secauto.metaschema.databind.DefaultBindingContext;
import gov.nist.secauto.oscal.lib.model.AssessmentPlan;
import gov.nist.secauto.oscal.lib.model.AssessmentResults;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.ComponentDefinition;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.SystemSecurityPlan;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OscalBindingContext
    extends DefaultBindingContext {
  @NonNull
  public static final StaticContext OSCAL_STATIC_METAPATH_CONTEXT = StaticContext.builder()
      .defaultModelNamespace(OscalModelConstants.NS_URI_OSCAL)
      .build();
  @NonNull
  private static final OscalBindingContext SINGLETON = new OscalBindingContext();

  @NonNull
  @SuppressFBWarnings(value = "SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", justification = "class initialization")
  public static OscalBindingContext instance() {
    return SINGLETON;
  }

  /**
   * Construct a new OSCAL-flavored binding context with custom constraints.
   *
   * @param modulePostProcessors
   *          a list of module post processors to call after loading a module
   */
  @SuppressFBWarnings(value = "SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR",
      justification = "public constructor allows customized use in specialized usecases")
  public OscalBindingContext(@NonNull List<IModuleLoader.IModulePostProcessor> modulePostProcessors) {
    super(modulePostProcessors);
    registerBindingMatcher(Catalog.class);
    registerBindingMatcher(Profile.class);
    registerBindingMatcher(SystemSecurityPlan.class);
    registerBindingMatcher(ComponentDefinition.class);
    registerBindingMatcher(AssessmentPlan.class);
    registerBindingMatcher(AssessmentResults.class);
    registerBindingMatcher(PlanOfActionAndMilestones.class);
  }

  /**
   * Construct a new OSCAL-flavored binding context.
   */
  protected OscalBindingContext() {
    registerBindingMatcher(Catalog.class);
    registerBindingMatcher(Profile.class);
    registerBindingMatcher(SystemSecurityPlan.class);
    registerBindingMatcher(ComponentDefinition.class);
    registerBindingMatcher(AssessmentPlan.class);
    registerBindingMatcher(AssessmentResults.class);
    registerBindingMatcher(PlanOfActionAndMilestones.class);
  }

  @NonNull
  public Catalog loadCatalog(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(Catalog.class, url);
  }

  @NonNull
  public Catalog loadCatalog(@NonNull Path path) throws IOException {
    return newBoundLoader().load(Catalog.class, path);
  }

  @NonNull
  public Catalog loadCatalog(@NonNull File file) throws IOException {
    return newBoundLoader().load(Catalog.class, file);
  }

  @NonNull
  public Profile loadProfile(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(Profile.class, url);
  }

  @NonNull
  public Profile loadProfile(@NonNull Path path) throws IOException {
    return newBoundLoader().load(Profile.class, path);
  }

  @NonNull
  public Profile loadProfile(@NonNull File file) throws IOException {
    return newBoundLoader().load(Profile.class, file);
  }

  @NonNull
  public SystemSecurityPlan loadSystemSecurityPlan(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(SystemSecurityPlan.class, url);
  }

  @NonNull
  public SystemSecurityPlan loadSystemSecurityPlan(@NonNull Path path) throws IOException {
    return newBoundLoader().load(SystemSecurityPlan.class, path);
  }

  @NonNull
  public SystemSecurityPlan loadSystemSecurityPlan(@NonNull File file) throws IOException {
    return newBoundLoader().load(SystemSecurityPlan.class, file);
  }

  @NonNull
  public ComponentDefinition loadComponentDefinition(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(ComponentDefinition.class, url);
  }

  @NonNull
  public ComponentDefinition loadComponentDefinition(@NonNull Path path) throws IOException {
    return newBoundLoader().load(ComponentDefinition.class, path);
  }

  @NonNull
  public ComponentDefinition loadComponentDefinition(@NonNull File file) throws IOException {
    return newBoundLoader().load(ComponentDefinition.class, file);
  }

  @NonNull
  public AssessmentPlan loadAssessmentPlan(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(AssessmentPlan.class, url);
  }

  @NonNull
  public AssessmentPlan loadAssessmentPlan(@NonNull Path path) throws IOException {
    return newBoundLoader().load(AssessmentPlan.class, path);
  }

  @NonNull
  public AssessmentPlan loadAssessmentPlan(@NonNull File file) throws IOException {
    return newBoundLoader().load(AssessmentPlan.class, file);
  }

  @NonNull
  public AssessmentResults loadAssessmentResults(@NonNull URL url) throws IOException, URISyntaxException {
    return newBoundLoader().load(AssessmentResults.class, url);
  }

  @NonNull
  public AssessmentResults loadAssessmentResults(@NonNull Path path) throws IOException {
    return newBoundLoader().load(AssessmentResults.class, path);
  }

  @NonNull
  public AssessmentResults loadAssessmentResults(@NonNull File file) throws IOException {
    return newBoundLoader().load(AssessmentResults.class, file);
  }

  @NonNull
  public PlanOfActionAndMilestones loadPlanOfActionAndMilestones(@NonNull URL url)
      throws IOException, URISyntaxException {
    return newBoundLoader().load(PlanOfActionAndMilestones.class, url);
  }

  @NonNull
  public PlanOfActionAndMilestones loadPlanOfActionAndMilestones(@NonNull Path path) throws IOException {
    return newBoundLoader().load(PlanOfActionAndMilestones.class, path);
  }

  @NonNull
  public PlanOfActionAndMilestones loadPlanOfActionAndMilestones(@NonNull File file) throws IOException {
    return newBoundLoader().load(PlanOfActionAndMilestones.class, file);
  }
}
