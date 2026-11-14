package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import restaurante.api.precio.*;
import restaurante.api.usuario.Usuario;
import java.util.List;

@RestController
@RequestMapping("/grupos-precio")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
public class GruposPrecioController {
    @Autowired private GrupoPrecioService service;

    @GetMapping
    public List<DatosGrupoPrecio> listar() { return service.listar(); }

    @PatchMapping("/{clave}")
    public DatosGrupoPrecio actualizar(@PathVariable String clave, @RequestBody @Valid DatosCambioPrecio datos,
                                      @AuthenticationPrincipal Usuario usuario) {
        return service.actualizar(clave, datos, usuario);
    }
}
