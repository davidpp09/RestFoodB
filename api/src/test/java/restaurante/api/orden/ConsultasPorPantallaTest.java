package restaurante.api.orden;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.controller.ordenes.MesasController;
import restaurante.api.mesa.DatosRespuestaMesa;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.UsuarioRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cuántas consultas SQL dispara cada pantalla que lista órdenes.
 *
 * El problema que vigila se llama "N+1": se trae la lista (1 consulta) y luego,
 * por cada elemento, se va otra vez a la base por sus platillos (N consultas).
 * Con 6 órdenes no se nota; con las ~165 de un día completo son cientos de
 * viajes a MySQL por cada recarga, y la de entregas se recarga con cada aviso
 * del WebSocket.
 *
 * El test crea VARIAS órdenes con varios platillos y exige un tope FIJO de
 * consultas. Si alguien vuelve a meter una consulta dentro del ciclo, el número
 * crece con las órdenes y el test truena.
 *
 * Corre contra la base real (no mocks) porque lo que se mide es justo el SQL que
 * genera Hibernate. Todo va en una transacción que se revierte al final.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ConsultasPorPantallaTest {

    /** Órdenes por tipo. Mayor que el tope: con N+1 es imposible quedar debajo. */
    private static final int ORDENES = 6;
    private static final int PLATILLOS_POR_ORDEN = 2;
    /** Lista + platillos de todas las órdenes en lote + holgura para el usuario/mesa. */
    private static final int TOPE_CONSULTAS = 4;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private OrdenService ordenService;
    @Autowired private MesasController mesasController;

    private Statistics estadisticas;
    private Usuario mesera;
    private Usuario repartidor;
    private Usuario admin;
    private final List<Long> ordenesLoza = new ArrayList<>();
    private final List<Long> ordenesLlevar = new ArrayList<>();
    private final List<Long> mesas = new ArrayList<>();

    @BeforeEach
    void prepararDatos() {
        estadisticas = emf.unwrap(SessionFactory.class).getStatistics();
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        long categoria = insertar("INSERT INTO categorias (nombre, impresora, max_activos) VALUES (?, 'COCINA', 7)",
                "Prueba N+1 " + sufijo);
        List<Long> productos = new ArrayList<>();
        for (int i = 0; i < PLATILLOS_POR_ORDEN * 2; i++) {
            productos.add(insertar("""
                    INSERT INTO productos (nombre, precio_comida, precio_desayuno, disponibilidad, id_categoria, eliminado)
                    VALUES (?, 100, 90, 1, ?, 0)""", "Platillo " + sufijo + "-" + i, categoria));
        }

        long idMesera = usuario("MESERO", sufijo);
        long idRepartidor = usuario("REPARTIDOR", sufijo);
        long idAdmin = usuario("ADMIN", sufijo);

        for (int i = 0; i < ORDENES; i++) {
            long mesa = insertar("INSERT INTO mesas (numero, estado, id_usuario_asignado, activa) VALUES (?, 'OCUPADA', ?, 1)",
                    "T" + sufijo + "-" + i, idMesera);
            mesas.add(mesa);
            ordenesLoza.add(orden(idMesera, mesa, "LOZA", i + 1, ahora, productos));
            ordenesLlevar.add(orden(idRepartidor, null, "LLEVAR", i + 1, ahora, productos));
        }

        mesera = usuarioRepository.findById(idMesera).orElseThrow();
        repartidor = usuarioRepository.findById(idRepartidor).orElseThrow();
        admin = usuarioRepository.findById(idAdmin).orElseThrow();
    }

    @Test
    @DisplayName("Panel de cocina: consultas fijas, sin importar cuántas órdenes haya")
    void cocina() {
        var lista = medir(admin, ordenService::listarOrdenesCocina);
        var nuestras = lista.stream().filter(o -> contiene(o.id_orden())).toList();
        assertEquals(ORDENES * 2, nuestras.size());
        nuestras.forEach(o -> assertEquals(PLATILLOS_POR_ORDEN, o.platillos().size()));
        nuestras.forEach(o -> o.platillos().forEach(p -> assertNotNull(p.nombre_producto())));
    }

    @Test
    @DisplayName("Entregas de hoy del repartidor: consultas fijas")
    void entregasHoy() {
        var lista = medir(repartidor, ordenService::obtenerEntregasHoy);
        assertEquals(ORDENES, lista.size());
        lista.forEach(e -> assertEquals(PLATILLOS_POR_ORDEN, e.platillos().size()));
        lista.forEach(e -> assertEquals(repartidor.getNombre(), e.nombreRepartidor()));
    }

    @Test
    @DisplayName("Entregas de hoy vistas por el admin: consultas fijas")
    void entregasHoyAdmin() {
        var lista = medir(admin, ordenService::obtenerEntregasHoy);
        assertTrue(lista.stream().map(DatosEntregaHoy::id_orden).toList().containsAll(ordenesLlevar));
    }

    @Test
    @DisplayName("Comandas del día (panel admin): consultas fijas")
    void comandasDelDia() {
        var lista = medir(admin, () -> ordenService.obtenerComandasDelDia(LocalDateTime.now().toLocalDate()));
        var nuestras = lista.stream().filter(c -> contiene(c.id_orden())).toList();
        assertEquals(ORDENES * 2, nuestras.size());
        nuestras.forEach(c -> assertEquals(PLATILLOS_POR_ORDEN, c.platillos().size()));
        // El orden de la pantalla: por empleado y dentro de cada uno por número de comanda.
        var deLaMesera = nuestras.stream().filter(c -> c.id_usuario().equals(mesera.getId_usuarios()))
                .map(DatosComandaEmpleado::numero_comanda).toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6), deLaMesera);
        assertTrue(nuestras.stream().filter(c -> c.tipo() == Tipo.LOZA).allMatch(c -> c.numeroMesa() != null));
    }

    @Test
    @DisplayName("Mis comandas de la mesera: consultas fijas")
    void misComandas() {
        var lista = medir(mesera, ordenService::obtenerMisComandasDelDia);
        assertEquals(ordenesLoza, lista.stream().map(DatosComandaEmpleado::id_orden).toList());
        lista.forEach(c -> assertEquals(PLATILLOS_POR_ORDEN, c.platillos().size()));
    }

    @Test
    @DisplayName("Sala del admin (/mesas): consultas fijas aunque haya muchas mesas con cuenta abierta")
    void salaAdmin() {
        List<DatosRespuestaMesa> lista = medir(admin, () -> mesasController.listar().getBody());
        verificarMesas(lista);
    }

    @Test
    @DisplayName("Tablet de la mesera (/mesas/mias): consultas fijas")
    void mesasDeLaMesera() {
        List<DatosRespuestaMesa> lista = medir(mesera, () -> mesasController.mias(mesera).getBody());
        assertEquals(mesas, lista.stream().map(DatosRespuestaMesa::id_mesa).toList());
        verificarMesas(lista);
    }

    // ---------- utilidades ----------

    private void verificarMesas(List<DatosRespuestaMesa> lista) {
        var nuestras = lista.stream().filter(m -> mesas.contains(m.id_mesa())).toList();
        assertEquals(ORDENES, nuestras.size());
        for (DatosRespuestaMesa m : nuestras) {
            assertTrue(ordenesLoza.contains(m.id_orden()), "Cada mesa trae su orden abierta");
            assertEquals(mesera.getNombre(), m.nombre_mesero());
            assertEquals(PLATILLOS_POR_ORDEN, m.platillos().size());
            assertNotNull(m.fechaApertura());
        }
    }

    /**
     * Corre la llamada con un usuario autenticado y cuenta sus sentencias SQL.
     * El clear() vacía la caché de primer nivel: sin él, las entidades que ya
     * cargó otra llamada no se volverían a pedir y el conteo saldría engañoso.
     */
    private <T> List<T> medir(Usuario quien, Supplier<List<T>> llamada) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(quien, null, quien.getAuthorities()));
        em.flush();
        em.clear();
        estadisticas.clear();
        try {
            List<T> resultado = llamada.get();
            long consultas = estadisticas.getPrepareStatementCount();
            assertTrue(consultas <= TOPE_CONSULTAS,
                    "Se dispararon " + consultas + " consultas para " + resultado.size()
                            + " elementos (tope " + TOPE_CONSULTAS + "): hay un N+1");
            return resultado;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean contiene(Long idOrden) {
        return ordenesLoza.contains(idOrden) || ordenesLlevar.contains(idOrden);
    }

    private long usuario(String rol, String sufijo) {
        return insertar("INSERT INTO usuarios (nombre, rol, contrasena, estatus, email) VALUES (?, ?, 'x', 1, ?)",
                rol + " " + sufijo, rol, rol.toLowerCase() + "-" + sufijo + "@prueba.test");
    }

    private long orden(long usuario, Long mesa, String tipo, int comanda, Timestamp fecha, List<Long> productos) {
        long orden = insertar("""
                INSERT INTO ordenes (fecha_apertura, estatus, total, id_usuario, id_mesa, tipo, servicio, numero_comanda)
                VALUES (?, 'PREPARANDO', 0, ?, ?, ?, 'COMIDA', ?)""", fecha, usuario, mesa, tipo, comanda);
        // Productos distintos en cada orden para que la caché de Hibernate no
        // esconda un N+1 sobre productos.
        for (int p = 0; p < PLATILLOS_POR_ORDEN; p++) {
            long producto = productos.get((comanda + p) % productos.size());
            insertar("""
                    INSERT INTO orden_detalle (cantidad, precio_unitario, subtotal, id_orden, id_producto)
                    VALUES (1, 100, 100, ?, ?)""", orden, producto);
        }
        return orden;
    }

    private long insertar(String sql, Object... parametros) {
        jdbc.update(sql, parametros);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
