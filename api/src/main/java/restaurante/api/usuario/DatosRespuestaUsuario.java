package restaurante.api.usuario;

public record DatosRespuestaUsuario(
        Long id,
        String nombre,
        String rol,
        String email,
        Boolean estatus
) {
}