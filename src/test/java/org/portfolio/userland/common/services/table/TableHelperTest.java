package org.portfolio.userland.common.services.table;

import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for <code>TableHelper</code> default/normalization handling. Focus: <code>prepareTableMeta()</code> is
 * the single source of truth for defaults, and <code>applyPagination()</code>/<code>fillTableMetaResp()</code>
 * normalize metadata themselves instead of duplicating fallback logic.
 */
public class TableHelperTest {
  // prepareTableMeta

  @Test
  public void nullMetaIsFullyDefaulted() {
    TableMetaReq actual = TableHelper.prepareTableMeta(null);
    assertThat(actual.pageSize()).isEqualTo(20);
    assertThat(actual.page()).isZero();
    assertThat(actual.sortBy()).isEqualTo("createdAt");
    assertThat(actual.sortOrder()).isEqualTo(EnSortOrder.DESC);
  }

  @Test
  public void invalidPageSizeFallsBackToDefault() {
    TableMetaReq req = TableMetaReq.builder().pageSize(0).page(2).build();
    assertThat(TableHelper.prepareTableMeta(req).pageSize()).as("Zero page size").isEqualTo(20);

    req = TableMetaReq.builder().pageSize(-5).page(2).build();
    assertThat(TableHelper.prepareTableMeta(req).pageSize()).as("Negative page size").isEqualTo(20);
  }

  @Test
  public void providedValuesAreKept() {
    TableMetaReq req = TableMetaReq.builder().pageSize(50).page(3).sortBy("username").sortOrder(EnSortOrder.ASC).build();
    TableMetaReq actual = TableHelper.prepareTableMeta(req);
    assertThat(actual.pageSize()).isEqualTo(50);
    assertThat(actual.page()).isEqualTo(3);
    assertThat(actual.sortBy()).isEqualTo("username");
    assertThat(actual.sortOrder()).isEqualTo(EnSortOrder.ASC);
  }

  //

  /**
   * Regression test for DRY fix: applyPagination must normalize metadata itself, so null meta does not cause NPE and
   * invalid page size falls back to the same default as everywhere else.
   */
  @Test
  public void applyPaginationHandlesNullAndInvalidMeta() {
    // Act: null metadata.
    @SuppressWarnings("unchecked")
    TypedQuery<Object> queryNull = mock(TypedQuery.class);
    TableHelper.applyPagination(queryNull, null);

    // Assert: first page with default page size.
    verify(queryNull).setFirstResult(0);
    verify(queryNull).setMaxResults(20);

    // Act: invalid (zero) page size, second page.
    @SuppressWarnings("unchecked")
    TypedQuery<Object> queryInvalid = mock(TypedQuery.class);
    TableHelper.applyPagination(queryInvalid, TableMetaReq.builder().pageSize(0).page(1).build());

    // Assert: offset computed from default page size.
    verify(queryInvalid).setFirstResult(20);
    verify(queryInvalid).setMaxResults(20);
  }

  @Test
  public void applyPaginationUsesProvidedValues() {
    @SuppressWarnings("unchecked")
    TypedQuery<Object> query = mock(TypedQuery.class);

    // Act.
    TableHelper.applyPagination(query, TableMetaReq.builder().pageSize(10).page(4).build());

    // Assert: offset = page * pageSize = 40.
    verify(query).setFirstResult(40);
    verify(query).setMaxResults(10);
  }

  //

  /**
   * Regression test for DRY fix: fillTableMetaResp must normalize metadata itself, so null meta does not cause NPE
   * and page size/count math uses the shared default.
   */
  @Test
  public void fillTableMetaRespHandlesNullAndInvalidMeta() {
    // Act: null metadata, non-zero entry count.
    TableMetaResp resp = TableHelper.fillTableMetaResp(null, 45L);

    // Assert: default page size used, pageCount = ceil(45 / 20) = 3.
    assertThat(resp.pageSize()).isEqualTo(20);
    assertThat(resp.pageCount()).isEqualTo(3L);
    assertThat(resp.entryCount()).isEqualTo(45L);
    assertThat(resp.page()).isZero();

    // Act: invalid (negative) page size, empty table.
    resp = TableHelper.fillTableMetaResp(TableMetaReq.builder().pageSize(-1).build(), 0L);

    // Assert: default page size reported, no pages.
    assertThat(resp.pageSize()).isEqualTo(20);
    assertThat(resp.pageCount()).isZero();
  }

  @Test
  public void fillTableMetaRespUsesProvidedValues() {
    TableMetaResp resp = TableHelper.fillTableMetaResp(
        TableMetaReq.builder().pageSize(10).page(2).sortBy("email").sortOrder(EnSortOrder.ASC).build(), 25L);

    assertThat(resp.pageSize()).isEqualTo(10);
    assertThat(resp.pageCount()).isEqualTo(3L); // ceil(25 / 10)
    assertThat(resp.entryCount()).isEqualTo(25L);
    assertThat(resp.page()).isEqualTo(2);
    assertThat(resp.sortBy()).isEqualTo("email");
    assertThat(resp.sortOrder()).isEqualTo(EnSortOrder.ASC);
  }

  //

  /**
   * Custom defaults passed to the full overload must flow consistently into pagination math - previously the
   * fallback in applyPagination hardcoded the global default, diverging from custom defPageSize.
   */
  @Test
  public void customDefaultsPropagateConsistently() {
    @SuppressWarnings("unchecked")
    TypedQuery<Object> query = mock(TypedQuery.class);

    // Arrange: meta with invalid page size normalized against CUSTOM default of 5.
    TableMetaReq meta = TableHelper.prepareTableMeta(
        TableMetaReq.builder().pageSize(0).page(3).build(), 5, 9, "id", EnSortOrder.ASC);

    // Act & Assert: pagination math uses custom page size (offset = 3 * 5).
    TableHelper.applyPagination(query, meta);
    verify(query).setFirstResult(15);
    verify(query).setMaxResults(5);

    // Act & Assert: response meta reports the custom page size and matching page count (ceil(11 / 5) = 3).
    TableMetaResp resp = TableHelper.fillTableMetaResp(meta, 11L);
    assertThat(resp.pageSize()).isEqualTo(5);
    assertThat(resp.page()).isEqualTo(3);
    assertThat(resp.pageCount()).isEqualTo(3L);
    assertThat(resp.sortBy()).isEqualTo("id");
    assertThat(resp.sortOrder()).isEqualTo(EnSortOrder.ASC);
  }
}
