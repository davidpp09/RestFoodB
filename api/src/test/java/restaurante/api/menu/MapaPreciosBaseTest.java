package restaurante.api.menu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import restaurante.api.precio.GrupoPrecioService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Diagnóstico explícito de catálogo real; no asume que el CI tenga productos sembrados. */
@SpringBootTest
@EnabledIfSystemProperty(named = "verificarMapa", matches = "true")
class MapaPreciosBaseTest {
    @Autowired MapaPrecios mapa;
    @Autowired GrupoPrecioService grupos;

    @Test void todosLosProductosDelMapaExistenYReportaDesfases() {
        var porClave = grupos.listar().stream().collect(Collectors.toMap(g -> g.clave(), Function.identity()));
        var faltantes = new ArrayList<String>();
        var desfases = new TreeSet<String>();
        for (var entrada : mapa.datos().entradas()) {
            var grupo = porClave.get(entrada.clave());
            if (grupo == null || grupo.productos().isEmpty()) {
                faltantes.add(entrada.clave() + ": grupo sin productos"); continue;
            }
            for (String nombre : entrada.productos()) {
                if (grupo.productos().stream().noneMatch(p -> p.nombre().equals(nombre))) faltantes.add(entrada.clave() + ": falta " + nombre);
            }
            for (var producto : grupo.productos()) {
                if (producto.precio() == null || producto.precio().compareTo(entrada.precioPlantilla()) != 0) {
                    desfases.add(entrada.clave() + " / " + producto.nombre() + ": base=" + producto.precio() + ", plantilla=" + entrada.precioPlantilla());
                }
            }
        }
        System.out.println("Desfases entre plantilla y base: " + desfases.size());
        desfases.forEach(System.out::println);
        assertTrue(faltantes.isEmpty(), String.join("; ", faltantes));
    }
}
