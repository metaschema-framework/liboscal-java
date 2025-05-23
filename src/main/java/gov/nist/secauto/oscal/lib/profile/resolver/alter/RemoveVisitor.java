/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.secauto.oscal.lib.profile.resolver.alter;

import gov.nist.secauto.metaschema.core.util.CollectionUtil;
import gov.nist.secauto.metaschema.core.util.ObjectUtils;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.CatalogGroup;
import gov.nist.secauto.oscal.lib.model.Control;
import gov.nist.secauto.oscal.lib.model.ControlPart;
import gov.nist.secauto.oscal.lib.model.Link;
import gov.nist.secauto.oscal.lib.model.Parameter;
import gov.nist.secauto.oscal.lib.model.Property;
import gov.nist.secauto.oscal.lib.model.control.catalog.ICatalogVisitor;
import gov.nist.secauto.oscal.lib.model.metadata.IProperty;
import gov.nist.secauto.oscal.lib.profile.resolver.ProfileResolutionEvaluationException;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class RemoveVisitor implements ICatalogVisitor<Boolean, RemoveVisitor.Context> {
  public enum TargetType {
    PARAM("param", Parameter.class),
    PROP("prop", Property.class),
    LINK("link", Link.class),
    PART("part", ControlPart.class);

    @NonNull
    private static final Map<Class<?>, TargetType> CLASS_TO_TYPE;
    @NonNull
    private static final Map<String, TargetType> NAME_TO_TYPE;
    @NonNull
    private final String fieldName;
    @NonNull
    private final Class<?> clazz;

    static {
      {
        Map<Class<?>, TargetType> map = new ConcurrentHashMap<>();
        for (TargetType type : values()) {
          map.put(type.getClazz(), type);
        }
        CLASS_TO_TYPE = CollectionUtil.unmodifiableMap(map);
      }

      {
        Map<String, TargetType> map = new ConcurrentHashMap<>();
        for (TargetType type : values()) {
          map.put(type.fieldName(), type);
        }
        NAME_TO_TYPE = CollectionUtil.unmodifiableMap(map);
      }
    }

    /**
     * Get the target type associated with the provided {@code clazz}.
     *
     * @param clazz
     *          the class to identify the target type for
     * @return the associated target type or {@code null} if the class is not
     *         associated with a target type
     */
    @Nullable
    public static TargetType forClass(@NonNull Class<?> clazz) {
      Class<?> target = clazz;
      TargetType retval;
      // recurse over parent classes to find a match
      do {
        retval = CLASS_TO_TYPE.get(target);
      } while (retval == null && (target = target.getSuperclass()) != null);
      return retval;
    }

    /**
     * Get the target type associated with the provided field {@code name}.
     *
     * @param name
     *          the field name to identify the target type for
     * @return the associated target type or {@code null} if the name is not
     *         associated with a target type
     */
    @Nullable
    public static TargetType forFieldName(@Nullable String name) {
      return name == null ? null : NAME_TO_TYPE.get(name);
    }

    TargetType(@NonNull String fieldName, @NonNull Class<?> clazz) {
      this.fieldName = fieldName;
      this.clazz = clazz;
    }

    /**
     * Get the field name associated with the target type.
     *
     * @return the name
     */
    public String fieldName() {
      return fieldName;
    }

    /**
     * Get the bound class associated with the target type.
     *
     * @return the class
     */
    public Class<?> getClazz() {
      return clazz;
    }

  }

  @NonNull
  private static final RemoveVisitor INSTANCE = new RemoveVisitor();

  private static final Map<TargetType, Set<TargetType>> APPLICABLE_TARGETS;

  static {
    APPLICABLE_TARGETS = new EnumMap<>(TargetType.class);
    APPLICABLE_TARGETS.put(TargetType.PARAM, Set.of(TargetType.PROP, TargetType.LINK));
    APPLICABLE_TARGETS.put(TargetType.PART, Set.of(TargetType.PART, TargetType.PROP, TargetType.LINK));
  }

  private static Set<TargetType> getApplicableTypes(@NonNull TargetType type) {
    return APPLICABLE_TARGETS.getOrDefault(type, CollectionUtil.emptySet());
  }

  private static <T> boolean handle(
      @NonNull TargetType itemType,
      @NonNull Supplier<? extends Collection<T>> supplier,
      @Nullable Function<T, Boolean> handler,
      @NonNull Context context) {

    boolean handleChildren = !Collections.disjoint(context.getTargetItemTypes(), getApplicableTypes(itemType));
    boolean retval = false;
    if (context.isMatchingType(itemType)) {
      // if the item type is applicable, attempt to remove any items
      Iterator<T> iter = supplier.get().iterator();
      while (iter.hasNext()) {
        T item = iter.next();

        if (item == null || context.isApplicableTo(item)) {
          iter.remove();
          retval = true;
          // ignore removed items and their children
        } else if (handler != null && handleChildren) {
          // handle child items since they are applicable to the search criteria
          retval = retval || handler.apply(item);
        }
      }
    } else if (handleChildren && handler != null) {
      for (T item : supplier.get()) {
        if (item != null) {
          retval = retval || handler.apply(item);
        }
      }
    }
    return retval;
  }

  /**
   * Apply the remove directive.
   *
   * @param control
   *          the control target
   * @param objectName
   *          the name flag of a matching node to remove
   * @param objectClass
   *          the class flag of a matching node to remove
   * @param objectId
   *          the id flag of a matching node to remove
   * @param objectNamespace
   *          the namespace flag of a matching node to remove
   * @param itemType
   *          the type of a matching node to remove
   * @return {@code true} if the modification was made or {@code false} otherwise
   * @throws ProfileResolutionEvaluationException
   *           if a processing error occurred during profile resolution
   */
  public static boolean remove(
      @NonNull Control control,
      @Nullable String objectName,
      @Nullable String objectClass,
      @Nullable String objectId,
      @Nullable String objectNamespace,
      @Nullable TargetType itemType) {
    return INSTANCE.visitControl(
        control,
        new Context(objectName, objectClass, objectId, objectNamespace, itemType));
  }

  @Override
  public Boolean visitCatalog(Catalog catalog, Context context) {
    // not required
    throw new UnsupportedOperationException("not needed");
  }

  @Override
  public Boolean visitGroup(CatalogGroup group, Context context) {
    // not required
    throw new UnsupportedOperationException("not needed");
  }

  @NonNull
  private static <T> List<T> modifiableListOrEmpty(@Nullable List<T> list) {
    return list == null ? CollectionUtil.emptyList() : list;
  }

  @Override
  public Boolean visitControl(Control control, Context context) {
    assert context != null;

    // visit params
    boolean retval = handle(
        TargetType.PARAM,
        () -> modifiableListOrEmpty(control.getParams()),
        child -> visitParameter(ObjectUtils.notNull(child), context),
        context);

    // visit props
    retval = retval || handle(
        TargetType.PROP,
        () -> modifiableListOrEmpty(control.getProps()),
        null,
        context);

    // visit links
    retval = retval || handle(
        TargetType.LINK,
        () -> modifiableListOrEmpty(control.getLinks()),
        null,
        context);

    return retval || handle(
        TargetType.PART,
        () -> modifiableListOrEmpty(control.getParts()),
        child -> visitPart(child, context),
        context);
  }

  @Override
  public Boolean visitParameter(Parameter parameter, Context context) {
    assert context != null;

    // visit props
    boolean retval = handle(
        TargetType.PROP,
        () -> modifiableListOrEmpty(parameter.getProps()),
        null,
        context);

    return retval || handle(
        TargetType.LINK,
        () -> modifiableListOrEmpty(parameter.getLinks()),
        null,
        context);
  }

  /**
   * Visit the control part.
   *
   * @param part
   *          the bound part object
   * @param context
   *          the visitor context
   * @return {@code true} if the removal was applied or {@code false} otherwise
   */
  public boolean visitPart(ControlPart part, Context context) {
    assert context != null;

    // visit props
    boolean retval = handle(
        TargetType.PROP,
        () -> modifiableListOrEmpty(part.getProps()),
        null,
        context);

    // visit links
    retval = retval || handle(
        TargetType.LINK,
        () -> modifiableListOrEmpty(part.getLinks()),
        null,
        context);

    return retval || handle(
        TargetType.PART,
        () -> modifiableListOrEmpty(part.getParts()),
        child -> visitPart(child, context),
        context);
  }

  static final class Context {
    /**
     * Types with an "name" flag.
     */
    @NonNull
    private static final Set<TargetType> NAME_TYPES = ObjectUtils.notNull(
        Set.of(TargetType.PART, TargetType.PROP));
    /**
     * Types with an "class" flag.
     */
    @NonNull
    private static final Set<TargetType> CLASS_TYPES = ObjectUtils.notNull(
        Set.of(TargetType.PARAM, TargetType.PART, TargetType.PROP));
    /**
     * Types with an "id" flag.
     */
    @NonNull
    private static final Set<TargetType> ID_TYPES = ObjectUtils.notNull(
        Set.of(TargetType.PARAM, TargetType.PART));
    /**
     * Types with an "ns" flag.
     */
    @NonNull
    private static final Set<TargetType> NAMESPACE_TYPES = ObjectUtils.notNull(
        Set.of(TargetType.PART, TargetType.PROP));

    @Nullable
    private final String objectName;
    @Nullable
    private final String objectClass;
    @Nullable
    private final String objectId;
    @Nullable
    private final String objectNamespace;
    @NonNull
    private final Set<TargetType> targetItemTypes;

    private static boolean filterTypes(
        @NonNull Set<TargetType> effectiveTypes,
        @NonNull String criteria,
        @NonNull Set<TargetType> allowedTypes,
        @Nullable String value,
        @Nullable TargetType itemType) {
      boolean retval = false;
      if (value != null) {
        retval = effectiveTypes.retainAll(allowedTypes);
        if (itemType != null && !allowedTypes.contains(itemType)) {
          throw new ProfileResolutionEvaluationException(
              String.format("%s='%s' is not supported for items of type '%s'",
                  criteria,
                  value,
                  itemType.fieldName()));
        }
      }
      return retval;
    }

    private Context(
        @Nullable String objectName,
        @Nullable String objectClass,
        @Nullable String objectId,
        @Nullable String objectNamespace,
        @Nullable TargetType itemType) {

      // determine the set of effective item types to search for
      // this helps with short-circuit searching for parts of the graph that cannot
      // match
      @NonNull
      Set<TargetType> targetItemTypes = ObjectUtils.notNull(EnumSet.allOf(TargetType.class));
      filterTypes(targetItemTypes, "by-name", NAME_TYPES, objectName, itemType);
      filterTypes(targetItemTypes, "by-class", CLASS_TYPES, objectClass, itemType);
      filterTypes(targetItemTypes, "by-id", ID_TYPES, objectId, itemType);
      filterTypes(targetItemTypes, "by-ns", NAMESPACE_TYPES, objectNamespace, itemType);

      if (itemType != null) {
        targetItemTypes.retainAll(Set.of(itemType));
      }

      if (targetItemTypes.isEmpty()) {
        throw new ProfileResolutionEvaluationException("The filter matches no available item types");
      }

      this.objectName = objectName;
      this.objectClass = objectClass;
      this.objectId = objectId;
      this.objectNamespace = objectNamespace;
      this.targetItemTypes = CollectionUtil.unmodifiableSet(targetItemTypes);
    }

    @Nullable
    public String getObjectName() {
      return objectName;
    }

    @Nullable
    public String getObjectClass() {
      return objectClass;
    }

    @Nullable
    public String getObjectId() {
      return objectId;
    }

    @NonNull
    public Set<TargetType> getTargetItemTypes() {
      return targetItemTypes;
    }

    public boolean isMatchingType(@NonNull TargetType type) {
      return getTargetItemTypes().contains(type);
    }

    @Nullable
    public String getObjectNamespace() {
      return objectNamespace;
    }

    private static boolean checkValue(@Nullable String actual, @Nullable String expected) {
      return expected == null || expected.equals(actual);
    }

    public boolean isApplicableTo(@NonNull Object obj) {
      TargetType objectType = TargetType.forClass(obj.getClass());

      boolean retval = objectType != null && getTargetItemTypes().contains(objectType);
      if (retval) {
        assert objectType != null;

        // check other criteria
        String actualName = null;
        String actualClass = null;
        String actualId = null;
        String actualNamespace = null;

        switch (objectType) {
        case PARAM: {
          Parameter param = (Parameter) obj;
          actualClass = param.getClazz();
          actualId = param.getId();
          break;
        }
        case PROP: {
          Property prop = (Property) obj;
          actualName = prop.getName();
          actualClass = prop.getClazz();
          actualNamespace = IProperty.normalizeNamespace(prop.getNs()).toASCIIString();
          break;
        }
        case PART: {
          ControlPart part = (ControlPart) obj;
          actualName = part.getName();
          actualClass = part.getClazz();
          String partId = part.getId();
          if (partId != null) {
            actualId = partId;
          }
          actualNamespace = IProperty.normalizeNamespace(part.getNs()).toASCIIString();
          break;
        }
        case LINK:
          // do nothing
          break;
        default:
          throw new UnsupportedOperationException(objectType.name().toLowerCase(Locale.ROOT));
        }

        retval = checkValue(actualName, getObjectName())
            && checkValue(actualClass, getObjectClass())
            && checkValue(actualId, getObjectId())
            && checkValue(actualNamespace, getObjectNamespace());
      }
      return retval;
    }
  }
}
