# RestFood API — Backend

Sistema de gestión para restaurante construido con **Spring Boot 3.5.10** y **Java 17**. Maneja autenticación JWT, órdenes en tiempo real via WebSocket, impresión de tickets y reportes de corte de caja.

---

## Stack tecnológico

| Tecnología | Versión | Uso |
|---|---|---|
| Spring Boot | 3.5.10 | Framework principal |
| Java | 17 | Lenguaje |
| Maven | — | Build tool |
| MySQL | 5.7+ | Base de datos |
| Spring Data JPA / Hibernate | — | ORM |
| Spring Security | — | Autenticación y autorización |
| Auth0 java-jwt | 4.4.0 | Generación y validación de JWT |
| Spring WebSocket (STOMP) | — | Tiempo real (cocina, mesas, tickets) |
| Flyway | — | Migraciones de base de datos |
| SpringDoc OpenAPI | — | Documentación Swagger |
| Jakarta Bean Validation | — | Validación de inputs |

---

## Requisitos

- Java 17
- MySQL 5.7 o superior
- Maven 3.8+
- Variable de entorno `JWT_SECRET` configurada

---

## Configuración

### Variables de entorno

```bash
JWT_SECRET=tu_clave_secreta_jwt
```

### Base de datos

Crea la base de datos manualmente:

```sql
CREATE DATABASE restaurante;
```

Flyway aplica las migraciones automáticamente al iniciar la app (baseline en versión 0).

### `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/restaurante
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

---

## Levantar el proyecto

```bash
cd RestFoodB/api
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Arquitectura

```
src/main/java/restaurante/api/
├── controller/          # Controladores REST y WS
├── domain/
│   ├── entity/          # Entidades JPA
│   ├── dto/             # DTOs de entrada y salida
│   ├── repository/      # Interfaces JPA
│   └── service/         # Lógica de negocio
├── infra/
│   ├── security/        # JWT, filtros, configuración Spring Security
│   ├── websocket/       # Configuración STOMP
│   ├── errores/         # Manejador global de excepciones
│   └── impresora/       # Servicio de impresión (USB / TCP-IP)
```

---

## Entidades de base de datos

### `usuario`
| Campo | Tipo | Descripción |
|---|---|---|
| id_usuarios | Long (PK) | ID autoincremental |
| nombre | String UNIQUE | Nombre de usuario |
| email | String UNIQUE | Correo (usado para login) |
| contrasena | String | Hash BCrypt |
| rol | Enum | ADMIN, DEV, MESERO, COCINA, CAJERO, REPARTIDOR |
| estatus | Boolean | Activo / inactivo (soft delete) |
| seccion | Integer | Sección de mesas asignada al mesero |

### `mesas`
| Campo | Tipo | Descripción |
|---|---|---|
| id_mesas | Long (PK) | — |
| numero | String UNIQUE | Número de mesa |
| estado | Enum | LIBRE / OCUPADA |

### `ordenes`
| Campo | Tipo | Descripción |
|---|---|---|
| id_ordenes | Long (PK) | — |
| numero_comanda | Integer | Número secuencial de comanda |
| tipo | Enum | LOZA (mesa) / LLEVAR (para llevar) |
| servicio | Enum | DESAYUNO / COMIDA |
| estatus | Enum | PREPARANDO / SERVIDO / PAGADA |
| total | BigDecimal | Total calculado automáticamente |
| fecha_apertura | LocalDateTime | Cuándo se abrió la orden |
| fecha_cierre | LocalDateTime | Cuándo se cerró (nullable) |
| id_usuario (FK) | Long | Mesero/repartidor que la abrió |
| id_mesa (FK) | Long | Mesa asociada (nullable para LLEVAR) |

### `orden_detalles`
| Campo | Tipo | Descripción |
|---|---|---|
| id_detalle | Long (PK) | — |
| cantidad | Integer | Cantidad del platillo |
| precio_unitario | BigDecimal | Precio al momento del pedido |
| subtotal | BigDecimal | cantidad × precio_unitario |
| comentarios | String | Instrucciones especiales (nullable) |
| id_orden (FK) | Long | Orden a la que pertenece |
| id_producto (FK) | Long | Producto ordenado |

### `productos`
| Campo | Tipo | Descripción |
|---|---|---|
| id_productos | Long (PK) | — |
| nombre | String UNIQUE | Nombre del platillo |
| precio_comida | BigDecimal | Precio turno comida |
| precio_desayuno | BigDecimal | Precio turno desayuno |
| disponibilidad | Boolean | Si está disponible hoy |
| id_categoria (FK) | Long | Categoría del platillo |

Restricción: máximo 7 productos activos por categoría.

### `categorias`
| Campo | Tipo | Descripción |
|---|---|---|
| id_categorias | Long (PK) | — |
| nombre | String UNIQUE | Nombre de la categoría |
| impresora | String | Nombre de la impresora de cocina asignada |

### `evento_orden` (Auditoría)
Registra cada cambio en las órdenes para reportes y trazabilidad.

| Campo | Tipo | Descripción |
|---|---|---|
| id_evento | Long (PK) | — |
| tipo_evento | Enum | MESA_ABIERTA, MESA_CERRADA, PLATILLO_NUEVO, PLATILLO_MODIFICADO, PLATILLO_CANCELADO |
| timestamp | LocalDateTime | Cuándo ocurrió |
| id_orden (FK) | Long | Orden afectada |
| id_mesa | Long | Mesa (desnormalizado) |
| id_usuario (FK) | Long | Usuario que realizó la acción |
| nombre_mesero | String | Desnormalizado para reportes |
| nombre_producto | String | Platillo afectado (nullable) |
| cantidad_anterior | Integer | Valor antes del cambio |
| cantidad_nueva | Integer | Valor después del cambio |
| comentarios_anterior | String | Comentario previo |
| comentarios_nuevo | String | Comentario nuevo |

---

## Endpoints

### Autenticación — `/login`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/login` | Pública | Login con email/contraseña, retorna JWT |

**Body:**
```json
{ "email": "mesero@rest.com", "contrasena": "1234" }
```
**Response:**
```json
{ "jwTtoken": "eyJ..." }
```

---

### Órdenes — `/ordenes`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/ordenes` | ADMIN, DEV, MESERO, REPARTIDOR | Abrir nueva orden |
| GET | `/ordenes` | ADMIN, DEV, CAJERO, MESERO | Listar órdenes (paginado) |
| GET | `/ordenes/activa/{id_mesa}` | ADMIN, DEV, CAJERO, MESERO | Obtener orden activa de una mesa |
| GET | `/ordenes/entregas/hoy` | ADMIN, DEV, REPARTIDOR | Órdenes de entrega del día |
| PUT | `/ordenes/{id}/cerrar` | ADMIN, DEV, CAJERO, MESERO, REPARTIDOR | Cerrar orden e imprimir ticket |
| POST | `/ordenes/{id}/reimprimir-ticket` | ADMIN, DEV, CAJERO, MESERO | Reimprimir ticket final |
| POST | `/ordenes/{id}/reenviar-cocina` | ADMIN, DEV, MESERO | Reenviar comanda a cocina |

---

### Detalle de Órdenes — `/ordendetalles`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/ordendetalles` | ADMIN, DEV, MESERO, REPARTIDOR | Sincronizar platillos de una orden |

Este endpoint es el núcleo del sistema. Recibe la comanda completa y calcula diferencias: crea nuevos detalles, actualiza modificados y elimina los removidos. Dispara impresión de ticket de cocina y broadcast WebSocket.

**Body:**
```json
{
  "id_orden": 1,
  "id_usuario": 3,
  "platillos": [
    { "id_detalle": null, "id_producto": 5, "cantidad": 2, "comentarios": "sin cebolla" },
    { "id_detalle": 14,   "id_producto": 8, "cantidad": 1, "comentarios": null }
  ]
}
```

---

### Mesas — `/mesas`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/mesas` | ADMIN, DEV, MESERO | Crear nueva mesa |
| GET | `/mesas` | ADMIN, DEV, MESERO | Listar todas con su orden activa |
| GET | `/mesas/rango/{inicio}/{fin}` | ADMIN, DEV, MESERO | Mesas en rango (para secciones de meseros) |

---

### Cocina — `/cocina`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| GET | `/cocina` | ADMIN, DEV, COCINA | Listar órdenes pendientes |
| PATCH | `/cocina/{id}/servido` | ADMIN, DEV, COCINA | Marcar orden como servida |

---

### Productos — `/productos`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/productos` | DEV | Crear producto |
| GET | `/productos` | ADMIN, DEV, MESERO, REPARTIDOR | Listar todos con categoría |
| PUT | `/productos/{id}` | DEV | Actualizar producto |
| DELETE | `/productos/{id}` | DEV | Eliminar producto |
| PATCH | `/productos/{id}/dia` | DEV, REPARTIDOR | Actualizar disponibilidad/precio del día |
| PUT | `/productos/desactivar-dia/{categoriaId}` | DEV, REPARTIDOR | Desactivar todos los de una categoría |

---

### Categorías — `/categorias`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| GET | `/categorias` | ADMIN, DEV, REPARTIDOR | Listar categorías |
| POST | `/categorias` | ADMIN, DEV | Crear categoría |

---

### Usuarios — `/usuarios`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| POST | `/usuarios` | ADMIN, DEV | Crear usuario |
| GET | `/usuarios` | ADMIN, DEV, CAJERO | Listar usuarios (paginado) |
| PUT | `/usuarios` | ADMIN, DEV, CAJERO | Actualizar usuario |
| DELETE | `/usuarios/{id}` | ADMIN, DEV, CAJERO | Baja lógica (soft delete) |
| PUT | `/usuarios/activar/{id}` | ADMIN, DEV, CAJERO | Reactivar usuario |
| DELETE | `/usuarios/eliminar/{id}` | ADMIN, DEV, CAJERO | Eliminar físico |

---

### Admin / Reportes — `/admin`

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| GET | `/admin?fecha=YYYY-MM-DD` | ADMIN, DEV | Corte de caja del día |
| GET | `/admin/cancelaciones?desde=&hasta=` | ADMIN, DEV | Cancelaciones por mesero en rango de fechas |

**Response corte (`DatosCorteDia`):**
```json
{
  "totalGeneral": 4500.00,
  "totalDesayuno": 1200.00,
  "totalComida": 3300.00,
  "totalPlatillosLoza": 87,
  "totalPlatillosParaLlevar": 34,
  "ventasPorEmpleado": [...]
}
```

---

## WebSocket (STOMP)

**Endpoint de conexión:** `ws://localhost:8080/ws-restfood`  
**Fallback SockJS:** `http://localhost:8080/ws-restfood`  
**Prefijo app:** `/app`  
**Broker topics:** `/topic`

### Topics disponibles

| Topic | Cuándo se emite | Quién escucha |
|---|---|---|
| `/topic/mesas` | Al abrir o cerrar una orden de mesa | Panel admin, panel mesero |
| `/topic/cocina` | Al sincronizar platillos (nuevo/modificado/cancelado) | Panel cocina |
| `/topic/tickets` | Al cerrar una orden | Panel admin (impresión) |

**Payload `/topic/cocina`** (ejemplo ticket de cocina):
```json
{
  "numero_comanda": 42,
  "tipo": "LOZA",
  "mesa": "5",
  "platillos": [
    { "nombre": "Enchiladas", "cantidad": 2, "estado": "NUEVO", "comentarios": "" },
    { "nombre": "Agua fresca", "cantidad": 1, "estado": "CANCELADO", "comentarios": "" }
  ]
}
```

---

## Seguridad

- **Autenticación:** JWT con `Authorization: Bearer <token>` en cada request
- **Contraseñas:** BCrypt
- **Sesión:** STATELESS (sin cookies ni sesiones del servidor)
- **CORS:** Wildcard `*` (ajustar para producción)
- **CSRF:** Deshabilitado (API stateless)
- **Rutas públicas:** POST `/login`, docs Swagger, endpoint WS

### Roles y permisos

| Rol | Acceso principal |
|---|---|
| ADMIN | Todo excepto gestión de productos (DEV) |
| DEV | Todo, incluyendo CRUD de productos y categorías |
| MESERO | Mesas, órdenes, platillos de su sección |
| COCINA | Ver pedidos pendientes, marcar como servido |
| CAJERO | Cerrar órdenes, ver usuarios, reportes |
| REPARTIDOR | Órdenes para llevar, disponibilidad de platillos del día |

---

## Impresión de tickets

El servicio `ImpresoraService` soporta:
- **USB local:** Impresoras POS-58 conectadas por USB
- **TCP/IP:** Configuración lista (comentada) para impresoras de red

Cada categoría tiene un campo `impresora` que determina a qué impresora se enruta el ticket de cocina de esa categoría.

Se imprime automáticamente al:
1. Sincronizar platillos → ticket de cocina
2. Cerrar orden → ticket del cliente

---

## Manejo de errores

`TratadorDeErrores.java` intercepta excepciones globalmente:

| Excepción | HTTP | Descripción |
|---|---|---|
| `ValidacionException` | 400 | Violación de regla de negocio |
| `RecursoNoEncontradoException` | 404 | Entidad no encontrada |
| `MethodArgumentNotValidException` | 400 | Fallo de Bean Validation |
| Otras | 500 | Error interno |

**Response de error:**
```json
{ "mensaje": "Descripción del error", "ruta": "/ordenes/cerrar/99" }
```

---

## Relaciones entre entidades

```
Usuario ──< Orden ──< OrdenDetalle >── Producto >── Categoria
Mesa    ──< Orden
Orden   ──< EventoOrden
Usuario ──< EventoOrden
```

---

## Notas de producción

- Cambiar `spring.jpa.hibernate.ddl-auto=update` a `validate` en producción
- Definir `JWT_SECRET` como variable de entorno segura (mínimo 256 bits)
- Restringir CORS a los dominios permitidos en `SecurityConfigurations.java`
- Configurar IP de impresoras de red en `ImpresoraService.java`
- Revisar credenciales de base de datos en `application.properties`
