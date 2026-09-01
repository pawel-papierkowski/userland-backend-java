package org.portfolio.userland.common.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.portfolio.userland.common.constants.ValidConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates token.
 */
@NotBlank(message = "Token string is required")
@Size(min = ValidConst.TOKEN_LEN_MIN, max = ValidConst.TOKEN_LEN_MAX, message = "Token string must be between "+ValidConst.TOKEN_LEN_MIN+" and "+ValidConst.TOKEN_LEN_MAX+" characters")
@Pattern(
    regexp = ValidConst.TOKEN_REGEXPR,
    message = "Token string must be alphanumeric"
)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidToken {
  String message() default "";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
