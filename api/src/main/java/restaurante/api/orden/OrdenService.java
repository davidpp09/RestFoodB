package restaurante.api.orden;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import restaurante.api.evento.EventoOrden;
import restaurante.api.evento.EventoOrdenRepository;
import restaurante.api.evento.TipoEvento;
import restaurante.api.admin.DatosCancelacionMesero;
import restaurante.api.admin.DatosCorteDia;
import restaurante.api.admin.DatosProductoCancelado;
import restaurante.api.admin.DatosVentaEmpleado;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.mesa.Estado;
import restaurante.api.mesa.MesaRepository;
import restaurante.api.ordenDetalle.*;
import restaurante.api.producto.ProductoRepository;
import restaurante.api.usuario.Roles;
import restaurante.api.usuario.Usuario;
import restaurante.api.usuario.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    @Autowired
    private restaurante.api.infra.impresora.ImpresoraService impresoraService;
    @Autowired
    private EventoOrdenRepository eventoOrdenRepository;


    private boolean esSuperUsuario(Roles rol) {
        return rol.equals(Roles.DEV) || rol.equals(Roles.ADMIN);
    }

    @Transactional // ✅ faltaba
    public DatosApertura abrirCuenta(DatosAbrirOrden datos) {
        var usuario = usuarioRepository.getReferenceById(datos.id_usuario());
        boolean conMesa = usuario.getRol().equals(Roles.MESERO) || esSuperUsuario(usuario.getRol());

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia    = LocalDate.now().atTime(LocalTime.MAX);
        int numeroComanda = ordenRepository.countByUsuarioIdAndFechaBetween(
                usuario.getId_usuarios(), inicioDia, finDia).intValue() + 1;

        if (conMesa) {
            if (datos.id_mesa() == null) {
                throw new ValidacionException("Debes indicar el número de mesa.");
            }
            var mesa = mesaRepository.getReferenceById(datos.id_mesa());
            mesa.abrirMesa();
            Orden ordenGuardada = ordenRepository.save(new Orden(mesa, usuario, datos.tipo(), datos.servicio(), numeroComanda));

            eventoOrdenRepository.save(new EventoOrden(ordenGuardada, usuario, TipoEvento.MESA_ABIERTA));

            DatosMesaAbierta avisoMesa = new DatosMesaAbierta(
                    datos.id_mesa(),
                    mesa.getEstado(),
                    usuario.getNombre(),
                    ordenGuardada.getId_ordenes(),
                    numeroComanda
            );
            messagingTemplate.convertAndSend("/topic/mesas", avisoMesa);
            System.out.println("✅ [WS /topic/mesas] Mesa abierta: " + avisoMesa);
            return new DatosApertura(ordenGuardada.getId_ordenes(), numeroComanda);
        } else {
            // REPARTIDOR u otros roles sin mesa
            Orden ordenGuardada = ordenRepository.save(new Orden(null, usuario, datos.tipo(), datos.servicio(), numeroComanda));
            System.out.println("✅ Orden sin mesa creada, id: " + ordenGuardada.getId_ordenes() + " comanda #" + numeroComanda);
            return new DatosApertura(ordenGuardada.getId_ordenes(), numeroComanda);
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
        
        // 1. Obtener los platillos actuales de la base de datos
        var platosEnDb = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
        
        // 2. Obtener los IDs que vienen en el payload (los que aún existen en el carrito)
        List<Long> idsRecibidos = datos.platillos().stream()
                .map(DatosPlatilloLote::id_detalle)
                .filter(Objects::nonNull)
                .toList();

        // 3. Eliminar platillos que ya no están en el carrito
        for (OrdenDetalle platilloDb : platosEnDb) {
            if (!idsRecibidos.contains(platilloDb.getId_detalle())) {
                String impresora = platilloDb.getProducto().getCategoria().getImpresora();
                eventoOrdenRepository.save(new EventoOrden(orden, usuario, TipoEvento.PLATILLO_CANCELADO,
                        platilloDb.getProducto().getNombre(),
                        platilloDb.getCantidad(), 0,
                        platilloDb.getPrecio_unitario(),
                        platilloDb.getComentarios(), null));
                ordenDetalleRepository.delete(platilloDb);
                ticketCocina.add(new DatosPlatilloTicket("🔴 CANCELADO", platilloDb.getProducto().getNombre(), 0, "Cancelado por el mesero", impresora));
            }
        }

        // 4. Procesar platillos del frontend (nuevos o modificados)
        for (DatosPlatilloLote platillo : datos.platillos()) {
            if (platillo.id_detalle() == null) {
                var producto = productoRepository.findById(platillo.id_producto()).orElseThrow();
                String impresora = producto.getCategoria().getImpresora();
                OrdenDetalle nuevoDetalle = ordenDetalleRepository.save(new OrdenDetalle(platillo, producto, orden));
                eventoOrdenRepository.save(new EventoOrden(orden, usuario, TipoEvento.PLATILLO_NUEVO,
                        producto.getNombre(),
                        null, platillo.cantidad(),
                        nuevoDetalle.getPrecio_unitario(),
                        null, platillo.comentarios()));
                ticketCocina.add(new DatosPlatilloTicket("🟢 NUEVO", producto.getNombre(), platillo.cantidad(), platillo.comentarios(), impresora));
            } else {
                var modificado = ordenDetalleRepository.findById(platillo.id_detalle()).orElseThrow();
                var producto   = productoRepository.findById(platillo.id_producto()).orElseThrow();
                String impresora = producto.getCategoria().getImpresora();

                boolean cambioCantidad    = !modificado.getCantidad().equals(platillo.cantidad());
                boolean cambioComentarios = (modificado.getComentarios() == null && platillo.comentarios() != null && !platillo.comentarios().isEmpty())
                        || (modificado.getComentarios() != null && !modificado.getComentarios().equals(platillo.comentarios()));

                if (cambioCantidad || cambioComentarios) {
                    eventoOrdenRepository.save(new EventoOrden(orden, usuario, TipoEvento.PLATILLO_MODIFICADO,
                            producto.getNombre(),
                            modificado.getCantidad(), platillo.cantidad(),
                            modificado.getPrecio_unitario(),
                            modificado.getComentarios(), platillo.comentarios()));
                    modificado.actualizarPlatillo(platillo);
                    ticketCocina.add(new DatosPlatilloTicket("🟡 MODIFICADO", producto.getNombre(), platillo.cantidad(), platillo.comentarios(), impresora));
                }
            }
        }

        ordenDetalleRepository.flush();
        var platosActualizados = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
        orden.recalcularTotal(platosActualizados);

        List<DatosDetalleRespuesta> platillosMapeados = platosActualizados.stream()
                .map(DatosDetalleRespuesta::new)
                .toList();

        Long idMesa = orden.getMesa() != null ? orden.getMesa().getId_mesas() : null;
        DatosRespuestaOrden respuesta  = new DatosRespuestaOrden(orden.getId_ordenes(), orden.getNumero_comanda(), orden.getTotal(), platillosMapeados, orden.getTipo().toString(), idMesa);
        DatosTicketCocina  ticketFinal = new DatosTicketCocina(idMesa, orden.getId_ordenes(), orden.getNumero_comanda(), orden.getUsuario().getNombre(), orden.getTipo(), ticketCocina);

        messagingTemplate.convertAndSend("/topic/cocina", ticketFinal);

        // Notificar al panel de admin
        Map<String, Object> updateAdmin = new HashMap<>();
        if (idMesa != null) {
            updateAdmin.put("id_mesa", idMesa);
            updateAdmin.put("estado", "OCUPADA");
        }
        updateAdmin.put("id_orden", orden.getId_ordenes());
        updateAdmin.put("platillos", platillosMapeados);
        messagingTemplate.convertAndSend("/topic/mesas", updateAdmin);
        
        System.out.println("✅ [WS /topic/cocina] Ticket enviado orden #" + orden.getId_ordenes() + " con " + ticketCocina.size() + " platillos");

        // Enviar impresión física
        impresoraService.imprimirComandaCocina(ticketFinal);

        return respuesta;
    }

    @Transactional
    public DatosRespuestaCuenta darCuenta(Long id) {
        var orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ValidacionException("Orden no encontrada"));

        // ✅ VALIDACIÓN: Solo el mesero que abrió la orden o un ADMIN/DEV pueden cerrarla
        var usuarioAutenticado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!esSuperUsuario(usuarioAutenticado.getRol()) && !orden.getUsuario().getId_usuarios().equals(usuarioAutenticado.getId_usuarios())) {
            throw new ValidacionException("No tienes permiso para cerrar esta orden porque no la abriste tú.");
        }

        // ✅ VALIDACIÓN 1: No se puede cerrar una orden ya pagada
        List<Orden> otrasOrdenes = new ArrayList<>();
        if (orden.getMesa() != null) {
            var ordenesActivas = ordenRepository.findByMesaAndEstatus(
                    orden.getMesa(),
                    Estatus.PREPARANDO
            );
            otrasOrdenes = ordenesActivas.stream()
                    .filter(o -> !o.getId_ordenes().equals(id))
                    .toList();
        }

        // ✅ Llama al método de dominio que tiene las reglas de negocio
        orden.finalizar(otrasOrdenes);

        // ✅ Si tiene mesa, liberarla y avisar por WebSocket
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();

            DatosMesaAbierta avisoMesa = new DatosMesaAbierta(
                    orden.getMesa().getId_mesas(),
                    orden.getMesa().getEstado(),
                    "",
                    null,
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
                orden.getNumero_comanda(),
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

        // ✅ IMPRIMIR TICKET FÍSICO DE CLIENTE
        impresoraService.imprimirTicketCliente(ticket);

        eventoOrdenRepository.save(new EventoOrden(orden, usuarioAutenticado, TipoEvento.MESA_CERRADA));

        // ✅ AVISAR A COCINA QUE LA ORDEN FUE CERRADA (independientemente del estatus)
        messagingTemplate.convertAndSend("/topic/cocina", Map.of("accion", "CERRADA", "id_orden", orden.getId_ordenes()));
        System.out.println("🍳 [WS /topic/cocina] Orden cerrada: #" + orden.getId_ordenes());

        // ✅ Devolver ticket al frontend
        return ticket;
    }

    public void reenviarACocina(Long idOrden) {
        var orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new ValidacionException("Orden no encontrada"));

        if (orden.getEstatus().equals(Estatus.PAGADA)) {
            throw new ValidacionException("No se puede reenviar una orden ya pagada.");
        }

        var platillos = ordenDetalleRepository.findAllByOrdenId(idOrden);

        List<DatosPlatilloTicket> ticketCocina = platillos.stream()
                .map(p -> new DatosPlatilloTicket(
                        "🔄 REENVIO",
                        p.getProducto().getNombre(),
                        p.getCantidad(),
                        p.getComentarios(),
                        p.getProducto().getCategoria().getImpresora()
                ))
                .toList();

        Long idMesa = orden.getMesa() != null ? orden.getMesa().getId_mesas() : null;
        DatosTicketCocina ticketFinal = new DatosTicketCocina(
                idMesa,
                orden.getId_ordenes(),
                orden.getNumero_comanda(),
                orden.getUsuario().getNombre(),
                orden.getTipo(),
                ticketCocina
        );

        messagingTemplate.convertAndSend("/topic/cocina", ticketFinal);
        System.out.println("🔄 [WS /topic/cocina] Reenvío a cocina Orden #" + idOrden);

        impresoraService.imprimirComandaCocina(ticketFinal);
    }

    public DatosRespuestaCuenta reimprimirTicket(Long idOrden) {
        var orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new ValidacionException("Orden no encontrada"));

        var platillos = ordenDetalleRepository.findAllByOrdenId(idOrden);
        List<DatosDetalleRespuesta> platillosMapeados = platillos.stream()
                .map(DatosDetalleRespuesta::new)
                .toList();

        DatosRespuestaCuenta ticket = new DatosRespuestaCuenta(
                orden.getId_ordenes(),
                orden.getNumero_comanda(),
                orden.getMesa() != null ? orden.getMesa().getId_mesas() : null,
                orden.getTipo().toString(),
                orden.getFecha_apertura(),
                orden.getFechaCierre(),
                platillosMapeados,
                orden.getTotal(),
                orden.getEstatus().toString()
        );

        // Re-enviar por WebSocket al frontend que esté escuchando /topic/tickets
        messagingTemplate.convertAndSend("/topic/tickets", ticket);
        System.out.println("🖨️ [WS /topic/tickets] Reimpresión ticket Orden #" + idOrden);

        // Imprimir físicamente
        impresoraService.imprimirTicketCliente(ticket);

        return ticket;
    }

    public DatosRespuestaOrden obtenerOrdenActiva(Long id_mesa) {
        var orden = ordenRepository.findActivaByMesa(id_mesa)
                .orElseThrow(() -> new ValidacionException("No hay orden activa para esta mesa"));

        var platillos = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
        List<DatosDetalleRespuesta> platillosMapeados = platillos.stream()
                .map(DatosDetalleRespuesta::new)
                .toList();

        return new DatosRespuestaOrden(orden.getId_ordenes(), orden.getNumero_comanda(), orden.getTotal(), platillosMapeados, orden.getTipo().toString(), orden.getMesa() != null ? orden.getMesa().getId_mesas() : null);
    }

    public List<DatosEntregaHoy> obtenerEntregasHoy() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin    = LocalDate.now().atTime(LocalTime.MAX);
        return ordenRepository.findEntregasDelDia(Tipo.LLEVAR, inicio, fin)
                .stream()
                .map(orden -> {
                    var platillos = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes())
                            .stream().map(DatosDetalleRespuesta::new).toList();
                    return new DatosEntregaHoy(
                            orden.getId_ordenes(),
                            orden.getNumero_comanda(),
                            orden.getFecha_apertura(),
                            orden.getEstatus(),
                            orden.getTotal(),
                            platillos
                    );
                }).toList();
    }

    public List<DatosRespuestaOrden> listarOrdenesCocina() {
        return ordenRepository.findByEstatus(Estatus.PREPARANDO).stream().map(orden -> {
            var platillos = ordenDetalleRepository.findAllByOrdenId(orden.getId_ordenes());
            List<DatosDetalleRespuesta> platillosMapeados = platillos.stream()
                    .map(DatosDetalleRespuesta::new)
                    .toList();
            return new DatosRespuestaOrden(orden.getId_ordenes(), orden.getNumero_comanda(), orden.getTotal(), platillosMapeados, orden.getTipo().toString(), orden.getMesa() != null ? orden.getMesa().getId_mesas() : null);
        }).toList();
    }

    @Transactional
    public void marcarOrdenServida(Long idOrden) {
        var orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new ValidacionException("Orden no encontrada"));
        orden.marcarComoServido();
        // Opcional: avisar por websocket enviando un objeto JSON válido
        messagingTemplate.convertAndSend("/topic/cocina", Map.of("mensaje", "Orden " + idOrden + " está lista", "id_orden", idOrden));
    }

    @Transactional
    public DatosCorteDia master(LocalDate fecha) {
        var inicio = fecha.atStartOfDay();
        var fin    = fecha.atTime(LocalTime.MAX);
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

    public List<DatosCancelacionMesero> cancelacionesPorMesero(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin    = hasta.atTime(LocalTime.MAX);

        var eventos = eventoOrdenRepository.findByTipoEventoAndTimestampBetween(
                TipoEvento.PLATILLO_CANCELADO, inicio, fin);

        return eventos.stream()
                .collect(Collectors.groupingBy(EventoOrden::getNombreMesero))
                .entrySet().stream()
                .map(entry -> {
                    List<DatosProductoCancelado> productos = entry.getValue().stream()
                            .collect(Collectors.groupingBy(
                                    e -> e.getNombreProducto() != null ? e.getNombreProducto() : "Desconocido",
                                    Collectors.counting()))
                            .entrySet().stream()
                            .map(p -> new DatosProductoCancelado(p.getKey(), p.getValue()))
                            .sorted(Comparator.comparingLong(DatosProductoCancelado::veces).reversed())
                            .toList();

                    return new DatosCancelacionMesero(
                            entry.getKey(),
                            (long) entry.getValue().size(),
                            productos
                    );
                })
                .sorted(Comparator.comparingLong(DatosCancelacionMesero::totalCancelaciones).reversed())
                .toList();
    }
}