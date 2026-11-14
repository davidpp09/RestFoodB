package restaurante.api.precio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record DatosHistorialPrecio(Long id, CampoPrecio campo, BigDecimal precioAnterior,
        BigDecimal precioNuevo, Long idUsuario, String usuario, OffsetDateTime fecha, String origen) {
    public DatosHistorialPrecio(HistorialPrecio h) {
        this(h.getId(), h.getCampo(), h.getPrecioAnterior(), h.getPrecioNuevo(),
                h.getUsuario().getId_usuarios(), h.getUsuario().getNombre(),
                h.getFecha().atZone(ZoneId.systemDefault()).toOffsetDateTime(), h.getOrigen());
    }
}
