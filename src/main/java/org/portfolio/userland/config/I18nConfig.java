package org.portfolio.userland.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Multi-language config. Normally Spring expects .properties files.
 * This configures Spring to search in base directory and all subdirectories for any
 * YAML file and treat it as translation file to use.
 */
@Configuration
@Slf4j
public class I18nConfig {
  /** Base directory for translations. */
  private final static String BASE_DIR = "i18n";
  /** Find all YAML files in base directory and subdirectories. The '**' tells Spring to search all subdirectories recursively. */
  private final static String LOCATION_PATTERN = "classpath*:"+BASE_DIR+"/**/*.yaml";

  /**
   * Defines message source.
   * @return Message source.
   * @throws IOException If resource resolver fails.
   */
  @Bean
  public MessageSource messageSource() throws IOException {
    // Instantiate as an anonymous subclass to override setResourceLoader
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource() {
      @Override
      public void setResourceLoader(org.springframework.core.io.ResourceLoader resourceLoader) {
        // Intercept whatever Spring injects (the ApplicationContext) and wrap it
        org.springframework.core.io.ResourceLoader customLoader = new org.springframework.core.io.ResourceLoader() {
          @Override
          public Resource getResource(String location) {
            if (location.endsWith(".properties")) {
              // Try .yaml first
              Resource yaml = resourceLoader.getResource(location.replace(".properties", ".yaml"));
              if (yaml.exists()) return yaml;

              // Try .yml second
              Resource yml = resourceLoader.getResource(location.replace(".properties", ".yml"));
              if (yml.exists()) return yml;
            }
            // Fallback to original
            return resourceLoader.getResource(location);
          }

          @Override
          public ClassLoader getClassLoader() {
            return resourceLoader.getClassLoader();
          }
        };
        // Pass our custom wrapper up to the parent class
        super.setResourceLoader(customLoader);
      }
    };

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
      if (i18nIndex != -1) {
        // Extract everything from BASE_DIR to the end of the string
        // e.g., "i18n/emails/welcome_pl.yml"
        String relativePath = path.substring(i18nIndex);

        // Strip the locale suffix and .yml extension
        // e.g., "i18n/emails/welcome_pl.yml" -> "i18n/emails/welcome"
        String baseName = relativePath.replaceAll("(_[a-zA-Z]{2}(_[a-zA-Z]{2})?)?\\.ya?ml$", "");

        // Add the classpath prefix required by ReloadableResourceBundleMessageSource
        baseNames.add("classpath:" + baseName);
      }
    }

    return baseNames.toArray(new String[0]);
  }

  /** Custom inner class to parse YAML into Properties. */
  private static class YamlPropertiesPersister extends DefaultPropertiesPersister {
    @Override
    public void load(Properties props, InputStream is) throws IOException {
      loadYaml(props, new InputStreamResource(is));
    }

    @Override
    public void load(Properties props, java.io.Reader reader) throws IOException {
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
