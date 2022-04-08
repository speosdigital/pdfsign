package be.speos.pdf.sign.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The different types of encoding used to compute a signature in a PDF file.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum PdfEncodingSignatureType {
    SIGNATURE_FIELD_SHA1(0, "", PdfSignatureMethod.SIGNATURE_FIELD, "A pdf signature field with SHA-1 hash algorithm."),

    PKCS7_OBJECT_MD5(1, "MD5", PdfSignatureMethod.PKCS7_OBJECT, "A pdf PKCS7 object with MD5 hash algorithm."),

    PKCS7_OBJECT_SHA1(2, "SHA-1", PdfSignatureMethod.PKCS7_OBJECT, "A pdf PKCS7 object with SHA-1 hash algorithm."),

    PKCS7_OBJECT_SHA256(3, "SHA-256", PdfSignatureMethod.PKCS7_OBJECT, "A pdf PKCS7 object with SHA-256 hash algorithm."),

    PKCS7_OBJECT_SHA512(4, "SHA-512", PdfSignatureMethod.PKCS7_OBJECT, "A pdf PKCS7 object with SHA-512 hash algorithm.");

    private final int id;

    private final String hashAlgorithm;

    private final PdfSignatureMethod pdfSignatureMethod;

    private final String description;

}
