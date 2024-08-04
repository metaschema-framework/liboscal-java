/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.model.util;

import gov.nist.secauto.metaschema.core.metapath.item.node.AbstractRecursionPreventingNodeItemVisitor;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyInstanceGroupedNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IAssemblyNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import gov.nist.secauto.metaschema.core.metapath.item.node.INodeItemFactory;
import gov.nist.secauto.metaschema.core.model.IAssemblyDefinition;
import gov.nist.secauto.metaschema.core.model.IModule;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;

public class RecursionCollectingNodeItemVisitor
    extends AbstractRecursionPreventingNodeItemVisitor<Void, Void> {

  private final Map<IAssemblyDefinition, AssemblyRecord> assemblyAnalysis = new LinkedHashMap<>();

  public Set<AssemblyRecord> getRecursiveAssemblyDefinitions() {
    return assemblyAnalysis.values().stream()
        .filter(AssemblyRecord::isRecursive)
        .collect(Collectors.toSet());
  }

  public void visit(@NonNull IModule module) {
    visitMetaschema(INodeItemFactory.instance().newModuleNodeItem(module), null);
  }

  @Override
  public Void visitAssembly(IAssemblyNodeItem item, Void context) {
    IAssemblyDefinition definition = item.getDefinition();

    // get the assembly record from the cache
    AssemblyRecord record = assemblyAnalysis.get(definition);
    boolean retval = true;
    if (record == null) {
      record = new AssemblyRecord(definition);
      assemblyAnalysis.put(definition, record);
    } else if (isDecendant(item, definition)) {
      record.markRecursive();
      record.addLocation(item);
    }
    return super.visitAssembly(item, context);
  }

  @Override
  public Void visitAssembly(IAssemblyInstanceGroupedNodeItem item, Void context) {
    return visitAssembly((IAssemblyNodeItem) item, context);
  }

  @Override
  protected Void defaultResult() {
    return null;
  }

  public static final class AssemblyRecord {
    @NonNull
    private final IAssemblyDefinition definition;
    private boolean recursive; // false
    @NonNull
    private final List<IDefinitionNodeItem<?, ?>> locations = new LinkedList<>();

    private AssemblyRecord(@NonNull IAssemblyDefinition definition) {
      this.definition = definition;
    }

    public IAssemblyDefinition getDefinition() {
      return definition;
    }

    public boolean isRecursive() {
      return recursive;
    }

    public void markRecursive() {
      recursive = true;
    }

    @NonNull
    public List<IDefinitionNodeItem<?, ?>> getLocations() {
      return locations;
    }

    public void addLocation(@NonNull IDefinitionNodeItem<?, ?> location) {
      this.locations.add(location);
    }
  }

}
