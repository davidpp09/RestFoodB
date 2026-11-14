package restaurante.api.producto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DatosNuevoPlatilloDiaPrecisionTest {
    @Test void rechazaFraccionesQueLaBaseRedondearia() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var errores = factory.getValidator().validate(
                    new DatosNuevoPlatilloDia("Platillo de prueba", new BigDecimal("105.001")));
            assertTrue(errores.stream().anyMatch(error ->
                    error.getPropertyPath().toString().equals("precio") &&
                    error.getConstraintDescriptor().getAnnotation() instanceof jakarta.validation.constraints.Digits));
        }
    }

    @Test void admiteCentavosExactosHastaElLimite() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String precio : new String[]{"0.01", "105", "105.50", "9999.99"}) {
                assertTrue(factory.getValidator().validate(
                        new DatosNuevoPlatilloDia("Platillo de prueba", new BigDecimal(precio))).isEmpty(), precio);
            }
        }
    }
}
