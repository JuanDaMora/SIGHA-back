# 🔄 Guía de Migración - Setup Actual → Nueva Estrategia

Esta guía te ayudará a migrar de tu configuración actual a la nueva estrategia de deployment modular.

## 📊 Situación Actual vs. Nueva Estrategia

### Situación Actual (Tu Servidor)
```
- docker-compose.multitenancy.yml (en raíz)
- Backends corriendo con docker compose up en raíz
- Variables hardcodeadas o .env en raíz
- Red: sigha-network
```

### Nueva Estrategia
```
- deployment/ con estructura modular
- .env individual para cada componente
- Scripts automatizados (build.sh, deploy.sh)
- Red: sigha-preprod
- Dockerfile multi-stage optimizado
```

## 🚦 Pasos de Migración

### Paso 1: Preparación (Sin Downtime Todavía)

```bash
# En tu servidor, navegar al directorio del backend
cd /home/judamov/sigha/SIGHA-back

# Asegurarte de tener la última versión del código con la nueva estructura
git pull  # O como obtengas el código actualizado

# Verificar que existen los archivos nuevos
ls -la deployment/
ls -la Dockerfile.multistage build.sh
```

### Paso 2: Configurar Variables de Entorno

```bash
cd deployment

# Copiar templates a .env
cp database/env.example database/.env
cp backend-biomedica/env.example backend-biomedica/.env
cp backend-ciencia-datos/env.example backend-ciencia-datos/.env
cp backend-inteligencia-artificial/env.example backend-inteligencia-artificial/.env
cp backend-sistemas/env.example backend-sistemas/.env
```

#### Variables Críticas a Configurar

**database/.env:**
```bash
NAME=sigha-database-preprod
POSTGRES_USER=admin
POSTGRES_PASSWORD=TU_PASSWORD_REAL
POSTGRES_DB=sigha
```

**backend-biomedica/.env:**
```bash
APP_IMAGE=sigha-backend:latest
APP_CONTAINER_NAME=sigha-biomedica-backend-preprod
APP_PORT=8081
APP_INTERNAL_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://sigha-db-preprod:5432/sigha
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=TU_PASSWORD_REAL
SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=ing_biomedica
APP_SECRET_KEY=TU_SECRET_KEY_REAL
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password
SIGHA_URL_ACCESS=http://localhost:5173
```

**Repetir para cada backend**, cambiando:
- `APP_PORT`: 8082, 8083, 8084
- `APP_CONTAINER_NAME`: nombre del contenedor
- `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA`: schema correspondiente
- `SIGHA_URL_ACCESS`: URL del frontend correspondiente

### Paso 3: Construir Nueva Imagen

```bash
cd /home/judamov/sigha/SIGHA-back

# Build con el nuevo Dockerfile multi-stage
./build.sh 1.0.0

# Verificar que se creó
docker images | grep sigha-backend
```

### Paso 4: Backup de Base de Datos (CRÍTICO)

```bash
# Si tienes BD corriendo actualmente, hacer backup
docker exec sigha-database pg_dump -U admin sigha > backup_sigha_$(date +%Y%m%d_%H%M%S).sql

# O si es el otro nombre de contenedor
docker ps | grep postgres
docker exec [NOMBRE_CONTENEDOR] pg_dump -U admin sigha > backup_sigha_$(date +%Y%m%d_%H%M%S).sql
```

### Paso 5: Detener Servicios Actuales

```bash
# Detener backends actuales
docker stop sigha-back-biomedica-1 sigha-back-ciencia-datos-1 \
  sigha-back-inteligencia-artificial-1 sigha-back-sistemas-1 2>/dev/null || true

# O usando docker compose si están con compose
docker compose -f docker-compose.multitenancy.yml down

# Listar contenedores de SIGHA que aún corren
docker ps | grep sigha
```

### Paso 6: Migrar Base de Datos

#### Opción A: Base de Datos Existente

Si quieres reutilizar tu base de datos actual:

```bash
# 1. Detener contenedor actual (si existe)
docker stop sigha-database
docker rename sigha-database sigha-database-old

# 2. Desplegar nueva BD usando el mismo volumen
cd deployment/database
# Editar docker-compose.yml para usar el volumen existente si lo deseas
docker compose up -d

# 3. Conectar a red sigha-preprod
docker network connect sigha-preprod sigha-db-preprod
```

#### Opción B: Base de Datos Nueva (Recomendado)

```bash
cd deployment

# Crear red
docker network create sigha-preprod

# Desplegar BD nueva
./deploy.sh database

# Importar backup
docker exec -i sigha-db-preprod psql -U admin -d sigha < backup_sigha_*.sql

# O importar desde tus archivos SQL originales
docker exec -i sigha-db-preprod psql -U admin -d sigha < /ruta/a/01_core_tablas_base.sql
# ... etc
```

### Paso 7: Desplegar Backends Nuevos

```bash
cd deployment

# Opción 1: Desplegar todos a la vez
./deploy.sh all  # Incluye BD + 4 backends

# Opción 2: Desplegar uno por uno (más controlado)
./deploy.sh biomedica
./deploy.sh ciencia-datos
./deploy.sh ia
./deploy.sh sistemas

# Ver estado
./deploy.sh status
```

### Paso 8: Verificación

```bash
# Ver logs de cada backend
docker logs -f sigha-biomedica-backend-preprod
docker logs -f sigha-ciencia-datos-backend-preprod
docker logs -f sigha-ia-backend-preprod
docker logs -f sigha-sistemas-backend-preprod

# Probar endpoints
curl http://localhost:8081/public/auth/login \
  -H "Content-Type: application/json" \
  -d '{"documento":"1002458178","password":"2345"}'

# Verificar health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Paso 9: Actualizar Frontends

Tus frontends necesitan apuntar a los nuevos puertos:

```bash
# En cada docker-compose.yml de frontend
VITE_API_URL=http://localhost:8081  # Para Biomédica
VITE_API_URL=http://localhost:8082  # Para Ciencia de Datos
VITE_API_URL=http://localhost:8083  # Para IA
VITE_API_URL=http://localhost:8084  # Para Sistemas

# Reiniciar frontends
docker compose restart
```

### Paso 10: Limpieza (Opcional)

Una vez verificado que todo funciona:

```bash
# Eliminar contenedores viejos
docker rm sigha-database-old
docker rm sigha-back-biomedica-1 sigha-back-ciencia-datos-1 \
  sigha-back-inteligencia-artificial-1 sigha-back-sistemas-1

# Eliminar imágenes viejas
docker images | grep sigha
docker rmi [IMAGEN_VIEJA]

# Eliminar red vieja (si no la usas)
docker network rm sigha-network
```

## 🔧 Resolución de Problemas

### Backend no se conecta a BD

```bash
# Verificar que la red existe y está conectada
docker network inspect sigha-preprod

# Asegurarse de que la BD está en la red
docker network connect sigha-preprod sigha-db-preprod

# Verificar logs
docker logs sigha-db-preprod
```

### Error "Image not found"

```bash
# Reconstruir imagen
cd /home/judamov/sigha/SIGHA-back
./build.sh 1.0.0

# Verificar
docker images | grep sigha-backend
```

### Backend devuelve roles incorrectos

```bash
# Verificar variable SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA
# En el .env del backend correspondiente

cd deployment/backend-sistemas
cat .env | grep DEFAULT_SCHEMA

# Debe ser:
# SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA=ing_sistemas
```

### Puerto ya en uso

```bash
# Ver qué proceso usa el puerto
sudo lsof -i :8081

# Matar proceso o cambiar puerto en .env
nano backend-biomedica/.env
# Cambiar APP_PORT=8081 a otro puerto
```

## 📋 Checklist de Migración

- [ ] Backup de base de datos realizado
- [ ] Código actualizado con nueva estructura
- [ ] Archivos .env configurados para todos los componentes
- [ ] Imagen Docker construida (./build.sh)
- [ ] Red sigha-preprod creada
- [ ] Servicios actuales detenidos
- [ ] Base de datos migrada y funcionando
- [ ] 4 backends desplegados
- [ ] Endpoints verificados con curl
- [ ] Frontends apuntando a nuevos puertos
- [ ] Roles filtrados correctamente por programa
- [ ] Limpieza de recursos antiguos

## 🎯 Diferencias Clave a Recordar

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| Docker Compose | Uno en raíz | Uno por componente en deployment/ |
| Variables | .env en raíz | .env individual por carpeta |
| Red | sigha-network | sigha-preprod |
| Build | Manual o sin script | ./build.sh automatizado |
| Deploy | docker compose up | ./deploy.sh [componente] |
| Logs | docker compose logs | docker logs [nombre-contenedor] |

## 💡 Ventajas Después de Migrar

1. ✅ **Despliegue independiente**: Actualiza solo el backend que cambió
2. ✅ **Mejor seguridad**: .env separados, no todos los secrets en un lugar
3. ✅ **Más rápido**: Build cacheable, solo rebuilds lo necesario
4. ✅ **Más fácil**: `./deploy.sh biomedica` en vez de comandos largos
5. ✅ **Mejor organización**: Estructura clara y documentada

## 📞 ¿Necesitas Ayuda?

Si encuentras problemas durante la migración:

1. Revisa los logs: `docker logs [nombre-contenedor]`
2. Verifica las variables: `cat deployment/backend-*/. env | grep SCHEMA`
3. Consulta la documentación: `deployment/README.md`
4. Revisa la estrategia completa: `ESTRATEGIA_DEPLOYMENT.md`

---

**¡Buena suerte con la migración!** 🚀

