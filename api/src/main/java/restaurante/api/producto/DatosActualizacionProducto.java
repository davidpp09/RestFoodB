package restaurante.api.producto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DatosActualizacionProducto(
        @NotNull Long id,
        String nombre,
        @Positive BigDecimal precio_comida,
        @Positive BigDecimal precio_desayuno,
        Boolean disponibilidad,
        Long id_categoria
) {}
