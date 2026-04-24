package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user management. All endpoints here require administration permissions.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/users/all</code> - get all users.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts.")
public class UserAdminController {
  // TODO actually implement it
}
