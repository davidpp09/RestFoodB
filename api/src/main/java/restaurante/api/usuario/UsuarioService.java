package restaurante.api.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.mesa.MesaService;

/**
 * Alta, cambios y bajas de empleados.
 *
 * Antes esta lógica vivía en UsuariosController. Se movió aquí por dos razones:
 * el controller debe encargarse solo de HTTP (recibir, validar el formato,
 * responder), y las reglas de quién puede tocar qué cuenta son reglas de
 * negocio que se prueban mejor sin levantar la capa web.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MesaService mesaService;

    @Transactional
    public Usuario registrar(DatosRegistroUsuario datos, Usuario solicitante) {
        if (datos.rol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede crear una cuenta DEV.");
        }
        Usuario usuario = new Usuario(datos);
        // La contraseña nunca se guarda tal cual: se guarda su hash.
        usuario.setContrasena(passwordEncoder.encode(datos.contrasena()));
        return repository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(DatosActualizacionUsuario datos, Usuario solicitante) {
        Usuario usuario = buscar(datos.id_usuarios());
        if (usuario.getRol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede modificar una cuenta DEV.");
        }
        if (datos.rol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede asignar el rol DEV.");
        }
        usuario.actualizarInformacion(datos);
        // Si deja de ser mesera, sus mesas quedan libres para repartirse
        if (usuario.getRol() != Roles.MESERO) {
            mesaService.quitarAsignaciones(usuario.getId_usuarios());
        }
        return usuario;
    }

    /** Reset de contraseña por un administrador (el empleado no necesita la anterior). */
    @Transactional
    public void cambiarContrasena(Long id, String contrasena, Usuario solicitante) {
        Usuario usuario = buscar(id);
        if (usuario.getRol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede cambiar la contraseña de una cuenta DEV.");
        }
        usuario.setContrasena(passwordEncoder.encode(contrasena));
    }

    @Transactional
    public void darDeBaja(Long id, Usuario solicitante) {
        Usuario usuario = buscar(id);
        if (usuario.getRol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede dar de baja una cuenta DEV.");
        }
        usuario.eliminarUsuario(id);
        // Una mesera dada de baja no puede atender: sus mesas quedan sin asignar
        // y la pantalla de Personal las marca para repartirlas.
        mesaService.quitarAsignaciones(id);
    }

    @Transactional
    public void activar(Long id, Usuario solicitante) {
        Usuario usuario = buscar(id);
        if (usuario.getRol() == Roles.DEV) {
            exigirDev(solicitante, "Solo un usuario DEV puede reactivar una cuenta DEV.");
        }
        usuario.activarUsuario(id);
    }

    /** Borrado físico: riesgoso, puede romper FK e historial de órdenes. El permiso (solo DEV) está en el controller. */
    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    /**
     * Regla de la casa: las cuentas DEV y el rol DEV solo los maneja otro DEV.
     * Va en un solo método y se llama en TODAS las operaciones sobre cuentas:
     * cuando vivía repetida en el controller, solo `actualizar` la aplicaba, y
     * un ADMIN podía crear un DEV nuevo o dar de baja al que ya existía.
     */
    private static void exigirDev(Usuario solicitante, String mensaje) {
        if (solicitante == null || solicitante.getRol() != Roles.DEV) {
            throw new ValidacionException(mensaje);
        }
    }

    private Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
