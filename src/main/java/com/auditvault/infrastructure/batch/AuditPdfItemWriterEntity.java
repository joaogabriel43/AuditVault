package com.auditvault.infrastructure.batch;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.awt.*;
import java.io.FileOutputStream;

public class AuditPdfItemWriterEntity implements ItemWriter<String> {

    private final String outputPath;

    public AuditPdfItemWriterEntity(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath, true));
        document.open();
        for (String line : chunk.getItems()) {
            document.add(new Paragraph(line,
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK)));
        }
        document.close();
    }

    public String getOutputPath() {
        return outputPath;
    }
}
