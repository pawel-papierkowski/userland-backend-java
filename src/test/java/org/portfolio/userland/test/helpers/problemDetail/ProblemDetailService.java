package org.portfolio.userland.test.helpers.problemDetail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Handle asserting Problem Detail.
 */
@Service
public class ProblemDetailService {
  protected final ObjectMapper objectMapper = new ObjectMapper();

  //

  /**
   * Check Problem Detail for 401 Unauthorized error.
   * @param actualMvcResult Actual result of MVC call.
   * @param instance Expected value of instance field.
   */
  public void assertPdUnauthorized(MvcResult actualMvcResult, String instance)
      throws JsonProcessingException, UnsupportedEncodingException {
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        "Authentication is required to access this resource.",
        instance,
        "https://api.general.org/errors/unauthorized",
        Map.of()
    );
    assertPd(actualMvcResult, expectedPdb);
  }

  /**
   * Check Problem Detail for 403 Forbidden error.
   * @param actualMvcResult Actual result of MVC call.
   * @param instance Expected value of instance field.
   */
  public void assertPdForbidden(MvcResult actualMvcResult, String instance)
      throws JsonProcessingException, UnsupportedEncodingException {
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to access this resource.",
        instance,
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    assertPd(actualMvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Assert Problem Detail boxes. Actual is raw JSON and is converted to ProblemDetailBox first.
   * @param actualMvcResult Actual result of MVC call.
   * @param expectedPdb Expected Problem Detail box.
   */
  public void assertPd(MvcResult actualMvcResult, ProblemDetailBox expectedPdb)
      throws JsonProcessingException, UnsupportedEncodingException {
    String jsonResponse = actualMvcResult.getResponse().getContentAsString();
    ProblemDetailBox actualPdb = convert(jsonResponse);
    assertPd(actualPdb, expectedPdb);
  }

  /**
   * Assert Problem Detail boxes. Actual is raw JSON and is converted to ProblemDetailBox first.
   * @param actualRawJson Actual raw JSON that represents Problem Detail box.
   * @param expectedPdb Expected Problem Detail box.
   */
  public void assertPd(String actualRawJson, ProblemDetailBox expectedPdb) throws JsonProcessingException {
    ProblemDetailBox actualPdb = convert(actualRawJson);
    assertPd(actualPdb, expectedPdb);
  }

  /**
   * Assert Problem Detail boxes directly.
   * @param actualPdb Actual Problem Detail box.
   * @param expectedPdb Expected Problem Detail box.
   */
  public void assertPd(ProblemDetailBox actualPdb, ProblemDetailBox expectedPdb) {
    assertThat(actualPdb.status()).as("PD HTTP status is wrong").isEqualTo(expectedPdb.status());
    assertThat(actualPdb.title()).as("PD title is wrong").isEqualTo(expectedPdb.title());
    assertThat(actualPdb.detail()).as("PD detail is wrong").isEqualTo(expectedPdb.detail());
    assertThat(actualPdb.instance()).as("PD instance is wrong").isEqualTo(expectedPdb.instance());
    assertThat(actualPdb.type()).as("PD type is wrong").isEqualTo(expectedPdb.type());

    // Now params, if any.
    assertThat(actualPdb.params().size()).as("PD count of params is wrong").isEqualTo(expectedPdb.params().size());
    for (String key : actualPdb.params().keySet()) {
      assertThat(expectedPdb.params().containsKey(key)).isTrue();
      Map<String, String> actualParamMap = actualPdb.params().get(key);
      Map<String, String> expectedParamMap = expectedPdb.params().get(key);
      assertThat(actualParamMap).as("PD param map for "+key+" is wrong").isEqualTo(expectedParamMap);
    }
  }

  //

  /**
   * Convert raw JSON string to Problem Detail box. Can handle custom properties, though only one level deep.
   * @param rawJson JSON string.
   * @return Problem Detail box instance.
   */
  private ProblemDetailBox convert(String rawJson) throws JsonProcessingException {
    JsonNode json = objectMapper.readTree(rawJson);
    Map<String, Map<String, String>> params = convertParams(json);
    return new ProblemDetailBox(
        json.get("status").asInt(),
        json.get("title").asText(),
        json.get("detail").asText(),
        json.get("instance").asText(),
        json.get("type").asText(),
        params
    );
  }

  /** Default fields of Problem Detail. */
  List<String> PD_DEFAULTS = List.of("status", "title", "detail", "instance", "type");

  /**
   * Convert custom parameters for Problem Detail. Note we handle only one layer.
   * @param jsonNode JSON object that represent Problem Detail.
   * @return Map of params.
   */
  private Map<String, Map<String, String>> convertParams(JsonNode jsonNode) {
    HashMap<String, Map<String, String>> paramsMap = Maps.newHashMap();

    jsonNode.propertyStream()
        .filter(e -> !PD_DEFAULTS.contains(e.getKey())) // skip already handled fields
        .forEach(e -> paramsMap.put(e.getKey(), convertInnerParams(e.getValue())));

    return paramsMap;
  }

  /**
   * Convert inner parameters.
   * @param jsonNode JSON node for one parameter.
   * @return Map of inner parameters.
   */
  private Map<String, String> convertInnerParams(JsonNode jsonNode) {
    HashMap<String, String> paramsMap = Maps.newHashMap();

    jsonNode.propertyStream()
        .forEach(e -> paramsMap.put(e.getKey(), e.getValue().textValue()));

    return paramsMap;
  }
}
