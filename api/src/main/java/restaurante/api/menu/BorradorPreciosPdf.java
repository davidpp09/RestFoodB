package restaurante.api.menu;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.form.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import java.io.*;
import java.util.*;

/** Borra glifos del contenido, conservando los avances y los nombres del mismo Tj. */
final class BorradorPreciosPdf extends PDFTextStripper {
    private static class Glifo {
        final int codigo;
        final float avance;
        boolean borrar;
        Glifo(int codigo, float avance) { this.codigo = codigo; this.avance = avance; }
    }
    private static class Texto {
        PDFont fuente;
        final List<Glifo> glifos = new ArrayList<>();
        boolean cambia() { return glifos.stream().anyMatch(g -> g.borrar); }
    }
    private static class Contexto {
        final Object fuente;
        int indice;
        Texto texto;
        Contexto(Object fuente) { this.fuente = fuente; }
    }
    private Contexto contexto;
    private Glifo glifo;
    private final Map<Object, Map<Integer, Texto>> textos = new IdentityHashMap<>();
    private final Map<COSStream, PDFormXObject> formas = new IdentityHashMap<>();
    private final List<MapaPrecios.Entrada> entradas;
    private final Set<MapaPrecios.Caja> renglones;
    BorradorPreciosPdf(List<MapaPrecios.Entrada> entradas) { this(entradas, Set.of()); }
    BorradorPreciosPdf(List<MapaPrecios.Entrada> entradas, Set<MapaPrecios.Caja> renglones) {
        this.entradas = entradas; this.renglones = renglones;
    }

    void aplicar(PDDocument documento) throws IOException {
        getText(documento);
        for (var registro : textos.entrySet()) {
            if (registro.getValue().values().stream().noneMatch(Texto::cambia)) continue;
            Object fuente = registro.getKey();
            List<Object> tokens = fuente instanceof PDPage pagina ? new PDFStreamParser(pagina).parse()
                    : new PDFStreamParser(formas.get((COSStream) fuente)).parse();
            List<Object> salida = new ArrayList<>(), operandos = new ArrayList<>();
            int indice = 0;
            for (Object token : tokens) {
                if (!(token instanceof Operator op)) { operandos.add(token); continue; }
                Texto texto = esTexto(op.getName()) ? registro.getValue().get(indice++) : null;
                if (texto != null && texto.cambia()) {
                    if (!op.getName().equals("Tj") && !op.getName().equals("TJ")) throw new IOException("Operador de texto no calibrado");
                    COSArray arreglo = new COSArray();
                    int[] posicion = {0};
                    for (Object operando : operandos) {
                        if (operando instanceof COSString cadena) transformar(cadena, texto, posicion, arreglo);
                        else if (operando instanceof COSArray partes) {
                            for (COSBase parte : partes) {
                                if (parte instanceof COSString cadena) transformar(cadena, texto, posicion, arreglo);
                                else arreglo.add(parte);
                            }
                        }
                    }
                    salida.add(arreglo); salida.add(Operator.getOperator("TJ"));
                } else { salida.addAll(operandos); salida.add(op); }
                operandos.clear();
            }
            if (fuente instanceof PDPage pagina) {
                PDStream nuevo = new PDStream(documento);
                try (var out = nuevo.createOutputStream(COSName.FLATE_DECODE)) { new ContentStreamWriter(out).writeTokens(salida); }
                pagina.setContents(nuevo);
            } else try (var out = ((COSStream) fuente).createOutputStream(COSName.FLATE_DECODE)) {
                new ContentStreamWriter(out).writeTokens(salida);
            }
        }
    }
    private void transformar(COSString cadena, Texto texto, int[] posicion, COSArray salida) throws IOException {
        byte[] bytes = cadena.getBytes();
        var entrada = new ByteArrayInputStream(bytes);
        while (entrada.available() > 0) {
            int inicio = bytes.length - entrada.available();
            int codigo = texto.fuente.readCode(entrada);
            int fin = bytes.length - entrada.available();
            Glifo g = texto.glifos.get(posicion[0]++);
            if (g.codigo != codigo) throw new IOException("La codificación del precio no coincide con la calibración");
            if (g.borrar) salida.add(new COSFloat(-g.avance));
            else salida.add(new COSString(Arrays.copyOfRange(bytes, inicio, fin)));
        }
    }
    @Override public void processPage(PDPage pagina) throws IOException {
        contexto = new Contexto(pagina); super.processPage(pagina);
    }
    @Override public void showForm(PDFormXObject forma) throws IOException {
        Contexto padre = contexto;
        formas.put(forma.getCOSObject(), forma); contexto = new Contexto(forma.getCOSObject());
        try { super.showForm(forma); } finally { contexto = padre; }
    }
    @Override public void showTransparencyGroup(PDTransparencyGroup grupo) throws IOException {
        Contexto padre = contexto;
        formas.put(grupo.getCOSObject(), grupo); contexto = new Contexto(grupo.getCOSObject());
        try { super.showTransparencyGroup(grupo); } finally { contexto = padre; }
    }
    @Override protected void processOperator(Operator operador, List<COSBase> operandos) throws IOException {
        if (contexto != null && esTexto(operador.getName())) {
            contexto.texto = new Texto();
            textos.computeIfAbsent(contexto.fuente, k -> new HashMap<>()).put(contexto.indice++, contexto.texto);
        }
        super.processOperator(operador, operandos);
    }
    @Override protected void showGlyph(Matrix matriz, PDFont fuente, int codigo, Vector desplazamiento) throws IOException {
        var estado = getGraphicsState().getTextState();
        // TJ usa milésimas del tamaño de fuente, antes de la escala horizontal.
        float espacio = estado.getCharacterSpacing() + (codigo == 32 ? estado.getWordSpacing() : 0);
        glifo = new Glifo(codigo, desplazamiento.getX() * 1000 + espacio / estado.getFontSize() * 1000);
        if (contexto != null && contexto.texto != null) {
            contexto.texto.fuente = fuente; contexto.texto.glifos.add(glifo);
        }
        super.showGlyph(matriz, fuente, codigo, desplazamiento);
        glifo = null;
    }
    @Override protected void processTextPosition(TextPosition posicion) {
        if (glifo == null) return;
        boolean dentro = entradas.stream().filter(e -> e.pagina() == getCurrentPageNo())
                .flatMap(e -> e.cajas().stream()).anyMatch(c -> c.contiene(posicion.getXDirAdj(), posicion.getYDirAdj()));
        boolean renglon = entradas.stream().filter(e -> e.pagina() == getCurrentPageNo())
                .flatMap(e -> e.cajas().stream()).filter(renglones::contains)
                .anyMatch(c -> c.contieneRenglon(posicion.getXDirAdj(), posicion.getYDirAdj()));
        // Solo las etiquetas recalibradas y reescritas permiten borrar letras.
        glifo.borrar = renglon || dentro && posicion.getUnicode().codePoints().allMatch(c -> Character.isDigit(c) || Character.isWhitespace(c) || c == '$' || c == '.');
    }
    private static boolean esTexto(String op) { return op.equals("Tj") || op.equals("TJ") || op.equals("'") || op.equals("\""); }
}
