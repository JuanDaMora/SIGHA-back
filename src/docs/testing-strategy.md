# Estrategia de Testing – SIGHA Backend

## Para qué sirven los tests en este proyecto

Los tests del backend cumplen tres propósitos fundamentales:

1. **Prevención de regresiones**: Detectan automáticamente cuando un cambio de código rompe funcionalidad que ya estaba operando correctamente.
2. **Validación de reglas de negocio**: Documentan y verifican que la lógica crítica (autenticación, disponibilidad docente, gestión de grupos, validaciones de acceso) se comporta exactamente como se diseñó.
3. **Soporte a refactorización**: Permiten reorganizar código interno con confianza, siempre que los tests continúen pasando.

---

## Tipos de prueba existentes

### 1. Tests Unitarios (`src/test/java/.../service/`)

Son los tests más abundantes y rápidos. Prueban cada servicio de forma **aislada**, reemplazando todas sus dependencias externas (repositorios, otros servicios) con **mocks de Mockito**.

**Servicios cubiertos (todos los `*ServiceImpl` y `IndividualAvailabilityImpl`):**
- `UserRolServiceImplTest` – validación de roles y privilegios por programa.
- `JwtServiceImplTest` – generación, validación y parsing de tokens JWT.
- `AuthServiceImplTest` – login, registro, cambio de contraseña, consulta de usuarios, datos propios, carga masiva vacía.
- `AvailabilityServiceImplTest` – disponibilidad global e individual de docentes.
- `IndividualAvailabilityImplTest` – gestión de apertura/cierre de disponibilidad por docente.
- `GroupServiceImplTest` – CRUD de grupos, filtros, conflictos de horario, borrado masivo por semestre.
- `ScheduleServiceImplTest` – creación y eliminación de horarios por grupo.
- `SemesterServiceImplTest` – gestión de semestres.
- `SubjectServiceImplTest` – gestión de asignaturas.
- `AreaServiceImplTest` – gestión de áreas académicas.
- `LevelSubjectServiceImplTest` – niveles de asignatura.
- `TypeDocumentServiceImplTest` – tipos de documento y siglas.
- `EmailServiceImplTest` – envío de correos de bienvenida y recuperación.
- `RoleServiceImplTest` – listado de roles del sistema.
- `ProgramServiceImplTest` – listado de programas académicos.
- `StatusAvailabilityServiceImplTest` – estados de disponibilidad horaria.

### 2. Plantillas de Integración (preparadas, no activas por defecto)

#### H2 en memoria – `BaseRepositoryTest`
Para tests de repositorio con Spring Data JPA. Arrancan únicamente la capa de persistencia, sin servidor web ni seguridad. Son rápidos y no requieren infraestructura externa.

#### Testcontainers – `BaseContainerTest`
Para casos donde el comportamiento real del motor (MySQL/PostgreSQL) importa: queries nativas, constraints específicas, comportamiento transaccional complejo. Requieren Docker y la dependencia de Testcontainers activada en el `pom.xml`.

---

## Convención de nomenclatura

Todos los tests siguen el patrón:

```
shouldX_whenY
```

Ejemplos:
- `shouldReturnTokenOnSuccessfulLogin`
- `shouldThrowForbiddenWhenNotAdmin`
- `shouldDeleteObsoleteAvailabilityForNonAdmin`

Y siguen el patrón **AAA (Arrange – Act – Assert)**:
- **Arrange**: Preparar datos y configurar mocks.
- **Act**: Llamar al método bajo prueba.
- **Assert**: Verificar el resultado o efecto.

---

## Cobertura

Los reportes de cobertura JaCoCo se generan en `target/site/jacoco/index.html` después de ejecutar los tests.

**Metas progresivas:**
- Objetivo inicial: 60% de cobertura en el paquete `service.impl`.
- Objetivo siguiente: 75% en servicios críticos (auth, availability, grupos).
