package restaurante.api.infra.impresora;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import restaurante.api.orden.DatosPlatilloTicket;
import restaurante.api.orden.DatosTicketCocina;
import restaurante.api.orden.Tipo;

import javax.print.PrintService;

@Service
public class ImpresoraService {

    @Value("${impresora.cocina.nombre}")
    private String nombreImpresoraCocina;

    @Async
    public void imprimirComandaCocina(DatosTicketCocina ticket) {
        try {
            // Obtener el servicio de impresión por nombre exacto en Windows
            PrintService printService = PrinterOutputStream.getPrintServiceByName(nombreImpresoraCocina);
            if (printService == null) {
                System.err.println("🖨️❌ No se encontró la impresora llamada: " + nombreImpresoraCocina);
                return;
            }

            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            EscPos escpos = new EscPos(printerOutputStream);

            // Estilos
            Style titulo = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Center)
                    .setBold(true);

            Style subtitulo = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Center)
                    .setBold(true);

            Style normal = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1);

            Style negrita = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setBold(true);
                    
            Style accionStyle = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Right);

            // Cabecera del ticket
            escpos.writeLF(titulo, "NUEVA ORDEN");
            escpos.writeLF(subtitulo, "ORDEN #" + ticket.id_orden());
            escpos.writeLF("================================");
            
            if (ticket.tipo() == Tipo.LOZA && ticket.id_mesa() != null) {
                escpos.writeLF(titulo, "MESA " + ticket.id_mesa());
            } else {
                escpos.writeLF(titulo, "PARA LLEVAR");
            }
            
            escpos.writeLF("Mesero: " + ticket.nombre());
            escpos.writeLF("================================");
            escpos.feed(1);

            // Platillos
            for (DatosPlatilloTicket p : ticket.platillos()) {
                escpos.writeLF(negrita, p.cantidad() + "x " + p.nombre());
                
                if (p.comentarios() != null && !p.comentarios().isBlank()) {
                    escpos.writeLF(normal, "  *" + p.comentarios() + "*");
                }
                
                // Imprimir la acción si no es "Gris" o si representa algo vital (NUEVO, CANCELADO, MODIFICADO)
                if (p.accion() != null && !p.accion().isEmpty()) {
                    // Limpiamos los emojis para la impresora térmica porque suelen fallar al imprimir
                    String accionLimpia = p.accion().replaceAll("[^a-zA-Z ]", "").trim();
                    escpos.writeLF(accionStyle, "[" + accionLimpia + "]");
                }
                escpos.feed(1);
            }

            escpos.writeLF("================================");
            escpos.writeLF(normal, "        -- FIN ORDEN --         ");
            
            // Avanzar papel y cortar
            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);
            escpos.close();

            System.out.println("🖨️✅ Ticket impreso correctamente en: " + nombreImpresoraCocina);

        } catch (Exception e) {
            System.err.println("🖨️❌ Error al imprimir en cocina: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
