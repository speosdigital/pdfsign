package be.speos.pdf.sign.service.impl;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;

import be.speos.pdf.sign.dto.PdfEncodingSignatureType;
import be.speos.pdf.sign.dto.PdfSignatureMethod;
import be.speos.pdf.sign.service.PDFSignatureManager;
import be.speos.pdf.sign.util.FileSystem;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ITextPDFSignatureManager implements PDFSignatureManager {

    private static final String SIGNED_PDF_FILENAME_SUFFIX = "_signed";

    @Override
    public void signPDF(String pathToPdfToSign, String pathToSignedPdf, boolean suffix, Certificate[] certificates, PrivateKey privateKey, PdfEncodingSignatureType signatureType, String signatureReason, String signatureLocation, boolean signatureVisible, Object signatureBlock) throws Exception {
        StringBuilder signedFilename = new StringBuilder(pathToSignedPdf);

        if (suffix) {
            signedFilename.insert(signedFilename.length() - 4, SIGNED_PDF_FILENAME_SUFFIX);
        }

        if ((pathToPdfToSign == null) || (pathToSignedPdf == null)) {
            log.error("No PDF to sign or no destination file. Pdf to sign = '" + pathToPdfToSign + "'. Signed Pdf name = '" + pathToSignedPdf + "'.");
            throw new IllegalArgumentException("pathToPdfToSign and pathToSignedPdf cannot be null.");
        } else if ((pathToPdfToSign.isEmpty()) || (pathToSignedPdf.isEmpty())) {
            log.error("No PDF to sign or no destination file. Pdf to sign = '" + pathToPdfToSign + "'. Signed Pdf name = '" + pathToSignedPdf + "'.");
            throw new IllegalArgumentException("pathToPdfToSign and pathToSignedPdf cannot be empty.");
        } else if (!FileSystem.exists(pathToPdfToSign)) {
            log.error("PDF to sign '" + pathToPdfToSign + "' does not exists. Impossible to produce signed PDF.");
            throw new IllegalArgumentException(pathToPdfToSign + "' does not exists.'");
        }

        PdfReader reader = new PdfReader(pathToPdfToSign);
        log.debug("Open PDF reader for '" + pathToPdfToSign + "' : DONE");

        String signedPdfDirectory = FileSystem.getDirPath(signedFilename.toString());
        if (!FileSystem.exists(signedPdfDirectory)) {
            FileSystem.createDir(signedPdfDirectory);
        }

        FileOutputStream writer = new FileOutputStream(signedFilename.toString());
        log.debug("Open PDF writer for '" + signedFilename.toString() + "' : DONE");

        Calendar cal = Calendar.getInstance();
        PdfStamper stamper = PdfStamper.createSignature(reader, writer, '\0');
        PdfSignatureAppearance signatureAppearance = stamper.getSignatureAppearance();

        String signatureContact = PdfPKCS7.getSubjectFields((X509Certificate) certificates[0]).getField("CN");
        signatureAppearance.setContact(signatureContact);

        signatureAppearance.setSignDate(cal);

        if (signatureReason != null && !signatureReason.isEmpty()) {
            signatureAppearance.setReason(signatureReason);
        }

        if (signatureLocation != null && !signatureLocation.isEmpty()) {
            signatureAppearance.setLocation(signatureLocation);
        }

        if (signatureVisible) {
            if (signatureBlock == null) {
                log.error("INTERNAL : No signature block defined.");
            } else if (!(signatureBlock instanceof Rectangle)) {
                log.error("INTERNAL : Wrong signature block object. It should be an instance of '" + Rectangle.class.getName() + "' instead of '" + signatureBlock.getClass().getName() + "'");
            }

            signatureAppearance.setVisibleSignature((Rectangle) signatureBlock, 1, null);
        }
        log.debug("Creation of signature handler for '" + signedFilename.toString() + "' : DONE");

        if (signatureType.getPdfSignatureMethod() == PdfSignatureMethod.SIGNATURE_FIELD) {
            signatureAppearance.setCrypto(privateKey, certificates, null, PdfSignatureAppearance.WINCER_SIGNED);
            stamper.close();
        } else if (signatureType.getPdfSignatureMethod() == PdfSignatureMethod.PKCS7_OBJECT) {
            PdfSignature signatureDictionnary = new PdfSignature(PdfName.VERISIGN_PPKVS, PdfName.ADBE_PKCS7_DETACHED);
            signatureDictionnary.setContact(signatureAppearance.getContact());
            signatureDictionnary.setDate(new PdfDate(signatureAppearance.getSignDate()));
            if (signatureReason != null && !signatureReason.isEmpty()) {
                signatureDictionnary.setReason(signatureAppearance.getReason());
            }
            if (signatureLocation != null && !signatureLocation.isEmpty()) {
                signatureDictionnary.setLocation(signatureAppearance.getLocation());
            }

            signatureAppearance.setCryptoDictionary(signatureDictionnary);
            signatureAppearance.setCrypto(privateKey, certificates, null, null);

            String hashAlgorithm = signatureType.getHashAlgorithm();
            if (hashAlgorithm == null || hashAlgorithm.isEmpty()) {
                log.error("INTERNAL : No algorithm defined in order to compute document hash for signature type : " + signatureType);
                throw new NoSuchAlgorithmException("No algorithm defined in order to compute document hash for signature type : " + signatureType);
            }

            int signatureEstimatedSize = this.getPKCS7Signature(new ByteArrayInputStream("fake".getBytes()), privateKey, certificates, hashAlgorithm).length;

            HashMap<PdfName, Integer> excludedSignatureContent = new HashMap<>();
            excludedSignatureContent.put(PdfName.CONTENTS, signatureEstimatedSize * 2 + 2);
            signatureAppearance.preClose(excludedSignatureContent);
            log.debug("Creation of signature dictionary for '" + signedFilename.toString() + "' : DONE");

            byte[] signature = this.getPKCS7Signature(signatureAppearance.getRangeStream(), privateKey, certificates, hashAlgorithm);

            byte[] paddedSignature = new byte[signatureEstimatedSize];
            System.arraycopy(signature, 0, paddedSignature, 0, signature.length);
            log.debug("Creation of signature of '" + signatureContact + "' for ' " + signedFilename.toString() + "' : DONE");

            PdfDictionary dictionaryElementsToUpdate = new PdfDictionary();
            dictionaryElementsToUpdate.put(PdfName.CONTENTS, new PdfString(paddedSignature).setHexWriting(true));
            signatureAppearance.close(dictionaryElementsToUpdate);
        }

        writer.close();
        log.debug("PDF Signing : " + signedFilename.toString() + " signed.");
    }

    private byte[] getPKCS7Signature(InputStream dataToSign, PrivateKey privateKey, Certificate[] certificates, String hashAlgorithmName) throws Exception {
        PdfPKCS7 signatureCreator = new PdfPKCS7(privateKey, certificates, null, hashAlgorithmName, null, false);

        byte[] buffer = new byte[2048];
        int len;
        while ((len = dataToSign.read(buffer)) > 0) {
            signatureCreator.update(buffer, 0, len);
        }

        return signatureCreator.getEncodedPKCS7();
    }

}
