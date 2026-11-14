package restaurante.api.precio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.producto.ProductoService;
import restaurante.api.producto.DatosRegistroProducto;
import restaurante.api.usuario.UsuarioRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class HistorialPrecioNuloIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductoRepository productos;
    @Autowired UsuarioRepository usuarios;
    @Autowired HistorialPrecioService historial;
    @Autowired ProductoService productoService;

    @Test void restaurarArchivadoPuedeRetirarPrecioYConservarHistorial() {
        String nombre = "Restaurado " + UUID.randomUUID();
        jdbc.update("INSERT INTO categorias(nombre,impresora,max_activos) VALUES (?, 'COCINA_1',7)", nombre);
        Long categoria = jdbc.queryForObject("SELECT id_categorias FROM categorias WHERE nombre = ?", Long.class, nombre);
        jdbc.update("INSERT INTO usuarios(nombre,rol,contrasena,estatus,email) VALUES (?, 'ADMIN','x',1,?)", nombre, nombre + "@prueba.test");
        Long usuario = jdbc.queryForObject("SELECT id_usuarios FROM usuarios WHERE email = ?", Long.class, nombre + "@prueba.test");
        jdbc.update("INSERT INTO productos(nombre,precio_comida,precio_desayuno,disponibilidad,eliminado,id_categoria) VALUES (?,105,80,0,1,?)", nombre, categoria);
        Long producto = jdbc.queryForObject("SELECT id_productos FROM productos WHERE nombre = ?", Long.class, nombre);

        assertDoesNotThrow(() -> {
            productoService.registrar(new DatosRegistroProducto(nombre, null, new BigDecimal("80.00"), true, categoria),
                    usuarios.findById(usuario).orElseThrow());
            productos.flush();
        });

        var restaurado = jdbc.queryForMap("SELECT precio_comida,precio_desayuno,eliminado FROM productos WHERE id_productos = ?", producto);
        assertNull(restaurado.get("precio_comida"));
        assertEquals(new BigDecimal("80.00"), restaurado.get("precio_desayuno"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE id_productos = ? AND eliminado = 0 AND disponibilidad = 1", Integer.class, producto));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM historial_precios WHERE id_producto = ?", Integer.class, producto));
        var registro = jdbc.queryForMap("SELECT campo,precio_anterior,precio_nuevo,id_usuario,origen FROM historial_precios WHERE id_producto = ?", producto);
        assertEquals("COMIDA", registro.get("campo"));
        assertEquals(new BigDecimal("105.00"), registro.get("precio_anterior"));
        assertNull(registro.get("precio_nuevo"));
        assertEquals(usuario.longValue(), ((Number) registro.get("id_usuario")).longValue());
        assertEquals("CARTA", registro.get("origen"));
    }

    @Test void persisteRetiradaDeImporteSinInventarUnPrecioCero() {
        String nombre = "Precio ausente " + UUID.randomUUID();
        jdbc.update("INSERT INTO categorias(nombre,impresora,max_activos) VALUES (?, 'COCINA_1',7)", nombre);
        Long categoria = jdbc.queryForObject("SELECT id_categorias FROM categorias WHERE nombre = ?", Long.class, nombre);
        jdbc.update("INSERT INTO usuarios(nombre,rol,contrasena,estatus,email) VALUES (?, 'ADMIN','x',1,?)", nombre, nombre + "@prueba.test");
        Long usuario = jdbc.queryForObject("SELECT id_usuarios FROM usuarios WHERE email = ?", Long.class, nombre + "@prueba.test");
        jdbc.update("INSERT INTO productos(nombre,precio_comida,precio_desayuno,disponibilidad,eliminado,id_categoria) VALUES (?,NULL,80,1,0,?)", nombre, categoria);
        Long producto = jdbc.queryForObject("SELECT id_productos FROM productos WHERE nombre = ?", Long.class, nombre);

        historial.registrar(productos.findById(producto).orElseThrow(), CampoPrecio.COMIDA,
                new BigDecimal("105.00"), usuarios.findById(usuario).orElseThrow(), "CARTA");

        var registro = jdbc.queryForMap("SELECT precio_anterior,precio_nuevo,id_usuario FROM historial_precios WHERE id_producto = ?", producto);
        assertEquals(new BigDecimal("105.00"), registro.get("precio_anterior"));
        assertNull(registro.get("precio_nuevo"));
        assertEquals(usuario.longValue(), ((Number) registro.get("id_usuario")).longValue());
    }
}
