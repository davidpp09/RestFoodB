package restaurante.api.infra.validacion;

import restaurante.api.infra.errores.ValidacionException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regla común para los reportes que reciben un periodo `desde`/`hasta`.
 *
 * Esos reportes (teórico contra real, cancelaciones por mesero) cargan en
 * memoria TODOS los registros del periodo para agruparlos. Sin tope, un
 * `desde=2020-01-01` haría que el servidor trajera años de movimientos de un
 * jalón: el kardex crece ~170 movimientos al día. Con el tope, el peor caso
 * queda acotado y predecible.
 *
 * 92 días = un trimestre completo, que es el periodo más largo que tiene
 * sentido revisar de corrido. Para más, se piden varios trimestres.
 */
public final class RangoFechas {

    public static final int MAX_DIAS = 92;

    private RangoFechas() {
    }

    /**
     * Lanza ValidacionException (400 con mensaje) si el rango está al revés o
     * pasa del tope. Los dos extremos cuentan: del 1 al 1 es un día.
     */
    public static void validar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ValidacionException("La fecha final (" + hasta + ") es anterior a la inicial (" + desde + ")");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > MAX_DIAS) {
            throw new ValidacionException("El periodo es de " + dias + " días; el máximo es " + MAX_DIAS
                    + " (un trimestre). Pide el reporte por partes.");
        }
    }
}
