/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package dev.metaschema.oscal.lib.profile.resolver.selection;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.metaschema.core.util.ObjectUtils;
import dev.metaschema.oscal.lib.model.control.catalog.IControl;
import edu.umd.cs.findbugs.annotations.NonNull;

@FunctionalInterface
public interface IControlSelectionFilter extends Function<IControl, Pair<Boolean, Boolean>> {

  @NonNull
  Pair<Boolean, Boolean> NON_MATCH = ObjectUtils.notNull(Pair.of(false, false));
  @NonNull
  Pair<Boolean, Boolean> MATCH = ObjectUtils.notNull(Pair.of(true, true));

  @NonNull
  IControlSelectionFilter ALL_MATCH = control -> MATCH;

  @NonNull
  IControlSelectionFilter NONE_MATCH = control -> NON_MATCH;

  @NonNull
  static IControlSelectionFilter matchIds(@NonNull String... identifiers) {
    return new IControlSelectionFilter() {
      private final Set<String> keys = Arrays.stream(identifiers).collect(Collectors.toUnmodifiableSet());

      @Override
      @NonNull
      public Pair<Boolean, Boolean> apply(IControl control) {
        return ObjectUtils.notNull(Pair.of(keys.contains(control.getId()), false));
      }

    };
  }

  /**
   * Determines if the control is matched by this filter. This method returns a
   * {@link Pair} where the first member of the pair indicates if the control
   * matches, and the second indicates if the match applies to child controls as
   * well.
   *
   * @param control
   *          the control to check for a match
   * @return a pair indicating the status of the match ({@code true} for a match
   *         or {@code false} otherwise), and if a match applies to child controls
   */
  @NonNull
  @Override
  Pair<Boolean, Boolean> apply(IControl control);
}
