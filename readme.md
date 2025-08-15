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
