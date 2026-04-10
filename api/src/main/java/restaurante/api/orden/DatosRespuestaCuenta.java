package restaurante.api.orden;

import restaurante.api.ordenDetalle.DatosDetalleRespuesta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DatosRespuestaCuenta(
        Long id_orden,
        Long numeroMesa,
        String tipoOrden,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        List<DatosDetalleRespuesta> platillos,
        BigDecimal total,
        String estatus
) {
}