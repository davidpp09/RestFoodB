package restaurante.api.producto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Cambio rápido de un platillo del día: el precio, prenderlo/apagarlo, o ambos.
 * Los dos son opcionales (null = no tocar ese campo).
 *
 * Antes no tenía validación: un precio negativo o con diez decimales entraba
 * directo a la carta. El frontend ya lo impedía, pero quien tenga el rol puede
 * mandar la petición a mano; la regla de verdad vive aquí.
 */
public record DatosActualizacionDia(
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 6, fraction = 2, message = "El precio admite hasta 2 decimales")
        BigDecimal precio_comida,
        Boolean disponibilidad
) {}
