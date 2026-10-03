package restaurante.api.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import restaurante.api.evento.EventoOrdenRepository;
import restaurante.api.evento.TipoEvento;
import restaurante.api.orden.OrdenRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Los periodos de la ficha del empleado: si "semana" o "mes" arrancan en el día
 * equivocado, los números se ven razonables y nadie nota que están mal.
 */
@ExtendWith(MockitoExtension.class)
class VentasEmpleadoServiceTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private EventoOrdenRepository eventoOrdenRepository;

    @InjectMocks
    private VentasEmpleadoService servicio;

    private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 7);
    private static final LocalDateTime FIN = MIERCOLES.atTime(LocalTime.MAX);

    private static List<Object[]> fila(long ordenes, String total) {
        return Collections.singletonList(new Object[]{ordenes, new BigDecimal(total)});
    }

    @Test
    @DisplayName("Hoy, semana desde el lunes y mes desde el día 1")
    void periodos() {
        when(ordenRepository.resumenVentasDe(8L, MIERCOLES.atStartOfDay(), FIN)).thenReturn(fila(12, "3400.00"));
        when(ordenRepository.resumenVentasDe(8L, LocalDate.of(2026, 10, 5).atStartOfDay(), FIN)).thenReturn(fila(40, "11000.50"));
        when(ordenRepository.resumenVentasDe(8L, LocalDate.of(2026, 10, 1).atStartOfDay(), FIN)).thenReturn(fila(70, "19999.99"));
        when(eventoOrdenRepository.contarPorUsuario(eq(8L), eq(TipoEvento.PLATILLO_CANCELADO),
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()), eq(FIN))).thenReturn(3L);

        var r = servicio.resumen(8L, MIERCOLES);

        assertEquals(12L, r.hoy().ordenes());
        assertEquals(new BigDecimal("3400.00"), r.hoy().total());
        assertEquals(40L, r.semana().ordenes());
        assertEquals(70L, r.mes().ordenes());
        assertEquals(new BigDecimal("19999.99"), r.mes().total());
        assertEquals(3L, r.platillos_cancelados_mes());
    }

    @Test
    @DisplayName("El lunes, la semana es solo ese día")
    void elLunesLaSemanaEmpiezaHoy() {
        LocalDate lunes = LocalDate.of(2026, 10, 5);
        when(ordenRepository.resumenVentasDe(eq(8L), any(), any())).thenReturn(fila(0, "0"));
        when(ordenRepository.resumenVentasDe(8L, lunes.atStartOfDay(), lunes.atTime(LocalTime.MAX))).thenReturn(fila(5, "900"));

        var r = servicio.resumen(8L, lunes);

        assertEquals(5L, r.hoy().ordenes());
        assertEquals(5L, r.semana().ordenes());
    }
}
