package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.util.UriComponentsBuilder;
import restaurante.api.producto.DatosActualizacionDia;
import restaurante.api.producto.DatosNuevoPlatilloDia;
import restaurante.api.producto.DatosActualizacionProducto;
import restaurante.api.producto.DatosRegistroProducto;
import restaurante.api.producto.DatosRespuestaProducto;
import restaurante.api.producto.ProductoService;

import java.util.List;

@RequestMapping("/productos")
@RestController
public class ProductosController {

    @Autowired
    private ProductoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosRespuestaProducto> registrar(@RequestBody @Valid DatosRegistroProducto datos, UriComponentsBuilder uriComponentsBuilder) {
        var producto = service.registrar(datos);
        return ResponseEntity.created(uriComponentsBuilder.path("/productos/{id}").buildAndExpand(producto.id()).toUri()).body(producto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'MESERO', 'REPARTIDOR')")
    public ResponseEntity<List<DatosRespuestaProducto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosRespuestaProducto> actualizar(@PathVariable Long id,
                                                              @RequestBody @Valid DatosActualizacionProducto datos) {
        return ResponseEntity.ok(service.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dia")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'REPARTIDOR')")
    public ResponseEntity<DatosRespuestaProducto> registrarDelDia(
            @RequestBody @Valid DatosNuevoPlatilloDia datos,
            UriComponentsBuilder uriComponentsBuilder) {
        var resultado = service.registrarDelDia(datos);
        if (!resultado.creado()) return ResponseEntity.ok(resultado.producto());
        return ResponseEntity.created(uriComponentsBuilder.path("/productos/{id}").buildAndExpand(resultado.producto().id()).toUri()).body(resultado.producto());
    }

    @DeleteMapping("/dia/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'REPARTIDOR')")
    public ResponseEntity<Void> archivarDelDia(@PathVariable Long id) {
        service.archivarDelDia(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/dia")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'REPARTIDOR')")
    public ResponseEntity<DatosRespuestaProducto> actualizarDia(@PathVariable Long id,
                                                                @RequestBody @Valid DatosActualizacionDia datos) {
        return ResponseEntity.ok(service.actualizarDia(id, datos));
    }

    @PutMapping("/desactivar-dia/{categoriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'REPARTIDOR')")
    public ResponseEntity<Void> desactivarDia(@PathVariable Long categoriaId) {
        service.desactivarDia(categoriaId);
        return ResponseEntity.noContent().build();
    }

}
