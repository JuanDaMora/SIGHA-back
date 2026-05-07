# Checklist de Pull Request – Testing

Antes de hacer merge de cualquier PR que modifique la capa de servicios, verifica:

## Obligatorio

- [ ] Cada método público nuevo o modificado en un `*ServiceImpl` tiene al menos un test unitario.
- [ ] Cada bugfix incluye un test de regresión que reproduce el escenario del error.
- [ ] Los tests siguen el patrón de nomenclatura `shouldX_whenY`.
- [ ] Los tests siguen el patrón AAA (Arrange – Act – Assert).
- [ ] Los tests son independientes entre sí (no dependen de orden de ejecución).
- [ ] Se verifica el comportamiento observable (salida, excepciones lanzadas, llamadas a `save`/`delete`), no detalles internos.

## Recomendado

- [ ] La cobertura de la clase modificada no baja del umbral actual del módulo.
- [ ] Los escenarios cubiertos incluyen: flujo exitoso, error de negocio (NOT_FOUND, FORBIDDEN, CONFLICT) y validación de entrada.
- [ ] No se añaden comentarios que solo narran el código.

## Convenciones de nombres

| Patrón | Ejemplo |
|--------|---------|
| Flujo exitoso | `shouldCreateGroupSuccessfully` |
| Error por permisos | `shouldThrowForbiddenWhenNotAdmin` |
| Error por dato faltante | `shouldThrowNotFoundWhenUserDoesNotExist` |
| Error de negocio/conflicto | `shouldThrowConflictWhenDescriptionAlreadyExists` |
| Comportamiento condicional | `shouldNotSaveWhenAvailabilityIsAlreadySame` |
