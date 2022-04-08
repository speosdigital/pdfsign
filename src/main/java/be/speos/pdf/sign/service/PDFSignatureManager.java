package be.speos.pdf.sign.service;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import be.speos.pdf.sign.dto.PdfEncodingSignatureType;

public interface PDFSignatureManager {

    void signPDF(final String pathToPdfToSign, final String pathToSignedPdf, boolean suffix, final Certificate[] certificates, final PrivateKey privateKey, final PdfEncodingSignatureType signatureType, final String signatureReason, final String signatureLocation, final boolean signatureVisible, final Object signatureBlock) throws Exception;
}
