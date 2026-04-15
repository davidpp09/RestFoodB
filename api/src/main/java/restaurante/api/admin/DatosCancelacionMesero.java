package restaurante.api.admin;

import java.util.List;

public record DatosCancelacionMesero(
        String nombreMesero,
        Long totalCancelaciones,
        List<DatosProductoCancelado> productos
) {}
