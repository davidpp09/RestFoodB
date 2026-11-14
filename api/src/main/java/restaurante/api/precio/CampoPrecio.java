package restaurante.api.precio;

import restaurante.api.producto.Producto;
import java.math.BigDecimal;

public enum CampoPrecio {
    DESAYUNO, COMIDA;

    public BigDecimal leer(Producto producto) {
        return this == DESAYUNO ? producto.getPrecio_desayuno() : producto.getPrecio_comida();
    }
}
