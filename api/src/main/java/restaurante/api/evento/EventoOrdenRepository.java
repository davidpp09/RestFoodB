package restaurante.api.evento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoOrdenRepository extends JpaRepository<EventoOrden, Long> {

    List<EventoOrden> findByTipoEventoAndTimestampBetween(
            TipoEvento tipoEvento, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COUNT(e) FROM EventoOrden e WHERE e.usuario.id_usuarios = :idUsuario AND e.tipoEvento = :tipo AND e.timestamp BETWEEN :inicio AND :fin")
    Long contarPorUsuario(@Param("idUsuario") Long idUsuario,
                          @Param("tipo") TipoEvento tipo,
                          @Param("inicio") LocalDateTime inicio,
                          @Param("fin") LocalDateTime fin);
}
