package restaurante.api.precio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface GrupoPrecioRepository extends JpaRepository<GrupoPrecio, Long> {
    @Query("select distinct g from GrupoPrecio g left join fetch g.productos order by g.clave")
    List<GrupoPrecio> listarConProductos();
    Optional<GrupoPrecio> findByClave(String clave);
    @Query(value = "select id_producto from grupos_precio_productos where id_grupo_precio = :id order by id_producto", nativeQuery = true)
    List<Long> idsDeProductos(Long id);
    @Query("select (count(g) > 0) from GrupoPrecio g join g.productos p where p.id_productos = :id")
    boolean contieneProducto(Long id);
}
