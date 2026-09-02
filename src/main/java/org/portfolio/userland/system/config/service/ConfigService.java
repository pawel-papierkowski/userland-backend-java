package org.portfolio.userland.system.config.service;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.exceptions.ConfigUnknownException;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * System configuration service. In general, we can assume system config will change rarely.
 * <p>Configuration names are very strict - see <code>ConfigConst</code> for list of allowed configuration names.</p>
 * <p>Reads are cached (see <code>CacheConfig</code>) to avoid a database round trip on every request -
 * the lockdown check in <code>LockdownFilter</code> reads a config value per request. Writes evict the
 * whole cache, which handles invalidation within this instance; other instances converge at worst after
 * the cache TTL. The cache is small and values are tiny strings, so eviction of all entries is cheap.</p>
 * <p>Note: only reason TTL is small are issues with other instances - if eviction happens on instance A, instance B
 * will still serve stale value until cache entry expires.</p>
 */
@Service
@RequiredArgsConstructor
public class ConfigService {
  /** Name of the cache used for config variable reads. */
  public static final String CONFIG_CACHE = "config";

  private final ConfigRepository configRepository;

  /**
   * Get value of configuration variable. It won't create missing configuration variable.
   * <p>Result is served from cache when available. Note: the cache key is the configuration name only.</p>
   * @param name Name of configuration variable.
   * @return Value of configuration variable.
   */
  @Cacheable(cacheNames = ConfigService.CONFIG_CACHE, key = "#name")
  public String get(String name) {
    if (!ConfigConst.DEFAULTS.containsKey(name)) throw new ConfigUnknownException(name);
    // Enforce "same name means same defaultValue" rule.
    String defaultValue = ConfigConst.DEFAULTS.get(name);
    return get(name, defaultValue);
  }

  /**
   * Get value of configuration variable. It won't create missing configuration variable.
   * <p>Note: the cache key is the name only, so the same configuration name must always resolve to the same default
   * value - otherwise a cache entry produced with one default could be served where another was expected. This
   * invariant is enforced by the public getter via <code>ConfigConst.DEFAULTS</code>.</p>
   * @param name Name of configuration variable.
   * @param defaultValue Default value to use if configuration variable is missing.
   * @return Value of configuration variable.
   */
  private String get(String name, String defaultValue) {
    Optional<Config> configEntryOpt = configRepository.findByName(name);
    if (configEntryOpt.isPresent()) return configEntryOpt.get().getValue();
    return defaultValue;
  }

  /**
   * Set configuration variable. Note: it must already exist. The update itself is performed as a single atomic
   * UPDATE statement, so concurrent writers cannot cause a lost update and no locking is needed.
   * @param name Name of configuration variable.
   * @param newValue New value of configuration variable.
   */
  @Transactional
  @CacheEvict(cacheNames = ConfigService.CONFIG_CACHE, key = "#name")
  public void set(String name, String newValue) {
    int updated = configRepository.updateValueByName(name, newValue);
    if (updated == 0) throw new ConfigUnknownException(name);
  }

  /**
   * Set configuration variable only if its value differs from the new one.
   * <p>The decision "did anything change?" is made atomically together with write itself (single conditional
   * UPDATE), so concurrent callers cannot both observe stale value and act on it - exactly one of them will see
   * the change as performed.</p>
   * <p>Note: cache eviction happens regardless of result.</p>
   * @param name Name of configuration variable.
   * @param newValue New value of configuration variable.
   * @return 1 if value was changed, 0 if it was already equal.
   * @throws ConfigUnknownException If configuration variable does not exist.
   */
  @Transactional
  @CacheEvict(cacheNames = ConfigService.CONFIG_CACHE, key = "#name")
  public int setIfChanged(String name, String newValue) {
    int updated = configRepository.updateValueByNameIfChanged(name, newValue);
    if (updated > 0) return updated;
    // Zero rows affected means either 'value was already equal' or 'variable is missing'. Preserve strict behavior
    // for the missing variable case.
    if (!configRepository.existsByName(name)) throw new ConfigUnknownException(name);
    return 0;
  }
}
