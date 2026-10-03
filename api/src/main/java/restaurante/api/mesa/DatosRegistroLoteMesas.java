package restaurante.api.mesa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Crea las mesas desde..hasta (ambas incluidas), p. ej. 51-65.
public record DatosRegistroLoteMesas(
        @NotNull @Positive Integer desde,
        @NotNull @Positive Integer hasta,
        Long id_usuario_asignado // opcional: la mesera que atenderá todas
) {
}
