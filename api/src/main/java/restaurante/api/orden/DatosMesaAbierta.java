package restaurante.api.orden;

import restaurante.api.mesa.Estado;

public record DatosMesaAbierta(
        Long id_mesa,
        Estado estado,
        String nombre_mesero,
        Long id_orden
) {
}