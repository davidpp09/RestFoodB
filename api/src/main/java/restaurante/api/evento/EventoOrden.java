package restaurante.api.evento;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.orden.Orden;
import restaurante.api.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "eventos_orden")
public class EventoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    // Denormalizado para queries de análisis sin joins
    @Column(name = "id_mesa")
    private Long idMesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // Denormalizado para análisis sin joins
    @Column(name = "nombre_mesero", nullable = false)
    private String nombreMesero;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    // Campos de producto (null en eventos de apertura/cierre)
    @Column(name = "nombre_producto")
    private String nombreProducto;

    @Column(name = "cantidad_anterior")
    private Integer cantidadAnterior;

    @Column(name = "cantidad_nueva")
    private Integer cantidadNueva;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "comentarios_anterior")
    private String comentariosAnterior;

    @Column(name = "comentarios_nuevo")
    private String comentariosNuevo;

    // Constructor para eventos de mesa (apertura/cierre)
    public EventoOrden(Orden orden, Usuario usuario, TipoEvento tipoEvento) {
        this.orden = orden;
        this.idMesa = orden.getMesa() != null ? orden.getMesa().getId_mesas() : null;
        this.usuario = usuario;
        this.nombreMesero = usuario.getNombre();
        this.timestamp = LocalDateTime.now();
        this.tipoEvento = tipoEvento;
    }

    // Constructor para eventos de platillo
    public EventoOrden(Orden orden, Usuario usuario, TipoEvento tipoEvento,
                       String nombreProducto, Integer cantidadAnterior, Integer cantidadNueva,
                       BigDecimal precioUnitario, String comentariosAnterior, String comentariosNuevo) {
        this.orden = orden;
        this.idMesa = orden.getMesa() != null ? orden.getMesa().getId_mesas() : null;
        this.usuario = usuario;
        this.nombreMesero = usuario.getNombre();
        this.timestamp = LocalDateTime.now();
        this.tipoEvento = tipoEvento;
        this.nombreProducto = nombreProducto;
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
        this.precioUnitario = precioUnitario;
        this.comentariosAnterior = comentariosAnterior;
        this.comentariosNuevo = comentariosNuevo;
    }
}
