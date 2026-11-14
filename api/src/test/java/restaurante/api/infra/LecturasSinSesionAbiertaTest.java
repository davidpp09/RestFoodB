package restaurante.api.infra;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Prueba de humo: cada GET del API responde algo distinto de 500 con datos que
 * tocan todas las relaciones (orden con mesa, mesera y platillos; entrega;
 * orden pagada; cancelación; insumo con kardex y receta).
 *
 * Existe por spring.jpa.open-in-view=false. Con esa opción en true (el default
 * de Spring) la sesión de Hibernate queda abierta durante TODA la petición, y
 * una relación LAZY se carga sola aunque el código ya haya salido del servicio.
 * Al apagarla, ese mismo acceso revienta con LazyInitializationException, que
 * el usuario ve como un 500. Este test es el que lo detecta.
 *
 * Por eso NO lleva @Transactional: si la petición corriera dentro de la
 * transacción del test, la sesión seguiría abierta y taparía justo el error que
 * se busca. Los datos se borran a mano al final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LecturasSinSesionAbiertaTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenService tokenService;
    @Autowired private UsuarioRepository usuarioRepository;

    private long admin, mesera, repartidor, categoria, producto, mesa, mesaLibre, insumo;
    private long ordenMesa, ordenLlevar, ordenPagada;

    @BeforeAll
    void datos() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        admin = insertar("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, 'ADMIN', 'x', 1, ?)",
                "Admin humo " + s, "admin-humo-" + s + "@prueba.test");
        mesera = insertar("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, 'MESERO', 'x', 1, ?)",
                "Mesera humo " + s, "mesera-humo-" + s + "@prueba.test");
        repartidor = insertar("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, 'REPARTIDOR', 'x', 1, ?)",
                "Repartidor humo " + s, "repartidor-humo-" + s + "@prueba.test");

        categoria = insertar("INSERT INTO categorias (nombre, impresora, max_activos) VALUES (?, 'COCINA_1', 7)", "Humo " + s);
        producto = insertar("""
                INSERT INTO productos (nombre, precio_comida, precio_desayuno, disponibilidad, id_categoria, eliminado)
                VALUES (?, 100, 90, 1, ?, 0)""", "Platillo humo " + s, categoria);
        mesa = insertar("INSERT INTO mesas (numero, estado, id_usuario_asignado, activa) VALUES (?, 'OCUPADA', ?, 1)",
                "H" + s, mesera);
        // Sin cuentas abiertas, para poder darla de baja y reactivarla.
        mesaLibre = insertar("INSERT INTO mesas (numero, estado, id_usuario_asignado, activa) VALUES (?, 'LIBRE', ?, 1)",
                "HL" + s, mesera);

        ordenMesa = orden(mesera, mesa, "LOZA", "PREPARANDO", ahora, null, 1);
        ordenLlevar = orden(repartidor, null, "LLEVAR", "PREPARANDO", ahora, null, 1);
        ordenPagada = orden(mesera, null, "LOZA", "PAGADA", ahora, ahora, 2);

        jdbc.update("""
                INSERT INTO eventos_orden (id_orden, id_usuario, tipo_evento, timestamp, nombre_mesero, nombre_producto, cantidad_anterior)
                VALUES (?, ?, 'PLATILLO_CANCELADO', ?, ?, ?, 1)""",
                ordenMesa, mesera, ahora, "Mesera humo " + s, "Platillo humo " + s);

        insumo = insertar("INSERT INTO insumos (nombre, unidad, stock_minimo, activo) VALUES (?, 'PIEZA', 0, 1)", "Insumo humo " + s);
        jdbc.update("""
                INSERT INTO movimientos_inventario (id_insumo, tipo, cantidad, id_usuario, id_orden, fecha, costo_total)
                VALUES (?, 'COMPRA', 10, ?, NULL, ?, 50)""", insumo, admin, ahora);
        jdbc.update("""
                INSERT INTO movimientos_inventario (id_insumo, tipo, cantidad, id_usuario, id_orden, fecha)
                VALUES (?, 'VENTA', -1, ?, ?, ?)""", insumo, mesera, ordenPagada, ahora);
        jdbc.update("INSERT INTO producto_insumo (id_producto, id_insumo, cantidad) VALUES (?, ?, 1)", producto, insumo);
    }

    @AfterAll
    void limpiar() {
        jdbc.update("DELETE FROM eventos_orden WHERE id_orden IN (?, ?, ?)", ordenMesa, ordenLlevar, ordenPagada);
        jdbc.update("DELETE FROM movimientos_inventario WHERE id_insumo = ?", insumo);
        jdbc.update("DELETE FROM producto_insumo WHERE id_insumo = ?", insumo);
        jdbc.update("DELETE FROM orden_detalle WHERE id_orden IN (?, ?, ?)", ordenMesa, ordenLlevar, ordenPagada);
        jdbc.update("DELETE FROM ordenes WHERE id_ordenes IN (?, ?, ?)", ordenMesa, ordenLlevar, ordenPagada);
        jdbc.update("DELETE FROM mesas WHERE id_mesas IN (?, ?)", mesa, mesaLibre);
        jdbc.update("DELETE FROM productos WHERE id_productos = ?", producto);
        jdbc.update("DELETE FROM categorias WHERE id_categorias = ?", categoria);
        jdbc.update("DELETE FROM insumos WHERE id_insumos = ?", insumo);
        jdbc.update("DELETE FROM usuarios WHERE id_usuarios IN (?, ?, ?)", admin, mesera, repartidor);
    }

    @Test
    @DisplayName("Ningún GET responde 500 (ni LazyInitializationException) con open-in-view apagado")
    void ningunGetRevienta() throws Exception {
        String hoy = LocalDate.now().toString();
        List<String[]> llamadas = List.of(
                // { usuario, ruta }
                new String[]{"admin", "/mesas"},
                new String[]{"mesera", "/mesas/mias"},
                new String[]{"admin", "/mesas/rango/1/100000"},
                new String[]{"admin", "/mesas/gestion"},
                new String[]{"admin", "/cocina"},
                new String[]{"admin", "/ordenes"},
                new String[]{"admin", "/ordenes/activa/" + mesa},
                new String[]{"admin", "/ordenes/entregas/hoy"},
                new String[]{"repartidor", "/ordenes/entregas/hoy"},
                new String[]{"mesera", "/ordenes/mis-comandas"},
                new String[]{"admin", "/admin/corte-dia?fecha=" + hoy},
                new String[]{"admin", "/admin/comandas-dia?fecha=" + hoy},
                new String[]{"admin", "/admin/cancelaciones?desde=" + hoy + "&hasta=" + hoy},
                new String[]{"admin", "/productos"},
                new String[]{"admin", "/categorias"},
                new String[]{"admin", "/usuarios"},
                new String[]{"admin", "/usuarios/todos"},
                new String[]{"mesera", "/usuarios/me"},
                new String[]{"admin", "/usuarios/" + mesera + "/ventas"},
                new String[]{"admin", "/inventario/insumos"},
                new String[]{"admin", "/inventario/insumos/" + insumo + "/kardex"},
                new String[]{"admin", "/inventario/insumos/" + insumo + "/movimientos?size=5"},
                new String[]{"admin", "/inventario/existencias"},
                new String[]{"admin", "/inventario/reportes/teorico-real?desde=" + hoy + "&hasta=" + hoy},
                new String[]{"admin", "/inventario/reportes/food-cost"},
                new String[]{"admin", "/inventario/recetas"},
                new String[]{"admin", "/inventario/insumos/" + insumo + "/receta"},
                new String[]{"admin", "/menu-dia/pdf"},
                new String[]{"admin", "/menu-dia/imagen"}
        );

        List<String> fallas = new ArrayList<>();
        for (String[] llamada : llamadas) {
            long id = switch (llamada[0]) {
                case "mesera" -> mesera;
                case "repartidor" -> repartidor;
                default -> admin;
            };
            String token = tokenService.generarToken(usuarioRepository.findById(id).orElseThrow());
            var respuesta = mvc.perform(get(llamada[1]).header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            if (respuesta.getStatus() >= 500) {
                String cuerpo = respuesta.getContentType() != null && respuesta.getContentType().contains("json")
                        ? respuesta.getContentAsString(StandardCharsets.UTF_8) : "(binario)";
                fallas.add(respuesta.getStatus() + " " + llamada[1] + " como " + llamada[0] + " -> " + cuerpo);
            }
        }
        assertTrue(fallas.isEmpty(), "GET que revientan:\n" + String.join("\n", fallas));
    }

    @Test
    @DisplayName("Las escrituras de mesas que devuelven la mesa con su mesera tampoco revientan")
    void escriturasDeMesas() throws Exception {
        String token = tokenService.generarToken(usuarioRepository.findById(admin).orElseThrow());
        // La mesa de prueba tiene mesera asignada: es justo la relación LAZY que
        // DatosMesaGestion lee para mostrar el nombre.
        var renombrar = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/mesas/" + mesa).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"numero\": \"HX" + mesa + "\"}"))
                .andReturn().getResponse();
        var baja = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/mesas/" + mesaLibre).header("Authorization", "Bearer " + token))
                .andReturn().getResponse();
        var alta = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/mesas/" + mesaLibre + "/activar").header("Authorization", "Bearer " + token))
                .andReturn().getResponse();

        for (var r : List.of(renombrar, baja, alta)) {
            String cuerpo = r.getContentAsString(StandardCharsets.UTF_8);
            assertTrue(r.getStatus() == 200, "Esperaba 200, llegó " + r.getStatus() + ": " + cuerpo);
            assertTrue(cuerpo.contains("Mesera humo"), "Debe traer el nombre de la mesera: " + cuerpo);
        }
    }

    // ---------- utilidades ----------

    private long orden(long usuario, Long mesa, String tipo, String estatus, Timestamp apertura, Timestamp cierre, int comanda) {
        long orden = insertar("""
                INSERT INTO ordenes (fecha_apertura, fecha_cierre, estatus, total, id_usuario, id_mesa, tipo, servicio, numero_comanda)
                VALUES (?, ?, ?, 100, ?, ?, ?, 'COMIDA', ?)""", apertura, cierre, estatus, usuario, mesa, tipo, comanda);
        jdbc.update("""
                INSERT INTO orden_detalle (cantidad, precio_unitario, subtotal, id_orden, id_producto)
                VALUES (1, 100, 100, ?, ?)""", orden, producto);
        return orden;
    }

    private long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
