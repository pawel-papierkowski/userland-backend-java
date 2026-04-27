package org.portfolio.userland.features.check.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.check.data.CheckInfoResp;
import org.portfolio.userland.system.BaseService;
import org.springframework.stereotype.Service;

/**
 * Service for check endpoints.
 */
@Service
@RequiredArgsConstructor
public class CheckService extends BaseService {
  public CheckInfoResp info() {
    return new CheckInfoResp(clockService.getNowUTC(), profile);
  }
}
