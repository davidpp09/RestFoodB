package restaurante.api.precio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.producto.DatosActualizacionProducto;
import restaurante.api.usuario.Usuario;
import java.util.List;

@Service
public class GrupoPrecioService {
    @Autowired private GrupoPrecioRepository repository;
    @Autowired private ProductoRepository productos;
    @Autowired private HistorialPrecioService historial;

    @Transactional(readOnly = true)
    public List<DatosGrupoPrecio> listar() {
        return repository.listarConProductos().stream().map(DatosGrupoPrecio::de).toList();
    }

    @Transactional
    public DatosGrupoPrecio actualizar(String clave, DatosCambioPrecio datos, Usuario usuario) {
        var grupo = repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el grupo " + clave));
        var ids = repository.idsDeProductos(grupo.getId());
        if (ids.isEmpty()) throw new ValidacionException("El grupo no tiene productos asociados");
        // Todos los caminos bloquean productos en orden de id: evita perder cambios
        // simultáneos y registrar un precio anterior que ya no sea el real.
        for (Long id : ids) {
            var producto = productos.buscarParaEditar(id).orElseThrow();
            var anterior = grupo.getCampo().leer(producto);
            producto.actualizar(new DatosActualizacionProducto(null,
                    grupo.getCampo() == CampoPrecio.COMIDA ? datos.precio() : null,
                    grupo.getCampo() == CampoPrecio.DESAYUNO ? datos.precio() : null, null, null), null);
            historial.registrar(producto, grupo.getCampo(), anterior, usuario, "MENU");
        }
        return DatosGrupoPrecio.de(grupo);
    }
}
