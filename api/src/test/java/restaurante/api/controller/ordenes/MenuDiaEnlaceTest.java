package restaurante.api.controller.ordenes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.menu.EnlacesDeDescarga;
import restaurante.api.menu.MenuDiaService;
import restaurante.api.producto.Producto;
import restaurante.api.producto.ProductoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Regresión: con la carta bloqueada (un grupo de precios con problemas) el
 * enlace de descarga se entregaba igual, y en la tablet del repartidor se
 * bajaba un JSON de error en vez del menú. El enlace debe validar lo mismo que
 * la descarga, y fallar ANTES de emitirse.
 */
@ExtendWith(MockitoExtension.class)
class MenuDiaEnlaceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private MenuDiaService menuDiaService;
    @Mock private EnlacesDeDescarga enlacesDeDescarga;
    @InjectMocks private MenuDiaController controller;

    @Test
    void conLaCartaBloqueadaNoSeEmiteElEnlace() throws Exception {
        when(productoRepository.findActivosDelDia()).thenReturn(List.of(mock(Producto.class)));
        when(menuDiaService.generarCarta(anyList()))
                .thenThrow(new ValidacionException("El menú no se puede imprimir"));

        assertThrows(ValidacionException.class, () -> controller.enlaceDescarga());
        verify(enlacesDeDescarga, never()).emitir();
    }

    @Test
    void conLaCartaSanaSeEmiteElEnlace() throws Exception {
        when(productoRepository.findActivosDelDia()).thenReturn(List.of(mock(Producto.class)));
        when(menuDiaService.generarCarta(anyList())).thenReturn(new byte[]{1});
        when(enlacesDeDescarga.emitir()).thenReturn("abc");

        var respuesta = controller.enlaceDescarga();

        assertEquals("/menu-dia/descargar?t=abc", respuesta.getBody().get("url"));
    }
}
