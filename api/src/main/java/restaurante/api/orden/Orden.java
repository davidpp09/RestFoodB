package restaurante.api.orden;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.mesa.Mesa;
import restaurante.api.ordenDetalle.OrdenDetalle;
import restaurante.api.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id_ordenes")
@Table(name = "ordenes")
@Entity(name = "orden")
public class Orden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_ordenes;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fecha_apertura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estatus estatus;

    @Column(nullable = false)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mesa")
    private Mesa mesa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Servicio servicio;


    public Orden(Long aLong) {
        this.id_ordenes = aLong;
    }

    public Orden(Mesa mesa, Usuario usuario, Tipo tipo, Servicio servicio) {
        this.id_ordenes = null;
        this.fecha_apertura = LocalDateTime.now();
        this.estatus = Estatus.PREPARANDO;
        this.total = BigDecimal.ZERO;
        this.usuario = usuario;
        this.mesa = mesa;
        this.tipo = tipo;
        this.fechaCierre = null;
        this.servicio = servicio;
    }

    public void recalcularTotal(List<OrdenDetalle> detalles) {
        this.total = BigDecimal.ZERO;

        for (OrdenDetalle platillo : detalles) {
            this.total = this.total.add(platillo.getSubtotal());
        }
    }

    public void finalizar() {
        this.estatus = Estatus.PAGADA;
        this.fechaCierre = LocalDateTime.now();
    }


}
