package com.gym.billing;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Arma el recibo de un pago en PDF (tamaño carta, tinta oscura sobre blanco para imprimir). */
final class ReceiptPdf {
    private static final Locale MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", MX);
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", MX);
    private static final Color INK = new Color(0x11, 0x11, 0x14);
    private static final Color MUTED = new Color(0x6b, 0x6b, 0x73);
    private static final Color RULE = new Color(0xdd, 0xdd, 0xe1);
    private static final Color BAND = new Color(0xf3, 0xf3, 0xf5);

    record GymInfo(String name, String address, String phone) {}

    private ReceiptPdf() {}

    static String folio(Payment p) {
        return "R-%06d".formatted(p.getId());
    }

    static String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(MX).format(amount);
    }

    static byte[] render(Payment p, GymInfo gym) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 56, 56, 56, 56);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Recibo " + folio(p));
        doc.addAuthor(gym.name());
        doc.open();

        Font brand = font(18, Font.BOLD, INK);
        Font small = font(9, Font.NORMAL, MUTED);
        Font label = font(9, Font.NORMAL, MUTED);
        Font body = font(11, Font.NORMAL, INK);
        Font bold = font(11, Font.BOLD, INK);

        // Encabezado: gimnasio a la izquierda, folio y fecha a la derecha.
        PdfPTable head = table(2, new float[]{3, 2});
        PdfPCell left = cell();
        left.addElement(new Paragraph(gym.name(), brand));
        String contact = String.join("  ·  ", java.util.stream.Stream.of(gym.address(), gym.phone())
                .filter(s -> s != null && !s.isBlank()).toList());
        if (!contact.isEmpty()) {
            left.addElement(new Paragraph(contact, small));
        }
        head.addCell(left);
        PdfPCell right = cell();
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph("Recibo de pago", font(14, Font.BOLD, INK));
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        Paragraph folio = new Paragraph("Folio " + folio(p), body);
        folio.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(folio);
        Paragraph date = new Paragraph(DAY_TIME.format(p.getPaidAt().atZone(ZoneId.systemDefault())), small);
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(date);
        head.addCell(right);
        doc.add(head);
        doc.add(rule());

        // Socio
        PdfPTable who = table(2, new float[]{1, 1});
        who.addCell(field("Socio", p.getUser().getFullName(), label, bold));
        who.addCell(field("Correo", p.getUser().getEmail(), label, body));
        who.setSpacingAfter(16);
        doc.add(who);

        // Concepto
        PdfPTable items = table(3, new float[]{5, 4, 2});
        items.addCell(headerCell("Concepto", label, Element.ALIGN_LEFT));
        items.addCell(headerCell("Periodo cubierto", label, Element.ALIGN_LEFT));
        items.addCell(headerCell("Importe", label, Element.ALIGN_RIGHT));
        String concept = p.getPlanName() + (p.getQuantity() > 1 ? " × " + p.getQuantity() : "");
        items.addCell(itemCell(concept, body, Element.ALIGN_LEFT));
        items.addCell(itemCell(DAY.format(p.getCoversFrom()) + " al " + DAY.format(p.getCoversTo()), body, Element.ALIGN_LEFT));
        items.addCell(itemCell(money(p.getAmount()), body, Element.ALIGN_RIGHT));
        PdfPCell totalLabel = itemCell("Total", bold, Element.ALIGN_RIGHT);
        totalLabel.setColspan(2);
        totalLabel.setBorder(Rectangle.NO_BORDER);
        items.addCell(totalLabel);
        PdfPCell total = itemCell(money(p.getAmount()), font(13, Font.BOLD, INK), Element.ALIGN_RIGHT);
        total.setBorder(Rectangle.NO_BORDER);
        items.addCell(total);
        items.setSpacingAfter(16);
        doc.add(items);

        PdfPTable extra = table(2, new float[]{1, 1});
        extra.addCell(field("Forma de pago", methodLabel(p.getMethod()), label, body));
        extra.addCell(field("Recibió", p.getRegisteredBy().getFullName(), label, body));
        if (p.getNotes() != null && !p.getNotes().isBlank()) {
            PdfPCell notes = field("Notas", p.getNotes(), label, body);
            notes.setColspan(2);
            extra.addCell(notes);
        }
        doc.add(extra);

        doc.add(rule());
        Paragraph thanks = new Paragraph("Gracias por entrenar con nosotros. Conserva este recibo como comprobante de tu pago.", small);
        thanks.setAlignment(Element.ALIGN_CENTER);
        doc.add(thanks);
        doc.close();
        return out.toByteArray();
    }

    private static String methodLabel(PaymentMethod m) {
        return switch (m) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case TRANSFERENCIA -> "Transferencia";
        };
    }

    private static Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, "Cp1252", false, size, style, color);
    }

    private static PdfPTable table(int columns, float[] widths) {
        PdfPTable t = new PdfPTable(columns);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        return t;
    }

    private static PdfPCell cell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    private static PdfPCell field(String name, String value, Font label, Font body) {
        PdfPCell c = cell();
        c.setPaddingBottom(10);
        c.addElement(new Paragraph(name, label));
        c.addElement(new Paragraph(value, body));
        return c;
    }

    private static PdfPCell headerCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(BAND);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(8);
        c.setHorizontalAlignment(align);
        return c;
    }

    private static PdfPCell itemCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPadding(8);
        c.setPaddingBottom(10);
        c.setHorizontalAlignment(align);
        return c;
    }

    /** Línea divisoria: una tabla vacía con borde inferior, para que mida lo mismo que las demás tablas. */
    private static PdfPTable rule() {
        PdfPTable t = table(1, new float[]{1});
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setBorderWidth(0.6f);
        c.setFixedHeight(1);
        t.addCell(c);
        t.setSpacingBefore(10);
        t.setSpacingAfter(18);
        return t;
    }
}
