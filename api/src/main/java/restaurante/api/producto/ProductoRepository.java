package restaurante.api.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto,Long> {

    @Query("SELECT p FROM producto p JOIN FETCH p.categoria")
    List<Producto> findAllWithCategoria();

    @Query("SELECT COUNT(p) FROM producto p WHERE p.categoria.id_categorias = :categoriaId AND p.disponibilidad = true")
    long countActivosPorCategoria(@Param("categoriaId") Long categoriaId);

    @Modifying
    @Query("UPDATE producto p SET p.disponibilidad = false WHERE p.categoria.id_categorias = :categoriaId")
    void desactivarPorCategoria(@Param("categoriaId") Long categoriaId);
}
