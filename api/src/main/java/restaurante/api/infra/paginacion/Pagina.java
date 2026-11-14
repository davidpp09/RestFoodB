package restaurante.api.infra.paginacion;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados, con la forma que ve el frontend.
 *
 * Por qué un DTO propio y no devolver el `Page` de Spring directo: el JSON de
 * `PageImpl` es un detalle interno de Spring Data, y Spring mismo advierte que
 * puede cambiar entre versiones. Subir de versión de Spring Boot no debería
 * romper la pantalla del kardex. Con este record el contrato de la API es
 * nuestro y solo cambia cuando nosotros lo decidimos.
 *
 * `pagina` empieza en 0, igual que el parámetro `page` que se recibe.
 */
public record Pagina<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {
    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapeo) {
        return new Pagina<>(
                page.getContent().stream().map(mapeo).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
