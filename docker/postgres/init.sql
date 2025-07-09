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
-- Datos base
INSERT INTO roles (name) VALUES
                             ('DIRECTOR DE ESCUELA'),
                             ('COORDINADOR ACADEMICO'),
                             ('PROFESOR')
    ON CONFLICT DO NOTHING;
insert into sigla (sigla )values
    ('C.C')
    on conflict do nothing;

INSERT INTO type_document (sigla_id,description) VALUES
    ('1','CEDULA DE CIUDADANIA')
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
VALUES (
           (SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 3'),
           '22954',
           'MATEMÁTICAS DISCRETAS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 6'),
           '21857',
           'ESTADÍSTICA I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 7'),
           '21857',
           'ESTADÍSTICA II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'MATEMÁTICAS COMPUTACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '12345',
           'ANÁLISIS NUMÉRICO',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 4'),
           '22957',
           'ELECTRICIDAD Y ELECTRÓNICA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22961',
           'SISTEMAS DIGITALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 6'),
           '12345',
           'ARQUITECTURA DE COMPUTADORES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 8'),
           '22972',
           'SISTEMAS OPERACIONALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ARQUITECTURA Y FUNCIONAMIENTO DEL COMPUTADOR'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'SISTEMAS DISTRIBUIDOS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 1'),
           '22948',
           'FUNDAMENTOS DE PROGRAMACIÓN',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 2'),
           '22951',
           'PROGRAMACIÓN ORIENTADA A OBJETOS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 3'),
           '22955',
           'ESTRUCTURA DE DATOS Y ANÁLISIS DE ALGORITMOS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 6'),
           '22967',
           'PROGRAMACIÓN EN LA WEB',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 4'),
           '12345',
           'AUTÓMATAS Y LENGUAJES FORMALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '24542',
           'ENTORNOS DE PROGRAMACIÓN',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ALGORÍTMICA E INFORMÁTICA'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'PROGRAMACIÓN DISTRIBUIDA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ADMINISTRACIÓN DE LA INFORMACIÓN'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 4'),
           '12345',
           'BASES DE DATOS I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ADMINISTRACIÓN DE LA INFORMACIÓN'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '12345',
           'BASES DE DATOS II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22963',
           'PENSAMIENTO SISTÉMICO Y ORGANIZACIONAL',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 6'),
           '12345',
           'SISTEMAS DE INFORMACIÓN',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'ADMINISTRATIVAS Y ORGANIZACIONALES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'AUDITORÍA DE SISTEMAS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 6'),
           '22965',
           'REDES DE COMPUTADORES I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 7'),
           '22970',
           'REDES DE COMPUTADORES II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'REDES Y COMUNICACIONES'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'GESTIÓN DE REDES EMPRESARIALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA DEL SOFTWARE'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 7'),
           '22969',
           'INGENIERÍA DEL SOFTWARE I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA DEL SOFTWARE'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 8'),
           '22973',
           'INGENIERÍA DEL SOFTWARE II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 8'),
           '22974',
           'SIMULACIÓN DIGITAL',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'TRATAMIENTO DE SEÑALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'MODELADO ESTRUCTURAL',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'INVESTIGACIÓN OPERACIONAL',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'MODELOS A GRAN ESCALA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'SISTEMAS'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'SISTEMAS DISCRETOS Y CONTINUOS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 7'),
           '12345',
           'INGENIERÍA ARTIFICIAL I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'INGENIERÍA ARTIFICIAL II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'INGENIERÍA ARTIFICIAL III',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'MICROCONTROLADORES I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'MICROCONTROLADORES II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'INGENIERÍA ARTIFICIAL'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 10'),
           '12345',
           'INFORMÁTICA BIOMÉDICA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '41333',
           'ALGORITMOS Y PROGRAMACION',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '27582',
           'ANALISIS DE DATOS A GRAN ESCALA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22958',
           'AUTOMATAS Y LENGUAJES FORMALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '40941',
           'BIOLOGIA CELULAR Y MOLECULAR',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '40937',
           'BIOQUIMICA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '23016',
           'ESTRUCTURAS COMPUTACIONALES',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '21870',
           'GERENCIA DE INFORMATICA I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '28664',
           'INNOVACION EDUCATIVA EN LA SOCIEDAD',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22971',
           'INTELIGENCIA ARTIFICIAL I',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '24552',
           'INTELIGENCIA ARTIFICIAL II',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '41331',
           'INTRODUCCION A LA ING EN CIENCIA DE DATOS',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '40938',
           'INTRODUCCION A LA INGENIERIA BIOMEDICA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '28665',
           'MODELOS DE NEGOCIOS EN LA SOCIEDAD DE LA INFORMACIÓN',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '27798',
           'OPTIMIZACION CONVEXA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '28091',
           'PRINCIPIOS Y PRACTICAS DE DESARROLLO DE SOFTWARE ORIENTADO',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '41017',
           'PROGRAMACION',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22490',
           'SEGURIDAD INFORMATICA',
           NOW(), NOW()
       );

INSERT INTO subject (id_area, id_level_subject, codigo, name, creation_date, update_date)
VALUES (
           (SELECT id FROM area WHERE description = 'DESCONOCIDO'),
           (SELECT id FROM level_subject WHERE description = 'NIVEL 5'),
           '22968',
           'SISTEMAS DE INFORMACION',
           NOW(), NOW()
       );


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
