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
        @Positive
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
                orden.getMesa().getId_mesas(),
                orden.getTipo(),
                orden.getFechaCierre(),
                orden.getServicio());
    }
}
