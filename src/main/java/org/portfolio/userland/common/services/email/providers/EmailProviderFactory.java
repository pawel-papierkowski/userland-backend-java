package org.portfolio.userland.common.services.email.providers;

import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.services.email.exception.UnknownEmailProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for email providers.
 */
@Component
public class EmailProviderFactory {
  /** Name of default provider. */
  @Value("${app.email.providers.default}")
  private String defaultProvider;

  /** Known providers. */
  private final Map<String, IntEmailProvider> providers;

  /**
   * We convert list of provider classes into a Map where the key is the custom provider name.
   * @param emailProviderList Spring automatically injects a list of ALL classes that implement EmailProvider.
   */
  public EmailProviderFactory(List<IntEmailProvider> emailProviderList) {
    this.providers = emailProviderList.stream()
        .collect(Collectors.toMap(
            IntEmailProvider::getProviderName,
            Function.identity()
        ));
  }

  /**
   * Fetch the correct provider at runtime based on a string.
   * @param providerName Provider name.
   * @return Service implementing that provider.
   */
  public IntEmailProvider getProvider(String providerName) {
    if (StringUtils.isEmpty(providerName)) providerName = defaultProvider;

    IntEmailProvider provider = providers.get(providerName);
    if (provider == null) throw new UnknownEmailProviderException(providerName);
    return provider;
  }
}
