package judamov.sipoh.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @SuppressWarnings("unchecked")
    private String messageBody(ResponseEntity<?> r) {
        return (String) ((Map<String, Object>) r.getBody()).get("message");
    }

    @Test
    @DisplayName("handleAppException - propaga status y mensaje")
    void shouldHandleAppException() {
        ResponseEntity<?> r = handler.handleAppException(
                new GenericAppException(HttpStatus.CONFLICT, "conflicto"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(messageBody(r)).isEqualTo("conflicto");
    }

    @Test
    @DisplayName("handleMissingHeader")
    void shouldHandleMissingHeader() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("semesterId");
        ResponseEntity<?> r = handler.handleMissingHeader(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageBody(r)).contains("semesterId");
    }

    @Test
    @DisplayName("handleMissingParam")
    void shouldHandleMissingParam() {
        MissingServletRequestParameterException ex = mock(MissingServletRequestParameterException.class);
        when(ex.getParameterName()).thenReturn("id");
        ResponseEntity<?> r = handler.handleMissingParam(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageBody(r)).contains("id");
    }

    @Test
    @DisplayName("handleUnreadableMessage")
    void shouldHandleUnreadableMessage() {
        ResponseEntity<?> r = handler.handleUnreadableMessage(mock(HttpMessageNotReadableException.class));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageBody(r)).contains("Cuerpo");
    }

    @Test
    @DisplayName("handleValidationErrors")
    void shouldHandleValidationErrors() {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new FieldError("obj", "email", "no válido"));
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(null, br);
        ResponseEntity<?> r = handler.handleValidationErrors(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageBody(r)).contains("email");
    }

    @Test
    @DisplayName("handleTypeMismatch")
    void shouldHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("groupId");
        ResponseEntity<?> r = handler.handleTypeMismatch(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageBody(r)).contains("groupId");
    }

    @Test
    @DisplayName("handleMethodNotSupported")
    void shouldHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");
        ResponseEntity<?> r = handler.handleMethodNotSupported(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(messageBody(r)).contains("PATCH");
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported")
    void shouldHandleMediaTypeNotSupported() {
        HttpMediaTypeNotSupportedException ex = mock(HttpMediaTypeNotSupportedException.class);
        when(ex.getContentType()).thenReturn(MediaType.APPLICATION_ATOM_XML);
        ResponseEntity<?> r = handler.handleMediaTypeNotSupported(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(messageBody(r)).contains("atom");
    }

    @Test
    @DisplayName("handleNoResourceFound")
    void shouldHandleNoResourceFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getResourcePath()).thenReturn("/api/missing");
        ResponseEntity<?> r = handler.handleNoResourceFound(ex);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(messageBody(r)).contains("/api/missing");
    }

    @Test
    @DisplayName("handleGenericException")
    void shouldHandleGenericException() {
        ResponseEntity<?> r = handler.handleGenericException(new RuntimeException("boom"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(messageBody(r)).contains("Error interno");
    }
}
