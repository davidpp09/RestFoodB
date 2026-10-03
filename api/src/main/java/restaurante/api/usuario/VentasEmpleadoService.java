package restaurante.api.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import restaurante.api.evento.EventoOrdenRepository;
import restaurante.api.evento.TipoEvento;
import restaurante.api.orden.OrdenRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Ventas de un empleado para su ficha en Personal.
 *
 * Mismo criterio que el corte del día (OrdenService.master): solo cuenta lo
 * PAGADO y por fecha de CIERRE. Las órdenes con estatus CANCELADA no se usan:
 * casi todas son diálogos de entregas abiertos y cerrados sin pedir nada. Lo
 * que sí es una cancelación real es el evento PLATILLO_CANCELADO.
 */
@Service
public class VentasEmpleadoService {

    @Autowired
    private OrdenRepository ordenRepository;
    @Autowired
    private EventoOrdenRepository eventoOrdenRepository;

    public DatosVentasEmpleado resumen(Long idUsuario, LocalDate hoy) {
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeroDeMes = hoy.withDayOfMonth(1);
        var finHoy = hoy.atTime(LocalTime.MAX);

        return new DatosVentasEmpleado(
                periodo(idUsuario, hoy, hoy),
                periodo(idUsuario, lunes, hoy),
                periodo(idUsuario, primeroDeMes, hoy),
                eventoOrdenRepository.contarPorUsuario(
                        idUsuario, TipoEvento.PLATILLO_CANCELADO, primeroDeMes.atStartOfDay(), finHoy)
        );
    }

    private DatosPeriodoVentas periodo(Long idUsuario, LocalDate desde, LocalDate hasta) {
        List<Object[]> filas = ordenRepository.resumenVentasDe(
                idUsuario, desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));
        Object[] fila = filas.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : filas.get(0);
        return new DatosPeriodoVentas(
                ((Number) fila[0]).longValue(),
                fila[1] instanceof BigDecimal bd ? bd : new BigDecimal(fila[1].toString()));
    }
}
