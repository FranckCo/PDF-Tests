package fr.insee.test.itext7;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SmartWriter {

    static String inFolderPath = "src/main/resources/data/VQS/";
    static String inFileName = "col5000004603.impr.VQS_2021_REL.1.dlm.pdf";
    static String outFolderPath = "src/main/resources/data/VQS/";

    public static void main(String[] args) throws Exception {

        int copies = 3;
        List<String> filesToMerge = new ArrayList<>();
        for (int copy = 0; copy < copies; copy++) filesToMerge.add(inFolderPath + inFileName);

        String outFileName = "Merged-" + copies + ".pdf";
        String outFilePath = outFolderPath + outFileName;
        merge(filesToMerge, outFilePath);

        long inFileSize = (new File(inFolderPath + inFileName)).length();
        long outFileSize = (new File(outFilePath)).length();

        StringBuilder report = new StringBuilder("Merged ").append(copies).append((copies > 1) ? " copies" : " copy");
        report.append(" of file ").append(inFileName).append(" of size ").append(inFileSize).append(" kb.");
        report.append("\nResulting file ").append(outFileName).append(" is of size ").append(outFileSize).append(" kb.");
        System.out.println(report);
    }

    /**
     * Merges a list of PDF files using the smart mode (resources are not duplicated).
     *
     * @param inFilePaths List of paths of input PDF files.
     * @param outFilePath Path of the resulting merged PDF file.
     */
    public static void merge(List<String> inFilePaths, String outFilePath) {

        try (PdfWriter smartWriter = new PdfWriter(outFilePath)) {
            smartWriter.setSmartMode(true);
            PdfDocument outPDF = new PdfDocument(smartWriter);
            PdfMerger merger = new PdfMerger(outPDF);
            for (String inFilePath : inFilePaths) {
                PdfDocument pdfSource = new PdfDocument(new PdfReader(inFilePath));
                merger.merge(pdfSource, 1, pdfSource.getNumberOfPages());
                pdfSource.close();
            }
            merger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
