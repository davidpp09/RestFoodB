package restaurante.api.usuario;

import java.math.BigDecimal;

public record DatosPeriodoVentas(
        Long ordenes,
        BigDecimal total
) {
}
