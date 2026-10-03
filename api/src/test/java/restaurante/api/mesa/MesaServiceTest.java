package restaurante.api.mesa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.orden.OrdenRepository;
import restaurante.api.usuario.Roles;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El reparto de mesas entre meseras.
 *
 * Lo que se fija: cada mesa tiene UNA mesera, solo una MESERO activa puede
 * recibir mesas, "cubrir turno" mueve todas las mesas de una a otra, y la
 * mesera sigue viendo la mesa donde tiene una cuenta abierta aunque el
 * encargado se la haya pasado a otra en pleno servicio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MesaServiceTest {

    @Mock private MesaRepository mesaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private OrdenRepository ordenRepository;

    @InjectMocks
    private MesaService servicio;

    private Usuario empleado(long id, String nombre, Roles rol, boolean activo) {
        Usuario u = Mockito.mock(Usuario.class);
        when(u.getId_usuarios()).thenReturn(id);
        when(u.getNombre()).thenReturn(nombre);
        when(u.getRol()).thenReturn(rol);
        when(u.getEstatus()).thenReturn(activo);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private Mesa mesa(long id, Usuario asignada) {
        Mesa m = new Mesa(id, String.valueOf(id), Estado.LIBRE);
        m.asignarA(asignada);
        return m;
    }

    @Test
    @DisplayName("Asignar una mesa a otra mesera se la quita a la anterior")
    void asignar_CambiaDeDuena() {
        var valeria = empleado(8, "VALERIA", Roles.MESERO, true);
        var magui = empleado(12, "MAGUI", Roles.MESERO, true);
        var m5 = mesa(5, valeria);
        when(mesaRepository.findAllById(List.of(5L))).thenReturn(List.of(m5));

        servicio.asignar(new DatosAsignacionMesas(12L, List.of(5L)));

        assertTrue(m5.estaAsignadaA(magui));
        assertFalse(m5.estaAsignadaA(valeria));
    }

    @Test
    @DisplayName("Con id_usuario null las mesas quedan sin asignar")
    void asignar_ANadie_LasDejaLibres() {
        var valeria = empleado(8, "VALERIA", Roles.MESERO, true);
        var m5 = mesa(5, valeria);
        when(mesaRepository.findAllById(List.of(5L))).thenReturn(List.of(m5));

        servicio.asignar(new DatosAsignacionMesas(null, List.of(5L)));

        assertNull(m5.getUsuarioAsignado());
    }

    @Test
    @DisplayName("No se le asignan mesas a quien no es MESERO")
    void asignar_ACocina_Falla() {
        empleado(10, "COCINA", Roles.COCINA, true);
        var m5 = mesa(5, null);
        when(mesaRepository.findAllById(any())).thenReturn(List.of(m5));

        assertThrows(ValidacionException.class,
                () -> servicio.asignar(new DatosAsignacionMesas(10L, List.of(5L))));
        assertNull(m5.getUsuarioAsignado());
    }

    @Test
    @DisplayName("No se le asignan mesas a una mesera dada de baja")
    void asignar_AMeseraInactiva_Falla() {
        empleado(9, "MARELI", Roles.MESERO, false);

        assertThrows(ValidacionException.class,
                () -> servicio.asignar(new DatosAsignacionMesas(9L, List.of(5L))));
    }

    @Test
    @DisplayName("Si una de las mesas no existe, no se asigna ninguna")
    void asignar_MesaInexistente_Falla() {
        empleado(8, "VALERIA", Roles.MESERO, true);
        when(mesaRepository.findAllById(List.of(5L, 999L))).thenReturn(List.of(mesa(5, null)));

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.asignar(new DatosAsignacionMesas(8L, List.of(5L, 999L))));
    }

    @Test
    @DisplayName("Cubrir turno pasa TODAS las mesas de una mesera a otra")
    void cubrirTurno_MueveTodas() {
        var valeria = empleado(8, "VALERIA", Roles.MESERO, true);
        var magui = empleado(12, "MAGUI", Roles.MESERO, true);
        var mesas = List.of(mesa(1, valeria), mesa(2, valeria), mesa(3, valeria));
        when(mesaRepository.findAsignadasA(8L)).thenReturn(mesas);

        int movidas = servicio.cubrirTurno(new DatosCubrirTurno(8L, 12L));

        assertEquals(3, movidas);
        mesas.forEach(m -> assertTrue(m.estaAsignadaA(magui)));
    }

    @Test
    @DisplayName("Cubrir turno consigo misma es un error, no un no-op silencioso")
    void cubrirTurno_MismaMesera_Falla() {
        empleado(8, "VALERIA", Roles.MESERO, true);
        assertThrows(ValidacionException.class, () -> servicio.cubrirTurno(new DatosCubrirTurno(8L, 8L)));
    }

    @Test
    @DisplayName("La mesera ve sus mesas y también la mesa ajena donde tiene una cuenta abierta")
    void mesasDe_IncluyeCuentaAbiertaEnMesaReasignada() {
        var valeria = empleado(8, "VALERIA", Roles.MESERO, true);
        var magui = empleado(12, "MAGUI", Roles.MESERO, true);
        var m1 = mesa(1, valeria);
        var m2 = mesa(2, valeria);
        var m7 = mesa(7, magui); // se la pasaron a Magui con la cuenta de Valeria abierta
        when(mesaRepository.findActivasAsignadasA(8L)).thenReturn(List.of(m2, m1));
        // m2 aparece en las dos listas: no debe salir duplicada
        when(ordenRepository.findMesasConOrdenActivaDe(8L)).thenReturn(List.of(m7, m2));

        var mesas = servicio.mesasDe(valeria);

        assertEquals(List.of(1L, 2L, 7L), mesas.stream().map(Mesa::getId_mesas).toList());
    }

    @Test
    @DisplayName("ADMIN y DEV ven todas las mesas activas")
    void mesasDe_Admin_VeTodas() {
        var admin = empleado(7, "ADMIN", Roles.ADMIN, true);
        var todas = List.of(mesa(1, null), mesa(2, null));
        when(mesaRepository.findActivas()).thenReturn(todas);

        assertEquals(todas, servicio.mesasDe(admin));
    }

    @Test
    @DisplayName("No se crean dos mesas con el mismo número")
    void crear_NumeroRepetido_Falla() {
        when(mesaRepository.existeNumero("12")).thenReturn(true);

        assertThrows(ValidacionException.class, () -> servicio.crear(new DatosRegistroMesa(" 12 ", null)));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una mesa nueva se crea LIBRE, activa y con su mesera")
    void crear_ConMesera() {
        var magui = empleado(12, "MAGUI", Roles.MESERO, true);
        when(mesaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mesa nueva = servicio.crear(new DatosRegistroMesa("51", 12L));

        assertEquals("51", nueva.getNumero());
        assertEquals(Estado.LIBRE, nueva.getEstado());
        assertTrue(nueva.getActiva());
        assertTrue(nueva.estaAsignadaA(magui));
    }

    @Test
    @DisplayName("Una mesa con cuenta abierta no se da de baja")
    void darDeBaja_Ocupada_Falla() {
        var ocupada = new Mesa(3L, "3", Estado.OCUPADA);
        when(mesaRepository.findById(3L)).thenReturn(Optional.of(ocupada));

        assertThrows(ValidacionException.class, () -> servicio.darDeBaja(3L));
        assertTrue(ocupada.getActiva());
    }

    @Test
    @DisplayName("Una mesa dada de baja no se puede abrir")
    void mesaInactiva_NoSeAbre() {
        var mesa = new Mesa(3L, "3", Estado.LIBRE);
        mesa.darDeBaja();

        assertThrows(ValidacionException.class, mesa::abrirMesa);
    }

    @Test
    @DisplayName("Lote 51-65: crea las 15 mesas, LIBRES y con su mesera")
    void crearLote_CreaElRango() {
        var magui = empleado(12, "MAGUI", Roles.MESERO, true);
        when(mesaRepository.numerosExistentes(any())).thenReturn(List.of());
        when(mesaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Mesa> nuevas = servicio.crearLote(new DatosRegistroLoteMesas(51, 65, 12L));

        assertEquals(15, nuevas.size());
        assertEquals("51", nuevas.get(0).getNumero());
        assertEquals("65", nuevas.get(14).getNumero());
        nuevas.forEach(m -> {
            assertEquals(Estado.LIBRE, m.getEstado());
            assertTrue(m.estaAsignadaA(magui));
        });
    }

    @Test
    @DisplayName("Lote con mesas que ya existen: no crea NINGUNA y dice cuáles chocan")
    void crearLote_ConRepetidas_TodoONada() {
        when(mesaRepository.numerosExistentes(any())).thenReturn(List.of("53", "51"));

        var error = assertThrows(ValidacionException.class,
                () -> servicio.crearLote(new DatosRegistroLoteMesas(51, 55, null)));

        assertTrue(error.getMessage().contains("51, 53"), error.getMessage());
        verify(mesaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Rango al revés (65-51) se rechaza")
    void crearLote_AlReves_Falla() {
        assertThrows(ValidacionException.class, () -> servicio.crearLote(new DatosRegistroLoteMesas(65, 51, null)));
        verify(mesaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Un dedazo (51-650) no crea 600 mesas: tope de 50 por lote")
    void crearLote_Enorme_Falla() {
        assertThrows(ValidacionException.class, () -> servicio.crearLote(new DatosRegistroLoteMesas(51, 650, null)));
        verify(mesaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Una sola mesa por lote (51-51) también vale")
    void crearLote_UnaSola() {
        when(mesaRepository.numerosExistentes(any())).thenReturn(List.of());
        when(mesaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, servicio.crearLote(new DatosRegistroLoteMesas(51, 51, null)).size());
    }
}
