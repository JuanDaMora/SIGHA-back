package judamov.sipoh;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de contexto de Spring Boot.
 * Desactivado en CI/local por requerir variables de entorno externas (DB, mail).
 * Activar manualmente en entornos con infraestructura completa.
 */
@SpringBootTest
@Disabled("Requiere variables de entorno externas (DB, mail). Ejecutar solo con infraestructura completa.")
class SIPOHApplicationTests {

	@Test
	void contextLoads() {
	}

}
