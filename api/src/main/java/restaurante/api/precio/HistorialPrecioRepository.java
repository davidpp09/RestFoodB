package restaurante.api.precio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {
    @Query("select h from HistorialPrecio h join fetch h.usuario where h.producto.id_productos = :id order by h.fecha desc, h.id desc")
    List<HistorialPrecio> listarDeProducto(Long id);
    @Query("select (count(h) > 0) from HistorialPrecio h where h.producto.id_productos = :id")
    boolean contieneProducto(Long id);
}
