package restaurante.api.controller.ordenes;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import restaurante.api.mesa.*;
import restaurante.api.orden.OrdenRepository;
import restaurante.api.ordenDetalle.DatosDetalleRespuesta;
import restaurante.api.ordenDetalle.OrdenDetalleRepository;
import restaurante.api.usuario.Usuario;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RequestMapping("/mesas")
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'MESERO')")
public class MesasController {

    @Autowired
    private MesaRepository repository;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private OrdenDetalleRepository ordenDetalleRepository;

    // ---------- Lo que ven las tablets ----------

    // Vista de sala del ADMIN: todas las mesas activas con su cuenta abierta.
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatosRespuestaMesa>> listar() {
        return ResponseEntity.ok(repository.findActivas().stream().map(this::conOrdenActiva).toList());
    }

    // Tablet de la mesera: solo sus mesas (ver MesaService.mesasDe).
    @GetMapping("/mias")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatosRespuestaMesa>> mias(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(mesaService.mesasDe(usuario).stream().map(this::conOrdenActiva).toList());
    }

    // Lo usa el frontend anterior a /mias. Se puede borrar cuando todas las tablets
    // hayan recargado la versión nueva.
    @GetMapping("/rango/{inicio}/{fin}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatosRespuestaMesa>> mesasRango(@PathVariable long inicio, @PathVariable long fin) {
        return ResponseEntity.ok(repository.buscarPorRango(inicio, fin).stream().map(this::conOrdenActiva).toList());
    }

    // ---------- Gestión (pantalla de Personal) ----------

    @GetMapping("/gestion")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<List<DatosMesaGestion>> gestion() {
        return ResponseEntity.ok(repository.findTodasConAsignacion().stream().map(DatosMesaGestion::new).toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosMesaGestion> registrar(@RequestBody @Valid DatosRegistroMesa datos, UriComponentsBuilder uriComponentsBuilder) {
        Mesa mesa = mesaService.crear(datos);
        URI url = uriComponentsBuilder.path("/mesas/{id}").buildAndExpand(mesa.getId_mesas()).toUri();
        return ResponseEntity.created(url).body(new DatosMesaGestion(mesa));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosMesaGestion> renombrar(@PathVariable Long id, @RequestBody @Valid DatosActualizacionMesa datos) {
        return ResponseEntity.ok(new DatosMesaGestion(mesaService.renombrar(id, datos)));
    }

    // Baja lógica: la mesa conserva su historial de órdenes.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosMesaGestion> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(new DatosMesaGestion(mesaService.darDeBaja(id)));
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<DatosMesaGestion> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(new DatosMesaGestion(mesaService.reactivar(id)));
    }

    @PutMapping("/asignacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<List<DatosMesaGestion>> asignar(@RequestBody @Valid DatosAsignacionMesas datos) {
        return ResponseEntity.ok(mesaService.asignar(datos).stream().map(DatosMesaGestion::new).toList());
    }

    @PostMapping("/cubrir-turno")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<Map<String, Integer>> cubrirTurno(@RequestBody @Valid DatosCubrirTurno datos) {
        return ResponseEntity.ok(Map.of("mesas_movidas", mesaService.cubrirTurno(datos)));
    }

    private DatosRespuestaMesa conOrdenActiva(Mesa m) {
        var orden = ordenRepository.findActivaByMesa(m.getId_mesas()).orElse(null);
        List<DatosDetalleRespuesta> platillos = List.of();
        String nombreMesero = null;
        if (orden != null) {
            platillos = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes())
                    .stream().map(DatosDetalleRespuesta::new).toList();
            nombreMesero = orden.getUsuario().getNombre();
        }
        return new DatosRespuestaMesa(
                m.getId_mesas(),
                m.getNumero(),
                m.getEstado().toString(),
                orden != null ? orden.getId_ordenes() : null,
                nombreMesero,
                platillos,
                orden != null ? orden.getFecha_apertura() : null
        );
    }
}
