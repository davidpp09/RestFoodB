package restaurante.api.ordenDetalle;

import jakarta.validation.constraints.PositiveOrZero;

public record DatosPlatilloLote(

        Long id_detalle,
        Long id_producto,
        @PositiveOrZero
        Integer cantidad,
        String comentarios

) {

}
