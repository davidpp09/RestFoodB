package restaurante.api.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los índices de V9 existen y tienen las columnas en el orden correcto.
 *
 * Un índice que falta no rompe nada: las consultas siguen respondiendo igual,
 * solo que más lento cada mes. Por eso nadie lo nota hasta que el corte del día
 * tarda. Este test lo detecta en el CI, que construye la base desde V0.
 *
 * Se revisa el ORDEN de las columnas además del nombre, porque (fecha, estatus)
 * y (estatus, fecha) no sirven para las mismas consultas.
 */
@SpringBootTest
class IndicesBaseDatosTest {

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0}.{1} = ({2})")
    @DisplayName("Los índices de los reportes y del kardex existen con sus columnas en orden")
    @CsvSource(delimiter = ';', value = {
            "ordenes;               idx_ordenes_estatus_cierre;   estatus,fecha_cierre",
            "ordenes;               idx_ordenes_usuario_apertura; id_usuario,fecha_apertura",
            "ordenes;               idx_ordenes_tipo_apertura;    tipo,fecha_apertura",
            "ordenes;               idx_ordenes_apertura;         fecha_apertura",
            "eventos_orden;         idx_eventos_tipo_fecha;       tipo_evento,timestamp",
            "movimientos_inventario; idx_movimientos_insumo_fecha; id_insumo,fecha",
    })
    void indiceExiste(String tabla, String indice, String columnasEsperadas) {
        List<String> columnas = jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                ORDER BY seq_in_index
                """, String.class, tabla.trim(), indice.trim());

        assertEquals(List.of(columnasEsperadas.trim().split(",")), columnas,
                "El índice " + indice + " no existe o sus columnas cambiaron de orden");
    }
}
