package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import restaurante.api.usuario.*;

import java.net.URI;

@RequestMapping("/usuarios")
@RestController
public class UsuariosController {

    @Autowired
    private UsuarioRepository repository;

    @PostMapping
    @Transactional
    public ResponseEntity<DatosRespuestaUsuario> registrar(@RequestBody @Valid DatosRegistroUsuario datosRegistroUsuario, UriComponentsBuilder uriComponentsBuilder) {
        Usuario usuario = repository.save(new Usuario(datosRegistroUsuario));
        DatosRespuestaUsuario datosRespuesta = new DatosRespuestaUsuario(
                usuario.getId_usuarios(),
                usuario.getNombre(),
                usuario.getRol().toString(),
                usuario.getEmail(),
                usuario.getEstatus()
        );
        URI url = uriComponentsBuilder.path("/usuarios/{id}").buildAndExpand(usuario.getId_usuarios()).toUri();
        return ResponseEntity.created(url).body(datosRespuesta);
    }

    @GetMapping
    public ResponseEntity<Page<DatosListaUsuario>> listar(@PageableDefault(size = 10, sort = {"nombre"}) Pageable pagina) {
        var page = repository.findAllByEstatusTrue(pagina).map(DatosListaUsuario::new);
        return ResponseEntity.ok(page);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<DatosRespuestaUsuario> actualizar(@RequestBody @Valid DatosActualizacionUsuario datos) {
        var usuario = repository.getReferenceById(datos.id_usuarios());
        usuario.actualizarInformacion(datos);
        return ResponseEntity.ok(new DatosRespuestaUsuario(
                usuario.getId_usuarios(),
                usuario.getNombre(),
                usuario.getRol().toString(),
                usuario.getEmail(),
                usuario.getEstatus()
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity eliminarLogico(@PathVariable Long id) {
        var usuario = repository.getReferenceById(id);
        usuario.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/activar/{id}")
    @Transactional
    public ResponseEntity activar(@PathVariable Long id) {
        var usuario = repository.getReferenceById(id);
        usuario.activarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/eliminar/{id}")
    @Transactional
    public ResponseEntity eliminar(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}