package restaurante.api.menu;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import restaurante.api.precio.DatosGrupoPrecio;
import restaurante.api.infra.errores.ValidacionException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreciosMenuPdfTest {
    @Test void reemplazaTodosLosPreciosSinBorrarNombres() throws Exception {
        var mapa = new MapaPrecios();
        var grupos = grupos(mapa, "desayuno-31", "50.50");
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf"); var documento = Loader.loadPDF(in.readAllBytes())) {
            String antes = new PDFTextStripper().getText(documento);
            new PreciosMenuPdf(mapa).aplicar(documento, grupos, false);
            Files.createDirectories(Path.of("target/menu-precios"));
            try (var salida = Files.newOutputStream(Path.of("target/menu-precios/carta-prueba.pdf"))) { documento.save(salida); }
            String despues = new PDFTextStripper().getText(documento);
            Files.writeString(Path.of("target/menu-precios/texto-prueba.txt"), despues);
            int cajas = mapa.datos().entradas().stream().mapToInt(e -> e.cajas().size()).sum();
            assertEquals(186, cajas);
            assertEquals(2, despues.split("\\$50\\.5", -1).length - 1);
            comprobarTamano(documento);
            assertEquals(cajas, despues.chars().filter(c -> c == '$').count(), "No deben sobrevivir precios antiguos");
            assertTrue(letras(antes).equals(letras(despues)), "No se puede borrar ninguna letra de los nombres");
            Files.createDirectories(Path.of("target/menu-precios"));
        }
    }
    @Test void rechazaImprimirSinGruposPeroPermiteDiagnosticarlosEnEditor() throws Exception {
        var mapa = new MapaPrecios();
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf"); var documento = Loader.loadPDF(in.readAllBytes())) {
            var servicio = new PreciosMenuPdf(mapa);
            assertThrows(ValidacionException.class, () -> servicio.aplicar(documento, List.of(), false));
            servicio.aplicar(documento, List.of(), true);
            assertTrue(new PDFTextStripper().getText(documento).contains("SIN PRECIO"));
        }
    }
    @Test void rechazaImportesQueNoCabenYConservaEtiquetasEnEditor() throws Exception {
        var mapa = new MapaPrecios();
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf"); var documento = Loader.loadPDF(in.readAllBytes())) {
            String antes = new PDFTextStripper().getText(documento);
            var servicio = new PreciosMenuPdf(mapa);
            var grupos = grupos(mapa, "desayuno-31", "999999.99");
            var error = assertThrows(ValidacionException.class, () -> servicio.aplicar(documento, grupos, false));
            assertTrue(error.getMessage().contains("no cabe de forma legible"));
            assertEquals(antes, new PDFTextStripper().getText(documento), "Un rechazo no modifica el PDF");
            servicio.aplicar(documento, grupos, true);
            String despues = new PDFTextStripper().getText(documento);
            assertTrue(despues.contains("!"));
            assertEquals(letras(antes), letras(despues));
            comprobarTamano(documento);
        }
    }
    @Test void permiteImportesGrandesCuandoCaben() throws Exception {
        var mapa = new MapaPrecios();
        // Pozole dispone de un renglón amplio: no restringir importes solo por su número de dígitos.
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf"); var documento = Loader.loadPDF(in.readAllBytes())) {
            new PreciosMenuPdf(mapa).aplicar(documento, grupos(mapa, "desayuno-01", "999999.99"), false);
            assertTrue(new PDFTextStripper().getText(documento).contains("$999999.99"));
            comprobarTamano(documento);
        }
    }
    @Test void rechazaGruposConProductosFaltantes() throws Exception {
        var mapa = new MapaPrecios();
        var grupos = new ArrayList<>(grupos(mapa, "desayuno-31", "50"));
        var original = grupos.get(0);
        grupos.set(0, new DatosGrupoPrecio(original.clave(), original.nombre(), original.campo(),
                original.precio(), false, original.productos(), List.of("Producto esperado")));
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf"); var documento = Loader.loadPDF(in.readAllBytes())) {
            var error = assertThrows(ValidacionException.class, () -> new PreciosMenuPdf(mapa).aplicar(documento, grupos, false));
            assertTrue(error.getMessage().contains("Producto esperado"));
        }
    }
    private List<DatosGrupoPrecio> grupos(MapaPrecios mapa, String clave, String importe) {
        var claves = new HashSet<String>();
        return mapa.datos().entradas().stream().filter(e -> claves.add(e.clave())).map(e -> {
            var precio = e.clave().equals(clave) ? new BigDecimal(importe) : e.precioPlantilla();
            var productos = e.productos().stream().map(n -> new DatosGrupoPrecio.ProductoPrecio(1L, n, precio)).toList();
            return new DatosGrupoPrecio(e.clave(), e.nombre(), e.campo(), precio, false, productos);
        }).toList();
    }
    private void comprobarTamano(org.apache.pdfbox.pdmodel.PDDocument documento) throws Exception {
        new PDFTextStripper() {
            @Override protected void processTextPosition(org.apache.pdfbox.text.TextPosition posicion) {
                if (posicion.getUnicode().equals("$") || posicion.getUnicode().equals("!"))
                    // getFontSizeInPt trunca a entero; YScale conserva el tamaño efectivo con decimales.
                    assertTrue(posicion.getYScale() >= 6.49f, "Los importes deben ser legibles");
                super.processTextPosition(posicion);
            }
        }.getText(documento);
    }
    private List<Integer> letras(String texto) {
        return texto.codePoints().filter(Character::isLetter).sorted().boxed().toList();
    }
}
