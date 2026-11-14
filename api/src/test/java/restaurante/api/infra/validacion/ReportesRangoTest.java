package restaurante.api.infra.validacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.UsuarioRepository;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los dos reportes con periodo libre aplican el tope por HTTP, con el mismo
 * formato de error ({"mensaje": ...}) que el frontend ya sabe mostrar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportesRangoTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TokenService tokenService;
    @Autowired private UsuarioRepository usuarioRepository;

    private String token;

    @BeforeEach
    void admin() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, 'ADMIN', 'x', 1, ?)",
                "Admin rango " + sufijo, "admin-rango-" + sufijo + "@prueba.test");
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        token = tokenService.generarToken(usuarioRepository.findById(id).orElseThrow());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/inventario/reportes/teorico-real", "/admin/cancelaciones"})
    @DisplayName("Un periodo de años da 400 con mensaje")
    void periodoLargoRechazado(String ruta) throws Exception {
        mvc.perform(get(ruta).param("desde", "2020-01-01").param("hasta", "2026-11-14")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("máximo es 92")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/inventario/reportes/teorico-real", "/admin/cancelaciones"})
    @DisplayName("Un periodo al revés da 400 con mensaje")
    void periodoInvertido(String ruta) throws Exception {
        mvc.perform(get(ruta).param("desde", "2026-11-14").param("hasta", "2026-11-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("anterior")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/inventario/reportes/teorico-real", "/admin/cancelaciones"})
    @DisplayName("Un mes normal y la llamada sin fechas siguen respondiendo 200")
    void periodoNormal(String ruta) throws Exception {
        mvc.perform(get(ruta).param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
