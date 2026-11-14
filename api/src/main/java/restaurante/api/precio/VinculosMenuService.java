package restaurante.api.precio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.menu.MapaPrecios;
import restaurante.api.producto.Producto;
import java.util.List;
import java.util.Objects;

@Service
public class VinculosMenuService {
    @Autowired private MapaPrecios mapa;
    @Autowired private GrupoPrecioRepository grupos;

    /** Sincroniza los grupos del mapa con el nombre actual del platillo. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void vincular(Producto producto) {
        if (Boolean.TRUE.equals(producto.getEliminado())) return;
        var correspondientes = mapa.datos().entradas().stream()
                .filter(e -> e.productos().contains(producto.getNombre()))
                .map(MapaPrecios.Entrada::clave).distinct().toList();
        // Un nombre corregido deja de pertenecer a sus renglones anteriores.
        // Los grupos ajenos al mapa conservan sus asociaciones explícitas.
        var anteriores = mapa.datos().entradas().stream().map(MapaPrecios.Entrada::clave)
                .distinct().filter(clave -> !correspondientes.contains(clave)).toList();
        if (!anteriores.isEmpty()) grupos.desvincularDeGrupos(producto.getId_productos(), anteriores);
        correspondientes.forEach(clave ->
                        grupos.vincularSiFalta(clave, producto.getId_productos()));
    }

    public List<String> faltantes(GrupoPrecio grupo) {
        return mapa.datos().entradas().stream().filter(e -> Objects.equals(e.clave(), grupo.getClave()))
                .flatMap(e -> e.productos().stream()).distinct()
                .filter(nombre -> grupo.getProductos().stream().noneMatch(p ->
                        !Boolean.TRUE.equals(p.getEliminado()) && nombre.equals(p.getNombre())))
                .toList();
    }
}
