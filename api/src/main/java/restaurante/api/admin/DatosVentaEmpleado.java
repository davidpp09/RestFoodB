package restaurante.api.admin;

import java.math.BigDecimal;

public record DatosVentaEmpleado(
        String nombre,
        Integer cantidad,
        BigDecimal total
) {
}
