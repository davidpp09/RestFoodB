package restaurante.api.menu;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.precio.DatosGrupoPrecio;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PreciosMenuPdf {
    private static final float MINIMO = 6.5f;
    private final MapaPrecios mapa;
    public PreciosMenuPdf(MapaPrecios mapa) { this.mapa = mapa; }
    private record Escritura(int pagina, MapaPrecios.Caja caja, PDFont fuente, float tamano,
                             String texto, float x, boolean renglon, boolean problema) {}

    public void aplicar(PDDocument documento, List<DatosGrupoPrecio> grupos, boolean vistaEditor) throws IOException {
        var porClave = grupos.stream().collect(Collectors.toMap(DatosGrupoPrecio::clave, Function.identity()));
        var problemas = new LinkedHashSet<String>();
        var escrituras = new ArrayList<Escritura>();
        for (var entrada : mapa.datos().entradas()) {
            var grupo = porClave.get(entrada.clave());
            String problema = grupo == null || grupo.productos().isEmpty() ? "sin productos asociados"
                    : !grupo.faltantes().isEmpty() ? "faltan productos: " + String.join(", ", grupo.faltantes())
                    : grupo.mixto() ? "precios distintos" : grupo.precio() == null ? "sin precio" : null;
            if (problema != null) problemas.add(entrada.nombre() + ": " + problema);
            String precio = problema != null ? (grupo != null && grupo.mixto() ? "VARIOS" : "SIN PRECIO")
                    : "$" + grupo.precio().stripTrailingZeros().toPlainString();
            for (var caja : entrada.cajas()) {
                var escritura = preparar(entrada.pagina(), caja, precio, problema != null);
                if (escritura == null) {
                    problemas.add(entrada.nombre() + ": el importe no cabe de forma legible en la carta");
                    // Un signo visible permite seleccionar el precio en el editor sin ocultar su etiqueta.
                    escritura = preparar(entrada.pagina(), caja, "!", true);
                }
                escrituras.add(escritura);
            }
        }
        // Validar todo antes de modificar el documento evita entregar una carta parcial.
        if (!vistaEditor && !problemas.isEmpty()) {
            throw new ValidacionException("Revisa Menú y precios antes de imprimir: " + String.join("; ", problemas));
        }
        var renglones = escrituras.stream().filter(Escritura::renglon).map(Escritura::caja).collect(Collectors.toSet());
        new BorradorPreciosPdf(mapa.datos().entradas(), renglones).aplicar(documento);
        for (int numero = 1; numero <= documento.getNumberOfPages(); numero++) {
            var pagina = documento.getPage(numero - 1);
            try (var lienzo = new PDPageContentStream(documento, pagina, PDPageContentStream.AppendMode.APPEND, true, true)) {
                for (var escritura : escrituras) {
                    if (escritura.pagina() != numero) continue;
                    lienzo.setNonStrokingColor(escritura.problema() ? java.awt.Color.RED : java.awt.Color.BLACK);
                    lienzo.beginText();
                    lienzo.setFont(escritura.fuente(), escritura.tamano());
                    lienzo.newLineAtOffset(escritura.x(), pagina.getCropBox().getUpperRightY() - escritura.caja().base());
                    lienzo.showText(escritura.texto());
                    lienzo.endText();
                }
            }
        }
    }

    private Escritura preparar(int pagina, MapaPrecios.Caja caja, String precio, boolean problema) throws IOException {
        var fuente = new PDType1Font(caja.encabezado() ? Standard14Fonts.FontName.TIMES_ROMAN
                : caja.negrita() ? (caja.cursiva() ? Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE : Standard14Fonts.FontName.HELVETICA_BOLD)
                : caja.cursiva() ? Standard14Fonts.FontName.HELVETICA_OBLIQUE : Standard14Fonts.FontName.HELVETICA);
        float disponible = caja.ancho();
        if (caja.encabezado()) {
            float borde = caja.x() >= 297.75f ? 297.75f : 0;
            disponible = Math.max(disponible, Math.min(48f, caja.x() + caja.ancho() - borde));
        }
        float tamano = Math.min(Math.max(MINIMO, caja.tamano()), disponible * 1000 / fuente.getStringWidth(precio));
        boolean renglon = tamano < MINIMO;
        String etiqueta = "";
        if (renglon) {
            if (caja.etiqueta() == null || caja.etiqueta().isBlank()) return null;
            etiqueta = caja.etiqueta() + " ";
            disponible = caja.x() + caja.ancho() - caja.xTexto();
            tamano = Math.min(Math.max(MINIMO, caja.tamano()), disponible * 1000 / fuente.getStringWidth(etiqueta + precio));
            if (tamano < MINIMO) return null;
        }
        float ancho = fuente.getStringWidth(etiqueta + precio) / 1000 * tamano;
        float punto = fuente.getStringWidth(".") / 1000 * tamano;
        int puntos = caja.encabezado() ? 0 : Math.max(0, (int) ((disponible - ancho) / punto));
        String texto = etiqueta + ".".repeat(puntos) + precio;
        float x = caja.x() + caja.ancho() - fuente.getStringWidth(texto) / 1000 * tamano;
        return new Escritura(pagina, caja, fuente, tamano, texto, x, renglon, problema);
    }
}
