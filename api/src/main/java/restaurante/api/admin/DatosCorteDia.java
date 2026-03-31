package restaurante.api.admin;

import java.math.BigDecimal;
import java.util.List;

public record DatosCorteDia(
        List<DatosVentaEmpleado> ventaEmpleados,
        BigDecimal totalDesayuno,
        BigDecimal totalComida,
        Long totalMesas,
        Long totalParaLlevar,
        BigDecimal totalGeneral

        ) {
}
