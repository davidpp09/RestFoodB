package restaurante.api.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import restaurante.api.controller.ordenes.UsuariosController;

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
 * La clase UsuariosController deja entrar al CAJERO. Lo nuevo de la pantalla de
 * Personal (la lista completa con los dados de baja, y lo que vendió cada quien)
 * es solo del ADMIN y DEV: si un método nuevo se queda sin @PreAuthorize propio,
 * hereda el de la clase y el CAJERO ve las ventas de todos. Este test lo atrapa.
 */
class PermisosUsuariosTest {

    private static final Pattern ROL = Pattern.compile("'([A-Z_]+)'");

    private static Set<String> rolesDe(String nombreMetodo) {
        List<Method> metodos = Arrays.stream(UsuariosController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nombreMetodo))
                .toList();
        assertEquals(1, metodos.size(), "Se esperaba un solo método '" + nombreMetodo + "'");
        PreAuthorize anotacion = metodos.get(0).getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion, nombreMetodo + " heredaría el permiso de la clase (incluye CAJERO)");
        Set<String> roles = new LinkedHashSet<>();
        Matcher m = ROL.matcher(anotacion.value());
        while (m.find()) roles.add(m.group(1));
        return roles;
    }

    @Test
    @DisplayName("Lista completa, ventas, alta, edición y bajas: solo ADMIN y DEV")
    void personal_SoloAdminYDev() {
        Set<String> esperado = Set.of("ADMIN", "DEV");
        for (String metodo : List.of("listarTodos", "ventas", "registrar", "actualizar",
                "cambiarContrasena", "eliminarLogico", "activar")) {
            assertEquals(esperado, rolesDe(metodo), metodo);
        }
    }
}
