package restaurante.api.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.UsuarioRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /inventario/insumos/{id}/movimientos — el kardex por páginas.
 *
 * Se prueba por HTTP y con un JWT real (no con mocks de seguridad) porque lo que
 * importa es el contrato completo: el filtro de seguridad, el tope de tamaño que
 * pone Spring, la lista blanca del orden y la forma del JSON que recibe el front.
 *
 * Los datos van en una transacción que se revierte al final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KardexPaginadoTest {

    /** Más de dos páginas por defecto (20) y no múltiplo de 20, para que la última quede incompleta. */
    private static final int MOVIMIENTOS = 45;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenService tokenService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ObjectMapper json;

    private long insumo;
    private String tokenAdmin;
    private String tokenMesero;
    /** Los ids en el orden en que el kardex debe mostrarlos: lo más nuevo primero. */
    private final List<Long> esperadoDesc = new ArrayList<>();

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        long admin = usuario("ADMIN", sufijo);
        long mesero = usuario("MESERO", sufijo);
        tokenAdmin = tokenService.generarToken(usuarioRepository.findById(admin).orElseThrow());
        tokenMesero = tokenService.generarToken(usuarioRepository.findById(mesero).orElseThrow());

        jdbc.update("INSERT INTO insumos (nombre, unidad, stock_minimo, activo) VALUES (?, 'PIEZA', 0, 1)",
                "Insumo kardex " + sufijo);
        insumo = ultimoId();

        // De tres en tres comparten la MISMA fecha a propósito: es el caso que
        // rompe una paginación sin desempate (un renglón sale en dos páginas y
        // otro en ninguna).
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 12, 0);
        List<Long> asc = new ArrayList<>();
        for (int i = 0; i < MOVIMIENTOS; i++) {
            jdbc.update("""
                    INSERT INTO movimientos_inventario (id_insumo, tipo, cantidad, motivo, id_usuario, fecha)
                    VALUES (?, 'VENTA', -1, ?, ?, ?)""",
                    insumo, "mov " + i, admin, Timestamp.valueOf(base.plusMinutes(i / 3)));
            asc.add(ultimoId());
        }
        for (int i = asc.size() - 1; i >= 0; i--) {
            esperadoDesc.add(asc.get(i));
        }
    }

    @Test
    @DisplayName("Sin parámetros: primera página de 20, lo más nuevo primero, con los totales")
    void paginaPorDefecto() throws Exception {
        JsonNode pagina = leer(pedir("", tokenAdmin).andExpect(status().isOk()));

        assertEquals(0, pagina.get("pagina").asInt());
        assertEquals(20, pagina.get("tamano").asInt());
        assertEquals(MOVIMIENTOS, pagina.get("totalElementos").asLong());
        assertEquals(3, pagina.get("totalPaginas").asInt());
        assertFalse(pagina.get("ultima").asBoolean());
        assertEquals(esperadoDesc.subList(0, 20), ids(pagina));

        JsonNode primero = pagina.get("contenido").get(0);
        assertEquals("VENTA", primero.get("tipo").asText());
        assertEquals(insumo, primero.get("id_insumo").asLong());
        assertNotNull(primero.get("usuario").asText());
    }

    @Test
    @DisplayName("La última página trae el resto y dice que es la última")
    void ultimaPagina() throws Exception {
        JsonNode pagina = leer(pedir("?page=2", tokenAdmin).andExpect(status().isOk()));

        assertEquals(esperadoDesc.subList(40, 45), ids(pagina));
        assertTrue(pagina.get("ultima").asBoolean());
    }

    @Test
    @DisplayName("Recorrer todas las páginas da cada movimiento exactamente una vez, aunque compartan fecha")
    void sinRepetidosNiHuecos() throws Exception {
        List<Long> todos = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            todos.addAll(ids(leer(pedir("?size=10&page=" + p, tokenAdmin).andExpect(status().isOk()))));
        }
        assertEquals(esperadoDesc, todos);
        assertEquals(MOVIMIENTOS, new HashSet<>(todos).size());
    }

    @Test
    @DisplayName("Pedir size=100000 se recorta al máximo de 100")
    void tamanoMaximoRespetado() throws Exception {
        JsonNode pagina = leer(pedir("?size=100000", tokenAdmin).andExpect(status().isOk()));

        assertEquals(100, pagina.get("tamano").asInt());
        assertEquals(MOVIMIENTOS, pagina.get("contenido").size());
    }

    @Test
    @DisplayName("Se puede invertir el orden por fecha")
    void ordenAscendente() throws Exception {
        JsonNode pagina = leer(pedir("?sort=fecha,asc&size=3", tokenAdmin).andExpect(status().isOk()));

        // Los tres primeros comparten fecha; el desempate por id va descendente.
        Set<Long> tresMasViejos = Set.copyOf(esperadoDesc.subList(42, 45));
        assertEquals(tresMasViejos, Set.copyOf(ids(pagina)));
    }

    @Test
    @DisplayName("Ordenar por un campo fuera de la lista blanca da 400 con mensaje")
    void ordenInvalidoRechazado() throws Exception {
        pedir("?sort=usuario.contrasena", tokenAdmin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("usuario.contrasena")));
    }

    @Test
    @DisplayName("Un insumo que no existe da 404, no una página vacía")
    void insumoInexistente() throws Exception {
        mvc.perform(get("/inventario/insumos/{id}/movimientos", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un MESERO no puede ver el kardex (403)")
    void meseroNoPuede() throws Exception {
        pedir("", tokenMesero).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El /kardex de siempre sigue respondiendo la lista completa (no se rompe el front actual)")
    void kardexViejoIntacto() throws Exception {
        String cuerpo = mvc.perform(get("/inventario/insumos/{id}/kardex", insumo)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode lista = json.readTree(cuerpo);

        assertTrue(lista.isArray());
        assertEquals(MOVIMIENTOS, lista.size());
    }

    // ---------- utilidades ----------

    private ResultActions pedir(String consulta, String token) throws Exception {
        return mvc.perform(get("/inventario/insumos/" + insumo + "/movimientos" + consulta)
                .header("Authorization", "Bearer " + token));
    }

    private JsonNode leer(ResultActions respuesta) throws Exception {
        return json.readTree(respuesta.andReturn().getResponse().getContentAsString());
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.get("contenido").forEach(m -> ids.add(m.get("id_movimiento").asLong()));
        return ids;
    }

    private long usuario(String rol, String sufijo) {
        jdbc.update("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, ?, 'x', 1, ?)",
                rol + " " + sufijo, rol, rol.toLowerCase() + "-kardex-" + sufijo + "@prueba.test");
        return ultimoId();
    }

    private long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
