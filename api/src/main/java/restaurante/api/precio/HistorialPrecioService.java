package restaurante.api.precio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.producto.Producto;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.usuario.Usuario;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class HistorialPrecioService {
    @Autowired private HistorialPrecioRepository repository;
    @Autowired private ProductoRepository productos;

    // MANDATORY: el precio y su auditoría se confirman o revierten juntos.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void registrar(Producto producto, CampoPrecio campo, BigDecimal anterior, Usuario usuario, String origen) {
        BigDecimal nuevo = campo.leer(producto);
        if (nuevo == null && anterior == null) return;
        if (anterior == null || nuevo == null || anterior.compareTo(nuevo) != 0) {
            repository.save(new HistorialPrecio(producto, campo, anterior, nuevo, usuario, origen));
        }
    }

    @Transactional(readOnly = true)
    public List<DatosHistorialPrecio> listar(Long id) {
        if (!productos.existsById(id)) throw new RecursoNoEncontradoException("No existe el platillo " + id);
        return repository.listarDeProducto(id).stream().map(DatosHistorialPrecio::new).toList();
    }
}
