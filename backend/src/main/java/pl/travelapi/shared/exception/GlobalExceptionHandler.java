package pl.travelapi.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * One error format for the whole API: RFC 9457 {@link ProblemDetail}.
 * {@code title} and {@code detail} are user-facing (Polish), the mobile app displays them directly.
 * Validation errors additionally carry {@code fieldErrors} (field name -> message); the validation
 * annotations of request DTOs must therefore define Polish {@code message}s. Errors of the whole object
 * (class-level constraints) have no field, so their messages become the {@code detail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Nieprawidłowa wartość.";
			fieldErrors.putIfAbsent(error.getField(), message);
		}
		List<String> globalMessages = ex.getBindingResult().getGlobalErrors().stream()
			.map(ObjectError::getDefaultMessage)
			.filter(message -> message != null && !message.isBlank())
			.toList();
		String detail = globalMessages.isEmpty() ? "Przesłane dane są nieprawidłowe." : String.join(" ", globalMessages);
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Nieprawidłowe żądanie", detail);
		problem.setProperty("fieldErrors", fieldErrors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Nieprawidłowe żądanie",
				"Nie można odczytać przesłanych danych. Sprawdź format żądania.");
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/**
	 * Standard Spring MVC errors (unknown path, wrong HTTP method, unsupported media type, ...) are
	 * produced by {@link ResponseEntityExceptionHandler} with English texts. Handlers in this class pass
	 * their own body, so only the ones created by Spring (body == null) are translated here.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (body == null && response != null && response.getBody() instanceof ProblemDetail problem) {
			localize(problem, statusCode.value());
		}
		return response;
	}

	private static void localize(ProblemDetail problem, int status) {
		switch (status) {
			case 400 -> {
				problem.setTitle("Nieprawidłowe żądanie");
				problem.setDetail("Żądanie jest nieprawidłowe lub brakuje wymaganych danych.");
			}
			case 404 -> {
				problem.setTitle("Nie znaleziono");
				problem.setDetail("Nie znaleziono żądanego zasobu.");
			}
			case 405 -> {
				problem.setTitle("Metoda niedozwolona");
				problem.setDetail("Ta metoda HTTP nie jest obsługiwana dla tego adresu.");
			}
			case 406 -> {
				problem.setTitle("Nieobsługiwany format odpowiedzi");
				problem.setDetail("Nie można zwrócić odpowiedzi w żądanym formacie.");
			}
			case 415 -> {
				problem.setTitle("Nieobsługiwany typ danych");
				problem.setDetail("Ten typ danych nie jest obsługiwany. Wyślij dane w formacie JSON.");
			}
			default -> {
				problem.setTitle("Błąd żądania");
				problem.setDetail("Nie można przetworzyć żądania.");
			}
		}
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Nie znaleziono", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Konflikt", ex.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials(BadCredentialsException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Brak autoryzacji", "Nieprawidłowy e-mail lub hasło");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Brak autoryzacji",
				"Wymagane jest zalogowanie. Zaloguj się ponownie.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Brak dostępu", "Nie masz uprawnień do wykonania tej operacji.");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd serwera",
				"Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
