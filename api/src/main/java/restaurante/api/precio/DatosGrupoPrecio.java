package restaurante.api.precio;

import java.math.BigDecimal;
import java.util.List;

public record DatosGrupoPrecio(String clave, String nombre, CampoPrecio campo,
                               BigDecimal precio, boolean mixto, List<ProductoPrecio> productos) {
    public record ProductoPrecio(Long id, String nombre, BigDecimal precio) {}
    public static DatosGrupoPrecio de(GrupoPrecio grupo) {
        var productos = grupo.getProductos().stream().map(p ->
                new ProductoPrecio(p.getId_productos(), p.getNombre(), grupo.getCampo().leer(p))).toList();
        var primero = productos.isEmpty() ? null : productos.get(0).precio();
        boolean mixto = productos.stream().anyMatch(p -> (p.precio() == null || primero == null) ? p.precio() != primero : p.precio().compareTo(primero) != 0);
        return new DatosGrupoPrecio(grupo.getClave(), grupo.getNombre(), grupo.getCampo(),
                mixto ? null : primero, mixto, productos);
    }
}
