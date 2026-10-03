package restaurante.api.mesa;

import jakarta.validation.constraints.NotNull;

// Pasa TODAS las mesas de una mesera a otra (p. ej. cuando una falta).
public record DatosCubrirTurno(
        @NotNull Long de,
        @NotNull Long a
) {
}
