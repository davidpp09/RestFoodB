package restaurante.api.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import restaurante.api.precio.CampoPrecio;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.HexFormat;
import java.security.MessageDigest;

@Component
public class MapaPrecios {
    public record Caja(float x, float y, float ancho, float alto, float base, float tamano,
                       boolean negrita, boolean encabezado, float xTexto, String etiqueta, boolean cursiva) {
        public boolean contieneRenglon(float px, float py) {
            return px >= xTexto - 0.2f && px <= x + ancho + 0.2f && py >= y - 0.2f && py <= y + alto + 0.2f;
        }
        public boolean contiene(float px, float py) {
            return px >= x - 0.2f && px <= x + ancho + 0.2f && py >= y - 0.2f && py <= y + alto + 0.2f;
        }
    }
    public record Entrada(String clave, int pagina, CampoPrecio campo, String nombre,
                          List<String> productos, BigDecimal precioPlantilla, List<Caja> cajas) {}
    public record Datos(String plantillaSha256, float anchoPagina, float altoPagina, List<Entrada> entradas) {}
    private final Datos datos;

    public MapaPrecios() throws IOException {
        try (var in = getClass().getResourceAsStream("/menu/mapa-precios.json")) {
            if (in == null) throw new IOException("No existe el mapa de precios");
            datos = new ObjectMapper().readValue(in, Datos.class);
        }
        try (var in = getClass().getResourceAsStream("/menu/plantilla-menu.pdf")) {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
            if (!datos.plantillaSha256().equals(hash)) throw new IOException("La plantilla cambió: hay que recalibrar el mapa de precios");
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    public Datos datos() { return datos; }
}
