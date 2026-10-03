package restaurante.api.mesa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import restaurante.api.controller.ordenes.MesasController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Quién puede tocar las mesas.
 *
 * Hasta el 2026-10 cualquier MESERO podía dar de alta mesas (el POST heredaba
 * el permiso de la clase). Ahora crear, renombrar, dar de baja y repartir mesas
 * es solo del ADMIN y DEV. La mesera solo lee las suyas.
 *
 * Mismo enfoque que PermisosProductosTest: lee la anotación, que es de donde
 * Spring saca la regla, y compara conjuntos exactos.
 */
class PermisosMesasTest {

    private static final Pattern ROL = Pattern.compile("'([A-Z_]+)'");

    private static Set<String> roles(PreAuthorize anotacion) {
        Set<String> roles = new LinkedHashSet<>();
        Matcher m = ROL.matcher(anotacion.value());
        while (m.find()) roles.add(m.group(1));
        return roles;
    }

    /** El permiso efectivo: el del método si tiene, si no el de la clase. */
    private static Set<String> rolesDe(String nombreMetodo) {
        List<Method> metodos = Arrays.stream(MesasController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nombreMetodo))
                .toList();
        assertEquals(1, metodos.size(), "Se esperaba un solo método '" + nombreMetodo + "'");

        PreAuthorize delMetodo = metodos.get(0).getAnnotation(PreAuthorize.class);
        if (delMetodo != null) return roles(delMetodo);
        PreAuthorize deClase = MesasController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(deClase, "MesasController se quedó sin @PreAuthorize de clase");
        return roles(deClase);
    }

    @Test
    @DisplayName("Crear, renombrar, dar de baja y repartir mesas: solo ADMIN y DEV")
    void gestion_SoloAdminYDev() {
        Set<String> esperado = Set.of("ADMIN", "DEV");
        for (String metodo : List.of("gestion", "registrar", "registrarLote", "renombrar", "darDeBaja",
                "reactivar", "asignar", "cubrirTurno")) {
            assertEquals(esperado, rolesDe(metodo), metodo);
        }
    }

    @Test
    @DisplayName("Ver las mesas: la mesera también")
    void lectura_IncluyeMesero() {
        Set<String> esperado = Set.of("ADMIN", "DEV", "MESERO");
        assertEquals(esperado, rolesDe("listar"));
        assertEquals(esperado, rolesDe("mias"));
    }
}
