package restaurante.api.ordenDetalle;

import java.math.BigDecimal;

public record DatosDetalleRespuesta(
        Long id_detalle,
        Integer cantidad,
        BigDecimal precio_unitario,
        BigDecimal subtotal,
        String comentarios,
        Long id_orden,
        Long id_producto
) {
    public DatosDetalleRespuesta(OrdenDetalle ordenDetalle){
        this(
                ordenDetalle.getId_detalle(),
                ordenDetalle.getCantidad(),
                ordenDetalle.getPrecio_unitario(),
                ordenDetalle.getSubtotal(),
                ordenDetalle.getComentarios(),
                ordenDetalle.getOrden().getId_ordenes(),
                ordenDetalle.getProducto().getId_productos()
        );

    }
}
