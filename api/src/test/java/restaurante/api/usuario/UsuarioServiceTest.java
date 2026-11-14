package restaurante.api.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.mesa.MesaService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Quién puede tocar qué cuenta.
 *
 * Regla de la casa: las cuentas DEV y el rol DEV solo los maneja otro DEV. Un
 * ADMIN administra al personal, pero no puede darse (ni darle a nadie) el rol
 * con más poder del sistema, ni tocar la cuenta de quien lo mantiene.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MesaService mesaService;

    @InjectMocks private UsuarioService servicio;

    private static Usuario usuario(long id, Roles rol) {
        return new Usuario(id, "USUARIO " + id, rol, "hash", true, "u" + id + "@prueba.test", null);
    }

    private static final Usuario DEV = usuario(1, Roles.DEV);
    private static final Usuario ADMIN = usuario(2, Roles.ADMIN);

    // ---------- actualizar ----------

    @Test
    @DisplayName("Un ADMIN no puede modificar una cuenta DEV")
    void adminNoModificaCuentaDev() {
        when(repository.findById(10L)).thenReturn(Optional.of(usuario(10, Roles.DEV)));
        var datos = new DatosActualizacionUsuario(10L, "otro", null, null, null);

        assertThrows(ValidacionException.class, () -> servicio.actualizar(datos, ADMIN));
    }

    @Test
    @DisplayName("Un ADMIN no puede darle el rol DEV a nadie")
    void adminNoAsignaRolDev() {
        when(repository.findById(10L)).thenReturn(Optional.of(usuario(10, Roles.MESERO)));
        var datos = new DatosActualizacionUsuario(10L, null, null, Roles.DEV, null);

        assertThrows(ValidacionException.class, () -> servicio.actualizar(datos, ADMIN));
    }

    @Test
    @DisplayName("Un DEV sí puede asignar el rol DEV")
    void devAsignaRolDev() {
        var mesero = usuario(10, Roles.MESERO);
        when(repository.findById(10L)).thenReturn(Optional.of(mesero));

        servicio.actualizar(new DatosActualizacionUsuario(10L, null, null, Roles.DEV, null), DEV);

        assertEquals(Roles.DEV, mesero.getRol());
    }

    @Test
    @DisplayName("Si deja de ser mesera, sus mesas quedan sin asignar")
    void dejaDeSerMesera() {
        var mesera = usuario(10, Roles.MESERO);
        when(repository.findById(10L)).thenReturn(Optional.of(mesera));

        servicio.actualizar(new DatosActualizacionUsuario(10L, null, null, Roles.REPARTIDOR, null), ADMIN);

        verify(mesaService).quitarAsignaciones(10L);
    }

    // ---------- contraseña ----------

    @Test
    @DisplayName("Un ADMIN no puede cambiar la contraseña de una cuenta DEV")
    void adminNoCambiaContrasenaDev() {
        when(repository.findById(10L)).thenReturn(Optional.of(usuario(10, Roles.DEV)));

        assertThrows(ValidacionException.class, () -> servicio.cambiarContrasena(10L, "Nueva#123", ADMIN));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("La contraseña se guarda como hash, nunca tal cual")
    void contrasenaComoHash() {
        var mesero = usuario(10, Roles.MESERO);
        when(repository.findById(10L)).thenReturn(Optional.of(mesero));
        when(passwordEncoder.encode("Nueva#123")).thenReturn("$2a$hash");

        servicio.cambiarContrasena(10L, "Nueva#123", ADMIN);

        assertEquals("$2a$hash", mesero.getContrasena());
    }

    // ---------- registrar ----------

    @Test
    @DisplayName("Al registrar, la contraseña se guarda como hash")
    void registrarConHash() {
        when(passwordEncoder.encode("Clave#123")).thenReturn("$2a$hash");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario nuevo = servicio.registrar(
                new DatosRegistroUsuario("Ana", Roles.MESERO, "Clave#123", "ana@prueba.test", 1), ADMIN);

        assertEquals("$2a$hash", nuevo.getContrasena());
    }

    // ---------- regresión: la regla DEV solo se aplicaba al actualizar ----------

    @Test
    @DisplayName("Un ADMIN no puede CREAR un usuario con rol DEV (antes sí podía)")
    void adminNoCreaUsuarioDev() {
        var datos = new DatosRegistroUsuario("Intruso", Roles.DEV, "Clave#123", "intruso@prueba.test", null);

        assertThrows(ValidacionException.class, () -> servicio.registrar(datos, ADMIN));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un DEV sí puede crear otro DEV")
    void devCreaUsuarioDev() {
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario nuevo = servicio.registrar(
                new DatosRegistroUsuario("Otro dev", Roles.DEV, "Clave#123", "dev2@prueba.test", null), DEV);

        assertEquals(Roles.DEV, nuevo.getRol());
    }

    @Test
    @DisplayName("Un ADMIN no puede dar de baja una cuenta DEV (antes sí podía)")
    void adminNoDaDeBajaDev() {
        var dev = usuario(10, Roles.DEV);
        when(repository.findById(10L)).thenReturn(Optional.of(dev));

        assertThrows(ValidacionException.class, () -> servicio.darDeBaja(10L, ADMIN));
        assertTrue(dev.getEstatus(), "La cuenta DEV debe seguir activa");
    }

    @Test
    @DisplayName("Un ADMIN no puede reactivar una cuenta DEV (antes sí podía)")
    void adminNoReactivaDev() {
        var dev = new Usuario(10L, "DEV BAJA", Roles.DEV, "hash", false, "d@prueba.test", null);
        when(repository.findById(10L)).thenReturn(Optional.of(dev));

        assertThrows(ValidacionException.class, () -> servicio.activar(10L, ADMIN));
        assertFalse(dev.getEstatus());
    }

    @Test
    @DisplayName("Un ADMIN sí puede dar de baja a una mesera, y se liberan sus mesas")
    void adminDaDeBajaMesera() {
        var mesera = usuario(10, Roles.MESERO);
        when(repository.findById(10L)).thenReturn(Optional.of(mesera));

        servicio.darDeBaja(10L, ADMIN);

        assertFalse(mesera.getEstatus());
        verify(mesaService).quitarAsignaciones(10L);
    }
}
