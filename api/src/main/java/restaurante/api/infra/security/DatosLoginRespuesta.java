package restaurante.api.infra.security;

public record DatosLoginRespuesta(
        String jwTtoken,
        String rol,
        String nombre,
        Long id_usuarios,
        Integer seccion,
        String destino
) {
}
