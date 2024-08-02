package ru.suek.pdf;

import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.*;
import com.vaadin.flow.component.notification.Notification;
import org.bouncycastle.tsp.TimeStampToken;
import ru.CryptoPro.Crypto.CryptoProvider;
import ru.CryptoPro.JCP.JCP;
import ru.CryptoPro.JCP.tools.Array;
import ru.CryptoPro.JCSP.JCSP;

import java.io.*;
import java.security.*;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Пример подписи и проверки PDF документа.
 * Используется пропатченный itextpdf версии 5.5.5
 * с патчем.
 *
 * @author Copyright 2004-2014 Crypto-Pro. All rights reserved.
 * @version 2.5
 */
public class SignVerifyPDFExample {

    /**
     * Исходный PDF документ.
     */
    private static final String IN_PDF_FILE =
            System.getProperty("user.dir")
                    + File.separator
                    + "resources"
                    + File.separator
                    + "data"
                    + File.separator
                    + "PDF"
                    + File.separator
                    + "source.pdf";

    /**
     * Папка для сохранения подписанных PDF документов.
     */
    private static final String OUT_PDF_FILE =
            System.getProperty("user.dir")
                    + File.separator
                    + "temp"
                    + File.separator;

    /**
     * Корневой сертификат (для помещения цепочки сертификатов
     * в PDF документ).
     */
    private static final String ROOT_CERT =
            System.getProperty("user.dir")
                    + File.separator
                    + "resources"
                    + File.separator
                    + "data"
                    + File.separator
                    + "CERTS"
                    + File.separator
                    + "VERIFY"
                    + File.separator
                    + "certnew.cer";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
//        not found: ru.CryptoPro.reprov.x509
//        JCPInit.initProviders(false);
//        Security.addProvider(new JCSP());

        byte[] rootContent = Array.readFile(ROOT_CERT);
        X509Certificate root = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(rootContent));

        // ГОСТ Р 34.10-2012 (256)
        sign(
                genKeyPair(JCP.GOST_DH_2012_256_NAME, CryptoProvider.PROVIDER_NAME),
                JCP.GOST_DIGEST_2012_256_NAME,
                JCP.GOST_SIGN_2012_256_NAME,
                JCP.PROVIDER_NAME,
                "CN=exc_2012_256, C=RU",
                root,
                IN_PDF_FILE,
                OUT_PDF_FILE + "signed.2012_256.pdf",
                "Crypto-Pro LLC", "Test signature (2012-256)", "+7 TEL",
                false,
                Certificates.HTTP_ADDRESS
        );

        verify(OUT_PDF_FILE + "signed.2012_256.pdf", null, null, JCSP.PROVIDER_NAME);

        // ГОСТ Р 34.10-2012 (512)
        sign(
                genKeyPair(JCP.GOST_DH_2012_512_NAME, CryptoProvider.PROVIDER_NAME),
                JCP.GOST_DIGEST_2012_512_NAME,
                JCP.GOST_SIGN_2012_512_NAME,
                JCP.PROVIDER_NAME,
                "CN=exc_2012_512, C=RU",
                root,
                IN_PDF_FILE,
                OUT_PDF_FILE + "signed.2012_512.pdf",
                "Crypto-Pro LLC", "Test signature (2012-512)", "+7 TEL",
                false,
                Certificates.HTTP_ADDRESS
        );

        verify(OUT_PDF_FILE + "signed.2012_512.pdf", null, null, JCSP.PROVIDER_NAME);

    }

    /**
     * Генерация ключевой пары.
     *
     * @param keyAlgName Алгоритм ключа.
     * @param provider   Имя провайдера.
     * @return ключевая пара.
     * @throws Exception
     */
    public static KeyPair genKeyPair(String keyAlgName, String provider) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(keyAlgName, provider);
        return kpg.generateKeyPair();
    }

    /**
     * Подпись PDF документа.
     *
     * @param keyPair       Ключевая пара.
     * @param hashAlgorithm Алгоритм хеширования.
     * @param signAlgName   Алгоритм подписи.
     * @param signProvider  Провайдер хеширования и подписи.
     * @param dnName        DN для создания сертификата.
     * @param root          Корневой сертификат для построения цепочки.
     * @param fileToSign    Исходный PDF документ.
     * @param signedFile    Подписанный PDF документ.
     * @param location      Адрес.
     * @param reason        Описание.
     * @param httpAddress   Адрес УЦ.
     * @throws Exception
     */
    public static void sign(KeyPair keyPair, String hashAlgorithm,
                            String signAlgName, String signProvider, String dnName,
                            X509Certificate root, String fileToSign, String signedFile,
                            String location, String reason, String contact, boolean isCAdES, String
                                    httpAddress) throws Exception {

        sign(keyPair, hashAlgorithm, signAlgName, signProvider,
                dnName, root, fileToSign, signedFile, location,
                reason, contact, false, isCAdES, httpAddress);

    }

    /**
     * Подпись PDF документа.
     *
     * @param keyPair       Ключевая пара.
     * @param hashAlgorithm Алгоритм хеширования.
     * @param signAlgName   Алгоритм подписи.
     * @param signProvider  Провайдер хеширования и подписи.
     * @param dnName        DN для создания сертификата.
     * @param root          Корневой сертификат для построения цепочки.
     * @param fileToSign    Исходный PDF документ.
     * @param signedFile    Подписанный PDF документ.
     * @param append        True, если добавляется еще одна подпись.
     * @param location      Адрес.
     * @param reason        Описание.
     * @param httpAddress   Адрес УЦ.
     * @throws Exception
     */
    public static void sign(KeyPair keyPair, String hashAlgorithm,
                            String signAlgName, String signProvider, String dnName,
                            X509Certificate root, String fileToSign, String signedFile,
                            String location, String reason, String contact, boolean append, boolean isCAdES,
                            String httpAddress) throws Exception {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        byte[] signerContent = Certificates.createRequestAndGetCert(
                keyPair, signAlgName, signProvider, dnName, httpAddress);

        Certificate signer = factory.generateCertificate(
                new ByteArrayInputStream(signerContent));

        Certificate[] chain = new Certificate[2];
        chain[0] = signer;
        chain[1] = root;

        sign(keyPair.getPrivate(), hashAlgorithm, signProvider,
                chain, fileToSign, signedFile, location, reason, contact, append);

    }

    /**
     * Подпись PDF документа.
     *
     * @param privateKey    Ключ подписи.
     * @param hashAlgorithm Алгоритм хеширования.
     * @param signProvider  Провайдер хеширования и подписи.
     * @param chain         Цепочка сертификатов.
     * @param fileToSign    Исходный PDF документ.
     * @param signedFile    Подписанный PDF документ.
     * @param append        True, если добавляется еще одна подпись.
     * @param location      Адрес.
     * @param reason        Описание.
     * @throws Exception
     */
    public static void sign(PrivateKey privateKey, String hashAlgorithm,
                            String signProvider, Certificate[] chain, String fileToSign,
                            String signedFile, String location, String reason, String contact, boolean append) throws Exception {
        System.out.println("hashAlgorithm: " + hashAlgorithm + ", signProvider: " + signProvider + ", location: " + location + ", reason: " + reason + ", append: " + append);

        PdfReader reader = new PdfReader(fileToSign);
        try (FileOutputStream fout = new FileOutputStream(signedFile)) {

            PdfStamper stp = append
                    ? PdfStamper.createSignature(reader, fout, '\0', null, true)
                    : PdfStamper.createSignature(reader, fout, '\0');

            PdfSignatureAppearance appearance = stp.getSignatureAppearance();

            appearance.setCertificate(chain[0]);
            appearance.setReason(reason);
            appearance.setLocation(location);
            appearance.setContact(contact);

            PdfSignature dic = new PdfSignature(PdfName.ADOBE_CryptoProPDF,
                    PdfName.ADBE_PKCS7_DETACHED);
            System.out.println("cert type: " + chain[0].getType() + ", got DIC, signed file to: " + signedFile);

            dic.setReason(appearance.getReason());
            dic.setLocation(appearance.getLocation());
            dic.setSignatureCreator(appearance.getSignatureCreator());
            dic.setContact(appearance.getContact());
            dic.setDate(new PdfDate(appearance.getSignDate())); // time-stamp will over-rule this

            appearance.setCryptoDictionary(dic);
            int estimatedSize = 8192;

            HashMap<PdfName, Integer> exc = new HashMap<>();
            exc.put(PdfName.CONTENTS, Integer.valueOf(estimatedSize * 2 + 2));

            appearance.preClose(exc);

            InputStream data = appearance.getRangeStream();

            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            byte[] hash = DigestAlgorithms.digest(data, md);

            Calendar cal = Calendar.getInstance();

            PdfPKCS7 pkcs7 = null;
            try {
                pkcs7 = new PdfPKCS7(privateKey, chain, hashAlgorithm, signProvider, null, true);
            } catch (InvalidKeyException | NoSuchProviderException | NoSuchAlgorithmException iek) {
                System.out.println("any error: " + iek.getMessage());
                new RuntimeException(iek);
            }

            byte[] sh = pkcs7.getAuthenticatedAttributeBytes(hash, cal, null, null, MakeSignature.CryptoStandard.CMS);

            pkcs7.update(sh, 0, sh.length);

            System.out.println("incoming algo: " + md.getAlgorithm() + "time: " + cal.getTime());
            byte[] encodedSig = pkcs7.getEncodedPKCS7(hash, cal, (TSAClient) null, (byte[]) null, (Collection) null, MakeSignature.CryptoStandard.CMS);

            if (estimatedSize < encodedSig.length) {
                throw new IOException("Not enough space");
            }

            byte[] paddedSig = new byte[estimatedSize];
            System.arraycopy(encodedSig, 0, paddedSig, 0, encodedSig.length);

            PdfDictionary dic2 = new PdfDictionary();
            dic2.put(PdfName.CONTENTS, new PdfString(paddedSig).setHexWriting(true));

            appearance.close(dic2);
            stp.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            reader.close();
        }
    }

    /**
     * Проверка подписи PDF документа.
     *
     * @param fileToVerify PDF документ для проверки.
     * @param trustStore   Хранилище с корневыми сертификатами.
     * @param crl          CRL для проверки цепочки сертификатов.
     * @param provider     Имя провайдера для проверки подписи.
     * @return количество подписей.
     * @throws Exception
     */
    public static int verify(String fileToVerify, KeyStore trustStore,
                             CRL crl, String provider) throws Exception {

        List<CRL> crlList = null;

        if (crl != null) {
            crlList = new ArrayList<>(1);
            crlList.add(crl);
        } // if

        PdfReader checker = new PdfReader(fileToVerify);
        AcroFields af = checker.getAcroFields();

        ArrayList<String> signatureNames = af.getSignatureNames();
        if (signatureNames.size() == 0) {
            throw new Exception("Signatures not found.");
        }

        for (String signatureName : signatureNames) {
            PdfPKCS7 pk = af.verifySignature(signatureName, provider);

            boolean verified = pk.verify();
            if (!verified) {
                Notification.show("Подпись установлена, но не проходит проверку методом verifySignature класса PdfPKCS7");
//                throw new Exception("Invalid signature: " + signatureName);
            } else {
                Notification.show("Подпись подтверждена");
            }

            Calendar calendar = pk.getSignDate();
            X509Certificate pkc[] = (X509Certificate[]) pk.getSignCertificateChain();
            TimeStampToken ts = pk.getTimeStampToken();

            if (ts != null) {
                boolean imprint = pk.verifyTimestampImprint();
                calendar = pk.getTimeStampDate();
                System.out.println("Timestamp imprint verified: " + imprint + ", Timestamp date: " + calendar);
            }

            System.out.println("Signature: " + signatureName + ", Certificated subject: " +
                    pk.getSigningCertificate().getSubjectDN());

            System.out.println("Document wasn't modified.");
            if (trustStore != null) {
                List<VerificationException> fails = CertificateVerification
                        .verifyCertificates(pkc, trustStore, crlList, calendar);

                if (fails.isEmpty()) {
                    System.out.println("Certificates verified against the key store");
                } else {
                    System.err.println("Certificate validation failed: ");
                    for (VerificationException fail : fails) {
                        fail.printStackTrace();
                    }

                    throw new Exception("Validation failed.");
                }
            }

        }

        return signatureNames.size();

    }

}
