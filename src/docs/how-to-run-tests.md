# Cómo ejecutar los tests – SIGHA Backend

## Requisitos previos

- Java 17+
- Maven 3.8+
- No se requiere base de datos ni servidor de correo para los tests unitarios.

---

## Ejecutar todos los tests

```bash
mvn test
```

Esto ejecuta todos los tests unitarios y genera el reporte de cobertura JaCoCo en:

```
target/site/jacoco/index.html
```

---

## Ejecutar una clase de test específica

```bash
mvn test -Dtest=AuthServiceImplTest
```

---

## Ejecutar varios tests específicos

```bash
mvn test -Dtest="AuthServiceImplTest,AvailabilityServiceImplTest"
```

---

## Ejecutar un método de test específico

```bash
mvn test -Dtest="AuthServiceImplTest#shouldReturnTokenOnSuccessfulLogin"
```

---

## Ejecutar solo los tests de una capa

Tests de servicios:
```bash
mvn test -Dtest="judamov.sipoh.service.*"
```

---

## Generar solo el reporte de cobertura (sin re-ejecutar tests)

```bash
mvn jacoco:report
```

El reporte HTML estará disponible en `target/site/jacoco/index.html`. Abrirlo con cualquier navegador.

---

## Ejecutar con perfil de test explícito

Los tests unitarios usan automáticamente `src/test/resources/application-test.properties` (mediante `@ActiveProfiles("test")`). Si necesitas forzarlo:

```bash
mvn test -Dspring.profiles.active=test
```

---

## Tests de integración con H2 (repositorios)

Para extender `BaseRepositoryTest` en un test de repositorio:

```java
class IUserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private IUserRepository userRepository;

    @Test
    void shouldFindByDocumento() {
        // ...
    }
}
```

Ejecutar igual con `mvn test`.

---

## Tests de integración con Testcontainers (motor real)

> **Requiere Docker corriendo en la máquina.**

Pasos para activar Testcontainers:
1. Añadir la dependencia `testcontainers:mysql` y `testcontainers:junit-jupiter` en `pom.xml` (ver comentarios en `BaseContainerTest.java`).
2. Descomentar las anotaciones `@Testcontainers` y `@Container` en `BaseContainerTest`.
3. Anotar los tests de integración con `@Tag("integration")` para separarlos.

Para ejecutar solo tests de integración:
```bash
mvn test -Dgroups=integration
```

Para excluirlos del ciclo normal:
```bash
mvn test -DexcludedGroups=integration
```

---

## Ver el reporte de cobertura

Después de `mvn test`, abre en el navegador:
```
target/site/jacoco/index.html
```

Las columnas clave:
- **Lines**: Porcentaje de líneas de código ejecutadas.
- **Branches**: Porcentaje de ramas condicionales (if/else) cubiertas.
- **Methods**: Porcentaje de métodos invocados al menos una vez.
