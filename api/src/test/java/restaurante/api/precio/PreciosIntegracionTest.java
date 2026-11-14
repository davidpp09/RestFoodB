package restaurante.api.precio;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.UsuarioRepository;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sin transacción de test: comprueba commit, rollback y lectura LAZY reales. */
@SpringBootTest
@AutoConfigureMockMvc
class PreciosIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TokenService tokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired GrupoPrecioService grupos;
    @MockitoSpyBean HistorialPrecioService historial;
    HistorialPrecioService historialReal;
    long categoria, primero, segundo, grupo;
    String clave, sufijo;
    Map<String, Long> ids = new HashMap<>();

    @BeforeEach void datos() {
        historialReal = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(historial);
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        for (String rol : List.of("ADMIN", "DEV", "MESERO", "COCINA", "REPARTIDOR")) {
            ids.put(rol, insertar("INSERT INTO usuarios(nombre,rol,contrasena,estatus,email) VALUES (?,?, 'x',1,?)",
                    "Precio " + rol, rol, rol + sufijo + "@prueba.test"));
        }
        categoria = insertar("INSERT INTO categorias(nombre,impresora,max_activos) VALUES (?, 'COCINA_1',7)", "Precio " + sufijo);
        primero = producto("Uno ", 95);
        segundo = producto("Dos ", 100);
        clave = "prueba-" + sufijo;
        grupo = insertar("INSERT INTO grupos_precio(clave,nombre,campo) VALUES (?, 'Grupo de prueba','COMIDA')", clave);
        jdbc.update("INSERT INTO grupos_precio_productos VALUES (?,?), (?,?)", grupo, primero, grupo, segundo);
    }

    @AfterEach void limpiar() {
        reset(historialReal);
        if (org.springframework.test.context.transaction.TestTransaction.isActive()) return;
        jdbc.update("DELETE FROM historial_precios WHERE id_producto IN (SELECT id_productos FROM productos WHERE id_categoria = ?)", categoria);
        jdbc.update("DELETE FROM grupos_precio_productos WHERE id_grupo_precio = ?", grupo);
        jdbc.update("DELETE FROM grupos_precio WHERE id_grupo_precio = ?", grupo);
        jdbc.update("DELETE FROM productos WHERE id_categoria = ?", categoria);
        jdbc.update("DELETE FROM categorias WHERE id_categorias = ?", categoria);
        ids.values().forEach(id -> jdbc.update("DELETE FROM usuarios WHERE id_usuarios = ?", id));
    }

    @Test void cambioDeGrupoConHistorialYSinDuplicarCambiosIguales() throws Exception {
        mvc.perform(get("/grupos-precio").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.clave == '" + clave + "')].mixto").value(true));
        cambiar("DEV", "115.50").andExpect(status().isOk()).andExpect(jsonPath("mixto").value(false));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE id_categoria = ? AND precio_comida = 115.50 AND precio_desayuno = 80", Integer.class, categoria));
        assertEquals(2, conteoHistorial());
        cambiar("ADMIN", "115.50").andExpect(status().isOk());
        assertEquals(2, conteoHistorial());
        mvc.perform(get("/productos/" + primero + "/historial-precios").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].precioAnterior").value(95))
                .andExpect(jsonPath("$[0].precioNuevo").value(115.50)).andExpect(jsonPath("$[0].origen").value("MENU"))
                .andExpect(jsonPath("$[0].idUsuario").value(ids.get("DEV")))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty());
    }

    @Test void grupoEsTodoONadaSiFallaElHistorial() {
        AtomicInteger llamadas = new AtomicInteger();
        doAnswer(inv -> {
            if (llamadas.incrementAndGet() == 2) throw new IllegalStateException("Fallo simulado de auditoría");
            return inv.callRealMethod();
        }).when(historialReal).registrar(any(), any(), any(), any(), any());
        assertThrows(IllegalStateException.class, () -> grupos.actualizar(clave,
                new DatosCambioPrecio(new java.math.BigDecimal("120")), usuarios.findById(ids.get("ADMIN")).orElseThrow()));
        assertEquals(0, conteoHistorial());
        assertEquals(95, jdbc.queryForObject("SELECT precio_comida FROM productos WHERE id_productos = ?", Integer.class, primero));
        assertEquals(100, jdbc.queryForObject("SELECT precio_comida FROM productos WHERE id_productos = ?", Integer.class, segundo));
    }

    @Test void permisosHttpDeGruposEHistorial() throws Exception {
        for (String rol : List.of("MESERO", "COCINA", "REPARTIDOR")) {
            cambiar(rol, "115").andExpect(status().isForbidden());
            mvc.perform(get("/grupos-precio").header("Authorization", token(rol))).andExpect(status().isForbidden());
            mvc.perform(get("/productos/" + primero + "/historial-precios").header("Authorization", token(rol))).andExpect(status().isForbidden());
        }
        assertEquals(0, conteoHistorial());
    }

    @Test void repartidorNoPuedeCambiarPreciosDeLaCartaPorLaRutaDelDia() throws Exception {
        mvc.perform(patch("/productos/" + primero + "/dia").header("Authorization", token("REPARTIDOR"))
                .contentType("application/json").content("{\"precio_comida\":120}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/productos/desactivar-dia/" + categoria).header("Authorization", token("REPARTIDOR")))
                .andExpect(status().isBadRequest());
        assertEquals(0, conteoHistorial());
        assertEquals(95, jdbc.queryForObject("SELECT precio_comida FROM productos WHERE id_productos = ?", Integer.class, primero));
    }

    @Test void precioInvalidoNoEscribe() throws Exception {
        for (String precio : List.of("-1", "0.001", "1000000", "null")) {
            cambiar("ADMIN", precio).andExpect(status().isBadRequest()).andExpect(jsonPath("mensaje").isNotEmpty());
        }
        assertEquals(0, conteoHistorial());
    }

    @Test void cartaYDiaRegistranAmbosCampos() throws Exception {
        mvc.perform(put("/productos/" + primero).header("Authorization", token("ADMIN"))
                .contentType("application/json").content("{\"precio_comida\":110,\"precio_desayuno\":90}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/productos/" + primero + "/dia").header("Authorization", token("ADMIN"))
                .contentType("application/json").content("{\"precio_comida\":120}"))
                .andExpect(status().isOk()).andExpect(jsonPath("precioDesayuno").value(120));
        assertEquals(4, conteoHistorial());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ? AND origen = 'DIA'", Integer.class, primero));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void altaYRestauracionDelDiaConservanElHistorial() throws Exception {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM categorias WHERE nombre LIKE 'Comida del d%a'", Integer.class) == 0) {
            insertar("INSERT INTO categorias(nombre,impresora,max_activos) VALUES ('Comida del dia','COCINA_1',6)");
        }
        String cuerpo = "{\"nombre\":\"Día " + sufijo + "\",\"precio\":105}";
        var alta = mvc.perform(post("/productos/dia").header("Authorization", token("REPARTIDOR"))
                .contentType("application/json").content(cuerpo)).andExpect(status().isCreated()).andReturn().getResponse();
        long id = json.readTree(alta.getContentAsString()).get("id").asLong();
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ? AND precio_anterior IS NULL AND origen = 'DIA'", Integer.class, id));
        mvc.perform(delete("/productos/dia/" + id).header("Authorization", token("REPARTIDOR"))).andExpect(status().isNoContent());
        mvc.perform(post("/productos/dia").header("Authorization", token("REPARTIDOR"))
                .contentType("application/json").content(cuerpo)).andExpect(status().isOk()).andExpect(jsonPath("id").value(id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ?", Integer.class, id));
        mvc.perform(delete("/productos/dia/" + id).header("Authorization", token("REPARTIDOR"))).andExpect(status().isNoContent());
        mvc.perform(post("/productos/dia").header("Authorization", token("REPARTIDOR"))
                .contentType("application/json").content(cuerpo.replace("105", "120")))
                .andExpect(status().isOk()).andExpect(jsonPath("precioDesayuno").value(120));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ?", Integer.class, id));
        // Solo este caso usa rollback de test: el ensayo de atomicidad superior no lo usa.
    }

    @Test void productoConHistorialSeArchivaAlEliminar() throws Exception {
        cambiar("ADMIN", "110").andExpect(status().isOk());
        mvc.perform(delete("/productos/" + primero).header("Authorization", token("ADMIN"))).andExpect(status().isNoContent());
        assertTrue(jdbc.queryForObject("SELECT eliminado FROM productos WHERE id_productos = ?", Boolean.class, primero));
        assertEquals(2, conteoHistorial());
    }

    private org.springframework.test.web.servlet.ResultActions cambiar(String rol, String precio) throws Exception {
        return mvc.perform(patch("/grupos-precio/" + clave).header("Authorization", token(rol))
                .contentType("application/json").content("{\"precio\":" + precio + "}"));
    }
    private int conteoHistorial() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto IN (?,?)", Integer.class, primero, segundo);
    }
    private String token(String rol) { return "Bearer " + tokens.generarToken(usuarios.findById(ids.get(rol)).orElseThrow()); }
    private long producto(String nombre, int precio) {
        return insertar("INSERT INTO productos(nombre,precio_comida,precio_desayuno,disponibilidad,id_categoria,eliminado) VALUES (?,?,80,0,?,0)", nombre + sufijo, precio, categoria);
    }
    private long insertar(String sql, Object... parametros) {
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            return ps;
        }, key);
        return key.getKey().longValue();
    }
}
