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
 * Validates password.
 */
@NotBlank(message = "Password is required")
@Size(min = ValidConst.PASS_LEN_MIN, max = ValidConst.PASS_LEN_MAX, message = "Password must be between "+ValidConst.PASS_LEN_MIN+" and "+ValidConst.PASS_LEN_MAX+" characters")
@Pattern(
    regexp = ValidConst.PASS_REGEXPR,
    message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
  String message() default "";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
