# 🚀 SIGHA - Deployment Multi-Tenant

Esta carpeta contiene la configuración de despliegue para el sistema SIGHA con arquitectura multi-tenant.

## 📁 Estructura

```
deployment/
├── database/               # Base de datos PostgreSQL unificada
│   ├── docker-compose.yml
│   └── env.example
├── backend-biomedica/      # Backend Ing. Biomédica (Puerto 8081)
│   ├── docker-compose.yml
│   └── env.example
├── backend-ciencia-datos/  # Backend Ing. Ciencia de Datos (Puerto 8082)
│   ├── docker-compose.yml
│   └── env.example
├── backend-inteligencia-artificial/  # Backend Ing. IA (Puerto 8083)
│   ├── docker-compose.yml
│   └── env.example
├── backend-sistemas/       # Backend Ing. Sistemas (Puerto 8084)
│   ├── docker-compose.yml
│   └── env.example
├── deploy.sh               # Script automático de despliegue
└── README.md               # Este archivo
```

## 🎯 Arquitectura

- **1 Base de Datos Unificada**: PostgreSQL con esquema `core` + 4 esquemas de programa
- **4 Backends Independientes**: Cada uno apunta a su esquema específico
- **Network Compartida**: `sigha-preprod` conecta todos los servicios

```
┌─────────────────────────────────────────────────────────┐
│                    sigha-preprod (network)               │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐                                        │
│  │  PostgreSQL  │  (Puerto 5433:5432)                   │
│  │  sigha DB    │                                        │
│  └──────┬───────┘                                        │
│         │                                                 │
│    ┌────┴────┬────────┬─────────┐                       │
│    │         │        │         │                        │
│  ┌─▼──┐   ┌─▼──┐  ┌─▼──┐   ┌─▼──┐                     │
│  │8081│   │8082│  │8083│   │8084│                      │
│  │Bio │   │CD  │  │ IA │   │Sis │                      │
│  └────┘   └────┘  └────┘   └────┘                      │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

## 🛠️ Requisitos Previos

1. **Docker y Docker Compose** instalados
2. **Imagen del backend** construida:
   ```bash
   cd /home/judamov/sigha/SIGHA-back
   ./build.sh
   ```

## 📋 Pasos de Despliegue

### Opción 1: Despliegue Automático (Recomendado)

```bash
cd /home/judamov/sigha/SIGHA-back/deployment

# 1. Configurar variables de entorno para cada componente
cp database/env.example database/.env
cp backend-biomedica/env.example backend-biomedica/.env
cp backend-ciencia-datos/env.example backend-ciencia-datos/.env
cp backend-inteligencia-artificial/env.example backend-inteligencia-artificial/.env
cp backend-sistemas/env.example backend-sistemas/.env

# 2. Editar cada .env con tus valores reales
nano database/.env
nano backend-biomedica/.env
# ... etc

# 3. Dar permisos de ejecución
chmod +x deploy.sh

# 4. Desplegar todo
./deploy.sh all

# O desplegar componentes específicos:
./deploy.sh database
./deploy.sh biomedica
./deploy.sh ciencia-datos
./deploy.sh ia
./deploy.sh sistemas

# Ver estado
./deploy.sh status
```

### Opción 2: Despliegue Manual

#### 1. Crear la red

```bash
docker network create sigha-preprod
```

#### 2. Desplegar Base de Datos

```bash
cd database
cp env.example .env
nano .env  # Editar credenciales
docker compose up -d
```

Importar datos:

```bash
docker exec -i sigha-db-preprod psql -U admin -d sigha < /ruta/a/datos.sql
```

#### 3. Desplegar Backends

Para cada backend:

```bash
cd backend-biomedica
cp env.example .env
nano .env  # Configurar SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=ing_biomedica
docker compose up -d

cd ../backend-ciencia-datos
cp env.example .env
nano .env  # Configurar SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=ing_ciencia_de_datos
docker compose up -d

# ... etc
```

## 🔍 Verificación

### Ver logs de un servicio

```bash
cd backend-sistemas
docker compose logs -f
```

### Probar endpoints

```bash
# Login en cada backend
curl -X POST http://localhost:8081/public/auth/login \
  -H "Content-Type: application/json" \
  -d '{"documento":"1002458178","password":"2345"}'

# Verificar roles filtrados por programa
TOKEN="tu_token_aqui"
curl -X GET http://localhost:8084/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "userId: 1"
```

### Ver estado de todos los contenedores

```bash
docker ps --filter "name=sigha"
```

## 🔧 Comandos Útiles

### Reiniciar un backend

```bash
cd backend-sistemas
docker compose restart
```

### Reconstruir y reiniciar (después de cambios en código)

```bash
# 1. Rebuild de la imagen
cd /home/judamov/sigha/SIGHA-back
./build.sh

# 2. Recrear contenedor
cd deployment/backend-sistemas
docker compose down
docker compose up -d --force-recreate
```

### Ver logs en tiempo real

```bash
docker compose -f backend-sistemas/docker-compose.yml logs -f
```

### Detener todo

```bash
docker stop sigha-db-preprod \
  sigha-biomedica-backend-preprod \
  sigha-ciencia-datos-backend-preprod \
  sigha-ia-backend-preprod \
  sigha-sistemas-backend-preprod
```

## 🔐 Seguridad

⚠️ **IMPORTANTE**: Los archivos `.env` contienen información sensible y **NO** deben subirse a Git.

Asegúrate de que `.gitignore` incluye:

```
.env
.env.*
!.env.example
```

## 📝 Variables de Entorno Clave

### Base de Datos (`database/.env`)

- `POSTGRES_USER`: Usuario de PostgreSQL
- `POSTGRES_PASSWORD`: Contraseña de PostgreSQL
- `POSTGRES_DB`: Nombre de la base de datos

### Backends (`backend-*//.env`)

- `APP_IMAGE`: Imagen Docker a usar
- `APP_PORT`: Puerto externo (8081-8084)
- `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA`: **CRUCIAL** - Define a qué esquema apunta cada backend
- `APP_SECRET_KEY`: Llave secreta para JWT
- `MAIL_*`: Configuración SMTP para emails

## 🐛 Troubleshooting

### Backend no se conecta a la base de datos

```bash
# Verificar que la red existe
docker network inspect sigha-preprod

# Conectar manualmente si es necesario
docker network connect sigha-preprod sigha-db-preprod
```

### Backend devuelve todos los roles en lugar de filtrados

Verifica que `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA` esté correctamente configurado en el `.env` del backend.

### Puerto ya en uso

```bash
# Ver qué proceso usa el puerto
sudo lsof -i :8081

# Cambiar APP_PORT en el .env del backend
```

## 📚 Documentación Adicional

- [Dockerfile Multi-Stage](../Dockerfile.multistage)
- [Script de Build](../build.sh)
- [Implementación Multi-Tenancy](../IMPLEMENTACION_MULTITENANCY.md)

## 🎉 ¡Listo!

Una vez desplegado, tus frontends pueden conectarse a:

- Biomédica: `http://localhost:8081`
- Ciencia de Datos: `http://localhost:8082`
- Inteligencia Artificial: `http://localhost:8083`
- Sistemas: `http://localhost:8084`

Cada backend filtrará automáticamente los datos según su esquema configurado.

