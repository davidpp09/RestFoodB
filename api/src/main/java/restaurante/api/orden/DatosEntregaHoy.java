package restaurante.api.orden;

import restaurante.api.ordenDetalle.DatosDetalleRespuesta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DatosEntregaHoy(
        Long id_orden,
        LocalDateTime fechaApertura,
        Estatus estatus,
        BigDecimal total,
        List<DatosDetalleRespuesta> platillos
) {}
