package org.portfolio.userland.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Multi-language config. Normally Spring expects .properties files. We want to use .yaml files instead.
 * This configuration class tells Spring to search in base directory and all subdirectories for any YAML files and treat
 * it as translation file to use.
 */
@Configuration
@Slf4j
public class I18nConfig {
  /** Base directory for translations. */
  private final static String BASE_DIR = "i18n";
  /** Find all YAML files in base directory and subdirectories. The '**' tells Spring to search all subdirectories recursively. */
  private final static String LOCATION_PATTERN = "classpath*:"+BASE_DIR+"/**/*.yaml";

  /** Strip the locale suffix and .yaml extension. */
  private final static String REGEX_REPLACE = "(_[a-zA-Z]{2}(_[a-zA-Z]{2})?)?\\.yaml$";

  /**
   * Defines and configures message source that can handle yml/yaml files.
   * @return Message source.
   * @throws IOException If resource resolver fails.
   */
  @Bean
  public MessageSource messageSource() throws IOException {
    YamlMessageSource messageSource = new YamlMessageSource();
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setPropertiesPersister(new YamlPropertiesPersister());
    messageSource.setBasenames(findYamlBasenames());
    return messageSource;
  }

  /**
   * Find all base names dynamically, including those in subdirectories.
   * @return Array of base names.
   * @throws IOException If resource resolver fails.
   */
  private String[] findYamlBasenames() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources(LOCATION_PATTERN);
    Set<String> baseNames = new HashSet<>();

    for (Resource resource : resources) {
      // Get the full URL/URI string of the resource to extract its relative path
      String path = resource.getURL().toString();

      // Find where BASE_DIR starts in the absolute path
      int i18nIndex = path.indexOf(BASE_DIR+"/");
      if (i18nIndex == -1) continue;

      // Extract everything from BASE_DIR to the end of the string. Example: "i18n/emails/welcome_pl.yaml".
      String relativePath = path.substring(i18nIndex);
      // Strip the locale suffix and .yaml extension. Example: "i18n/emails/welcome_pl.yaml" -> "i18n/emails/welcome".
      String baseName = relativePath.replaceAll(REGEX_REPLACE, "");
      baseNames.add("classpath:" + baseName); // Add the classpath prefix required by ReloadableResourceBundleMessageSource.
    }

    return baseNames.toArray(new String[0]);
  }

  //

  /** Custom resource loader that can handle YAML files. */
  private static class YamlResourceLoader implements ResourceLoader {
    /** We actually use original loader, just with some adjustments. */
    private final ResourceLoader resourceLoader;

    private YamlResourceLoader(ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
    }

    @Override
    public Resource getResource(String location) {
      if (location.endsWith(".properties")) {
        // Try .yaml extension.
        Resource yaml = resourceLoader.getResource(location.replace(".properties", ".yaml"));
        if (yaml.exists()) return yaml;
      }
      // Fallback to original loader.
      return resourceLoader.getResource(location);
    }

    @Override
    public ClassLoader getClassLoader() {
      return resourceLoader.getClassLoader();
    }
  }

  /** Custom message source that can handle yml/yaml files. */
  private static class YamlMessageSource extends ReloadableResourceBundleMessageSource {
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      // Intercept whatever Spring injects (the ApplicationContext) and wrap it.
      YamlResourceLoader customLoader = new YamlResourceLoader(resourceLoader);
      // Pass our custom wrapper up to the parent class
      super.setResourceLoader(customLoader);
    }
  }

  /** Custom inner class to parse YAML into Properties. */
  private static class YamlPropertiesPersister extends DefaultPropertiesPersister {
    @Override
    public void load(@NonNull Properties props, @NonNull InputStream is) {
      loadYaml(props, new InputStreamResource(is));
    }

    @Override
    public void load(@NonNull Properties props, @NonNull Reader reader) throws IOException {
      // We read the characters from the Reader and convert them back to a Resource.
      String yamlContent = FileCopyUtils.copyToString(reader);
      Resource resource = new ByteArrayResource(yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      loadYaml(props, resource);
    }

    private void loadYaml(Properties props, Resource resource) {
      YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
      factory.setResources(resource); // Wrap the InputStream in a Spring Resource.
      factory.afterPropertiesSet(); // Initialize the factory.

      // Let the factory convert the YAML tree into flat Properties.
      Properties yamlProperties = factory.getObject();
      if (yamlProperties != null) props.putAll(yamlProperties);
    }
  }
}
