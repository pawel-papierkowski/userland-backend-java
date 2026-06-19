package org.portfolio.userland.common.services.table;

import com.google.common.collect.Lists;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;

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
    return TableHelper.prepareTableMeta(tableMetaReq, DEFAULT_PAGE_SIZE, DEFAULT_PAGE, DEFAULT_SORT_BY, DEFAULT_SORT_ORDER);
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
  public static TableMetaReq prepareTableMeta(TableMetaReq tableMetaReq, int defPageSize, int defPage, @NonNull String defSortBy, @NonNull EnSortOrder defSortOrder) {
    if (tableMetaReq == null) {
      tableMetaReq = TableMetaReq.builder()
          .pageSize(defPageSize)
          .page(defPage)
          .sortBy(defSortBy)
          .sortOrder(defSortOrder)
          .build();
    } else {
      // Fill in any missing fields in the provided tableMeta with defaults.
      if (tableMetaReq.pageSize() == null) tableMetaReq = tableMetaReq.toBuilder().pageSize(defPageSize).build();
      if (tableMetaReq.page() == null) tableMetaReq = tableMetaReq.toBuilder().page(defPage).build();
      if (tableMetaReq.sortBy() == null || tableMetaReq.sortBy().isBlank()) tableMetaReq = tableMetaReq.toBuilder().sortBy(defSortBy).build();
      if (tableMetaReq.sortOrder() == null) tableMetaReq = tableMetaReq.toBuilder().sortOrder(defSortOrder).build();
    }
    return tableMetaReq;
  }

  /**
   * Applies sorting.
   * @param cb Criteria builder.
   * @param cq Criteria query.
   * @param entity Entity.
   * @param tableMetaReq Table metadata.
   * @param <E> Entity class.
   */
  public static <E> void applySorting(CriteriaBuilder cb, CriteriaQuery<E> cq, Root<E> entity, TableMetaReq tableMetaReq) {
    List<Order> order = Lists.newArrayList();
    // Determine custom sorting.
    Order customOrder;
    Path<?> path = resolvePath(entity, tableMetaReq.sortBy());
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
   * @param entity Entity.
   * @param fields Field names. If separated by dot, we assume it is join.
   * @return Path.
   * @param <E> Entity class.
   */
  private static <E> Path<?> resolvePath(Root<E> entity, String fields) {
    String[] fieldsArr = fields.split("\\.");

    From<?, ?> from = entity; // Root extends From, so this works
    for (int i = 0; i < fieldsArr.length - 1; i++) {
      from = from.join(fieldsArr[i]);
    }
    return from.get(fieldsArr[fieldsArr.length - 1]);
  }

  /**
   * Applies pagination.
   * @param query Typed query.
   * @param tableMetaReq Table metadata. Must be filled properly.
   * @param <E> Entity.
   */
  public static <E> void applyPagination(TypedQuery<E> query, TableMetaReq tableMetaReq) {
    // Apply pagination.
    query.setFirstResult(tableMetaReq.page() * tableMetaReq.pageSize());
    query.setMaxResults(tableMetaReq.pageSize());
  }

  //

  /**
   * Fill metadata for table page response.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return Metadata for table page response.
   */
  public static TableMetaResp fillTableMetaResp(TableMetaReq tableMetaReq, Long entryCount) {
    Long pageCount = entryCount == 0 ? 0L : (entryCount/tableMetaReq.pageSize()) + 1;
    return TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .page(tableMetaReq.page())
        .pageSize(tableMetaReq.pageSize())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();
  }
}
