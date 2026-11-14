package restaurante.api.producto;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import restaurante.api.categoria.Categoria;
import restaurante.api.categoria.CategoriaRepository;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.ordenDetalle.OrdenDetalleRepository;

import java.util.List;
import java.math.BigDecimal;
import restaurante.api.precio.*;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.Roles;

@Service
public class ProductoService {

    @Autowired private HistorialPrecioService historial;
    @Autowired private HistorialPrecioRepository historialRepository;
    @Autowired private GrupoPrecioRepository grupos;

    public record ResultadoAltaDia(DatosRespuestaProducto producto, boolean creado) {}

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private OrdenDetalleRepository ordenDetalleRepository;

    @Transactional
    public DatosRespuestaProducto registrar(DatosRegistroProducto datos, Usuario usuario) {
        Categoria categoria = categoriaRepository.findById(datos.id_categoria()).orElseThrow();
        // El nombre es único: si existe un producto con borrado suave se reutiliza ese registro
        var existente = repository.buscarPorNombreParaEditar(datos.nombre());
        if (existente.isPresent() && Boolean.TRUE.equals(existente.get().getEliminado())) {
            Producto revivido = repository.buscarParaEditar(existente.get().getId_productos()).orElseThrow();
            var antes = precios(revivido);
            revivido.restaurar(datos, categoria);
            auditar(revivido, antes, usuario, "CARTA");
            return new DatosRespuestaProducto(revivido);
        }
        Producto producto = repository.save(new Producto(datos, categoria));
        auditar(producto, new BigDecimal[]{null, null}, usuario, "CARTA");
        return new DatosRespuestaProducto(producto);
    }

    @Transactional(readOnly = true)
    public List<DatosRespuestaProducto> listar() {
        var lista = repository.findAllWithCategoria().stream().map(DatosRespuestaProducto::new).toList();
        return lista;
    }

    @Transactional
    public DatosRespuestaProducto actualizar(Long id,
                                              DatosActualizacionProducto datos, Usuario usuario) {
        Producto producto = repository.buscarParaEditar(id).orElseThrow();
        Categoria categoria = datos.id_categoria() != null
                ? categoriaRepository.findById(datos.id_categoria()).orElseThrow()
                : null;
        var antes = precios(producto);
        producto.actualizar(datos, categoria);
        auditar(producto, antes, usuario, "CARTA");
        return new DatosRespuestaProducto(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = repository.buscarParaEditar(id).orElseThrow();
        if (ordenDetalleRepository.existePorProducto(id) || historialRepository.contieneProducto(id) || grupos.contieneProducto(id)) {
            // Con ventas registradas no se puede borrar físicamente (FK protege el historial)
            producto.marcarEliminado();
        } else {
            repository.delete(producto);
        }
    }

    /**
     * Alta de un platillo del día por el repartidor.
     *
     * Es un endpoint aparte del POST /productos general a propósito: ese permite
     * elegir categoría y crearía productos en cualquier parte de la carta. Aquí
     * la categoría la resuelve el servidor, así que el permiso que se le da al
     * repartidor es exactamente el que necesita y ni uno más.
     *
     * Nace APAGADO: dar de alta y poner en el menú de hoy son dos cosas. Si
     * naciera activo se saltaría el tope de la categoría, que es justo lo que
     * cabe en el recuadro del PDF.
     */
    @Transactional
    public ResultadoAltaDia registrarDelDia(
            DatosNuevoPlatilloDia datos, Usuario usuario) {

        Categoria categoriaDia = categoriaRepository.findCategoriaDelDia()
                .orElseThrow(() -> new ValidacionException(
                        "No existe la categoría \"Comida del día\": pídele al administrador que la cree"));

        String nombre = datos.nombre().trim();

        var existente = repository.buscarPorNombreParaEditar(nombre);
        if (existente.isPresent()) {
            Producto producto = repository.buscarParaEditar(existente.get().getId_productos()).orElseThrow();
            if (!Boolean.TRUE.equals(producto.getEliminado())) {
                throw new ValidacionException("Ya existe un platillo con ese nombre: " + producto.getNombre());
            }
            if (usuario != null && usuario.getRol() == Roles.REPARTIDOR) exigirCategoriaDelDia(producto.getCategoria());
            // Estaba archivado: se revive en vez de duplicarlo, para no llenar el
            // catálogo de repetidos y no perder su historial de ventas.
            var antes = precios(producto);
            producto.restaurar(
                    new DatosRegistroProducto(nombre, datos.precio(), datos.precio(), false, categoriaDia.getId_categorias()),
                    categoriaDia);
            auditar(producto, antes, usuario, "DIA");
            return new ResultadoAltaDia(new DatosRespuestaProducto(producto), false);
        }

        var nuevo = new DatosRegistroProducto(
                nombre, datos.precio(), datos.precio(), false, categoriaDia.getId_categorias());
        Producto producto = repository.save(new Producto(nuevo, categoriaDia));
        auditar(producto, new BigDecimal[]{null, null}, usuario, "DIA");

        return new ResultadoAltaDia(new DatosRespuestaProducto(producto), true);
    }

    /**
     * Archiva un platillo del día. Borrado suave siempre: el producto puede tener
     * ventas viejas colgando y el historial no se toca.
     *
     * Solo funciona sobre la categoría del día — si no, este permiso serviría para
     * que un repartidor borrara cualquier cosa de la carta.
     */
    @Transactional
    public void archivarDelDia(Long id) {
        Producto producto = repository.buscarParaEditar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el platillo " + id));

        Categoria categoriaDia = categoriaRepository.findCategoriaDelDia()
                .orElseThrow(() -> new ValidacionException("No existe la categoría \"Comida del día\""));

        if (!categoriaDia.getId_categorias().equals(producto.getCategoria().getId_categorias())) {
            throw new ValidacionException(
                    "Desde aquí solo se archivan platillos del día, y ese es de " + producto.getCategoria().getNombre());
        }

        producto.marcarEliminado();
    }

    @Transactional
    public DatosRespuestaProducto actualizarDia(Long id,
                                                 DatosActualizacionDia datos, Usuario usuario) {
        Producto producto = repository.buscarParaEditar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el platillo " + id));
        if (usuario != null && usuario.getRol() == Roles.REPARTIDOR) exigirCategoriaDelDia(producto.getCategoria());
        if (Boolean.TRUE.equals(datos.disponibilidad()) && !producto.getDisponibilidad()) {
            Categoria categoria = producto.getCategoria();
            // El tope vive en la base (columna max_activos), no clavado aquí: el menú
            // del día admite 6 porque su recuadro en el PDF tiene 6 renglones.
            int tope = categoria.getMaxActivos() != null
                    ? categoria.getMaxActivos()
                    : Categoria.TOPE_POR_DEFECTO;
            long activos = repository.countActivosPorCategoria(categoria.getId_categorias());
            if (activos >= tope) {
                // Excepción y no un String suelto en el cuerpo: así llega como
                // {"mensaje": ...}, la forma que el frontend sabe leer.
                throw new ValidacionException("Máximo " + tope + " platillos activos en " + categoria.getNombre());
            }
        }
        var antes = precios(producto);
        producto.actualizarDia(datos);
        auditar(producto, antes, usuario, "DIA");
        return new DatosRespuestaProducto(producto);
    }

    private void exigirCategoriaDelDia(Categoria categoria) {
        var dia = categoriaRepository.findCategoriaDelDia()
                .orElseThrow(() -> new ValidacionException("No existe la categoría Comida del día"));
        if (!dia.getId_categorias().equals(categoria.getId_categorias())) {
            throw new ValidacionException("Desde aquí solo se modifican platillos del día");
        }
    }

    private BigDecimal[] precios(Producto producto) {
        return new BigDecimal[]{producto.getPrecio_desayuno(), producto.getPrecio_comida()};
    }

    private void auditar(Producto producto, BigDecimal[] antes, Usuario usuario, String origen) {
        historial.registrar(producto, CampoPrecio.DESAYUNO, antes[0], usuario, origen);
        historial.registrar(producto, CampoPrecio.COMIDA, antes[1], usuario, origen);
    }

    @Transactional
    public void desactivarDia(Long categoriaId, Usuario usuario) {
        if (usuario != null && usuario.getRol() == Roles.REPARTIDOR) {
            exigirCategoriaDelDia(categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría")));
        }
        repository.desactivarPorCategoria(categoriaId);
    }
}
