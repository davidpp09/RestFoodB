package restaurante.api.controller.ordenes;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import restaurante.api.producto.DatosRegistroProducto;
import restaurante.api.producto.DatosRespuestaProducto;
import restaurante.api.producto.Producto;
import restaurante.api.producto.ProductoRepository;

import java.net.URI;

@RequestMapping("/productos")
@RestController
public class ProductosController {

    @Autowired
    private ProductoRepository repository;

    @PostMapping
    @Transactional
    public ResponseEntity<DatosRespuestaProducto> registrar(@RequestBody @Valid DatosRegistroProducto datosRegistroProducto, UriComponentsBuilder uriComponentsBuilder) {
        Producto producto = repository.save(new Producto(datosRegistroProducto));
        DatosRespuestaProducto datosRespuesta = new DatosRespuestaProducto(
                producto.getId_productos(),
                producto.getNombre(),
                producto.getPrecio_comida(),
                producto.getPrecio_desayuno(),
                producto.getDisponibilidad()
        );
        URI url = uriComponentsBuilder.path("/productos/{id}").buildAndExpand(producto.getId_productos()).toUri();
        return ResponseEntity.created(url).body(datosRespuesta);
    }
}