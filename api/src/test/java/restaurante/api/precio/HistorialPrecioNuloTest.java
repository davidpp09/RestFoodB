package restaurante.api.precio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import restaurante.api.producto.Producto;
import restaurante.api.usuario.Usuario;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistorialPrecioNuloTest {
    HistorialPrecioRepository repository;
    HistorialPrecioService service;
    Producto producto;
    Usuario usuario;

    @BeforeEach void preparar() {
        repository = mock(HistorialPrecioRepository.class);
        service = new HistorialPrecioService();
        ReflectionTestUtils.setField(service, "repository", repository);
        producto = mock(Producto.class);
        usuario = mock(Usuario.class);
    }

    @Test void retirarPrecioConservaImporteAnteriorYAutor() {
        service.registrar(producto, CampoPrecio.COMIDA, new BigDecimal("105.00"), usuario, "CARTA");
        var registro = ArgumentCaptor.forClass(HistorialPrecio.class);
        verify(repository).save(registro.capture());
        assertEquals(new BigDecimal("105.00"), registro.getValue().getPrecioAnterior());
        assertNull(registro.getValue().getPrecioNuevo());
        assertSame(usuario, registro.getValue().getUsuario());
        assertSame(producto, registro.getValue().getProducto());
    }

    @Test void dosPreciosAusentesNoGeneranCambio() {
        service.registrar(producto, CampoPrecio.COMIDA, null, usuario, "CARTA");
        verifyNoInteractions(repository);
    }

    @Test void asignarPrecioAntesAusenteSeAudita() {
        when(producto.getPrecio_comida()).thenReturn(new BigDecimal("105.00"));
        service.registrar(producto, CampoPrecio.COMIDA, null, usuario, "CARTA");
        var registro = ArgumentCaptor.forClass(HistorialPrecio.class);
        verify(repository).save(registro.capture());
        assertNull(registro.getValue().getPrecioAnterior());
        assertEquals(new BigDecimal("105.00"), registro.getValue().getPrecioNuevo());
    }

    @Test void distintaEscalaSinCambioDeImporteNoGeneraHistorial() {
        when(producto.getPrecio_comida()).thenReturn(new BigDecimal("105.00"));
        service.registrar(producto, CampoPrecio.COMIDA, new BigDecimal("105"), usuario, "CARTA");
        verifyNoInteractions(repository);
    }
}
