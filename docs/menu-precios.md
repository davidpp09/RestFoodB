# Menú y precios

La carta toma los importes de `productos`. Cada grupo representa un precio impreso y el campo que modifica (`DESAYUNO` o `COMIDA`); puede reunir varios platillos. Cambiar un grupo actualiza todos sus miembros y registra la auditoría en una sola transacción. Los productos se bloquean por id para coordinar cambios concurrentes.

## API y permisos

Las rutas requieren autenticación, salvo la descarga mediante su token temporal.

| Método y ruta | Roles | Uso |
| --- | --- | --- |
| `GET /grupos-precio` | ADMIN, DEV | Grupos, productos, precio común, indicador `mixto` y nombres `faltantes`. |
| `PATCH /grupos-precio/{clave}` | ADMIN, DEV | Cuerpo `{"precio":115.50}`; aplica el precio al campo del grupo. |
| `GET /productos/{id}/historial-precios` | ADMIN, DEV | Cambios de precio, autor, fecha y origen. |
| `GET /menu-dia/mapa-precios` | ADMIN, DEV | Coordenadas y correspondencias del editor. |
| `GET /menu-dia/editor/imagen?pagina=1` | ADMIN, DEV | PNG de la carta editable; admite páginas 1 y 2. |
| `GET /menu-dia/pdf` | ADMIN, DEV, REPARTIDOR | PDF completo listo para imprimir. |
| `GET /menu-dia/imagen?pagina=2` | ADMIN, DEV, REPARTIDOR | Vista PNG del menú para compartir. |
| `POST /menu-dia/enlace-descarga` | ADMIN, DEV, REPARTIDOR | Emite una URL de descarga con vigencia de tres minutos. |
| `GET /menu-dia/descargar?t=…` | Token temporal válido | Descarga el PDF sin cabecera JWT. |

Las altas, modificaciones y bajas generales de `/productos` corresponden a ADMIN y DEV. Las rutas del día (`POST /productos/dia`, `PATCH /productos/{id}/dia`, `DELETE /productos/dia/{id}` y `PUT /productos/desactivar-dia/{categoriaId}`) también permiten REPARTIDOR. El servidor comprueba que este último opere únicamente sobre Comida del día; no basta con ocultar controles en el frontend.

Un grupo incompleto no se puede actualizar. Los importes se validan con un máximo de dos decimales antes de persistir; no se depende del redondeo de MySQL. Los errores de validación se entregan mediante `mensaje`.

## Mapa y plantilla

Los recursos se encuentran en `api/src/main/resources/menu/`:

- `plantilla-menu.pdf`: diseño de las dos páginas.
- `mapa-precios.json`: 93 entradas, 90 grupos distintos y 186 posiciones de precio. Las posiciones repetidas del mismo precio comparten clave y se actualizan juntas.

SHA-256 de la plantilla correspondiente a este mapa:

```text
3462d5e4fc68b6daca775ef3b8c170908e3e67400c5dd6b81b49e057d870c270
```

`MapaPrecios` comprueba el SHA al arrancar. Sustituir el PDF exige recalibrar el mapa y actualizar su hash; no se debe actualizar únicamente el hash para omitir esa comprobación. `precioPlantilla` conserva el importe de referencia del diseño para el diagnóstico, pero el precio generado proviene de la base.

La impresión se bloquea si algún grupo está vacío, tiene miembros esperados faltantes, contiene precios diferentes (`mixto`), carece de precio o su importe no cabe de forma legible. La vista del editor permite localizar los problemas mediante `VARIOS`, `SIN PRECIO` o `!`. El menú final requiere además platillos activos del día.

## Migraciones y vínculos

Las migraciones aplicadas son inmutables. Toda corrección posterior debe usar una nueva versión de Flyway, sin editar V10, V11 ni V12:

- V10 crea grupos, asociaciones e historial de precios.
- V11 siembra los grupos y enlaza productos activos por nombre exacto, mediante comparación binaria. No depende de ids de otra base ni modifica precios del catálogo.
- V12 permite `precio_nuevo = NULL` en el historial para representar la retirada de un importe.

Los nombres ausentes no se inventan durante la migración. Al crear, restaurar o actualizar un producto, `VinculosMenuService` sincroniza sus asociaciones con el nombre actual: añade enlaces faltantes y retira enlaces a otros grupos del mapa que ya no correspondan. Las asociaciones de grupos ajenos al mapa se conservan. La operación es idempotente y participa en la transacción del producto.

Por ejemplo, si se renombra un platillo A como B y después se crea A de nuevo, el grupo de A debe modificar el producto nuevo, sin arrastrar al producto renombrado. Un nombre esperado ausente o archivado aparece en `faltantes`; se corrige el catálogo antes de imprimir.

## Auditoría y órdenes

El historial guarda campo, valor anterior, valor nuevo, usuario, fecha UTC y origen (`CARTA`, `DIA` o `MENU`). Solo registra cambios reales: `105` y `105.00` son el mismo importe. Un precio ausente se representa con `NULL`, no con cero; asignarlo o retirarlo también es un cambio auditable. Si falla la auditoría, se revierte la modificación de precios.

Las órdenes conservan el precio guardado en `OrdenDetalle`. Cambiar la carta afecta las nuevas capturas y no recalcula ventas ya registradas.

## Diagnóstico y recalibración

Ejecutar desde `api/`, con la configuración de una base de pruebas o copia aislada. El contexto de Spring puede aplicar migraciones al arrancar; elegir la base antes de ejecutar. En Windows se puede sustituir `./mvnw` por `.\mvnw.cmd`.

```sh
./mvnw test -Dtest=MapaPreciosBaseTest -DverificarMapa=true
```

Este diagnóstico está deshabilitado salvo que se indique la propiedad. Comprueba que los nombres del mapa estén asociados y reporta desfases entre los precios de la base y los precios originales de la plantilla. Los desfases se imprimen para revisión; por sí solos no hacen fallar la prueba, porque la base es la fuente de precios actuales. No sustituye la validación de `faltantes` de la API ni una revisión visual del PDF.

Para extraer las posiciones de los signos de precio de la plantilla:

```sh
./mvnw test -Dtest=CalibradorPlantillaMenu#coordenadasDePreciosDeTodaLaCarta -Dcalibrar=true -DfailIfNoTests=false
```

La herramienta escribe `target/menu-precios/calibracion.csv`. Sus coordenadas sirven como insumo para ajustar las cajas de `mapa-precios.json`; no reconstruye automáticamente el mapa. Después se verifica el SHA, la relación entre nombres y grupos, las dos páginas renderizadas, los importes largos y la ausencia de precios antiguos debajo de los nuevos.

## Despliegue

Desplegar primero el backend y dejar que Flyway aplique las migraciones; verificar arranque, catálogo, permisos y generación del menú antes de publicar el frontend que consume las rutas nuevas. La base de destino debe tener los platillos esperados o completarse mediante las operaciones del catálogo antes de imprimir.

Revisar que el proxy reenvíe `/grupos-precio` al backend, además de `/productos` y `/menu-dia`, y que conserve la cabecera `Authorization`. Una respuesta HTML del frontend en `/grupos-precio` indica una ruta de proxy incorrecta, aunque el backend esté funcionando.

Los comandos anteriores describen procedimientos de verificación; este documento no certifica ejecuciones ni resultados de CI.
