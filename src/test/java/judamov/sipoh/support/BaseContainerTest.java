package judamov.sipoh.support;

import org.springframework.test.context.ActiveProfiles;

/**
 * Clase base para tests de integración con Testcontainers (motor real).
 *
 * USO PREVISTO (cuando se añada la dependencia de Testcontainers):
 *
 *   1. Agregar en pom.xml (scope=test):
 *      <dependency>
 *          <groupId>org.testcontainers</groupId>
 *          <artifactId>mysql</artifactId>
 *      </dependency>
 *      <dependency>
 *          <groupId>org.testcontainers</groupId>
 *          <artifactId>junit-jupiter</artifactId>
 *      </dependency>
 *
 *   2. Descomentar la anotación @Testcontainers y el campo @Container en esta clase.
 *
 *   3. Extender esta clase en el test que lo necesite.
 *
 * Casos candidatos para Testcontainers:
 *   - Queries nativas con sintaxis específica de MySQL/PostgreSQL.
 *   - Comportamiento transaccional avanzado.
 *   - Constraints de base de datos no simulables con H2.
 *
 * Ejemplo de activación:
 * {@code
 *   // @Testcontainers
 *   // @Container
 *   // static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
 *
 *   class GroupRepositoryContainerTest extends BaseContainerTest {
 *       // tests que requieren motor real
 *   }
 * }
 */
@ActiveProfiles("test")
public abstract class BaseContainerTest {
    // Clase base intencionalmente vacía hasta activar Testcontainers.
}
