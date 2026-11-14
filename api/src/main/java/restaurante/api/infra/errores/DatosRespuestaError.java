package restaurante.api.infra.errores;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * La ÚNICA forma que tiene un error en todo el API:
 *
 *   { "mensaje": "texto para mostrar tal cual" }
 *   { "mensaje": "...", "errores": [ { "campo": "...", "error": "..." } ] }   ← solo validaciones
 *
 * `mensaje` siempre viene y siempre se puede mostrar al usuario: el frontend
 * lee `error.response.data.mensaje` en todas sus pantallas. `errores` solo
 * aparece cuando falló la validación de campos, por si una pantalla quiere
 * marcar cada campo; si no, con `mensaje` basta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatosRespuestaError(String mensaje, List<DatosErrorValidacion> errores) {

    public DatosRespuestaError(String mensaje) {
        this(mensaje, null);
    }
}
