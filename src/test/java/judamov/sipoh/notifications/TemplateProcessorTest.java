package judamov.sipoh.notifications;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemplateProcessor")
class TemplateProcessorTest {

    @Test
    @DisplayName("render - sustituye todas las variables {{clave}}")
    void shouldReplaceAllPlaceholders() {
        String out = TemplateProcessor.render(
                "Hola {{nombre}}, doc {{documento}}",
                Map.of("nombre", "Ana", "documento", "123")
        );
        assertThat(out).isEqualTo("Hola Ana, doc 123");
    }

    @Test
    @DisplayName("render - deja texto sin cambios si no hay variables coincidentes")
    void shouldLeaveUnknownPlaceholders() {
        String out = TemplateProcessor.render("Sin variables", Map.of("x", "y"));
        assertThat(out).isEqualTo("Sin variables");
    }
}
