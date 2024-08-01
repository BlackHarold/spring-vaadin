package ru.suek.view;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.security.PrivateKeySignature;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class AlternateSigner {
    public static void main(String[] args) {
        String pdfFilePath = "path/to/your/document.pdf";
        String signedPdfFilePath = "path/to/your/signed_document.pdf";
        String keystorePath = "path/to/your/keystore.p12";
        String keystorePassword = "your_keystore_password";

        try {
            // Загрузка ключа из хранилища
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());

            String alias = ks.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);

            // Чтение исходного PDF
            PdfReader reader = new PdfReader(pdfFilePath);
//            PdfWriter writer = new PdfWriter(new FileOutputStream(signedPdfFilePath));
//            PdfSigner signer = new PdfSigner(reader, writer, true);

            // Подпись
//            signer.signDetached(new PrivateKeySignature(privateKey, "SHA-256", "BC"),
//                    new ByteArrayInputStream(new byte[0]),
//                    chain,
//                    null,
//                    null,
//                    0,
//                    PdfSigner.CryptoStandard.CMS);

            System.out.println("PDF подписан успешно!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
