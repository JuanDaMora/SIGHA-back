#!/bin/bash
# ============================================
# Script de Build para SIGHA Backend
# ============================================
# Uso: ./build.sh [VERSION]
# Ejemplo: ./build.sh 1.0.0

set -e

VERSION=${1:-latest}
IMAGE_NAME="sigha-backend"
REGISTRY="docker.io/judamov"  # Cambiar según tu registry

echo "🏗️  Building SIGHA Backend v${VERSION}..."

# Limpiar builds anteriores
echo "📦 Limpiando builds anteriores..."
mvn clean

# Build con Maven
echo "⚙️  Compilando con Maven..."
mvn package -DskipTests

# Build de imagen Docker usando Dockerfile multi-stage
echo "🐳 Construyendo imagen Docker..."
docker build -f Dockerfile.multistage -t ${IMAGE_NAME}:${VERSION} .

# Tag adicionales
docker tag ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:latest

echo "✅ Build completado!"
echo "📦 Imagen: ${IMAGE_NAME}:${VERSION}"
echo ""
echo "Para hacer push al registry:"
echo "  docker tag ${IMAGE_NAME}:${VERSION} ${REGISTRY}/${IMAGE_NAME}:${VERSION}"
echo "  docker push ${REGISTRY}/${IMAGE_NAME}:${VERSION}"

