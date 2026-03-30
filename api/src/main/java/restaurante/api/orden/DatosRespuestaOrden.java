package restaurante.api.orden;

import restaurante.api.ordenDetalle.DatosDetalleRespuesta;

import java.math.BigDecimal;
import java.util.List;

public record DatosRespuestaOrden(

        Long id_orden,
        BigDecimal total,
        List<DatosDetalleRespuesta> platillos

) {}
