package com.auditvault.infrastructure.batch;

import com.auditvault.domain.AuditEvent;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.awt.*;
import java.io.FileOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditPdfItemWriter implements ItemWriter<String> {

    private final String outputPath;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditPdfItemWriter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath, true));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        for (String line : chunk.getItems()) {
            document.add(new Paragraph(line, bodyFont));
        }

        document.close();
    }

    public String getOutputPath() {
        return outputPath;
    }
}
