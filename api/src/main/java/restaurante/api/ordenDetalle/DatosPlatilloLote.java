package restaurante.api.ordenDetalle;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DatosPlatilloLote(

        @Positive
        Long id_detalle,
        @NotNull @Positive
        Long id_producto,
        @NotNull @Positive
        Integer cantidad,
        String comentarios
) {

}
