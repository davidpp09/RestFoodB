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
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT IGNORE INTO grupos_precio_productos(id_grupo_precio,id_producto) SELECT id_grupo_precio,:id FROM grupos_precio WHERE clave = :clave", nativeQuery = true)
    void vincularSiFalta(String clave, Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE gp FROM grupos_precio_productos gp JOIN grupos_precio g ON g.id_grupo_precio = gp.id_grupo_precio WHERE gp.id_producto = :id AND g.clave IN (:claves)", nativeQuery = true)
    void desvincularDeGrupos(Long id, List<String> claves);

    @Query("select (count(g) > 0) from GrupoPrecio g join g.productos p where p.id_productos = :id")
    boolean contieneProducto(Long id);
}
