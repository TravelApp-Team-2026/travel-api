package pl.travelapi.trip.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Rules that compare several fields of {@link SaveTripRequest}: the dates and the number of legs. */
@Documented
@Constraint(validatedBy = ValidTripRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTripRequest {

	String message() default "Nieprawidłowe dane podróży.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
