package org.portfolio.userland.common.services.web;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.constants.EnAppBuild;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link WebHelperService}.
 */
public class WebHelperServiceTest extends BaseIntegrationTest {
  @Autowired
  private WebHelperService webHelperService;

  // //////////////////////////////////////////////////////////////////////////
  // resolveWww

  @Test
  public void resolveWwwVueTestBuild() {
    // Act
    String result = webHelperService.resolveWww(EnFrontendFramework.VUE);

    // Assert
    assertThat(result).as("Vue URL in test build").isEqualTo("http://localhost:5173/userland-frontend-vue");
  }

  @Test
  public void resolveWwwAngularTestBuild() {
    // Act
    String result = webHelperService.resolveWww(EnFrontendFramework.ANGULAR);

    // Assert
    assertThat(result).as("Angular URL in test build").isEqualTo("http://localhost:5173/userland-frontend-angular");
  }

  @Test
  public void resolveWwwNullDefaultsToVue() {
    // Act
    String result = webHelperService.resolveWww(null);

    // Assert
    assertThat(result).as("Null frontend should default to vue").isEqualTo("http://localhost:5173/userland-frontend-vue");
  }

  @Test
  public void resolveWwwProdBuild() {
    // Arrange: we need to set build via reflection to pretend we are on production just for this test.
    EnAppBuild originalBuild = (EnAppBuild) ReflectionTestUtils.getField(webHelperService, "build");
    ReflectionTestUtils.setField(webHelperService, "build", EnAppBuild.PROD);

    try {
      // Act
      String result = webHelperService.resolveWww(EnFrontendFramework.VUE);

      // Assert
      assertThat(result).as("Vue URL in prod build").isEqualTo("https://pawelpapierkowski.net.pl/userland-frontend-vue");
    } finally {
      // Restore
      ReflectionTestUtils.setField(webHelperService, "build", originalBuild);
    }
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveLoginLink

  @Test
  public void resolveLoginLinkVue() {
    // Act
    String result = webHelperService.resolveLoginLink(EnFrontendFramework.VUE);

    // Assert
    assertThat(result).as("Vue login link").isEqualTo("http://localhost:5173/userland-frontend-vue/login");
  }

  @Test
  public void resolveLoginLinkAngular() {
    // Act
    String result = webHelperService.resolveLoginLink(EnFrontendFramework.ANGULAR);

    // Assert
    assertThat(result).as("Angular login link").isEqualTo("http://localhost:5173/userland-frontend-angular/login");
  }

  @Test
  public void resolveLoginLinkNullDefaultsToVue() {
    // Act
    String result = webHelperService.resolveLoginLink(null);

    // Assert
    assertThat(result).as("Null frontend should default to vue login link").isEqualTo("http://localhost:5173/userland-frontend-vue/login");
  }
}
