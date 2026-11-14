package restaurante.api.ordenDetalle;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import restaurante.api.orden.Tipo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrdenDetalleRepository extends JpaRepository<OrdenDetalle, Long> {


    @Query("SELECT d FROM ordenDetalle d WHERE d.orden.id_ordenes = :id")
    List<OrdenDetalle> findAllByOrdenId(Long id);

    /**
     * Los platillos de VARIAS órdenes en una sola consulta, para las pantallas
     * que listan órdenes (cocina, entregas, comandas, sala).
     *
     * Evita el "N+1": antes cada pantalla traía la lista de órdenes y luego iba
     * a la base una vez por orden (y otra por cada producto para leer su nombre).
     * El JOIN FETCH trae el producto en la misma consulta, porque
     * DatosDetalleRespuesta necesita su nombre.
     *
     * El ORDER BY conserva el orden en que se capturaron los platillos, que es
     * el que ya daba findAllByOrdenId.
     *
     * Nunca llamarla con una lista vacía: `IN ()` no es SQL válido. Para eso
     * está OrdenService.detallesPorOrden.
     */
    @Query("""
            SELECT d FROM ordenDetalle d
            JOIN FETCH d.producto
            WHERE d.orden.id_ordenes IN :ids
            ORDER BY d.id_detalle
            """)
    List<OrdenDetalle> findAllByOrdenIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT COUNT(od) > 0 FROM ordenDetalle od WHERE od.producto.id_productos = :productoId")
    boolean existePorProducto(@Param("productoId") Long productoId);

    @Query("""
                SELECT COALESCE(SUM(od.cantidad), 0)
                FROM ordenDetalle od
                JOIN od.orden o
                WHERE o.fechaCierre BETWEEN :inicio AND :fin
                AND o.estatus = 'PAGADA'
                AND o.tipo = :tipo
            """)
    Long contarPlatillosPorTipo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("tipo") Tipo tipo
    );


}
