package org.portfolio.userland.common.repositories;

import org.portfolio.userland.common.dto.TableMetaReq;

import java.time.LocalDateTime;

/**
 * Interface for table requests. Ensures there is always table metadata and other common fields.
 */
public interface TableReq {
  LocalDateTime createdFromAt();
  LocalDateTime createdToAt();
  TableMetaReq tableMeta();
}
