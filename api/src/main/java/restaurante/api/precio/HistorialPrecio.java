package restaurante.api.precio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.producto.Producto;
import restaurante.api.usuario.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "historial_precios")
@Getter
@NoArgsConstructor
public class HistorialPrecio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_producto") private Producto producto;
    @Enumerated(EnumType.STRING) @Column(length = 10) private CampoPrecio campo;
    @Column(name = "precio_anterior", precision = 10, scale = 2) private BigDecimal precioAnterior;
    @Column(name = "precio_nuevo", precision = 10, scale = 2) private BigDecimal precioNuevo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_usuario") private Usuario usuario;
    private LocalDateTime fecha;
    private String origen;

    public HistorialPrecio(Producto producto, CampoPrecio campo, BigDecimal anterior,
                           BigDecimal nuevo, Usuario usuario, String origen) {
        this.producto = producto;
        this.campo = campo;
        this.precioAnterior = anterior;
        this.precioNuevo = nuevo;
        this.usuario = usuario;
        this.origen = origen;
        this.fecha = LocalDateTime.now(ZoneOffset.UTC);
    }
}
