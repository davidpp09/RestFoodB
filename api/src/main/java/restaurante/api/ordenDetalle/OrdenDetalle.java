package restaurante.api.ordenDetalle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.orden.Orden;
import restaurante.api.orden.Servicio;
import restaurante.api.producto.Producto;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id_detalle")
@Table(name = "orden_detalle")
@Entity(name = "ordenDetalle")
public class OrdenDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_detalle;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precio_unitario;

    @Column(nullable = false)
    private BigDecimal subtotal;

    private String comentarios;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;


    public OrdenDetalle(DatosPlatilloLote datos, Producto producto, Orden orden) {
        this.id_detalle = null;
        this.cantidad = datos.cantidad();
        this.precio_unitario = (orden.getServicio().equals(Servicio.COMIDA))
                ? producto.getPrecio_comida()
                : producto.getPrecio_desayuno();
        this.subtotal = BigDecimal.valueOf(datos.cantidad()).multiply(this.precio_unitario);
        this.comentarios = datos.comentarios();
        this.orden = orden;
        this.producto = producto;
    }

    public void actualizarPlatillo(DatosPlatilloLote datos) {
        this.cantidad = datos.cantidad();
        this.comentarios = datos.comentarios();
        this.subtotal = this.precio_unitario.multiply(BigDecimal.valueOf(this.cantidad));
    }
}
