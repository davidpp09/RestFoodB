package restaurante.api.orden;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import restaurante.api.controller.ordenes.OrdenesController;
import restaurante.api.infra.impresora.ImpresoraService;
import restaurante.api.ordenDetalle.OrdenDetalleRepository;
import restaurante.api.usuario.Roles;
import restaurante.api.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reimpresión de tickets por el repartidor (2026-11-14).
 *
 * Lo que se vigila: que el repartidor PUEDA reimprimir sus entregas y que NO
 * pueda reimprimir las de otro cambiando el id en la petición. El ticket lleva
 * el total y los platillos, así que es información que no le toca.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReimprimirTicketTest {

    @Mock private OrdenRepository ordenRepository;
    @Mock private OrdenDetalleRepository ordenDetalleRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ImpresoraService impresoraService;
    @InjectMocks private OrdenService ordenService;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(long id, Roles rol) {
        var u = mock(Usuario.class);
        when(u.getId_usuarios()).thenReturn(id);
        when(u.getRol()).thenReturn(rol);
        when(u.getNombre()).thenReturn("Usuario " + id);
        return u;
    }

    private void autenticarComo(Usuario u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
    }

    /** Una entrega PARA LLEVAR levantada por {@code dueno}. */
    private void entregaDe(Usuario dueno) {
        var orden = mock(Orden.class);
        when(orden.getId_ordenes()).thenReturn(10L);
        when(orden.getNumero_comanda()).thenReturn(3);
        when(orden.getTipo()).thenReturn(Tipo.LLEVAR);
        when(orden.getEstatus()).thenReturn(Estatus.PAGADA);
        when(orden.getTotal()).thenReturn(new BigDecimal("180"));
        when(orden.getFecha_apertura()).thenReturn(LocalDateTime.now());
        when(orden.getUsuario()).thenReturn(dueno);
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(ordenDetalleRepository.findAllByOrdenId(10L)).thenReturn(List.of());
    }

    @Test
    void elRepartidorReimprimeSuPropiaEntrega() {
        var repartidor = usuario(5, Roles.REPARTIDOR);
        entregaDe(repartidor);
        autenticarComo(repartidor);

        var ticket = ordenService.reimprimirTicket(10L);

        assertEquals(10L, ticket.id_orden());
        verify(impresoraService).imprimirTicketCliente(any());
    }

    @Test
    void elRepartidorNoPuedeReimprimirLaEntregaDeOtro() {
        entregaDe(usuario(5, Roles.REPARTIDOR));
        autenticarComo(usuario(6, Roles.REPARTIDOR));

        assertThrows(AccessDeniedException.class, () -> ordenService.reimprimirTicket(10L));
        verify(impresoraService, never()).imprimirTicketCliente(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void elAdminSigueReimprimiendoCualquierOrden() {
        entregaDe(usuario(5, Roles.REPARTIDOR));
        autenticarComo(usuario(1, Roles.ADMIN));

        ordenService.reimprimirTicket(10L);

        verify(impresoraService).imprimirTicketCliente(any());
    }

    /**
     * El permiso del endpoint, comparado como conjunto EXACTO: avisa si se le
     * quita al repartidor y también si se le regala a COCINA.
     */
    @Test
    void elEndpointAdmiteAlRepartidorYNoACocina() throws Exception {
        var anotacion = OrdenesController.class.getMethod("reimprimirTicket", Long.class)
                .getAnnotation(PreAuthorize.class);
        Set<String> roles = Pattern.compile("'([A-Z_]+)'").matcher(anotacion.value()).results()
                .map(m -> m.group(1)).collect(Collectors.toSet());

        assertEquals(Set.of("ADMIN", "DEV", "CAJERO", "MESERO", "REPARTIDOR"), roles);
    }
}
