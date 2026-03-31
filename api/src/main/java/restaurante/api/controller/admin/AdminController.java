package restaurante.api.controller.admin;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import restaurante.api.admin.DatosCorteDia;
import restaurante.api.orden.DatosRespuestaOrden;
import restaurante.api.orden.OrdenService;

@RequestMapping("/admin")
@RestController
public class AdminController {

    @Autowired
    OrdenService ordenService;

    @GetMapping
    public DatosCorteDia corteDia(){
        return ordenService.master();
    }
}
