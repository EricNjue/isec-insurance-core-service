package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PdfGenerationService {

    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD);
    private static final Font FONT_SMALL_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

    public byte[] generateValuationLetter(Map<String, Object> data, List<AuthorizedValuer> valuers) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Date
            Paragraph datePara = new Paragraph(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), FONT_NORMAL);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            document.add(datePara);

            document.add(new Paragraph("\n"));

            // Header
            Paragraph header = new Paragraph("TO WHOM IT MAY CONCERN", FONT_BOLD);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            document.add(new Paragraph("\n"));

            // Insured Details
            document.add(new Paragraph("RE: VALUATION OF MOTOR VEHICLE", FONT_BOLD));
            document.add(new Paragraph("INSURED: " + data.get("insuredName"), FONT_NORMAL));
            document.add(new Paragraph("POLICY NO: " + data.get("policyNumber"), FONT_NORMAL));
            document.add(new Paragraph("REGISTRATION NO: " + data.get("registrationNumber"), FONT_NORMAL));

            document.add(new Paragraph("\n"));

            // Body
            String bodyText = "The above mentioned vehicle is covered with us for comprehensive insurance. " +
                    "To enable us to determine the value of the vehicle for insurance purposes, " +
                    "please arrange to have the vehicle valued by any of our authorized motor valuers listed below:";
            document.add(new Paragraph(bodyText, FONT_NORMAL));

            document.add(new Paragraph("\n"));

            // Valuers Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 4});

            addTableCell(table, "NAME OF VALUER", FONT_SMALL_BOLD);
            addTableCell(table, "LOCATION", FONT_SMALL_BOLD);
            addTableCell(table, "TELEPHONE", FONT_SMALL_BOLD);

            for (AuthorizedValuer valuer : valuers) {
                addTableCell(table, valuer.getCompanyName(), FONT_SMALL);
                addTableCell(table, valuer.getLocations(), FONT_SMALL);
                addTableCell(table, valuer.getPhoneNumbers(), FONT_SMALL);
            }
            document.add(table);

            document.add(new Paragraph("\n"));

            // Warning Section
            Paragraph warning = new Paragraph("PLEASE NOTE THE FOLLOWING:", FONT_BOLD);
            document.add(warning);

            com.lowagie.text.List bulletList = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
            bulletList.add(new ListItem("The valuation report must be submitted to us within 14 days from the date of this letter.", FONT_NORMAL));
            bulletList.add(new ListItem("If the report is not received within the stated period, the insurance cover will be restricted to THIRD PARTY ONLY.", FONT_NORMAL));
            document.add(bulletList);

            document.add(new Paragraph("\n"));

            // Sign off
            document.add(new Paragraph("Yours faithfully,", FONT_NORMAL));
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("__________________________", FONT_NORMAL));
            document.add(new Paragraph("UNDERWRITING DEPARTMENT", FONT_BOLD));

            document.close();
        } catch (DocumentException e) {
            log.error("Error generating PDF: {}", e.getMessage());
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
