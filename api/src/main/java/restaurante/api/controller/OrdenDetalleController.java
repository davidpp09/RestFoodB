package restaurante.api.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import restaurante.api.orden.OrdenService;
import restaurante.api.ordenDetalle.DatosSincronizarComanda;

@RequestMapping("/ordendetalles")
@RestController
public class OrdenDetalleController {

    @Autowired
    OrdenService ordenService;

    @PostMapping
    public void registrar(@RequestBody @Valid DatosSincronizarComanda datos){
        ordenService.enviarOrden(datos);
    }
}
