package restaurante.api.producto;

import java.math.BigDecimal;

public record DatosRespuestaProducto(
        Long id,
        String nombre,
        BigDecimal precioComida,
        BigDecimal precioDesayuno,
        Boolean disponibilidad
) {
}