package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user management. All endpoints here require administration panel access permissions.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/admin/users/all</code> - get all users. Note it returns only DTO: data present in table and available options.</li>
 *   <li><code>GET /api/admin/user/{id}</code> - get data about single user.</li>
 *   <li><code>GET /api/admin/user/{id}/history</code> - get data about history for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/tokens</code> - get data about tokens for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/jwt</code> - get data about JWT for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/config</code> - get data about config for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/permissions</code> - get data about permissions for given user.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts. Requires administration panel access permissions.")
public class UserAdminController {
  // TODO actually implement it
}
