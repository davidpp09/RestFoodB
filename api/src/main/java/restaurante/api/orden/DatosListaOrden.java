package restaurante.api.orden;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DatosListaOrden(

        @NotNull @PastOrPresent
        LocalDateTime fecha_apertura,
        @NotNull
        Estatus estatus,
        @NotNull @PositiveOrZero
        BigDecimal total,
        @NotNull @Positive
        Long id_usuario,
        Long id_mesa,
        @NotNull
        Tipo tipo,
        @PastOrPresent
        LocalDateTime fechaCierre,
        @NotNull
        Servicio servicio
) {
    public DatosListaOrden(Orden orden) {
        this(
                orden.getFecha_apertura(),
                orden.getEstatus(),
                orden.getTotal(),
                orden.getUsuario().getId_usuarios(),
                orden.getMesa() != null ? orden.getMesa().getId_mesas() : null, // ✅ null safe
                orden.getTipo(),
                orden.getFechaCierre(),
                orden.getServicio());
    }
}
