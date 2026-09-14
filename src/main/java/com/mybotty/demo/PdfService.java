package com.mybotty.demo;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generatePdf(
            String title,
            List<Map<String, Object>> information
    ) throws Exception {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        Document document =
                new Document();

        PdfWriter.getInstance(
                document,
                output
        );

        document.open();

        document.add(
                new Paragraph(title)
        );

        document.add(
                new Paragraph("\n")
        );

        int count = 1;

        for (Map<String, Object> item :
                information) {

            document.add(
                    new Paragraph(
                            count + ". "
                                    + String.valueOf(
                                    item.get("text")
                            )
                    )
            );

            document.add(
                    new Paragraph(
                            "Category: "
                                    + String.valueOf(
                                    item.get("category")
                            )
                    )
            );

            document.add(
                    new Paragraph(
                            "Created: "
                                    + String.valueOf(
                                    item.get("createdAt")
                            )
                    )
            );

            document.add(
                    new Paragraph("\n")
            );

            count++;
        }

        document.close();

        return output.toByteArray();
    }
}