/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.model.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.metaschema.core.metapath.item.node.IDefinitionNodeItem;
import dev.metaschema.core.model.MetaschemaException;
import dev.metaschema.core.model.constraint.IAllowedValue;
import dev.metaschema.core.model.constraint.IAllowedValuesConstraint;
import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.databind.model.IBoundModule;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.model.OscalCompleteModule;
import dev.metaschema.oscal.lib.model.util.AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord;
import edu.umd.cs.findbugs.annotations.NonNull;

class AbstractNodeItemVisitorTest {

  @Test
  @Disabled
  void testAllowedValues() throws MetaschemaException {
    IBoundModule module = OscalBindingContext.instance().registerModule(OscalCompleteModule.class);
    AllowedValueCollectingNodeItemVisitor walker = new AllowedValueCollectingNodeItemVisitor();
    walker.visit(module);

    System.out.println();
    System.out.println("Allowed Values");
    System.out.println("--------------");
    Map<IDefinitionNodeItem<?, ?>,
        List<AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord>> allowedValuesByTarget
            = ObjectUtils.notNull(walker.getAllowedValueLocations().stream()
                .flatMap(location -> location.getAllowedValues().stream())
                .collect(Collectors.groupingBy(AllowedValuesRecord::getTarget,
                    LinkedHashMap::new,
                    Collectors.mapping(Function.identity(), Collectors.toUnmodifiableList()))));
    sortMap(allowedValuesByTarget, ObjectUtils.notNull(Comparator.comparing(IDefinitionNodeItem::getMetapath)))
        .entrySet().stream()
        .forEachOrdered(entry -> {
          IDefinitionNodeItem<?, ?> target = ObjectUtils.requireNonNull(entry.getKey());
          List<AllowedValueCollectingNodeItemVisitor.AllowedValuesRecord> allowedValuesRecords = entry.getValue();

          System.out.println("node:             " + metapath(target));
          allowedValuesRecords.forEach(record -> {
            assert target.equals(record.getTarget());

            IAllowedValuesConstraint constraint = record.getAllowedValues();
            System.out.println("- allowed-values:");
            System.out.println("    location:     " + metapath(record.getLocation()));
            System.out.println("    target:       " + constraint.getTarget());
            System.out.println("    values:       " + values(constraint));
            System.out.println("    allow-other:  " + constraint.isAllowedOther());
            // System.out.println(" extensible: " + constraint.getExtensible());
            // System.out.println(" source: " + constraint.getSource());
          });
        });
  }

  private static String metapath(@NonNull IDefinitionNodeItem<?, ?> item) {
    return metapath(item.getMetapath());
  }

  private static String metapath(@NonNull String path) {
    return path.replace("[1]", "");
  }

  private static String values(@NonNull IAllowedValuesConstraint constraint) {
    return constraint.getAllowedValues().values().stream()
        .map(IAllowedValue::getValue)
        .collect(Collectors.joining(", "));
  }

  @NonNull
  private static <K, V> NavigableMap<K, V> sortMap(@NonNull Map<K, V> map, @NonNull Comparator<? super K> comparator) {
    TreeMap<K, V> retval = new TreeMap<>(comparator);
    retval.putAll(map);
    return retval;
  }

}
