package org.portfolio.userland.common.repositories;

import org.portfolio.userland.common.dto.TableMetaReq;

/**
 * Interface for table requests. Ensures there is always table metadata.
 */
public interface TableReq {
  TableMetaReq tableMeta();
}
