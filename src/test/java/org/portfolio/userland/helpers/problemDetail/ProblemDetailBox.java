package org.portfolio.userland.helpers.problemDetail;

import java.util.Map;

/**
 * Box that contains all Problem Detail data. Suitable for assertions.
 */
public record ProblemDetailBox (
    int status,
    String title,
    String detail,
    String instance,
    String type,
    Map<String, Map<String, String>> params /* Custom parameters. */
) {}
