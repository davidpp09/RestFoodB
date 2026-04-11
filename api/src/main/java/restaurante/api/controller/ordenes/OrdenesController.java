package restaurante.api.controller.ordenes;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import restaurante.api.orden.DatosAbrirOrden;
import restaurante.api.orden.DatosListaOrden;
import restaurante.api.orden.DatosRespuestaCuenta;
import restaurante.api.orden.OrdenService;

@RequestMapping("/ordenes")
@RestController
public class OrdenesController {

    @Autowired
    private OrdenService service;

    @PostMapping
    @Transactional
    public ResponseEntity<Long> abrirMesa(@RequestBody @Valid DatosAbrirOrden datos, UriComponentsBuilder uriBuilder) {
        Long id = service.abrirCuenta(datos);
        var url = uriBuilder.path("/ordenes/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(url).body(id);
    }

    @GetMapping
    public ResponseEntity<Page<DatosListaOrden>> mostrarMesas(@PageableDefault(size = 10) Pageable pagina) {
        var page = service.listar(pagina);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/activa/{id_mesa}")
    public ResponseEntity<Long> obtenerOrdenActiva(@PathVariable Long id_mesa) {
        return ResponseEntity.ok(service.obtenerOrdenActiva(id_mesa));
    }

    @PutMapping("/{id}/cerrar")
    @Transactional
    public ResponseEntity<DatosRespuestaCuenta> darCuenta(@PathVariable Long id) {  // ⬅️ CAMBIO AQUÍ
        var ticket = service.darCuenta(id);  // ⬅️ CAMBIO AQUÍ
        return ResponseEntity.ok(ticket);    // ⬅️ CAMBIO AQUÍ
    }
}