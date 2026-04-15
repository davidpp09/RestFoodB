package restaurante.api.evento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoOrdenRepository extends JpaRepository<EventoOrden, Long> {

    List<EventoOrden> findByTipoEventoAndTimestampBetween(
            TipoEvento tipoEvento, LocalDateTime inicio, LocalDateTime fin);
}
