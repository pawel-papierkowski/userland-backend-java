package org.portfolio.userland;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Entry point to Spring Boot application.</p>
 * <p>Note that placement of @SpringBootApplication determines root package to be scanned, including subpackages.</p>
 */
@SpringBootApplication
@RegisterReflectionForBinding({
		org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl.class // To prevent config error. Spring should not use it anyway on GraalVM.
})
public class UserLandApplication {
	/**
	 * Entry point.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserLandApplication.class, args);
	}
}
