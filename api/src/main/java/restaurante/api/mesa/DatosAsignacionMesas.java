package restaurante.api.mesa;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DatosAsignacionMesas(
        Long id_usuario,          // null = dejar las mesas sin asignar
        @NotEmpty List<Long> mesas
) {
}
