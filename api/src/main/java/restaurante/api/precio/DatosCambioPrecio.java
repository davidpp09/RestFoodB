package restaurante.api.precio;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DatosCambioPrecio(
        @NotNull(message = "Indica el precio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 6, fraction = 2, message = "El precio admite hasta 2 decimales")
        BigDecimal precio) {}
