package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.isec.platform.modules.documents.domain.ValuationLetter;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD);
    private static final Font FONT_SMALL_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

    private final ResourceLoader resourceLoader;

    @Value("${branding.company.name:Your Insurance Co. Ltd}")
    private String companyName;
    @Value("${branding.company.address:P.O. Box 12345-00100, Nairobi, Kenya}")
    private String companyAddress;
    @Value("${branding.company.contacts:+254 700 000 000 | info@example.com}")
    private String companyContacts;
    @Value("${branding.company.website:www.example.com}")
    private String companyWebsite;
    @Value("${branding.logo-path:classpath:/branding/logo.png}")
    private String logoPath;
    @Value("${branding.signature-path:classpath:/branding/signature.png}")
    private String signaturePath;

    private final QrCodeService qrCodeService;
    private final PdfSecurityService pdfSecurityService;

    public byte[] generateValuationLetter(Map<String, Object> data, List<AuthorizedValuer> valuers) {
        return generateValuationLetter(data, valuers, null);
    }

    public byte[] generateValuationLetter(Map<String, Object> data, List<AuthorizedValuer> valuers, ValuationLetter metadata) {
        log.info("Starting PDF generation for valuation letter. documentUuid={}", 
                metadata != null ? metadata.getDocumentUuid() : "N/A");
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            log.debug("Adding logo and header details");
            // Company Logo (top)
            addLogoIfAvailable(document);

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
            
            Paragraph insuredPara = new Paragraph();
            insuredPara.add(new Phrase("INSURED: ", FONT_BOLD));
            insuredPara.add(new Phrase(safe((String) data.get("insuredName")), FONT_NORMAL));
            document.add(insuredPara);

            Paragraph policyPara = new Paragraph();
            policyPara.add(new Phrase("POLICY NO: ", FONT_BOLD));
            policyPara.add(new Phrase(safe((String) data.get("policyNumber")), FONT_NORMAL));
            document.add(policyPara);

            Paragraph regPara = new Paragraph();
            regPara.add(new Phrase("REGISTRATION NO: ", FONT_BOLD));
            regPara.add(new Phrase(safe((String) data.get("registrationNumber")), FONT_NORMAL));
            document.add(regPara);

            document.add(new Paragraph("\n"));

            // Body
            String bodyText = "The above mentioned vehicle is covered with us for comprehensive insurance. " +
                    "To enable us to determine the value of the vehicle for insurance purposes, " +
                    "please arrange to have the vehicle valued by any of our authorized motor valuers listed below:";
            document.add(new Paragraph(bodyText, FONT_NORMAL));

            document.add(new Paragraph("\n"));

            // Valuers Table (Name, Contact Person, Email, Location, Telephone)
            log.debug("Adding valuers table with {} entries", valuers.size());
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 4, 3, 3});

            addTableCell(table, "NAME OF VALUER", FONT_SMALL_BOLD);
            addTableCell(table, "CONTACT PERSON", FONT_SMALL_BOLD);
            addTableCell(table, "EMAIL", FONT_SMALL_BOLD);
            addTableCell(table, "LOCATION", FONT_SMALL_BOLD);
            addTableCell(table, "TELEPHONE", FONT_SMALL_BOLD);

            for (AuthorizedValuer valuer : valuers) {
                addTableCell(table, safe(valuer.getCompanyName()), FONT_SMALL);
                addTableCell(table, safe(valuer.getContactPerson()), FONT_SMALL);
                addTableCell(table, safe(valuer.getEmail()), FONT_SMALL);
                addTableCell(table, safe(valuer.getLocations()), FONT_SMALL);
                addTableCell(table, safe(valuer.getPhoneNumbers()), FONT_SMALL);
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
            document.add(new Paragraph("\n"));
            addSignatureIfAvailable(document);
            document.add(new Paragraph("__________________________", FONT_NORMAL));
            document.add(new Paragraph("UNDERWRITING DEPARTMENT", FONT_BOLD));

            document.add(new Paragraph("\n"));

            // Company footer details
            Paragraph footerTitle = new Paragraph(companyName, FONT_BOLD);
            footerTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(footerTitle);
            Paragraph footerAddress = new Paragraph(companyAddress, FONT_SMALL);
            footerAddress.setAlignment(Element.ALIGN_CENTER);
            document.add(footerAddress);
            Paragraph footerContacts = new Paragraph(companyContacts + " | " + companyWebsite, FONT_SMALL);
            footerContacts.setAlignment(Element.ALIGN_CENTER);
            document.add(footerContacts);

            document.add(new Paragraph("\n"));

            // Regulatory footnote
            Paragraph regulator = new Paragraph("Regulated by the Insurance Regulatory Authority", FONT_SMALL);
            regulator.setAlignment(Element.ALIGN_CENTER);
            document.add(regulator);

            if (metadata != null) {
                log.debug("Adding security features: metadata and verification footer");
                addVerificationFooter(document, metadata);
                addMetadata(writer, metadata);
            }

            document.close();
            log.info("PDF generation completed successfully. size={} bytes", out.size());
        } catch (DocumentException e) {
            log.error("CRITICAL: Error during PDF generation", e);
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void addLogoIfAvailable(Document document) {
        addImageIfAvailable(document, logoPath, 120, 60, Element.ALIGN_LEFT);
    }

    private void addSignatureIfAvailable(Document document) {
        addImageIfAvailable(document, signaturePath, 100, 50, Element.ALIGN_LEFT);
    }

    private void addImageIfAvailable(Document document, String path, float width, float height, int alignment) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource != null && resource.exists()) {
                byte[] bytes = resource.getInputStream().readAllBytes();
                Image img = Image.getInstance(bytes);
                img.scaleToFit(width, height);
                img.setAlignment(alignment);
                document.add(img);
            } else {
                log.debug("Image resource not found at path: {}", path);
            }
        } catch (IOException | DocumentException ex) {
            log.warn("Could not load/add image from {}: {}", path, ex.getMessage());
        }
    }

    private void addVerificationFooter(Document document, ValuationLetter letter) {
        try {
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{4, 1});

            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(PdfPCell.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph p = new Paragraph("Scan to verify document authenticity", FONT_SMALL_BOLD);
            textCell.addElement(p);
            Paragraph p2 = new Paragraph("Document ID: " + letter.getDocumentUuid(), FONT_SMALL);
            textCell.addElement(p2);
            Paragraph p3 = new Paragraph("Verification URL: " + getVerificationUrl(letter.getDocumentUuid().toString()), FONT_SMALL);
            textCell.addElement(p3);

            footerTable.addCell(textCell);

            String qrUrl = getVerificationUrl(letter.getDocumentUuid().toString());
            byte[] qrCode = qrCodeService.generateQrCode(qrUrl, 100, 100);
            Image qrImage = Image.getInstance(qrCode);
            qrImage.scaleToFit(60, 60);
            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(PdfPCell.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footerTable.addCell(qrCell);

            document.add(new Paragraph("\n"));
            document.add(footerTable);
        } catch (Exception e) {
            log.warn("Could not add verification footer: {}", e.getMessage());
        }
    }

    private void addMetadata(PdfWriter writer, ValuationLetter letter) {
        com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
        writer.addViewerPreference(com.lowagie.text.pdf.PdfName.DISPLAYDOCTITLE, com.lowagie.text.pdf.PdfBoolean.PDFTRUE);
        
        // OpenPDF uses Info dictionary for metadata
        com.lowagie.text.pdf.PdfDictionary info = writer.getInfo();
        info.put(com.lowagie.text.pdf.PdfName.AUTHOR, new com.lowagie.text.pdf.PdfString(companyName));
        info.put(com.lowagie.text.pdf.PdfName.CREATOR, new com.lowagie.text.pdf.PdfString(companyName));
        info.put(com.lowagie.text.pdf.PdfName.TITLE, new com.lowagie.text.pdf.PdfString("Valuation Letter - " + letter.getVehicleRegistrationNumber()));
        
        // Custom metadata
        info.put(new com.lowagie.text.pdf.PdfName("documentId"), new com.lowagie.text.pdf.PdfString(letter.getDocumentUuid().toString()));
        info.put(new com.lowagie.text.pdf.PdfName("issuer"), new com.lowagie.text.pdf.PdfString(companyName));
        info.put(new com.lowagie.text.pdf.PdfName("issuedAt"), new com.lowagie.text.pdf.PdfString(letter.getGeneratedAt().toString()));
        info.put(new com.lowagie.text.pdf.PdfName("documentType"), new com.lowagie.text.pdf.PdfString("VALUATION_LETTER"));
        info.put(new com.lowagie.text.pdf.PdfName("verificationUrl"), new com.lowagie.text.pdf.PdfString(getVerificationUrl(letter.getDocumentUuid().toString())));
        info.put(new com.lowagie.text.pdf.PdfName("version"), new com.lowagie.text.pdf.PdfString("1.0"));
        if (letter.getDocumentHash() != null) {
            info.put(new com.lowagie.text.pdf.PdfName("documentHash"), new com.lowagie.text.pdf.PdfString(letter.getDocumentHash()));
        }
    }

    private String getVerificationUrl(String documentId) {
        return companyWebsite + "/verify/doc/" + documentId;
    }
}
