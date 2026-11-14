package restaurante.api.precio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.infra.errores.RecursoNoEncontradoException;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.producto.DatosActualizacionProducto;
import restaurante.api.usuario.Usuario;
import java.util.List;

@Service
public class GrupoPrecioService {
    @Autowired private GrupoPrecioRepository repository;
    @Autowired private ProductoRepository productos;
    @Autowired private HistorialPrecioService historial;
    @Autowired private VinculosMenuService vinculos;

    @Transactional(readOnly = true)
    public List<DatosGrupoPrecio> listar() {
        return repository.listarConProductos().stream().map(this::datosDe).toList();
    }

    @Transactional
    public DatosGrupoPrecio actualizar(String clave, DatosCambioPrecio datos, Usuario usuario) {
        var grupo = repository.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el grupo " + clave));
        var ids = repository.idsDeProductos(grupo.getId());
        if (ids.isEmpty()) throw new ValidacionException("El grupo no tiene productos asociados");
        // Todos los caminos bloquean productos en orden de id: evita perder cambios
        // simultáneos y registrar un precio anterior que ya no sea el real.
        var bloqueados = ids.stream().map(id -> productos.buscarParaEditar(id).orElseThrow()).toList();
        var faltantes = vinculos.faltantes(grupo);
        if (!faltantes.isEmpty()) throw new ValidacionException("Faltan platillos del grupo: " + String.join(", ", faltantes));
        for (var producto : bloqueados) {
            var anterior = grupo.getCampo().leer(producto);
            producto.actualizar(new DatosActualizacionProducto(null,
                    grupo.getCampo() == CampoPrecio.COMIDA ? datos.precio() : null,
                    grupo.getCampo() == CampoPrecio.DESAYUNO ? datos.precio() : null, null, null), null);
            historial.registrar(producto, grupo.getCampo(), anterior, usuario, "MENU");
        }
        return datosDe(grupo);
    }
    private DatosGrupoPrecio datosDe(GrupoPrecio grupo) {
        var datos = DatosGrupoPrecio.de(grupo);
        return new DatosGrupoPrecio(datos.clave(), datos.nombre(), datos.campo(), datos.precio(),
                datos.mixto(), datos.productos(), vinculos.faltantes(grupo));
    }
}
