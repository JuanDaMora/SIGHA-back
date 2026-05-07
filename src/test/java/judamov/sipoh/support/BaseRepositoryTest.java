package judamov.sipoh.support;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Clase base para tests de repositorio con H2 en memoria.
 *
 * Uso:
 *   - Extender esta clase en cualquier test de repositorio.
 *   - Usa @DataJpaTest: arranca solo la capa JPA (sin servidor web, sin seguridad).
 *   - Usa el perfil "test" que configura H2 en application-test.properties.
 *
 * Ejemplo:
 * {@code
 *   class IUserRepositoryTest extends BaseRepositoryTest {
 *
 *       @Autowired
 *       private IUserRepository userRepository;
 *
 *       @Test
 *       void shouldFindByDocumento() {
 *           // ...
 *       }
 *   }
 * }
 */
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseRepositoryTest {
    // Clase base intencionalmente vacía.
    // Agrega aquí utilidades comunes de repositorio si se necesitan en el futuro.
}
