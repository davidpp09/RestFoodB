package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import restaurante.api.orden.DatosAbrirOrden;
import restaurante.api.orden.DatosListaOrden;
import restaurante.api.orden.OrdenService;


@RequestMapping("/ordenes")
@RestController
public class OrdenesController {

    @Autowired
    OrdenService service;



    @PostMapping
    public void abrirMesa(@RequestBody @Valid DatosAbrirOrden datos){
        service.abrirCuenta(datos);
    }

    @GetMapping
   public Page<DatosListaOrden> mostrarMesas(@PageableDefault(size = 10)  Pageable pagina){
       return service.listar(pagina);
    }

    @PutMapping("/{id}/cerrar")
    public ResponseEntity darCuenta(@PathVariable Long id){
        service.darCuenta(id);
        return ResponseEntity.noContent().build();
    }

}
