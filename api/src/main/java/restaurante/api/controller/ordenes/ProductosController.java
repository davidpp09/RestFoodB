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
import restaurante.api.usuario.Usuario;
import restaurante.api.precio.DatosHistorialPrecio;
import restaurante.api.precio.HistorialPrecioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RequestMapping("/productos")
@RestController
public class ProductosController {

    @Autowired
    private ProductoService service;

    @Autowired private HistorialPrecioService historial;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosRespuestaProducto> registrar(@RequestBody @Valid DatosRegistroProducto datos, UriComponentsBuilder uriComponentsBuilder, @AuthenticationPrincipal Usuario usuario) {
        var producto = service.registrar(datos, usuario);
        return ResponseEntity.created(uriComponentsBuilder.path("/productos/{id}").buildAndExpand(producto.id()).toUri()).body(producto);
    }

    @GetMapping("/{id}/historial-precios")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public List<DatosHistorialPrecio> historial(@PathVariable Long id) {
        return historial.listar(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'MESERO', 'REPARTIDOR')")
    public ResponseEntity<List<DatosRespuestaProducto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosRespuestaProducto> actualizar(@PathVariable Long id,
                                                              @RequestBody @Valid DatosActualizacionProducto datos, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.actualizar(id, datos, usuario));
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
            UriComponentsBuilder uriComponentsBuilder, @AuthenticationPrincipal Usuario usuario) {
        var resultado = service.registrarDelDia(datos, usuario);
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
                                                                @RequestBody @Valid DatosActualizacionDia datos, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.actualizarDia(id, datos, usuario));
    }

    @PutMapping("/desactivar-dia/{categoriaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'REPARTIDOR')")
    public ResponseEntity<Void> desactivarDia(@PathVariable Long categoriaId, @AuthenticationPrincipal Usuario usuario) {
        service.desactivarDia(categoriaId, usuario);
        return ResponseEntity.noContent().build();
    }

}
