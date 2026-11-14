package restaurante.api.precio;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import restaurante.api.producto.*;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.UsuarioRepository;
import restaurante.api.infra.errores.ValidacionException;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VinculosMenuIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired ProductoService productos;
    @Autowired GrupoPrecioService grupos;
    @Autowired UsuarioRepository usuarios;
    long categoria, grupo, pollo, cerdo;
    Usuario usuario;

    @BeforeEach void datos() {
        String sufijo = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO categorias(nombre,impresora,max_activos) VALUES (?, 'COCINA_1',7)", sufijo);
        categoria = jdbc.queryForObject("SELECT id_categorias FROM categorias WHERE nombre = ?", Long.class, sufijo);
        jdbc.update("INSERT INTO usuarios(nombre,rol,contrasena,estatus,email) VALUES ('Prueba vínculos','ADMIN','x',1,?)", sufijo + "@prueba.test");
        usuario = usuarios.findById(jdbc.queryForObject("SELECT id_usuarios FROM usuarios WHERE email = ?", Long.class, sufijo + "@prueba.test")).orElseThrow();
        grupo = jdbc.queryForObject("SELECT id_grupo_precio FROM grupos_precio WHERE clave='desayuno-01'", Long.class);
        pollo = producto("Pozole de pollo");
        cerdo = producto("Pozole de cerdo");
        jdbc.update("DELETE FROM grupos_precio_productos WHERE id_grupo_precio = ?", grupo);
        jdbc.update("INSERT INTO grupos_precio_productos VALUES (?,?)", grupo, pollo);
        em.clear();
    }

    @Test void grupoParcialNoPuedeEditarSoloParteDeLosPlatillos() {
        var datos = grupos.listar().stream().filter(g -> g.clave().equals("desayuno-01")).findFirst().orElseThrow();
        assertEquals(java.util.List.of("Pozole de cerdo"), datos.faltantes());
        BigDecimal antes = jdbc.queryForObject("SELECT precio_desayuno FROM productos WHERE id_productos = ?", BigDecimal.class, pollo);
        assertThrows(ValidacionException.class, () -> grupos.actualizar("desayuno-01", new DatosCambioPrecio(new BigDecimal("140")), usuario));
        assertEquals(antes, jdbc.queryForObject("SELECT precio_desayuno FROM productos WHERE id_productos = ?", BigDecimal.class, pollo));
    }

    @Test void restaurarUnPlatilloFaltanteLoVinculaSinDuplicar() {
        jdbc.update("UPDATE productos SET eliminado = 1 WHERE id_productos = ?", cerdo);
        productos.registrar(new DatosRegistroProducto("Pozole de cerdo", new BigDecimal("115"), new BigDecimal("115"), false, categoria), usuario);
        em.flush(); em.clear();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM grupos_precio_productos WHERE id_grupo_precio = ? AND id_producto = ?", Integer.class, grupo, cerdo));
        assertTrue(grupos.listar().stream().filter(g -> g.clave().equals("desayuno-01")).findFirst().orElseThrow().faltantes().isEmpty());
        productos.actualizar(cerdo, new DatosActualizacionProducto(null, null, new BigDecimal("115"), null, null), usuario);
        em.flush();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM grupos_precio_productos WHERE id_grupo_precio = ? AND id_producto = ?", Integer.class, grupo, cerdo));
    }

    @Test void renombrarYRecrearNoHaceQueElGrupoCambieElProductoAnterior() {
        String nombreNuevo = "Pozole especial " + UUID.randomUUID();
        productos.actualizar(pollo, new DatosActualizacionProducto(nombreNuevo, null,
                new BigDecimal("77.00"), null, null), usuario);
        productos.registrar(new DatosRegistroProducto("Pozole de pollo", new BigDecimal("115.00"),
                new BigDecimal("115.00"), false, categoria), usuario);
        productos.actualizar(cerdo, new DatosActualizacionProducto(null, null,
                new BigDecimal("115.00"), null, null), usuario);
        em.flush(); em.clear();

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM grupos_precio_productos WHERE id_grupo_precio = ? AND id_producto = ?", Integer.class, grupo, pollo));
        Long nuevoPollo = jdbc.queryForObject("SELECT id_productos FROM productos WHERE nombre = 'Pozole de pollo'", Long.class);
        assertNotEquals(pollo, nuevoPollo.longValue());
        grupos.actualizar("desayuno-01", new DatosCambioPrecio(new BigDecimal("160.00")), usuario);
        em.flush();

        assertEquals(new BigDecimal("77.00"), jdbc.queryForObject("SELECT precio_desayuno FROM productos WHERE id_productos = ?", BigDecimal.class, pollo));
        assertEquals(new BigDecimal("160.00"), jdbc.queryForObject("SELECT precio_desayuno FROM productos WHERE id_productos = ?", BigDecimal.class, nuevoPollo));
        assertEquals(new BigDecimal("160.00"), jdbc.queryForObject("SELECT precio_desayuno FROM productos WHERE id_productos = ?", BigDecimal.class, cerdo));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ? AND origen = 'MENU' AND precio_nuevo = 160", Integer.class, pollo));
    }

    private long producto(String nombre) {
        var existentes = jdbc.queryForList("SELECT id_productos FROM productos WHERE BINARY nombre = BINARY ?", Long.class, nombre);
        if (!existentes.isEmpty()) {
            jdbc.update("UPDATE productos SET eliminado=0 WHERE id_productos=?", existentes.get(0));
            return existentes.get(0);
        }
        jdbc.update("INSERT INTO productos(nombre,precio_comida,precio_desayuno,disponibilidad,eliminado,id_categoria) VALUES (?,115,115,0,0,?)", nombre, categoria);
        return jdbc.queryForObject("SELECT id_productos FROM productos WHERE BINARY nombre=BINARY ?", Long.class, nombre);
    }
}
