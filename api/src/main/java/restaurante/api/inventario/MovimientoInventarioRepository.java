package restaurante.api.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /** El kardex de un insumo, lo más nuevo primero. Es la pantalla de "¿qué pasó aquí?". */
    @Query("""
            SELECT m FROM movimientoInventario m
            JOIN FETCH m.insumo
            JOIN FETCH m.usuario
            WHERE m.insumo.id_insumos = :idInsumo
            ORDER BY m.fecha DESC, m.id_movimiento DESC
            """)
    List<MovimientoInventario> kardexDe(Long idInsumo);

    /**
     * El kardex de un insumo por páginas. El orden NO va en la consulta: lo pone
     * el Pageable (ya validado contra la lista blanca en InventarioService).
     *
     * Lleva su propio countQuery porque Spring tiene que saber el total para
     * calcular cuántas páginas hay, y no puede derivarlo solo de una consulta con
     * JOIN FETCH. El conteo no necesita los joins: solo cuenta renglones.
     *
     * Lo atiende el índice (id_insumo, fecha) de V9: MySQL lee solo la página
     * pedida, ya ordenada, en vez de juntar todo el historial y ordenarlo.
     */
    @Query(value = """
            SELECT m FROM movimientoInventario m
            JOIN FETCH m.insumo
            JOIN FETCH m.usuario
            WHERE m.insumo.id_insumos = :idInsumo
            """,
            countQuery = """
            SELECT COUNT(m) FROM movimientoInventario m
            WHERE m.insumo.id_insumos = :idInsumo
            """)
    Page<MovimientoInventario> kardexPaginado(@Param("idInsumo") Long idInsumo, Pageable pagina);

    @Query("""
            SELECT m FROM movimientoInventario m
            JOIN FETCH m.insumo
            JOIN FETCH m.usuario
            WHERE m.fecha >= :desde AND m.fecha < :hasta
            ORDER BY m.fecha DESC, m.id_movimiento DESC
            """)
    List<MovimientoInventario> entre(LocalDateTime desde, LocalDateTime hasta);

    boolean existsByInsumoAndTipo(Insumo insumo, TipoMovimiento tipo);

    /**
     * Las compras que traen costo: la materia prima del promedio ponderado
     * (Fase 3). Las compras sin costo quedan fuera a propósito — cuentan
     * piezas, pero no pueden opinar sobre el precio.
     */
    @Query("""
            SELECT m FROM movimientoInventario m
            JOIN FETCH m.insumo
            WHERE m.tipo = restaurante.api.inventario.TipoMovimiento.COMPRA
              AND m.costo_total IS NOT NULL
            """)
    List<MovimientoInventario> comprasConCosto();
}
