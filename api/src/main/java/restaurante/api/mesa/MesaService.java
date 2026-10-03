package restaurante.api.mesa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.orden.OrdenRepository;
import restaurante.api.usuario.Roles;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.UsuarioRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alta, baja y reparto de mesas entre las meseras.
 *
 * Regla central: cada mesa tiene UNA mesera. Asignarla a otra se la quita a la
 * anterior, y OrdenService.abrirCuenta no deja a una mesera abrir una mesa que
 * no es suya.
 */
@Service
public class MesaService {

    @Autowired
    private MesaRepository mesaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private OrdenRepository ordenRepository;

    private static boolean esSuperUsuario(Usuario usuario) {
        return usuario.getRol() == Roles.ADMIN || usuario.getRol() == Roles.DEV;
    }

    /**
     * Las mesas que ve una mesera en su tablet: las que tiene asignadas, MÁS las
     * que tienen una cuenta abierta por ella. Lo segundo importa cuando el
     * encargado reasigna en pleno servicio: la mesera debe poder cobrar la cuenta
     * que ya abrió, aunque la mesa ya sea de otra.
     */
    public List<Mesa> mesasDe(Usuario usuario) {
        if (esSuperUsuario(usuario)) {
            return mesaRepository.findActivas();
        }
        Map<Long, Mesa> porId = new LinkedHashMap<>();
        mesaRepository.findActivasAsignadasA(usuario.getId_usuarios())
                .forEach(m -> porId.put(m.getId_mesas(), m));
        ordenRepository.findMesasConOrdenActivaDe(usuario.getId_usuarios())
                .forEach(m -> porId.putIfAbsent(m.getId_mesas(), m));
        return porId.values().stream()
                .sorted(Comparator.comparing(Mesa::getId_mesas))
                .toList();
    }

    @Transactional
    public Mesa crear(DatosRegistroMesa datos) {
        String numero = datos.numero().trim();
        if (mesaRepository.existeNumero(numero)) {
            throw new ValidacionException("Ya existe una mesa con el número " + numero + ".");
        }
        Usuario mesera = datos.id_usuario_asignado() != null ? meseraActiva(datos.id_usuario_asignado()) : null;
        return mesaRepository.save(new Mesa(numero, mesera));
    }

    @Transactional
    public Mesa renombrar(Long idMesa, DatosActualizacionMesa datos) {
        Mesa mesa = buscar(idMesa);
        String numero = datos.numero().trim();
        if (!numero.equals(mesa.getNumero()) && mesaRepository.existeNumero(numero)) {
            throw new ValidacionException("Ya existe una mesa con el número " + numero + ".");
        }
        mesa.renombrar(numero);
        return mesa;
    }

    @Transactional
    public Mesa darDeBaja(Long idMesa) {
        Mesa mesa = buscar(idMesa);
        mesa.darDeBaja();
        return mesa;
    }

    @Transactional
    public Mesa reactivar(Long idMesa) {
        Mesa mesa = buscar(idMesa);
        mesa.reactivar();
        return mesa;
    }

    /** Asigna las mesas indicadas a una mesera (o las deja libres si idUsuario es null). */
    @Transactional
    public List<Mesa> asignar(DatosAsignacionMesas datos) {
        Usuario mesera = datos.id_usuario() != null ? meseraActiva(datos.id_usuario()) : null;
        List<Mesa> mesas = mesaRepository.findAllById(datos.mesas());
        if (mesas.size() != datos.mesas().stream().distinct().count()) {
            throw new RecursoNoEncontradoException("Alguna de las mesas indicadas no existe.");
        }
        mesas.forEach(m -> m.asignarA(mesera));
        return mesas;
    }

    /** Pasa todas las mesas de una mesera a otra. Devuelve cuántas se movieron. */
    @Transactional
    public int cubrirTurno(DatosCubrirTurno datos) {
        if (datos.de().equals(datos.a())) {
            throw new ValidacionException("Elige a otra mesera para cubrir el turno.");
        }
        Usuario quienCubre = meseraActiva(datos.a());
        List<Mesa> mesas = mesaRepository.findAsignadasA(datos.de());
        mesas.forEach(m -> m.asignarA(quienCubre));
        return mesas.size();
    }

    /** Al dar de baja a una mesera o cambiarle el rol, sus mesas quedan sin asignar. */
    @Transactional
    public void quitarAsignaciones(Long idUsuario) {
        mesaRepository.findAsignadasA(idUsuario).forEach(m -> m.asignarA(null));
    }

    private Mesa buscar(Long idMesa) {
        return mesaRepository.findById(idMesa)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mesa no encontrada"));
    }

    private Usuario meseraActiva(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado"));
        if (usuario.getRol() != Roles.MESERO) {
            throw new ValidacionException("Solo se le pueden asignar mesas a una mesera (rol MESERO).");
        }
        if (!Boolean.TRUE.equals(usuario.getEstatus())) {
            throw new ValidacionException(usuario.getNombre() + " está dada de baja; reactívala antes de asignarle mesas.");
        }
        return usuario;
    }
}
