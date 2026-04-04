package restaurante.api.orden;

import java.util.List;

public record DatosTicketCocina(
        Long id_orden,
        String nombre,
        Tipo tipo,
        List<DatosPlatilloTicket> platillos
) {
}