package org.portfolio.userland.features.user.service;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.UserRepository;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.dto.UserRegisterReq;
import org.portfolio.userland.features.user.exception.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.mapper.UserRegisterMapper;
import org.springframework.stereotype.Service;

/**
 * Business logic for user registration.
 */
@Service
@RequiredArgsConstructor
public class UserRegisterService {
  private final UserRepository userRepository;
  private final UserRegisterMapper userRegisterMapper;

  /**
   * Registers user in UserLand system.
   * @param userRegisterReq User registration request.
   */
  public void register(UserRegisterReq userRegisterReq) {
    verifyRegistration(userRegisterReq);
    User entity = fillData(userRegisterReq);
    userRepository.save(entity);

    // TODO in future we will send e-mail with confirmation link.
  }

  /**
   * Fill user data.
   * @param userRegisterReq User registration request.
   * @return User data.
   */
  private User fillData(UserRegisterReq userRegisterReq) {
    User entity = userRegisterMapper.toEntity(userRegisterReq);
    // Simple fields like status or blocked are pre-filled already.
    return entity;
  }

  /**
   * Verifies state of user, ensuring it is allowed to be registered in system.
   * @param userRegisterReq User registration request.
   */
  private void verifyRegistration(UserRegisterReq userRegisterReq) {
    if (userRepository.existsByEmail(userRegisterReq.email()))
      throw new UserEmailAlreadyExistsException(userRegisterReq.email());
  }
}
