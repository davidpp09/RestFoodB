package restaurante.api.orden;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.admin.DatosCorteDia;
import restaurante.api.admin.DatosVentaEmpleado;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.mesa.Estado;
import restaurante.api.mesa.MesaRepository;
import restaurante.api.ordenDetalle.*;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.usuario.Roles;
import restaurante.api.usuario.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrdenService {

    @Autowired
    OrdenRepository ordenRepository;
    @Autowired
    MesaRepository mesaRepository;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    OrdenDetalleRepository ordenDetalleRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    private boolean esSuperUsuario(Roles rol) {
        return rol.equals(Roles.DEV) || rol.equals(Roles.ADMIN);
    }

    @Transactional // ✅ faltaba
    public Long abrirCuenta(DatosAbrirOrden datos) {
        var usuario = usuarioRepository.getReferenceById(datos.id_usuario());
        boolean conMesa = usuario.getRol().equals(Roles.MESERO) || esSuperUsuario(usuario.getRol());

        if (conMesa) {
            if (datos.id_mesa() == null) {
                throw new ValidacionException("Debes indicar el número de mesa.");
            }
            var mesa = mesaRepository.getReferenceById(datos.id_mesa());
            if (mesa.getEstado() == Estado.OCUPADA) {
                throw new ValidacionException("La mesa ya está en uso, no se puede abrir otra cuenta.");
            }
            mesa.abrirMesa();
            Orden ordenGuardada = ordenRepository.save(new Orden(mesa, usuario, datos.tipo(), datos.servicio()));

            DatosMesaAbierta avisoMesa = new DatosMesaAbierta(
                    datos.id_mesa(),
                    mesa.getEstado(),
                    usuario.getNombre(),
                    ordenGuardada.getId_ordenes()
            );
            messagingTemplate.convertAndSend("/topic/mesas", avisoMesa);
            System.out.println("✅ [WS /topic/mesas] Mesa abierta: " + avisoMesa);
            return ordenGuardada.getId_ordenes();
        } else {
            // REPARTIDOR u otros roles sin mesa
            Orden ordenGuardada = ordenRepository.save(new Orden(null, usuario, datos.tipo(), datos.servicio()));
            System.out.println("✅ Orden sin mesa creada, id: " + ordenGuardada.getId_ordenes());
            return ordenGuardada.getId_ordenes();
        }
    }

    public Page<DatosListaOrden> listar(Pageable pagina) {
        return ordenRepository.findAllByTipo(pagina, Tipo.LOZA).map(DatosListaOrden::new);
    }

    @Transactional
    public DatosRespuestaOrden enviarOrden(DatosSincronizarComanda datos) {
        var orden = ordenRepository.findByIdConBloqueo(datos.id_orden()).orElseThrow();
        var usuario = usuarioRepository.getReferenceById(datos.id_usuario());

        if (orden.getEstatus().equals(Estatus.PAGADA)) {
            throw new ValidacionException("La orden ya fue pagada, no puedes modificarla.");
        }

        // ✅ Superusuarios pueden modificar cualquier orden
        if (!esSuperUsuario(usuario.getRol()) && !orden.getUsuario().getId_usuarios().equals(datos.id_usuario())) {
            throw new ValidacionException("Solo el mesero asignado puede modificar esta orden.");
        }

        List<DatosPlatilloTicket> ticketCocina = new ArrayList<>();
        for (DatosPlatilloLote platillo : datos.platillos()) {
            if (platillo.id_detalle() == null) {
                var producto = productoRepository.getReferenceById(platillo.id_producto());
                OrdenDetalle detalle = new OrdenDetalle(platillo, producto, orden);
                ordenDetalleRepository.save(detalle);
                ticketCocina.add(new DatosPlatilloTicket("🟢 NUEVO", producto.getNombre(), platillo.cantidad(), platillo.comentarios()));
            } else {
                if (platillo.cantidad() == 0) {
                    var producto = productoRepository.getReferenceById(platillo.id_producto());
                    ordenDetalleRepository.deleteById(platillo.id_detalle());
                    ticketCocina.add(new DatosPlatilloTicket("🔴 CANCELADO", producto.getNombre(), 0, "No preparar"));
                } else {
                    var modificado = ordenDetalleRepository.getReferenceById(platillo.id_detalle());
                    var producto = productoRepository.getReferenceById(platillo.id_producto());
                    modificado.actualizarPlatillo(platillo);
                    ticketCocina.add(new DatosPlatilloTicket("🟡 MODIFICADO", producto.getNombre(), platillo.cantidad(), platillo.comentarios()));
                }
            }
        }

        var platosActualizados = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
        orden.recalcularTotal(platosActualizados);

        List<DatosDetalleRespuesta> platillosMapeados = platosActualizados.stream()
                .map(DatosDetalleRespuesta::new)
                .toList();

        DatosRespuestaOrden respuesta = new DatosRespuestaOrden(orden.getId_ordenes(), orden.getTotal(), platillosMapeados);
        DatosTicketCocina ticketFinal = new DatosTicketCocina(orden.getMesa().getId_mesas(), orden.getId_ordenes(), orden.getUsuario().getNombre(), orden.getTipo(), ticketCocina);

        messagingTemplate.convertAndSend("/topic/mesas", ticketFinal);
        System.out.println("✅ [WS /topic/mesas] Ticket enviado orden #" + orden.getId_ordenes() + " con " + ticketCocina.size() + " platillos");

        return respuesta;
    }

    @Transactional
    public DatosRespuestaCuenta darCuenta(Long id) {
        var orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("Orden no encontrada"));

        // ✅ VALIDACIÓN 1: No se puede cerrar una orden ya pagada
        if (orden.getEstatus().equals(Estatus.PAGADA)) {
            throw new ValidacionException("Esta orden ya fue pagada anteriormente");
        }

        // ✅ VALIDACIÓN 2: Si tiene mesa, verificar que la mesa esté ocupada
        if (orden.getMesa() != null) {
            if (orden.getMesa().getEstado().equals(Estado.LIBRE)) {
                throw new ValidacionException("La mesa ya fue liberada");
            }

            // ✅ VALIDACIÓN 3 CORREGIDA: Verificar si hay una orden MÁS NUEVA en la mesa
            var ordenesActivas = ordenRepository.findByMesaAndEstatus(
                    orden.getMesa(),
                    Estatus.PREPARANDO
            );

            // Filtrar órdenes que NO sean la que estamos cerrando
            var otrasOrdenes = ordenesActivas.stream()
                    .filter(o -> !o.getId_ordenes().equals(id))
                    .toList();

            // Si hay otras órdenes preparando en esta mesa
            if (!otrasOrdenes.isEmpty()) {
                // Verificar si alguna de esas órdenes es MÁS NUEVA que la que queremos cerrar
                boolean hayOrdenMasNueva = otrasOrdenes.stream()
                        .anyMatch(o -> o.getFecha_apertura().isAfter(orden.getFecha_apertura()));

                if (hayOrdenMasNueva) {
                    throw new ValidacionException(
                            "No puedes cerrar esta orden porque hay una orden más nueva activa en la mesa"
                    );
                }
            }
        }

        // ✅ Si pasa todas las validaciones, procede a cerrar
        orden.finalizar();

        // ✅ Si tiene mesa, liberarla y avisar por WebSocket
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();

            DatosMesaAbierta avisoMesa = new DatosMesaAbierta(
                    orden.getMesa().getId_mesas(),
                    orden.getMesa().getEstado(),
                    "",
                    null
            );
            messagingTemplate.convertAndSend("/topic/mesas", avisoMesa);
            System.out.println("✅ [WS /topic/mesas] Mesa liberada: " + orden.getMesa().getId_mesas());
        }

        // ✅ Obtener platillos para el ticket
        var platillos = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
        List<DatosDetalleRespuesta> platillosMapeados = platillos.stream()
                .map(DatosDetalleRespuesta::new)
                .toList();

        // ✅ Crear el ticket completo
        DatosRespuestaCuenta ticket = new DatosRespuestaCuenta(
                orden.getId_ordenes(),
                orden.getMesa() != null ? orden.getMesa().getId_mesas() : null,
                orden.getTipo().toString(),
                orden.getFecha_apertura(),
                orden.getFechaCierre(),
                platillosMapeados,
                orden.getTotal(),
                orden.getEstatus().toString()
        );

        // ✅ ENVIAR TICKET POR WEBSOCKET PARA IMPRESIÓN
        messagingTemplate.convertAndSend("/topic/tickets", ticket);
        System.out.println("🖨️ [WS /topic/tickets] Ticket enviado para impresión: Orden #" + orden.getId_ordenes());

        // ✅ Devolver ticket al frontend
        return ticket;
    }

    @Transactional
    public DatosCorteDia master() {
        var inicio = LocalDate.now().atStartOfDay();
        var fin = LocalDate.now().atTime(LocalTime.MAX);
        List<Orden> ordenes = ordenRepository.findByFechaCierreBetweenAndEstatus(inicio, fin, Estatus.PAGADA);
        var ventasAgrupadas = ordenes.stream()
                .collect(Collectors.groupingBy(o -> o.getUsuario().getNombre()));
        return new DatosCorteDia(
                ventaEmpleados(ventasAgrupadas),
                totalDesayuno(ordenes),
                totalComida(ordenes),
                platillosLoza(inicio, fin),
                platillosParaLlevar(inicio, fin),
                totalGeneral(ordenes)
        );
    }

    // --- Métodos de cálculo ---
    public BigDecimal totalGeneral(List<Orden> ordenes) {
        return ordenes.stream().map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalDesayuno(List<Orden> ordenes) {
        return ordenes.stream().filter(o -> Servicio.DESAYUNO.equals(o.getServicio())).map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalComida(List<Orden> ordenes) {
        return ordenes.stream().filter(o -> Servicio.COMIDA.equals(o.getServicio())).map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long platillosParaLlevar(LocalDateTime inicio, LocalDateTime fin) {
        return ordenDetalleRepository.contarPlatillosPorTipo(inicio, fin, Tipo.LLEVAR);
    }

    public Long platillosLoza(LocalDateTime inicio, LocalDateTime fin) {
        return ordenDetalleRepository.contarPlatillosPorTipo(inicio, fin, Tipo.LOZA);
    }

    public List<DatosVentaEmpleado> ventaEmpleados(Map<String, List<Orden>> ventasPorNombre) {
        return ventasPorNombre.entrySet().stream()
                .map(entry -> new DatosVentaEmpleado(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
    }
}