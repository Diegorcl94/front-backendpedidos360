package cl.duoc.rabbitadmin.exception;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AdminOperationException.class)
    public ResponseEntity<?> operation(AdminOperationException ex, HttpServletRequest request) {
        return error(ex.getStatus(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(),
                "Campo obligatorio o formato/longitud no permitido."));
        return error(HttpStatus.BAD_REQUEST, "Solicitud inválida.", request, fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<?> badRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Solicitud inválida; revisa campos, formato y JSON.", request, Map.of());
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<?> amqp(AmqpException ex, HttpServletRequest request) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ShutdownSignalException shutdown && shutdown.getReason() instanceof AMQP.Channel.Close close) {
                if (close.getReplyCode() == 404) {
                    return error(HttpStatus.NOT_FOUND, "El recurso RabbitMQ no existe.", request, Map.of());
                }
                if (close.getReplyCode() == 406 || close.getReplyCode() == 405) {
                    return error(HttpStatus.CONFLICT, "Recurso incompatible, ocupado o con mensajes; no se completó la operación.", request, Map.of());
                }
            }
        }
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo confirmar la operación en RabbitMQ. Revisa su estado antes de reintentar.", request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> notFound(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Endpoint inexistente.", request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> method(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido.", request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> media(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Usa Content-Type: application/json.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al administrar RabbitMQ.", request, Map.of());
    }

    private ResponseEntity<?> error(HttpStatus status, String message, HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), message,
                request.getRequestURI(), fields));
    }

    public record ApiError(Instant timestamp, int status, String message, String path, Map<String, String> fields) {}
}
