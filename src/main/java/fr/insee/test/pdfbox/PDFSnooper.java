package fr.insee.test.pdfbox;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Methods for inspecting PDF files.
 */
public class PDFSnooper {

	public static void main(String[] args) {

		String fileFolder = "src/main/resources/data/VQS/";
		// String inFileName = "col5000004603.impr.VQS_2021_REL.1.dlm.pdf";
		String inFileName = "col5000004972.impr.VQS_2021_REL_TEST.1.dlm.pdf";
		Path fileFolderPath = Paths.get(fileFolder);

		File pdfFile = new File(fileFolder + inFileName);
		try (final PDDocument document = PDDocument.load(pdfFile)) {
			PDDocumentInformation documentInformation = document.getDocumentInformation();
			// Print document information
			for (String key : documentInformation.getMetadataKeys())
				System.out.println(key + " - " + documentInformation.getPropertyStringValue(key));
			System.out.println("Number of pages: " + document.getNumberOfPages());
			// Print information about pages and images to the console
			PDPageTree pageList = document.getPages();
			for (PDPage page : pageList) {
				printPage(page, System.out, true);
				printImageSizes(extractImages(page, fileFolderPath, false), System.out);
			}
			// Print list of COS objects in the document
			System.out.println("\n--------- Document COS objects ---------");
			for (COSObject cosObject : document.getDocument().getObjects()) {
				printCOSObject(cosObject, System.out);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints details of a PDF page to a <code>PrintStream</code>, optionally dumping the PDF streams.
	 *
	 * @param page The PDF page (<code>PDPage</code>) to explore.
	 * @param out The <code>PrintStream</code> to print to (can be <code>System.out</code>).
	 * @param printStreams If <code>true</code>, the streams found in the PDF file will be dumped.
	 * @throws IOException In case of problems reading the PDF file or writing the report.
	 */
	public static void printPage(PDPage page, PrintStream out, boolean printStreams) throws IOException {

		out.println("\n--------- New page ---------");
		if (page.getMetadata() != null) {
			out.println("Page metadata:");
			IOUtils.copy(page.getMetadata().createInputStream(), out);
			out.println();
		}
		for (PDAnnotation annotation : page.getAnnotations())
			out.println("Found annotation " + annotation.getAnnotationName());
		for (Iterator<PDStream> it = page.getContentStreams(); it.hasNext(); ) {
			PDStream stream = it.next();
			out.println("Found stream of length " + stream.getLength());
			if (printStreams) {
				IOUtils.copy(stream.createInputStream(), out);
				out.println();
			}
		}
		PDResources pdResources = page.getResources();
		// Print metadata for images (but it seems to be always null)
		for (COSName name : pdResources.getXObjectNames()) {
			PDXObject object = pdResources.getXObject(name);
			if (object instanceof PDImageXObject) {
				PDImageXObject image = (PDImageXObject)object;
				if (image.getMetadata() != null) {
					out.println("Image metadata:");
					IOUtils.copy(image.getMetadata().createInputStream(), out);
					out.println();
				}
			}
		}
	}

	/**
	 * Extracts the images from a PDF page and saves them to a destination folder, returns the image sizes.
	 *
	 * @param page The PDF page (<code>PDPage</code>) to explore.
	 * @param destination <code>Path</code> of the destination folder.
	 * @param fileSizes If <code>true</code> the sizes of the file images are returned, otherwise the size of the PDF streams.
	 * @return A map with image names and corresponding sizes, plus the total sizes of files and PDF streams.
	 * @throws IOException In case of problems reading the PDF file or writing the report.
	 */
	public static Map<String, Long> extractImages(PDPage page, Path destination, boolean fileSizes) throws IOException {

		String imageFormat = "png";

		Map<String, Long> imageSizes = new HashMap<>();
		long totalFileSize = 0L;
		long totalPDFSize = 0L;
		PDResources pdResources = page.getResources();
		for (COSName name : pdResources.getXObjectNames()) {
			PDXObject object = pdResources.getXObject(name);
			if (object instanceof PDImageXObject) {
				PDImageXObject image = (PDImageXObject)object;
				String imageName = "Image " + StringUtils.getDigits(name.toString());
				String fileName = "extracted-image-" + imageName + "." + imageFormat;
				File imageFile = new File(destination.toFile(), fileName);
				ImageIO.write(image.getImage(), imageFormat, imageFile);
				long imageFileSize = imageFile.length();
				long imagePDFSize = ((COSInteger) object.getCOSObject().getDictionaryObject(COSName.LENGTH)).longValue();
				if (fileSizes) imageSizes.put(imageName, imageFileSize);
				else imageSizes.put(imageName, imagePDFSize);
				totalFileSize += imageFileSize;
				totalPDFSize += imagePDFSize;
			}
		}
		imageSizes.put("Total size in PDF", totalPDFSize);
		imageSizes.put("Total size as " + imageFormat.toUpperCase() + " files", totalFileSize);
		return imageSizes;
	}

	/**
	 * Prints information on a COS object to a <code>PrintStream</code>.
	 *
	 * @param object The <code>COSObject</code> to explore.
	 * @param out The <code>PrintStream</code> to print to (can be <code>System.out</code>).
	 */
	public static void printCOSObject(COSObject object, PrintStream out) {

		out.print("COS object " + object.getObjectNumber());
		out.print(" (" + object.getGenerationNumber() + ")");
		COSBase type = object.getDictionaryObject(COSName.TYPE);
		out.print((type == null) ? " (no type) " : " type " + type);
		COSBase subType = object.getDictionaryObject(COSName.SUBTYPE);
		out.print((subType == null) ? " (no subtype) " :  " subtype " + subType);
		out.println();
	}

	// Prints the map of image sizes produced by extractImages
	private static void printImageSizes(Map<String, Long> imageList, PrintStream out) {

		imageList.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.forEach(entry -> out.println(entry.getKey() + "\t\t" + entry.getValue() + " bytes"));
	}
}
