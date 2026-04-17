# RestFood API - Sistema de Gestión de Restaurante (Backend)

API que gestiona la lógica de negocio, persistencia y comunicación en tiempo real del sistema RestFood.

## Tecnologías

- **Java 17** + **Spring Boot 3.5.x**
- **Spring Security + JWT** — autenticación y autorización por roles
- **Spring Data JPA + MySQL 8** — persistencia
- **Flyway** — migraciones de base de datos
- **WebSockets (STOMP)** — notificaciones en tiempo real
- **Lombok** — reducción de boilerplate
- **SpringDoc OpenAPI (Swagger)** — documentación interactiva
- **Escpos-Coffee** — impresión de tickets térmicos ESC/POS

## Roles del sistema

| Rol | Acceso |
|-----|--------|
| `DEV` | Total (incluyendo borrado físico y configuración) |
| `ADMIN` | Total excepto operaciones destructivas de DEV |
| `CAJERO` | Órdenes, usuarios (lectura) y reportes |
| `MESERO` | Mesas, órdenes propias y comanda |
| `COCINA` | Vista y confirmación de órdenes pendientes |
| `REPARTIDOR` | Órdenes para llevar y gestión del menú del día |

## Endpoints

### Autenticación

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/login` | Login — devuelve JWT, rol, nombre y ruta de destino |

### Órdenes

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `POST` | `/ordenes` | ADMIN, DEV, MESERO, REPARTIDOR | Abrir mesa/orden |
| `GET` | `/ordenes` | ADMIN, DEV, CAJERO, MESERO | Listar órdenes paginadas |
| `GET` | `/ordenes/activa/{id_mesa}` | ADMIN, DEV, CAJERO, MESERO | Orden activa de una mesa |
| `GET` | `/ordenes/entregas/hoy` | ADMIN, DEV, REPARTIDOR | Entregas del día |
| `PUT` | `/ordenes/{id}/cerrar` | ADMIN, DEV, CAJERO, MESERO, REPARTIDOR | Cobrar y cerrar mesa |
| `POST` | `/ordenes/{id}/reimprimir-ticket` | ADMIN, DEV, CAJERO, MESERO | Reimprimir ticket de cliente |
| `POST` | `/ordenes/{id}/reenviar-cocina` | ADMIN, DEV, MESERO | Reenviar orden completa a cocina |

### Comanda

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `POST` | `/ordendetalles` | ADMIN, DEV, MESERO, REPARTIDOR | Sincronizar comanda (agrega, modifica y cancela platillos en un solo request) |

### Mesas

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `POST` | `/mesas` | ADMIN, DEV, MESERO | Registrar mesa |
| `GET` | `/mesas` | ADMIN, DEV, MESERO | Listar mesas con estado actual |
| `GET` | `/mesas/rango/{inicio}/{fin}` | ADMIN, DEV, MESERO | Mesas filtradas por rango de IDs |

### Cocina

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `GET` | `/cocina` | ADMIN, DEV, COCINA | Listar órdenes pendientes |
| `PATCH` | `/cocina/{id}/servido` | ADMIN, DEV, COCINA | Marcar orden como servida |

### Usuarios

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `GET` | `/usuarios/me` | Cualquier rol | Revalidar sesión con token existente |
| `POST` | `/usuarios` | ADMIN, DEV | Registrar usuario |
| `GET` | `/usuarios` | ADMIN, DEV, CAJERO | Listar usuarios activos |
| `PUT` | `/usuarios` | ADMIN, DEV | Actualizar usuario |
| `DELETE` | `/usuarios/{id}` | ADMIN, DEV | Baja lógica (desactiva) |
| `PUT` | `/usuarios/activar/{id}` | ADMIN, DEV | Reactivar usuario |
| `DELETE` | `/usuarios/eliminar/{id}` | DEV | Borrado físico |

### Productos

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `POST` | `/productos` | DEV | Registrar producto |
| `GET` | `/productos` | ADMIN, DEV, MESERO, REPARTIDOR | Listar productos con categoría |
| `PUT` | `/productos/{id}` | DEV | Actualizar producto |
| `DELETE` | `/productos/{id}` | DEV | Eliminar producto |
| `PATCH` | `/productos/{id}/dia` | DEV, REPARTIDOR | Activar/desactivar para el día (máx. 7 por categoría) |
| `PUT` | `/productos/desactivar-dia/{categoriaId}` | DEV, REPARTIDOR | Desactivar todos los productos de una categoría |

### Categorías

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `GET` | `/categorias` | ADMIN, DEV, REPARTIDOR | Listar categorías |
| `POST` | `/categorias` | ADMIN, DEV | Registrar categoría |

### Admin / Reportes

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `GET` | `/admin?fecha=YYYY-MM-DD` | ADMIN, DEV | Corte del día (ventas, platillos, meseros) |
| `GET` | `/admin/cancelaciones?desde=&hasta=` | ADMIN, DEV | Cancelaciones por mesero en rango de fechas |

## WebSocket topics

| Topic | Descripción |
|-------|-------------|
| `/topic/mesas` | Estado de mesas en tiempo real |
| `/topic/cocina` | Tickets y acciones de cocina |
| `/topic/tickets` | Ticket de cliente al cerrar mesa |

## Tipos de orden

| Valor | Descripción |
|-------|-------------|
| `LOZA` | Consumo en mesa |
| `LLEVAR` | Para llevar / entrega a domicilio |

## Impresoras

El sistema soporta hasta 3 impresoras térmicas configuradas en `application.properties`:

```properties
impresora.cocina1.nombre=POS-58 (1)   # Cocina principal
impresora.cocina2.nombre=POS-58 (2)   # Cocina secundaria (opcional)
impresora.tickets.nombre=POS-58 (3)   # Tickets de cliente
```

Preparado para migrar a TCP/IP: descomenta las líneas de IP/puerto en `application.properties`.

## Historial de eventos (`eventos_orden`)

Cada acción sobre una orden queda registrada automáticamente:

| Evento | Cuándo |
|--------|--------|
| `MESA_ABIERTA` | Al abrir una orden |
| `PLATILLO_NUEVO` | Al agregar un platillo |
| `PLATILLO_MODIFICADO` | Al cambiar cantidad o comentarios |
| `PLATILLO_CANCELADO` | Al eliminar un platillo |
| `MESA_CERRADA` | Al cobrar la mesa |

Útil para auditoría, reportes y entrenamiento de modelos de IA para detección de patrones.

## Requisitos

- Java 17+
- Maven 3.6+ (o usar el wrapper `mvnw` incluido)
- MySQL 8.0+ con base de datos `restaurante`
- Variables de entorno requeridas:

| Variable | Descripción | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secreto para firmar los tokens JWT | — (obligatorio) |
| `DB_URL` | URL de conexión a MySQL | `jdbc:mysql://localhost:3306/restaurante?...` |
| `DB_USER` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | `root` |
| `CORS_ORIGINS` | Orígenes permitidos para CORS/WebSocket | `http://localhost:5173,...` |

## Instalación y ejecución

```bash
cd RestFoodB/api

# Opción A — con Maven wrapper (no requiere Maven instalado)
./mvnw spring-boot:run

# Opción B — con Maven instalado globalmente
mvn spring-boot:run
```

La API arranca en `http://localhost:8080`.  
Documentación Swagger disponible en `http://localhost:8080/swagger-ui.html`.
