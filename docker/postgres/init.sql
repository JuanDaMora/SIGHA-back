CREATE SCHEMA IF NOT EXISTS sigha;
    SET search_path TO sigha;

CREATE TABLE email_templates (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    subject TEXT NOT NULL,
    body TEXT NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla sigla
CREATE TABLE IF NOT EXISTS sigla (
    id SERIAL PRIMARY KEY,
    sigla VARCHAR(255),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla roles
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla type_document
CREATE TABLE IF NOT EXISTS type_document (
    id SERIAL PRIMARY KEY,
    sigla_id INTEGER REFERENCES sigla(id),
    description VARCHAR(255) NOT NULL UNIQUE,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla semester
CREATE TABLE IF NOT EXISTS semester (
    id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL UNIQUE,
    start_date DATE NOT NULL UNIQUE,
    end_date DATE NOT NULL,
    availability BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla user
CREATE TABLE IF NOT EXISTS "user" (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    id_type_document INTEGER NOT NULL REFERENCES type_document(id),
        documento VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    token_hash VARCHAR(255),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS access_control (
    id SERIAL PRIMARY KEY,
    id_user INTEGER NOT NULL REFERENCES "user"(id),
    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
-- Tabla user_rol
CREATE TABLE IF NOT EXISTS user_rol (
    id SERIAL PRIMARY KEY,
    id_user INTEGER NOT NULL REFERENCES "user"(id),
    id_role INTEGER NOT NULL REFERENCES roles(id),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla area
CREATE TABLE IF NOT EXISTS area (
    id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla user_area
CREATE TABLE IF NOT EXISTS user_area (
    id SERIAL PRIMARY KEY,
    id_user INTEGER NOT NULL REFERENCES "user"(id),
    id_area INTEGER NOT NULL REFERENCES area(id),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla level_subject
CREATE TABLE IF NOT EXISTS level_subject (
    id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla subject
CREATE TABLE IF NOT EXISTS subject (
    id SERIAL PRIMARY KEY,
    id_area INTEGER NOT NULL REFERENCES area(id),
    id_level_subject INTEGER NOT NULL REFERENCES level_subject(id),
    codigo VARCHAR(255),
    name VARCHAR(255),
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla status_availability
CREATE TABLE IF NOT EXISTS status_availability (
    id SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Tabla availability
CREATE TABLE IF NOT EXISTS availability (
    id SERIAL PRIMARY KEY,
    id_user INTEGER NOT NULL REFERENCES "user"(id),
    id_semester INTEGER NOT NULL REFERENCES semester(id),
    id_status_availability INTEGER NOT NULL REFERENCES status_availability(id),
    start_time TIME NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
CREATE TABLE IF NOT EXISTS "group"(
    id SERIAL PRIMARY KEY,
    id_semester INTEGER NOT NULL REFERENCES semester(id),
    id_subject INTEGER NOT NULL REFERENCES subject(id),
    id_user INTEGER REFERENCES "user"(id),
    code VARCHAR(50) NOT NULL,
    max_capacity VARCHAR(10),
    enrolled VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS schedule(
    id SERIAL PRIMARY KEY,
    id_group INTEGER NOT NULL REFERENCES "group"(id),
    start_time TIME,
    day_of_week VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS sigha.individual_availability (
                                                             id SERIAL PRIMARY KEY,
                                                             id_user INTEGER NOT NULL REFERENCES "user"(id),
    id_semester INTEGER NOT NULL REFERENCES semester(id),
    is_active BOOLEAN not null default true,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
ALTER TABLE sigha.individual_availability
    ADD CONSTRAINT unique_user_semester UNIQUE (id_user, id_semester);

-- Datos base
INSERT INTO roles (name) VALUES
                             ('DIRECTOR DE ESCUELA'),
                             ('COORDINADOR ACADEMICO'),
                             ('PROFESOR')
    ON CONFLICT DO NOTHING;
insert into sigla (sigla )values
    ('C.C'),
    ('PPT'),
    ('C.E')
    on conflict do nothing;

INSERT INTO type_document (sigla_id,description) VALUES
    ('1','CEDULA DE CIUDADANIA'),
    ('2', 'PERMISO POR PROTECCION TEMPORAL'),
    ('3', 'CIUDADANIA EXTRANJERA')
    ON CONFLICT DO NOTHING;

INSERT INTO semester (description, start_date, end_date, created_at, updated_at)
VALUES ('2025-1', '2025-01-01', '2025-06-30', NOW(), NOW())
    ON CONFLICT DO NOTHING;

INSERT INTO status_availability (description)
VALUES
    ('ENVIADO'),
    ('APROBADO'),
    ('RECHAZADO')
    ON CONFLICT DO NOTHING;

INSERT INTO level_subject (description) VALUES
                                            ('NIVEL 1'),
                                            ('NIVEL 2'),
                                            ('NIVEL 3'),
                                            ('NIVEL 4'),
                                            ('NIVEL 5'),
                                            ('NIVEL 6'),
                                            ('NIVEL 7'),
                                            ('NIVEL 8'),
                                            ('NIVEL 9'),
                                            ('NIVEL 10')
    ON CONFLICT DO NOTHING;
INSERT INTO area (description) VALUES
                                   ('MATEMÁTICAS COMPUTACIONALES'),
                                   ('ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
                                   ('ALGORÍTMICA E INFORMÁTICA'),
                                   ('ADMINISTRACIÓN DE LA INFORMACIÓN'),
                                   ('ADMINISTRATIVAS Y ORGANIZACIONALES'),
                                   ('REDES Y COMUNICACIONES'),
                                   ('INGENIERÍA DEL SOFTWARE'),
                                   ('SISTEMAS'),
                                   ('INGENIERÍA ARTIFICIAL'),
                                   ('DESCONOCIDO')
    ON CONFLICT DO NOTHING;

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 3'),'22954','MATEMÁTICAS DISCRETAS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 6'),'21857','ESTADÍSTICA I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 7'),'21858','ESTADÍSTICA II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22962','ANÁLISIS NUMÉRICO',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),(SELECT id FROM level_subject WHERE description = 'NIVEL 4'),'22957','ELECTRICIDAD Y ELECTRÓNICA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22961','SISTEMAS DIGITALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),(SELECT id FROM level_subject WHERE description = 'NIVEL 6'),'22966','ARQUITECTURA DE COMPUTADORES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),(SELECT id FROM level_subject WHERE description = 'NIVEL 8'),'22972','SISTEMAS OPERACIONALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','SISTEMAS DISTRIBUIDOS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 1'),'22948','FUNDAMENTOS DE PROGRAMACIÓN',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 2'),'22951','PROGRAMACIÓN ORIENTADA A OBJETOS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 3'),'22955','ESTRUCTURA DE DATOS Y ANÁLISIS DE ALGORITMOS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 6'),'22967','PROGRAMACIÓN EN LA WEB',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 4'),'22958','AUTÓMATAS Y LENGUAJES FORMALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'24542','ENTORNOS DE PROGRAMACIÓN',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','PROGRAMACIÓN DISTRIBUIDA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ADMINISTRACIÓN DE LA INFORMACIÓN'),(SELECT id FROM level_subject WHERE description = 'NIVEL 4'),'22959','BASES DE DATOS I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ADMINISTRACIÓN DE LA INFORMACIÓN'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22960','BASES DE DATOS II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22963','PENSAMIENTO SISTÉMICO Y ORGANIZACIONAL',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 6'),'22968','SISTEMAS DE INFORMACIÓN',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','AUDITORÍA DE SISTEMAS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 6'),'22965','REDES DE COMPUTADORES I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 7'),'22970','REDES DE COMPUTADORES II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','GESTIÓN DE REDES EMPRESARIALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA DEL SOFTWARE'),(SELECT id FROM level_subject WHERE description = 'NIVEL 7'),'22969','INGENIERÍA DEL SOFTWARE I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA DEL SOFTWARE'),(SELECT id FROM level_subject WHERE description = 'NIVEL 8'),'22973','INGENIERÍA DEL SOFTWARE II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 8'),'22974','SIMULACIÓN DIGITAL',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','TRATAMIENTO DE SEÑALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','MODELADO ESTRUCTURAL',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','INVESTIGACIÓN OPERACIONAL',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','MODELOS A GRAN ESCALA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'SISTEMAS'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','SISTEMAS DISCRETOS Y CONTINUOS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 7'),'22971','INGENIERÍA ARTIFICIAL I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'24552','INGENIERÍA ARTIFICIAL II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','INGENIERÍA ARTIFICIAL III',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','MICROCONTROLADORES I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','MICROCONTROLADORES II',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),(SELECT id FROM level_subject WHERE description = 'NIVEL 10'),'12345','INFORMÁTICA BIOMÉDICA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'41333','ALGORITMOS Y PROGRAMACION',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'27582','ANALISIS DE DATOS A GRAN ESCALA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'40941','BIOLOGIA CELULAR Y MOLECULAR',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'40937','BIOQUIMICA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'23016','ESTRUCTURAS COMPUTACIONALES',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'21870','GERENCIA DE INFORMATICA I',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'28664','INNOVACION EDUCATIVA EN LA SOCIEDAD DE INFORMACION',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'41331','INTRODUCCION A LA ING EN CIENCIA DE DATOS',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'40938','INTRODUCCION A LA INGENIERIA BIOMEDICA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'28665','MODELOS DE NEGOCIOS EN LA SOCIEDAD DE LA INFORMACIÓN',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'27798','OPTIMIZACION CONVEXA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'28091','PRINCIPIOS Y PRACTICAS DE DESARROLLO DE SOFTWARE ORIENTADO',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'41017','PROGRAMACION',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22490','SEGURIDAD INFORMATICA',NOW(), NOW());

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'22968','SISTEMAS DE INFORMACION',NOW(), NOW());


INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES ((SELECT id FROM area WHERE description = 'DESCONOCIDO'),(SELECT id FROM level_subject WHERE description = 'NIVEL 5'),'27571','PROCESAMIENTO DE IMAGENES DIGITALES',NOW(), NOW());

---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--
-- -- ALGORITMOS Y PROGRAMACION -- A1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '41333'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'A1', '21', '21', NOW(), NOW());
--
-- -- ALGORITMOS Y PROGRAMACION -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '41333'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'E1', '21', '21', NOW(), NOW());
--
-- -- ANALISIS DE DATOS A GRAN ESCALA -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '27582'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'E1', '25', '21', NOW(), NOW());
--
-- -- ANALISIS NUMERICO -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22962'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C1', '25', '21', NOW(), NOW());
--
-- -- ANALISIS NUMERICO -- C2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22962'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C2', '25', '21', NOW(), NOW());
--
-- -- ANALISIS NUMERICO -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22962'),(SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'E1', '23', '23', NOW(), NOW());
--
-- -- ANALISIS NUMERICO -- E2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22962'), (SELECT id FROM sigha."user" u WHERE u.documento = '91511969' ) , 'E2', '23', '23', NOW(), NOW());
--
-- -- ANALISIS NUMERICO -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22962'), (SELECT id FROM sigha."user" u WHERE u.documento = '91511969' ) , 'F1', '23', '23', NOW(), NOW());
--
-- -- ARQUITECTURA DE COMPUTADORES -- A1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22966'), (SELECT id FROM sigha."user" u WHERE u.documento = '6750912' ) , 'A1', '25', '25', NOW(), NOW());
--
-- -- ARQUITECTURA DE COMPUTADORES -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22966'), (SELECT id FROM sigha."user" u WHERE u.documento = '91511969' ) , 'E1', '25', '25', NOW(), NOW());
--
-- -- ARQUITECTURA DE COMPUTADORES -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22966'), (SELECT id FROM sigha."user" u WHERE u.documento = '91506973' ) , 'F1', '25', '25', NOW(), NOW());
--
-- -- ARQUITECTURA DE COMPUTADORES -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22966'), (SELECT id FROM sigha."user" u WHERE u.documento = '6750912' ) , 'G1', '25', '25', NOW(), NOW());
--
-- -- AUTOMATAS Y LENGUAJES FORMALES -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22958'), (SELECT id FROM sigha."user" u WHERE u.documento = '1098617175' ) , 'C1', '22', '19', NOW(), NOW());
--
-- -- AUTOMATAS Y LENGUAJES FORMALES -- C2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22958'), (SELECT id FROM sigha."user" u WHERE u.documento = '1098717616' ) , 'C2', '22', '19', NOW(), NOW());
--
-- -- AUTOMATAS Y LENGUAJES FORMALES -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22958'), (SELECT id FROM sigha."user" u WHERE u.documento = '1098617175' ) , 'E1', '22', '19', NOW(), NOW());
--
-- -- BASES DE DATOS I -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22959'), (SELECT id FROM sigha."user" u WHERE u.documento = '6750912' ) , 'C1', '20', '19', NOW(), NOW());
--
-- -- BASES DE DATOS I -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22959'), (SELECT id FROM sigha."user" u WHERE u.documento = '1098662948' ) , 'G1', '21', '19', NOW(), NOW());
--
-- -- BASES DE DATOS I -- G2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22959'), (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ) , 'G2', '21', '19', NOW(), NOW());
--
-- -- BASES DE DATOS II -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22960'), (SELECT id FROM sigha."user" u WHERE u.documento = '1099371476' ) , 'F1', '21', '19', NOW(), NOW());
--
-- -- BASES DE DATOS II -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22960'), (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ) , 'F2', '21', '19', NOW(), NOW());
--
-- -- BASES DE DATOS II -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22960'), (SELECT id FROM sigha."user" u WHERE u.documento = '1099371476' ) , 'G1', '21', '19', NOW(), NOW());
--
-- -- BASES DE DATOS II -- G2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22960'), (SELECT id FROM sigha."user" u WHERE u.documento = '1099371476' ) , 'G2', '21', '19', NOW(), NOW());
--
-- -- BIOLOGIA CELULAR Y MOLECULAR -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at )
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40941'), (SELECT id FROM sigha."user" u WHERE u.documento = '0' ) , 'B1', '21', '19', NOW(), NOW());
--
-- -- BIOLOGIA CELULAR Y MOLECULAR -- B2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40941'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B2', '21', '19', NOW(), NOW());
--
-- -- BIOLOGIA CELULAR Y MOLECULAR -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40941'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- BIOQUIMICA -- PB1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40937'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'PB1', '21', '19', NOW(), NOW());
--
-- -- BIOQUIMICA -- PB2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40937'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'PB2', '21', '19', NOW(), NOW());
--
-- -- BIOQUIMICA -- PF1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40937'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098617404' ), 'PF1', '21', '19', NOW(), NOW());
--
-- -- BIOQUIMICA -- PF2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40937'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098617404' ), 'PF2', '21', '19', NOW(), NOW());
--
-- -- ELECTRICIDAD Y ELECTRONICA -- A1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22957'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1127942021' ), 'A1', '21', '19', NOW(), NOW());
--
-- -- ELECTRICIDAD Y ELECTRONICA -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22957'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1127942021' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- ELECTRICIDAD Y ELECTRONICA -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22957'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1127942021' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- ENTORNOS DE PROGRAMACION -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '24542'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91246221' ), 'E1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA I -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21857'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '591267' ), 'G1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA I -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21857'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '591267' ), 'E1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA I -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21857'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA I -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21857'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098604393' ), 'F2', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA II -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21858'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91275575' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA II -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21858'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '591267' ), 'E1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA II -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21858'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91477322' ), 'F1', '21', '19', NOW(), NOW());
--
-- -- ESTADISTICA II -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '21858'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098604393' ), 'F2', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE I -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22969'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE I -- C2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22969'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'C2', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE I -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22969'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F2', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE II -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22973'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'F2', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE II -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22973'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE II -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22973'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'E1', '21', '19', NOW(), NOW());
--
-- -- INGENIERIA DEL SOFTWARE II -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22973'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'G1', '21', '19', NOW(), NOW());
--
-- -- INNOVACION EDUCATIVA EN LA SOCIEDAD DE INFORMACION -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '28664'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL I -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22971'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098801497' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL I -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22971'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098801497' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL I -- E1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22971'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'E1', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL I -- E2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22971'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'E2', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL I -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22971'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F1', '21', '19', NOW(), NOW());
--
-- -- INTELIGENCIA ARTIFICIAL II -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '24552'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098717616' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- INTRODUCCION A LA ING EN CIENCIA DE DATOS -- PB1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '41331'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'PB1', '21', '19', NOW(), NOW());
--
-- -- INTRODUCCION A LA ING EN CIENCIA DE DATOS -- PD1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '41331'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91498272' ), 'PD1', '21', '19', NOW(), NOW());
--
-- -- INTRODUCCION A LA INGENIERIA BIOMEDICA -- PC1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40938'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'PC1', '21', '19', NOW(), NOW());
--
-- -- INTRODUCCION A LA INGENIERIA BIOMEDICA -- PE1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '40938'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'PE1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098617175' ), 'B1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1098617175' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- C1 (Doble con otro docente)
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91275575' ), 'C1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '91275575' ), 'F1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- F1 (Doble con docente desconocido)
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F1', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- F2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F2', '21', '19', NOW(), NOW());
--
-- -- MATEMATICAS DISCRETAS -- F3
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22954'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F3', '23', '21', NOW(), NOW());
--
-- -- MODELOS DE NEGOCIOS EN LA SOCIEDAD DE LA INFORMACIÓN -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '28665'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B1', '29', '28', NOW(), NOW());
--
-- -- OPTIMIZACION CONVEXA -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '27798'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C1', '25', '21', NOW(), NOW());
--
-- -- PENSAMIENTO SISTEMICO Y ORGANIZACIONAL -- A1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22963'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'A1', '24', '23', NOW(), NOW());
--
-- -- PENSAMIENTO SISTEMICO Y ORGANIZACIONAL -- B1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22963'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B1', '24', '24', NOW(), NOW());
--
-- -- PENSAMIENTO SISTEMICO Y ORGANIZACIONAL -- B2
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22963'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'B2', '24', '24', NOW(), NOW());
--
-- -- PENSAMIENTO SISTEMICO Y ORGANIZACIONAL -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '22963'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C1', '28', '28', NOW(), NOW());
--
-- -- PRINCIPIOS Y PRACTICAS DE DESARROLLO DE SOFTWARE ORIENTADO A -- C1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '28091'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'C1', '24', '23', NOW(), NOW());
--
-- -- PRINCIPIOS Y PRACTICAS DE DESARROLLO DE SOFTWARE ORIENTADO A -- G1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '28091'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '1049413439' ), 'G1', '25', '25', NOW(), NOW());
--
-- -- PROCESAMIENTO DE IMAGENES DIGITALES -- F1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '27571'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'F1', '25', '25', NOW(), NOW());
--
-- -- PROGRAMACION -- A1
-- INSERT INTO sigha."group" (id_semester, id_subject, id_user, code, max_capacity, enrolled, created_at, updated_at)
-- VALUES (1, (SELECT id FROM sigha.subject WHERE codigo = '41017'),
--         (SELECT id FROM sigha."user" u WHERE u.documento = '0' ), 'A1', '25', '20', NOW(), NOW());





INSERT INTO email_templates (code, subject, body) VALUES (
'credenciales_acceso',
'Bienvenido a SIGHA - Credenciales de acceso al sistema',
'<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
body {
font-family: Arial, sans-serif;
background-color: #f7f9fc;
color: #333;
}
.container {
background-color: #ffffff;
padding: 20px;
margin: auto;
border: 1px solid #e1e4e8;
border-radius: 8px;
max-width: 600px;
}
.header {
text-align: center;
margin-bottom: 20px;
}
.header h2 {
color: #0056b3;
}
.footer {
margin-top: 30px;
font-size: 12px;
color: #888;
text-align: center;
}
.credentials {
background-color: #f0f4f8;
padding: 10px;
border-radius: 5px;
}
</style>
</head>
<body>
<div class="container">
<div class="header">
<h2>Bienvenido a SIGHA</h2>
<p>Sistema de Gestión de Horarios Académicos</p>
</div>

<p>Estimado(a) <strong>{{nombre_destinatario}}</strong>,</p>

<p>Le damos la bienvenida al sistema <strong>SIGHA</strong>. A continuación encontrará sus credenciales de acceso personalizadas:</p>

<div class="credentials">
<p><strong>Documento:</strong> {{documento_destinatario}}</p>
<p><strong>Contraseña temporal:</strong> {{password_destinatario}}</p>
</div>

<p>Por favor, ingrese al sistema con estas credenciales y cambie su contraseña lo antes posible para garantizar la seguridad de su cuenta.</p>

<p>Si tiene alguna duda o inconveniente, no dude en contactar al equipo de soporte técnico.</p>

<div class="footer">
<p>Este es un mensaje automático del sistema SIGHA.</p>
</div>
</div>
</body>
</html>'
);

