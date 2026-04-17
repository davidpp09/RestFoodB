package restaurante.api.infra.security;

import org.springframework.stereotype.Service;
import restaurante.api.usuario.Roles;

import java.util.Map;

@Service
public class RoutingService {

    private static final Map<Roles, String> RUTAS = Map.of(
            Roles.DEV,        "/admin",
            Roles.ADMIN,      "/admin",
            Roles.CAJERO,     "/admin",
            Roles.MESERO,     "/mesero",
            Roles.COCINA,     "/cocina-panel",
            Roles.REPARTIDOR, "/entregas"
    );

    public String rutaPorRol(Roles rol) {
        return RUTAS.getOrDefault(rol, "/login");
    }
}
