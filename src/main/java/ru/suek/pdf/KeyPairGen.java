package ru.suek.pdf;

import ru.CryptoPro.JCP.params.AlgIdSpec;
import ru.CryptoPro.JCP.params.CryptDhAllowedSpec;
import ru.CryptoPro.JCP.params.OID;
import ru.CryptoPro.JCP.tools.AlgorithmTools;
import ru.CryptoPro.JCPRequest.GostCertificateRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class KeyPairGen {
    public static final String CONT_NAME_A_2001 = "Cont_A";
    public static final char[] PASSWORD_A_2001 = "a".toCharArray();
    public static final String DNAME_A_2001 = "CN=Container_A, O=CryptoPro, C=RU";
    public static final String CONT_NAME_B_2001 = "Cont_B";
    public static final char[] PASSWORD_B_2001 = "b".toCharArray();
    public static final String DNAME_B_2001 = "CN=Container_B, O=CryptoPro, C=RU";
    public static final String CONT_NAME_A_2012_256 = "Cont_A_2012_256";
    public static final char[] PASSWORD_A_2012_256 = "a2".toCharArray();
    public static final String DNAME_A_2012_256 = "CN=Container_A_2012_256, O=CryptoPro, C=RU";
    public static final String CONT_NAME_B_2012_256 = "Cont_B_2012_256";
    public static final char[] PASSWORD_B_2012_256 = "b2".toCharArray();
    public static final String DNAME_B_2012_256 = "CN=Container_B_2012_256, O=CryptoPro, C=RU";
    public static final String CONT_NAME_A_2012_512 = "Cont_A_2012_512";
    public static final char[] PASSWORD_A_2012_512 = "a3".toCharArray();
    public static final String DNAME_A_2012_512 = "CN=Container_A_2012_512, O=CryptoPro, C=RU";
    public static final String CONT_NAME_B_2012_512 = "Cont_B_2012_512";
    public static final char[] PASSWORD_B_2012_512 = "b3".toCharArray();
    public static final String DNAME_B_2012_512 = "CN=Container_B_2012_512, O=CryptoPro, C=RU";

    public KeyPairGen() {
    }

    public static void main(String[] var0) throws Exception {
        main_("GOST3410_2012_256", "Cont_A_2012_256", PASSWORD_A_2012_256, "CN=Container_A_2012_256, O=CryptoPro, C=RU", "Cont_B_2012_256", PASSWORD_B_2012_256, "CN=Container_B_2012_256, O=CryptoPro, C=RU");
        main_("GOST3410_2012_512", "Cont_A_2012_512", PASSWORD_A_2012_512, "CN=Container_A_2012_512, O=CryptoPro, C=RU", "Cont_B_2012_512", PASSWORD_B_2012_512, "CN=Container_B_2012_512, O=CryptoPro, C=RU");
    }

    public static void main_(String var0, String var1, char[] var2, String var3, String var4, char[] var5, String var6) throws Exception {
        saveKeyWithCert(genKey(var0), var1, var2, var3);
        OID var7 = new OID("1.2.643.2.2.19");
        OID var8 = new OID("1.2.643.2.2.35.2");
        OID var9 = new OID("1.2.643.2.2.30.1");
        OID var10 = new OID("1.2.643.2.2.31.1");
        if (var0.equals("GOST3410_2012_256")) {
            var7 = new OID("1.2.643.7.1.1.1.1");
            var8 = new OID("1.2.643.2.2.35.2");
            var9 = new OID("1.2.643.7.1.1.2.2");
            var10 = new OID("1.2.643.7.1.2.5.1.1");
        } else if (var0.equals("GOST3410_2012_512")) {
            var7 = new OID("1.2.643.7.1.1.1.2");
            var8 = new OID("1.2.643.7.1.2.1.2.1");
            var9 = new OID("1.2.643.7.1.1.2.3");
            var10 = new OID("1.2.643.7.1.2.5.1.1");
        }

        saveKeyWithCert(genKeyWithParams(var0, var7, var8, var9, var10), var4, var5, var6);
        KeyStore var11 = KeyStore.getInstance("HDImageStore");
        var11.load((InputStream)null, (char[])null);
        PrivateKey var12 = (PrivateKey)var11.getKey(var1, var2);
        PrivateKey var13 = (PrivateKey)var11.getKey(var4, var5);
        System.out.println("OK");
    }

    public static KeyPair genKey(String var0) throws Exception {
        KeyPairGenerator var1 = KeyPairGenerator.getInstance(var0);
        return var1.generateKeyPair();
    }

    public static KeyPair genKey(String var0, String var1) throws Exception {
        KeyPairGenerator var2 = KeyPairGenerator.getInstance(var0, var1);
        return var2.generateKeyPair();
    }

    public static KeyPair genKeyAllowDh(String var0) throws Exception {
        KeyPairGenerator var1 = KeyPairGenerator.getInstance(var0);
        var1.initialize(new CryptDhAllowedSpec());
        return var1.generateKeyPair();
    }

    public static KeyPair genKeyWithParams(String var0, OID var1, OID var2, OID var3, OID var4) throws Exception {
        KeyPairGenerator var5 = KeyPairGenerator.getInstance(var0);
        AlgIdSpec var6 = new AlgIdSpec(var1, var2, var3, var4);
        var5.initialize(var6);
        return var5.generateKeyPair();
    }

    public static void saveKeyWithCert(KeyPair var0, String var1, char[] var2, String var3) throws Exception {
        Certificate[] var4 = new Certificate[]{genSelfCert(var0, var3)};
        KeyStore var5 = KeyStore.getInstance("HDImageStore");
        var5.load((InputStream)null, (char[])null);
        var5.setKeyEntry(var1, var0.getPrivate(), var2, var4);
        var5.store((OutputStream)null, (char[])null);
    }

    public static Certificate genSelfCert(KeyPair var0, String var1) throws Exception {
        return genSelfCert(var0, var1, (String)null);
    }

    public static Certificate genSelfCert(KeyPair var0, String var1, String var2) throws Exception {
        GostCertificateRequest var3 = var2 != null ? new GostCertificateRequest(var2) : new GostCertificateRequest();
        String var4 = AlgorithmTools.getSignatureAlgorithmByPrivateKey(var0.getPrivate());
//        byte[] var5 = var3.getEncodedSelfCert(var0, var1, var4);
        CertificateFactory var6 = CertificateFactory.getInstance("X509");
//        return var6.generateCertificate(new ByteArrayInputStream(var5));
        return null;
    }
}

