# SIGHA - Sistema de Gestión Horaria

## 🧰 Requisitos Previos

- Docker y Docker Compose instalados.
- Java 17 instalado.
- Maven instalado (`mvn` disponible en el `PATH`).
- (Opcional) Cliente PostgreSQL como DBeaver o pgAdmin.


### Levanta la BD en docker

```Levanta la BD en docker
docker run --name sigha-database -e POSTGRES_USER=usuario -e POSTGRES_PASSWORD=clave123 -e POSTGRES_DB=sigha -p 5432:5432 -d postgres
```
### sql para poblar la bd
```
INSERT INTO sigha.roles (name) VALUES
('DIRECTOR_DE_ESCUELA'),
('COORDINADOR_ACADEMICO'),
('PROFESOR');

INSERT INTO sigha.type_document (description ) VALUES
('CEDULA DE CIUDADANIA');

INSERT INTO sigha.semester (description, start_date, end_date, created_at,updated_at)
VALUES ('2025-1', '2025-01-01', '2025-06-30', NOW(), NOW());

INSERT INTO status_availability (description)
VALUES
('ENVIADO'),
('APROBADO'),
('RECHAZADO')
ON CONFLICT DO NOTHING;
```
### Despigue del proyecto en docker y docker compose
```
mvn clean package -DskipTests

docker build -t sigha-back .

docker compose down -v

docker compose up --build

```

---

## Tests

### Ejecutar todos los tests unitarios

```bash
mvn test
```

### Ver reporte de cobertura

Después de ejecutar los tests, abrir en el navegador:
```
target/site/jacoco/index.html
```

### Documentación de testing

| Documento | Descripción |
|-----------|-------------|
| [`src/docs/testing-strategy.md`](src/docs/testing-strategy.md) | Estrategia de testing: para qué sirven y qué tipos existen |
| [`src/docs/how-to-run-tests.md`](src/docs/how-to-run-tests.md) | Guía completa de ejecución y lectura de resultados |
| [`src/docs/pr-checklist.md`](src/docs/pr-checklist.md) | Convenciones y checklist para PRs con nuevos tests |
