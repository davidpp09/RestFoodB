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


    @Transactional
    public Long abrirCuenta(DatosAbrirOrden datos) {
        var usuario = usuarioRepository.getReferenceById(datos.id_usuario());
        if (usuario.getRol().equals(Roles.MESERO)) {
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
            return ordenGuardada.getId_ordenes();
        } else {
            Orden ordenGuardada = ordenRepository.save(new Orden(null, usuario, datos.tipo(), datos.servicio()));
            return ordenGuardada.getId_ordenes();

        }
    }

    public Page<DatosListaOrden> listar(Pageable pagina) {
        return ordenRepository.findAllByTipo(pagina, Tipo.LOZA).map(DatosListaOrden::new);
    }

    @Transactional
    public DatosRespuestaOrden enviarOrden(DatosSincronizarComanda datos) {
        var orden = ordenRepository.findByIdConBloqueo(datos.id_orden()).orElseThrow();
        if (orden.getEstatus().equals(Estatus.PAGADA)) {
            throw new ValidacionException("La orden ya fue pagada, no puedes modificarla wey");
        }
        if (!orden.getUsuario().getId_usuarios().equals(datos.id_usuario())) {
            throw new ValidacionException("Solo un mesero puede tener la orden de la mesa");
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

        // 1. Empaquetamos el ticket exclusivo para la cocina
        DatosTicketCocina ticketFinal = new DatosTicketCocina(orden.getId_ordenes(), orden.getUsuario().getNombre(), orden.getTipo(), ticketCocina);

        // 2. ¡Enviamos el ticket a la impresora por el WebSocket! 🖨️
        messagingTemplate.convertAndSend("/topic/cocina", ticketFinal);

        // 3. El mesero recibe su respuesta normal
        return respuesta;
    }

    @Transactional
    public void darCuenta(Long id) {
        var orden = ordenRepository.findById(id).orElseThrow();

        orden.finalizar();

        // Si la orden tiene una mesa asignada, la liberamos 🔓
        if (orden.getMesa() != null) {
            orden.getMesa().liberar();
        }
    }


    @Transactional
    public BigDecimal totalGeneral(List<Orden> ordenes) {
        return ordenes.stream().map(Orden::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public BigDecimal totalDesayuno(List<Orden> ordenes) {
        return ordenes.stream()
                .filter(o -> Servicio.DESAYUNO.equals(o.getServicio()))
                .map(Orden::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public BigDecimal totalComida(List<Orden> ordenes) {
        return ordenes.stream()
                .filter(o -> Servicio.COMIDA.equals(o.getServicio()))
                .map(Orden::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Long pedidosParaLlevar(List<Orden> ordenes) {
        return ordenes.stream().filter(o -> o.getTipo().equals(Tipo.LLEVAR)).count();
    }

    @Transactional
    public Long pedidosLoza(List<Orden> ordenes) {
        return ordenes.stream().filter(o -> o.getTipo().equals(Tipo.LOZA)).count();
    }

    @Transactional
    public List<DatosVentaEmpleado> ventaEmpleados(Map<String, List<Orden>> ventasPorNombre) {
        return ventasPorNombre.entrySet().stream()
                .map(entry -> new DatosVentaEmpleado(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(Orden::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                )).toList();
    }

    @Transactional
    public DatosCorteDia corteDia(List<DatosVentaEmpleado> datosVentaEmpleados, BigDecimal totalDesayuno, BigDecimal totalComida, Long totalLoza, Long totalParaLlevar, BigDecimal totalGeneral) {
        return new DatosCorteDia(
                datosVentaEmpleados,
                totalDesayuno,
                totalComida,
                totalLoza,
                totalParaLlevar,
                totalGeneral
        );
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
                pedidosLoza(ordenes),
                pedidosParaLlevar(ordenes),
                totalGeneral(ordenes)
        );
    }
}


