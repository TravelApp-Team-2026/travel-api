package pl.travelapi.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {

	private int maxBytes;

	@Override
	public void initialize(MaxUtf8Bytes constraint) {
		this.maxBytes = constraint.value();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		// null is handled by @NotNull / @NotBlank
		return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= maxBytes;
	}
}
