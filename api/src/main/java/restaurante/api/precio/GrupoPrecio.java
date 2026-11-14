package restaurante.api.precio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.producto.Producto;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grupos_precio")
@Getter
@NoArgsConstructor
public class GrupoPrecio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_precio")
    private Long id;
    private String clave;
    private String nombre;
    @Enumerated(EnumType.STRING) @Column(length = 10)
    private CampoPrecio campo;
    @ManyToMany
    @JoinTable(name = "grupos_precio_productos",
            joinColumns = @JoinColumn(name = "id_grupo_precio"),
            inverseJoinColumns = @JoinColumn(name = "id_producto"))
    @OrderBy("id_productos ASC")
    private List<Producto> productos = new ArrayList<>();
}
