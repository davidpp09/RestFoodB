package restaurante.api.infra.validacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import restaurante.api.infra.errores.ValidacionException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Los bordes del tope: justo en el límite pasa, un día más no. */
class RangoFechasTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);

    @Test
    @DisplayName("Un solo día (desde = hasta) es válido")
    void unDia() {
        assertDoesNotThrow(() -> RangoFechas.validar(INICIO, INICIO));
    }

    @Test
    @DisplayName("Exactamente 92 días pasa; 93 no")
    void bordeDelTope() {
        assertDoesNotThrow(() -> RangoFechas.validar(INICIO, INICIO.plusDays(91)));
        var e = assertThrows(ValidacionException.class,
                () -> RangoFechas.validar(INICIO, INICIO.plusDays(92)));
        assertTrue(e.getMessage().contains("93 días"), e.getMessage());
    }

    @Test
    @DisplayName("Un rango al revés se rechaza con un mensaje que lo dice")
    void rangoInvertido() {
        var e = assertThrows(ValidacionException.class,
                () -> RangoFechas.validar(INICIO.plusDays(1), INICIO));
        assertTrue(e.getMessage().contains("anterior"), e.getMessage());
    }

    @Test
    @DisplayName("Años de historia de un jalón se rechazan")
    void anosDeHistoria() {
        assertThrows(ValidacionException.class,
                () -> RangoFechas.validar(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 11, 14)));
    }
}
