package org.portfolio.userland.common.services.table;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMeta;

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
   * @param tableMeta Table meta. Can be null.
   * @return Filled table meta.
   */
  public static TableMeta prepareTableMeta(TableMeta tableMeta) {
    return TableHelper.prepareTableMeta(tableMeta, DEFAULT_PAGE_SIZE, DEFAULT_PAGE, DEFAULT_SORT_BY, DEFAULT_SORT_ORDER);
  }

  /**
   * Handle TableMeta defaults.
   * @param tableMeta Table meta. Can be null.
   * @param defPageSize Page size.
   * @param defPage Page.
   * @param defSortBy Sort by.
   * @param defSortOrder Sort order.
   * @return Filled table meta.
   */
  public static TableMeta prepareTableMeta(TableMeta tableMeta, int defPageSize, int defPage, @NonNull String defSortBy, @NonNull EnSortOrder defSortOrder) {
    if (tableMeta == null) {
      tableMeta = TableMeta.builder()
          .pageSize(defPageSize)
          .page(defPage)
          .sortBy(defSortBy)
          .sortOrder(defSortOrder)
          .build();
    } else {
      // Fill in any missing fields in the provided tableMeta with defaults.
      if (tableMeta.pageSize() == null) tableMeta = tableMeta.toBuilder().pageSize(defPageSize).build();
      if (tableMeta.page() == null) tableMeta = tableMeta.toBuilder().page(defPage).build();
      if (tableMeta.sortBy() == null || tableMeta.sortBy().isBlank()) tableMeta = tableMeta.toBuilder().sortBy(defSortBy).build();
      if (tableMeta.sortOrder() == null) tableMeta = tableMeta.toBuilder().sortOrder(defSortOrder).build();
    }
    return tableMeta;
  }

  /**
   * Applies sorting.
   * @param cb Criteria builder.
   * @param cq Criteria query.
   * @param tableMeta Table metadata.
   * @param <T> Entity.
   */
  public static <T> void applySorting(CriteriaBuilder cb, CriteriaQuery<T> cq, Root<T> entity, TableMeta tableMeta) {
    // Apply sorting.
    if (tableMeta.sortOrder() == EnSortOrder.ASC) {
      cq.orderBy(cb.asc(entity.get(tableMeta.sortBy())));
    } else {
      cq.orderBy(cb.desc(entity.get(tableMeta.sortBy())));
    }
  }

  /**
   * Applies pagination.
   * @param query Typed query.
   * @param tableMeta Table metadata. Must be filled properly.
   * @param <T> Entity.
   */
  public static <T> void applyPagination(TypedQuery<T> query, TableMeta tableMeta) {
    // Apply pagination.
    query.setFirstResult(tableMeta.page() * tableMeta.pageSize());
    query.setMaxResults(tableMeta.pageSize());
  }
}
