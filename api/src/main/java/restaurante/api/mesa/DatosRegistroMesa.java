package restaurante.api.mesa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DatosRegistroMesa(
        @NotBlank @Size(max = 10) String numero,
        Long id_usuario_asignado // opcional: la mesera que la atenderá
) {
}
