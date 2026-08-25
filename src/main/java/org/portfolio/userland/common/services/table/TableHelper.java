package org.portfolio.userland.common.services.table;

import com.google.common.collect.Lists;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.*;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.exception.BadParamsException;

import java.util.List;

/**
 * Helper methods for handling table queries.
 */
public class TableHelper {
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int DEFAULT_PAGE = 0;
  private static final String DEFAULT_SORT_BY = "createdAt";
  private static final EnSortOrder DEFAULT_SORT_ORDER = EnSortOrder.DESC;

  /**
   * Handle TableMeta defaults.
   * @param tableMetaReq Table meta. Can be null.
   * @return Filled table meta.
   */
  public static TableMetaReq prepareTableMeta(TableMetaReq tableMetaReq) {
    return prepareTableMeta(tableMetaReq, DEFAULT_PAGE_SIZE, DEFAULT_PAGE, DEFAULT_SORT_BY, DEFAULT_SORT_ORDER);
  }

  /**
   * Handle TableMeta defaults.
   * @param tableMetaReq Table meta. Can be null.
   * @param defPageSize Page size.
   * @param defPage Page.
   * @param defSortBy Sort by.
   * @param defSortOrder Sort order.
   * @return Filled table meta.
   */
  public static TableMetaReq prepareTableMeta(TableMetaReq tableMetaReq, int defPageSize, int defPage,
                                              @NonNull String defSortBy, @NonNull EnSortOrder defSortOrder) {
    if (tableMetaReq == null) {
      tableMetaReq = TableMetaReq.builder()
          .pageSize(defPageSize)
          .page(defPage)
          .sortBy(defSortBy)
          .sortOrder(defSortOrder)
          .build();
    } else {
      // Fill in any missing fields in the provided tableMeta with defaults.
      if (tableMetaReq.pageSize() == null || tableMetaReq.pageSize() <= 0) tableMetaReq = tableMetaReq.toBuilder().pageSize(defPageSize).build();
      if (tableMetaReq.page() == null) tableMetaReq = tableMetaReq.toBuilder().page(defPage).build();
      if (tableMetaReq.sortBy() == null || tableMetaReq.sortBy().isBlank()) tableMetaReq = tableMetaReq.toBuilder().sortBy(defSortBy).build();
      if (tableMetaReq.sortOrder() == null) tableMetaReq = tableMetaReq.toBuilder().sortOrder(defSortOrder).build();
    }
    return tableMetaReq;
  }

  /**
   * Applies sorting.
   * <p>Note: sort field is client-supplied, so it is validated against the JPA metamodel first. Unknown or unusable
   * fields cause {@link BadParamsException} (HTTP 400) instead of a runtime error deep inside Hibernate (HTTP 500).</p>
   * @param cb Criteria builder.
   * @param cq Criteria query.
   * @param entity Entity.
   * @param metamodel JPA metamodel, used to validate sort field.
   * @param tableMetaReq Table metadata. Can be null or partially filled, defaults will be used.
   * @param <E> Entity class.
   */
  public static <E> void applySorting(CriteriaBuilder cb, CriteriaQuery<E> cq, Root<E> entity, Metamodel metamodel, TableMetaReq tableMetaReq) {
    tableMetaReq = prepareTableMeta(tableMetaReq); // single source of truth for defaults
    List<Order> order = Lists.newArrayList();
    // Determine custom sorting.
    Order customOrder;
    Path<?> path = resolvePath(entity, metamodel, tableMetaReq.sortBy());
    if (tableMetaReq.sortOrder() == EnSortOrder.ASC) {
      customOrder = cb.asc(path);
    } else {
      customOrder = cb.desc(path);
    }

    // Determine fallback order if more than one entity has same value in custom field.
    Order fallbackOrder; // all entities have id field so it is safe
    if (tableMetaReq.sortOrder() == EnSortOrder.ASC) {
      fallbackOrder = cb.asc(entity.get("id"));
    } else {
      fallbackOrder = cb.desc(entity.get("id"));
    }

    order.add(customOrder);
    order.add(fallbackOrder);
    cq.orderBy(order);
  }

  /**
   * Resolve path for entity. Can handle joins.
   * <p>Example: if <code>fields = "permission.name"</code>, code will join <code>permission</code> first, then use
   * field <code>name</code> on <code>permission</code> table.</p>
   * <p>Since sort field is client-supplied, every segment of the path is validated against the JPA metamodel first.
   * Unknown attributes, traversal through collection attributes and sorting by collection attributes are rejected with
   * {@link BadParamsException}. Existing joins on same attribute are reused instead of creating duplicate ones.</p>
   * @param entity Entity.
   * @param metamodel JPA metamodel, used to validate fields.
   * @param fields Field names. If separated by dot, we assume it is join.
   * @return Path.
   * @param <E> Entity class.
   */
  private static <E> Path<?> resolvePath(Root<E> entity, Metamodel metamodel, String fields) {
    String[] fieldsArr = fields.split("\\.");

    // Validate whole path against metamodel before touching the query. This prevents raw IllegalArgumentException
    // (which would surface as HTTP 500) for garbage input from client.
    ManagedType<?> managedType = entity.getModel();
    for (int i = 0; i < fieldsArr.length; i++) {
      Attribute<?, ?> attribute = resolveAttribute(managedType, fieldsArr[i], fields);
      boolean isLast = (i == fieldsArr.length - 1);

      if (attribute instanceof PluralAttribute) {
        // Collections cannot be meaningfully sorted nor joined-through for sorting purposes.
        throw new BadParamsException("Cannot sort by '"+fields+"'.");
      }
      if (!isLast) {
        // Intermediate segment must be a singular association to be joinable.
        if (!(attribute instanceof SingularAttribute<?, ?> singularAttribute) || !singularAttribute.isAssociation()) {
          throw new BadParamsException("Cannot sort by '"+fields+"'.");
        }
        managedType = metamodel.managedType(singularAttribute.getType().getJavaType());
      }
    }

    From<?, ?> from = entity; // Root extends From, so this works
    for (int i = 0; i < fieldsArr.length - 1; i++) {
      from = findOrJoin(from, fieldsArr[i]);
    }
    return from.get(fieldsArr[fieldsArr.length - 1]);
  }

  /**
   * Resolve attribute of given name in given managed type.
   * @param managedType Managed type to search in.
   * @param name Name of attribute.
   * @param sortBy Full sort field as given by client. Used only for error message.
   * @return Resolved attribute.
   */
  private static Attribute<?, ?> resolveAttribute(ManagedType<?> managedType, String name, String sortBy) {
    try {
      return managedType.getAttribute(name);
    } catch (IllegalArgumentException ex) {
      throw new BadParamsException("Cannot sort by '"+sortBy+"'.");
    }
  }

  /**
   * Find existing join on given attribute or create a new one. Reusing joins prevents creating duplicate joins when
   * multiple paths go through same association.
   * @param from Entity or join to search on.
   * @param name Name of attribute to join on.
   * @return Existing or newly created join.
   */
  private static From<?, ?> findOrJoin(From<?, ?> from, String name) {
    for (Join<?, ?> join : from.getJoins()) {
      if (join.getAttribute().getName().equals(name)) return join;
    }
    return from.join(name, JoinType.LEFT); // left join so sorting cannot accidentally filter out rows
  }

  /**
   * Applies pagination.
   * @param query Typed query.
   * @param tableMetaReq Table metadata. Can be null or partially filled, defaults will be used.
   * @param <E> Entity.
   */
  public static <E> void applyPagination(TypedQuery<E> query, TableMetaReq tableMetaReq) {
    tableMetaReq = prepareTableMeta(tableMetaReq); // single source of truth for defaults
    // Apply pagination.
    query.setFirstResult(tableMetaReq.page() * tableMetaReq.pageSize());
    query.setMaxResults(tableMetaReq.pageSize());
  }

  //

  /**
   * Fill metadata for table page response.
   * <p>Note: table metadata is normalized via {@link #prepareTableMeta(TableMetaReq)} first, so this method is safe
   * to call with null or partially filled metadata and stays consistent with default handling everywhere else.</p>
   * @param tableMetaReq Metadata for table page request. Can be null or partially filled.
   * @param entryCount Entry count.
   * @return Metadata for table page response.
   */
  public static TableMetaResp fillTableMetaResp(TableMetaReq tableMetaReq, Long entryCount) {
    tableMetaReq = prepareTableMeta(tableMetaReq); // single source of truth for defaults
    int pageSize = tableMetaReq.pageSize();
    long pageCount = entryCount <= 0 ? 0 : (entryCount + pageSize - 1) / pageSize;
    return TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .page(tableMetaReq.page())
        .pageSize(pageSize)
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();
  }
}
