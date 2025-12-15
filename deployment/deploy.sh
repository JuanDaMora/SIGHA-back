#!/bin/bash
# ============================================
# Script de Despliegue para SIGHA Backend Multi-Tenant
# ============================================
# Uso: ./deploy.sh [componente]
# Componentes: database | biomedica | ciencia-datos | ia | sistemas | all

set -e

DEPLOYMENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_NC='\033[0m' # No Color

print_header() {
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_NC}"
    echo -e "${COLOR_BLUE}  $1${COLOR_NC}"
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_NC}"
}

print_success() {
    echo -e "${COLOR_GREEN}✓ $1${COLOR_NC}"
}

print_error() {
    echo -e "${COLOR_RED}✗ $1${COLOR_NC}"
}

print_warning() {
    echo -e "${COLOR_YELLOW}⚠ $1${COLOR_NC}"
}

check_network() {
    if ! docker network inspect sigha-preprod &>/dev/null; then
        print_warning "Red 'sigha-preprod' no existe. Creándola..."
        docker network create sigha-preprod
        print_success "Red 'sigha-preprod' creada"
    else
        print_success "Red 'sigha-preprod' existe"
    fi
}

check_env_file() {
    local dir=$1
    if [ ! -f "$dir/.env" ]; then
        print_error "Archivo .env no encontrado en $dir"
        print_warning "Copia env.example a .env y configura tus variables:"
        echo "  cp $dir/env.example $dir/.env"
        echo "  nano $dir/.env"
        return 1
    fi
    return 0
}

deploy_component() {
    local component=$1
    local dir="${DEPLOYMENT_DIR}/${component}"
    
    print_header "Desplegando: $component"
    
    if [ ! -d "$dir" ]; then
        print_error "Directorio $dir no existe"
        return 1
    fi
    
    if ! check_env_file "$dir"; then
        return 1
    fi
    
    cd "$dir"
    
    # Detener si está corriendo
    docker compose down 2>/dev/null || true
    
    # Levantar con el nuevo .env
    docker compose up -d
    
    print_success "$component desplegado"
    
    # Mostrar logs
    echo ""
    echo "Ver logs con: docker compose -f $dir/docker-compose.yml logs -f"
}

deploy_database() {
    print_header "Desplegando Base de Datos"
    
    local dir="${DEPLOYMENT_DIR}/database"
    
    if ! check_env_file "$dir"; then
        return 1
    fi
    
    cd "$dir"
    
    # Verificar si ya existe
    if docker ps -a | grep -q "sigha-db-preprod"; then
        print_warning "Contenedor de base de datos ya existe"
        read -p "¿Deseas recrearlo? Esto ELIMINARÁ todos los datos (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose down -v
            print_warning "Base de datos eliminada"
        else
            print_warning "Saltando despliegue de base de datos"
            return 0
        fi
    fi
    
    docker compose up -d
    
    print_success "Base de datos desplegada"
    
    # Esperar a que esté lista
    echo "Esperando a que PostgreSQL esté listo..."
    sleep 5
    
    print_warning "Recuerda importar los datos:"
    echo "  docker exec -i sigha-db-preprod psql -U admin -d sigha < tu_archivo.sql"
}

show_status() {
    print_header "Estado de Servicios SIGHA"
    
    echo ""
    echo "Base de Datos:"
    docker ps --filter "name=sigha-db-preprod" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo ""
    echo "Backends:"
    docker ps --filter "name=sigha.*backend-preprod" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Main
COMPONENT=${1:-all}

case $COMPONENT in
    database)
        check_network
        deploy_database
        ;;
    biomedica)
        check_network
        deploy_component "backend-biomedica"
        ;;
    ciencia-datos)
        check_network
        deploy_component "backend-ciencia-datos"
        ;;
    ia)
        check_network
        deploy_component "backend-inteligencia-artificial"
        ;;
    sistemas)
        check_network
        deploy_component "backend-sistemas"
        ;;
    all)
        print_header "Despliegue Completo de SIGHA"
        check_network
        deploy_database
        echo ""
        deploy_component "backend-biomedica"
        echo ""
        deploy_component "backend-ciencia-datos"
        echo ""
        deploy_component "backend-inteligencia-artificial"
        echo ""
        deploy_component "backend-sistemas"
        echo ""
        show_status
        ;;
    status)
        show_status
        ;;
    *)
        print_error "Componente desconocido: $COMPONENT"
        echo "Uso: $0 [database|biomedica|ciencia-datos|ia|sistemas|all|status]"
        exit 1
        ;;
esac

print_success "¡Listo!"

