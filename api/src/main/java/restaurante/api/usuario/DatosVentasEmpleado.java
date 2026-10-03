package restaurante.api.usuario;

// Lo que se ve en la ficha del empleado. "semana" arranca el lunes y "mes" el día 1.
public record DatosVentasEmpleado(
        DatosPeriodoVentas hoy,
        DatosPeriodoVentas semana,
        DatosPeriodoVentas mes,
        Long platillos_cancelados_mes
) {
}
