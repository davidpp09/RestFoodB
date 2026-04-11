package restaurante.api.producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto,Long> {

    @Query("SELECT p FROM producto p JOIN FETCH p.categoria")
    List<Producto> findAllWithCategoria();
}
