package restaurante.api.mesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    @Query("SELECT m FROM mesa m WHERE m.id_mesas BETWEEN :inicio AND :fin")
    List<Mesa> buscarPorRango(@Param("inicio") Long inicio, @Param("fin") Long fin);

}
