package restaurante.api.infra.errores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.UsuarioRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Todo error del API tiene la misma forma: {"mensaje": "..."} y, solo en las
 * validaciones de campos, además "errores": [...].
 *
 * El frontend muestra `data.mensaje` en todas sus pantallas. Un error con otra
 * forma (cuerpo vacío, arreglo suelto, texto plano) se ve como "Error" a secas
 * y nadie sabe qué pasó. Este test recorre cada tipo de error por HTTP y exige
 * siempre la misma forma.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErroresUniformesTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenService tokenService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ObjectMapper json;

    private String admin;
    private String mesero;
    private String sufijo;

    @BeforeEach
    void usuarios() {
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        admin = token("ADMIN");
        mesero = token("MESERO");
    }

    @Test
    @DisplayName("Validación de campos: 400 con mensaje del primer campo y la lista de errores")
    void validacionDeCampos() throws Exception {
        JsonNode cuerpo = error(400, conToken(post("/inventario/insumos"), admin).content("{}"));

        assertTrue(cuerpo.get("errores").isArray());
        assertFalse(cuerpo.get("errores").isEmpty());
        JsonNode primero = cuerpo.get("errores").get(0);
        assertNotNull(primero.get("campo"));
        // El mensaje es el texto de un campo, no un genérico: lo que leía la
        // pantalla de cambiar contraseña con el formato viejo.
        assertEquals(primero.get("error").asText(), cuerpo.get("mensaje").asText());
    }

    @Test
    @DisplayName("PATCH /productos/{id}/dia: un precio negativo ahora se rechaza")
    void precioNegativoRechazado() throws Exception {
        long producto = producto(categoria(7), false);
        JsonNode cuerpo = error(400, conToken(patch("/productos/" + producto + "/dia"), admin)
                .content("{\"precio_comida\": -5}"));
        assertEquals("El precio no puede ser negativo", cuerpo.get("mensaje").asText());
    }

    @Test
    @DisplayName("PATCH /productos/{id}/dia: el tope de activos llega como {mensaje}, no como texto suelto")
    void topeDeActivos() throws Exception {
        long categoria = categoria(1);
        producto(categoria, true);
        long apagado = producto(categoria, false);

        JsonNode cuerpo = error(400, conToken(patch("/productos/" + apagado + "/dia"), admin)
                .content("{\"disponibilidad\": true}"));
        assertTrue(cuerpo.get("mensaje").asText().startsWith("Máximo 1 platillos activos"));
        assertNull(cuerpo.get("errores"), "errores solo va en validaciones de campos");
    }

    @Test
    @DisplayName("Un valor que no existe en un enum da 400, no 500")
    void enumInvalido() throws Exception {
        error(400, conToken(post("/inventario/movimientos"), admin)
                .content("{\"id_insumo\": 1, \"tipo\": \"REGALO\", \"cantidad\": 1}"));
    }

    @Test
    @DisplayName("Un parámetro con tipo incorrecto da 400 y nombra el parámetro")
    void parametroConTipoIncorrecto() throws Exception {
        JsonNode cuerpo = error(400, conToken(get("/admin/comandas-dia").param("fecha", "ayer"), admin));
        assertTrue(cuerpo.get("mensaje").asText().contains("fecha"));
    }

    @Test
    @DisplayName("findById().orElseThrow() de algo que no existe da 404 con mensaje, no 500")
    void registroInexistente() throws Exception {
        error(404, conToken(delete("/productos/" + Long.MAX_VALUE), admin));
        error(404, conToken(patch("/productos/" + Long.MAX_VALUE + "/dia"), admin).content("{}"));
    }

    @Test
    @DisplayName("Una ruta que no existe da 404 con mensaje, no 500")
    void rutaInexistente() throws Exception {
        error(404, conToken(get("/esta-ruta-no-existe"), admin));
    }

    @Test
    @DisplayName("Un método que la ruta no acepta da 405 con mensaje")
    void metodoNoPermitido() throws Exception {
        error(405, conToken(delete("/cocina"), admin));
    }

    @Test
    @DisplayName("403 y 401 también traen mensaje")
    void permisos() throws Exception {
        error(403, conToken(get("/inventario/insumos"), mesero));
        error(401, get("/inventario/insumos"));
    }

    // ---------- utilidades ----------

    /** Hace la petición, comprueba el código y que el cuerpo tenga un `mensaje` legible. */
    private JsonNode error(int codigo, MockHttpServletRequestBuilder peticion) throws Exception {
        var respuesta = mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();
        String texto = respuesta.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(codigo, respuesta.getStatus(), "Cuerpo: " + texto);
        JsonNode cuerpo = json.readTree(texto);
        assertTrue(cuerpo.isObject(), "El error debe ser un objeto JSON, llegó: " + texto);
        assertTrue(cuerpo.hasNonNull("mensaje") && !cuerpo.get("mensaje").asText().isBlank(),
                "Falta el mensaje: " + texto);
        return cuerpo;
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    private String token(String rol) {
        jdbc.update("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, ?, 'x', 1, ?)",
                rol + " " + sufijo, rol, rol.toLowerCase() + "-errores-" + sufijo + "@prueba.test");
        return tokenService.generarToken(usuarioRepository.findById(ultimoId()).orElseThrow());
    }

    private long categoria(int maxActivos) {
        jdbc.update("INSERT INTO categorias (nombre, impresora, max_activos) VALUES (?, 'COCINA', ?)",
                "Errores " + sufijo, maxActivos);
        return ultimoId();
    }

    private long producto(long categoria, boolean activo) {
        jdbc.update("""
                INSERT INTO productos (nombre, precio_comida, precio_desayuno, disponibilidad, id_categoria, eliminado)
                VALUES (?, 100, 90, ?, ?, 0)""", "Platillo " + UUID.randomUUID(), activo, categoria);
        return ultimoId();
    }

    private long ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
