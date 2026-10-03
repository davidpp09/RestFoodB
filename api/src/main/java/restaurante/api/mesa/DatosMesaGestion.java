package restaurante.api.mesa;

// Una mesa vista desde la pantalla de Personal: a quién está asignada y si está activa.
public record DatosMesaGestion(
        Long id_mesa,
        String numero,
        String estado,
        Boolean activa,
        Long id_usuario_asignado,
        String nombre_asignado
) {
    public DatosMesaGestion(Mesa mesa) {
        this(mesa.getId_mesas(),
                mesa.getNumero(),
                mesa.getEstado().name(),
                mesa.getActiva(),
                mesa.getUsuarioAsignado() != null ? mesa.getUsuarioAsignado().getId_usuarios() : null,
                mesa.getUsuarioAsignado() != null ? mesa.getUsuarioAsignado().getNombre() : null);
    }
}
