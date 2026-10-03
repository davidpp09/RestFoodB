package restaurante.api.mesa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    // Lo usa el frontend anterior (rangos fijos por sección). Queda mientras las
    // tablets no recarguen la versión nueva; después se puede borrar.
    @Query("SELECT m FROM mesa m WHERE m.id_mesas BETWEEN :inicio AND :fin AND m.activa = true")
    List<Mesa> buscarPorRango(@Param("inicio") Long inicio, @Param("fin") Long fin);

    // Lock pesimista: dos meseros abriendo la misma mesa a la vez se serializan;
    // el segundo ve la mesa ya OCUPADA en vez de crear una orden duplicada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM mesa m WHERE m.id_mesas = :id")
    Optional<Mesa> findByIdConBloqueo(Long id);

    @Query("SELECT m FROM mesa m WHERE m.activa = true ORDER BY m.id_mesas")
    List<Mesa> findActivas();

    // Para la pantalla de Personal: también las dadas de baja, para poder reactivarlas.
    @Query("SELECT m FROM mesa m LEFT JOIN FETCH m.usuarioAsignado ORDER BY m.id_mesas")
    List<Mesa> findTodasConAsignacion();

    @Query("SELECT m FROM mesa m WHERE m.activa = true AND m.usuarioAsignado.id_usuarios = :idUsuario ORDER BY m.id_mesas")
    List<Mesa> findActivasAsignadasA(@Param("idUsuario") Long idUsuario);

    @Query("SELECT m FROM mesa m WHERE m.usuarioAsignado.id_usuarios = :idUsuario")
    List<Mesa> findAsignadasA(@Param("idUsuario") Long idUsuario);

    @Query("SELECT COUNT(m) > 0 FROM mesa m WHERE m.numero = :numero")
    boolean existeNumero(@Param("numero") String numero);
}
