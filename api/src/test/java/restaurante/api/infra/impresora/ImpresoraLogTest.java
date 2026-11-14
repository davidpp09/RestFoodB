package restaurante.api.infra.impresora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import restaurante.api.orden.DatosRespuestaCuenta;
import restaurante.api.ordenDetalle.DatosDetalleRespuesta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un ticket que no sale en papel tiene que quedar en el log como ERROR, con
 * texto fijo ("SIN IMPRIMIR") para poder buscarlo:
 *
 *   journalctl -u restfood-backend --since today | grep "SIN IMPRIMIR"
 *
 * Es la pregunta que se hace el día que una mesera dice "no me salió el ticket",
 * y antes la respuesta era un println con emoji, sin nivel.
 */
@ExtendWith(OutputCaptureExtension.class)
class ImpresoraLogTest {

    @Test
    @DisplayName("Si la impresora no existe, el ticket queda en el log como ERROR 'SIN IMPRIMIR'")
    void impresoraInexistenteQuedaEnElLog(CapturedOutput salida) {
        var servicio = new ImpresoraService();
        // Sin IP: se busca por nombre en el sistema operativo, y este no existe.
        ReflectionTestUtils.setField(servicio, "nombreMeseras", "Impresora que no existe 7f3a");
        ReflectionTestUtils.setField(servicio, "ipMeseras", "");

        var platillo = new DatosDetalleRespuesta(1L, 1, new BigDecimal("120"), new BigDecimal("120"),
                null, 1L, 1L, "Bistec");
        var ticket = new DatosRespuestaCuenta(1L, 42, "5", "LOZA", LocalDateTime.now(), LocalDateTime.now(),
                List.of(platillo), new BigDecimal("120"), "CERRADA", "MARELI", "COMIDA");

        // Llamada directa (sin el proxy de Spring), así que @Async no aplica y
        // corre en este mismo hilo.
        servicio.imprimirTicketCliente(ticket);

        String log = salida.getAll();
        assertTrue(log.contains("ERROR"), "Debe registrarse con nivel ERROR:\n" + log);
        assertTrue(log.contains("SIN IMPRIMIR"), "Debe decir SIN IMPRIMIR para poder buscarlo:\n" + log);
        assertTrue(log.contains("Impresora que no existe 7f3a"), "Debe decir qué impresora falló:\n" + log);
    }
}
