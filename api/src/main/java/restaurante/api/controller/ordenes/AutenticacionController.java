package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import restaurante.api.infra.security.DatosLoginRespuesta;
import restaurante.api.infra.security.RoutingService;
import restaurante.api.infra.security.TokenService;
import restaurante.api.usuario.DatosAutenticacionUsuario;
import restaurante.api.usuario.Usuario;

@RestController
@RequestMapping("/login")
public class AutenticacionController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService token;

    @Autowired
    private RoutingService routingService;

    @PostMapping
    public ResponseEntity realizarLogin(@RequestBody @Valid DatosAutenticacionUsuario datos) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(datos.email(), datos.contrasena());
        var usuarioAutenticado = authenticationManager.authenticate(authToken);
        Usuario usuario = (Usuario) usuarioAutenticado.getPrincipal();

        String jwt = token.generarToken(usuario);
        String destino = routingService.rutaPorRol(usuario.getRol());

        return ResponseEntity.ok(new DatosLoginRespuesta(
                jwt,
                usuario.getRol().name(),
                usuario.getNombre(),
                usuario.getId_usuarios(),
                usuario.getSeccion(),
                destino
        ));
    }
}