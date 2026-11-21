package pl.travelapi.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text must not exceed {@link #value()} bytes when encoded as UTF-8.
 * Needed for passwords: BCrypt accepts at most 72 bytes (not characters) and Spring Security
 * throws {@link IllegalArgumentException} for anything longer.
 */
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
public @interface MaxUtf8Bytes {

	int value();

	String message() default "Wartość jest zbyt długa.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
