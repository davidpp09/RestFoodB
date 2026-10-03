package restaurante.api.mesa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosActualizacionMesa(
        @NotBlank @Size(max = 10) String numero
) {
}
