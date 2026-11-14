package restaurante.api.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record DatosRegistroProducto(

        @NotBlank String nombre,
        @Positive @Digits(integer = 6, fraction = 2, message = "El precio admite hasta 2 decimales") BigDecimal precio_comida,
        @Positive @Digits(integer = 6, fraction = 2, message = "El precio admite hasta 2 decimales") BigDecimal precio_desayuno,
        @NotNull Boolean disponibilidad,
        @NotNull Long id_categoria
) {
}
