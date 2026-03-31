package restaurante.api.producto;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.categoria.Categoria;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id_productos")
@Table(name = "productos")
@Entity(name = "producto")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_productos;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(name = "precio_comida", nullable = false)
    private BigDecimal precio_comida;

    @Column(name = "precio_desayuno", nullable = false)
    private BigDecimal precio_desayuno;

    @Column(nullable = false)
    private Boolean disponibilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    public Producto(DatosRegistroProducto datosRegistroProducto) {
        this.id_productos = null;
        this.nombre = datosRegistroProducto.nombre();
        this.precio_comida = datosRegistroProducto.precio_comida();
        this.precio_desayuno = datosRegistroProducto.precio_desayuno();
        this.disponibilidad = datosRegistroProducto.disponibilidad();
        this.categoria = new Categoria(datosRegistroProducto.id_categoria());
    }

}
