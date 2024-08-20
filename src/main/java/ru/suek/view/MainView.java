package ru.suek.view;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.ExternalBlankSignatureContainer;
import com.itextpdf.text.pdf.security.ExternalSignatureContainer;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PdfPKCS7;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.router.Route;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonObject;
import elemental.json.impl.JreJsonString;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.CryptoPro.JCP.JCP;
import ru.CryptoPro.JCSP.JCSP;
import ru.suek.model.FileDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ru.suek.event.SignContent.getStampTextBuilder;

@Route("")
@JavaScript("./js-1.0/cadesplugin_api.js")
@JavaScript("./js-1.0/crypto_plugin.js")
@JavaScript("./js-1.0/create_sign.js")
public class MainView extends VerticalLayout {
    /**
     * уникальное имя записываемого сертификата
     */

    private Grid<FileDTO> grid;
    private TextField filterField;
    private Button editButton;
    private Button deleteButton;

    private List<File> files;

    private List<File> getListElements(String directoryPath) {
        File folder = new File(directoryPath);
        List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf")));
        return files;
    }

    private ComboBox<JSONObject> getComboWithCertificates() {
        ComboBox<JSONObject> comboBox = new ComboBox<>();
        List<JSONObject> options = new ArrayList<>();
        this.getElement().executeJs("return certList()")
                .then(result -> {

                            JreJsonArray jreJsonArray = (JreJsonArray) result;
                            String s = jreJsonArray.toJson();
                            System.out.println("return certList()-> " + s);

                            JSONArray jsonArray = new JSONArray(s);
                            System.out.println("jsonArray" + jsonArray);

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject json = (JSONObject) jsonArray.get(i);
                                options.add(json);
                            }

                            comboBox.setItems(options);
                            comboBox.setItemLabelGenerator(eachCertificate -> eachCertificate.get("name").toString());
                        }
                );

        return comboBox;
    }

    public MainView(List<File> files) {
        files = getListElements("resources/data/PDF");
        System.out.println("resource pdf size: " + files.size());

        List<String> descriptions = new ArrayList<>();
        for (File file : files) {
            descriptions.add("Описание для файла " + file.getName());
        }

        AtomicInteger counter = new AtomicInteger(0);
        List<FileDTO> dtoFiles = files
                .stream()
                .map(file -> {
                    long fileLength = file.length();
                    long sizeMb = fileLength / 1024 / 1024;
                    long sizeKb = fileLength / 1024;
                    FileDTO dto = new FileDTO(file.getName(),
                            descriptions.get(counter.getAndIncrement()), file.getAbsolutePath(), sizeMb > 0 ? file.length() / 1024 / 1024 + " Мб" : sizeKb > 0 ? sizeKb + "Кб" : fileLength + " байт"
                    );
                    return dto;
                })
                .toList();

        System.out.println("file dto elements: " + dtoFiles);

        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);

        Button signButton = createSignButton(); //create button & set disable

        Div div = new Div();
        this.getElement().executeJs("return oAbout()")
                .then(result -> {
                            System.out.println("result: " + result.getClass().getSimpleName() + ": " + result);
                            JreJsonString jreJsonString = (JreJsonString) result;
                            String s = jreJsonString.toJson();
                            System.out.println("s: " + s);
                            if (s != null && !s.isEmpty()) {
                                div.setText("Версия плагина CryptoPro: " + s.replaceAll("\"", ""));
                                div.getStyle().set("background-color", "rgba(144, 238, 144, 0.5)");
                            }
                        }
                );

        if (div.getText() == null || "".equals(div.getText())) {
            div.setText("Версия плагина CryptoPro: неопределена, проверьте установку");
            div.getStyle().set("background-color", "rgba(255, 0, 0, 0.5)");
        }

        HorizontalLayout header = new HorizontalLayout(div);
        HorizontalLayout toolbar = new HorizontalLayout(signButton);
        grid = createGrid(dtoFiles);
        grid.addSelectionListener(event -> {
            signButton.setEnabled(event.getAllSelectedItems().size() > 0);
        });

        IFrame iFrame = new IFrame("html/footer.html");
        iFrame.setWidth("100%");
        iFrame.setHeight("33%");

        add(header, toolbar, grid, iFrame);
    }

    private Button createSignButton() {
        Button signButton = new Button("Подписать", VaadinIcon.PENCIL.create());
        signButton.addClickListener(e -> showDialog());
        signButton.setEnabled(false);

        return signButton;
    }

    private Button createUploadButton() {
        Button uploadButton = new Button("Указать сертификат");
        uploadButton.addClickListener(e -> showUploadDialog());

        return uploadButton;
    }

    private void showUploadDialog() {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("application/x-x509-ca-cert");
        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            InputStream inputStream = buffer.getInputStream();
            Notification.show("Сертификат " + fileName + " загружен успешно!");

            dialog.close();
        });

        dialog.add(upload);
        dialog.open();
    }

    private HorizontalLayout createToolbar(List<Component> elements) {

        Button btn = (Button) elements.get(0);
        HorizontalLayout toolbar = new HorizontalLayout(btn);
        toolbar.setWidth("100%");
        toolbar.setVerticalComponentAlignment(Alignment.END, btn);

        return toolbar;
    }

    private ComboBox<String> getAliasesBox(KeyStore keyStore) {
        Map<String, String> options = new HashMap<>();

        try {
            System.out.println("key store type: " + keyStore.getType());
            Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                Certificate[] certificateChain = keyStore.getCertificateChain(alias);
                X509Certificate x509Certificate = (X509Certificate) certificateChain[0];
                String ownerName = x509Certificate.getSubjectX500Principal().getName();
                String[] cnParts = ownerName.split(",");
                String cn = null;
                for (String part : cnParts) {
                    if (part.startsWith("CN=")) {
                        cn = part.substring(3);
                    }
                }

                if (cn == null) {
                    cn = alias;
                }

                options.put(alias, cn);
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        if (options.isEmpty()) {
            Notification.show("Не обнаружены хранилища корневых сертификатов");
            return new ComboBox<>();
        } else {
            ComboBox<String> aliasesBox = new ComboBox<>("Выберете сертификат для подписи");
            aliasesBox.setItems(options.keySet());
            aliasesBox.setItemLabelGenerator(options::get);

            return aliasesBox;
        }
    }

    private KeyStore getKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(JCSP.HD_STORE_NAME, JCSP.PROVIDER_NAME);
            keyStore.load(null, null);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return keyStore;
    }

    private void showDialog() {
        MultiSelect<Grid<FileDTO>, FileDTO> selection = grid.asMultiSelect();
        Notification.show(selection.getValue().parallelStream().map(FileDTO::getName).collect(Collectors.joining(",")));

        Set<FileDTO> selectedItems = selection.getSelectedItems();
        Iterator<FileDTO> it = selectedItems.iterator();
        while (it.hasNext()) {
            FileDTO fileDTO = it.next();
            System.out.println("fileDTO: " + fileDTO);
        }

        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        //cancel
        HorizontalLayout cancelLayout = new HorizontalLayout();
        Button cancelButton = new Button("", VaadinIcon.CLOSE.create());
        Icon icon = (Icon) cancelButton.getIcon();
        icon.setColor("red");
        cancelButton.addClickListener(e -> dialog.close());
        cancelLayout.add(cancelButton);
        cancelLayout.setJustifyContentMode(JustifyContentMode.END);

        //approve
        VerticalLayout approveLayout = new VerticalLayout();

//        KeyStore keyStore = getKeyStore();
//        ComboBox<String> comboBox = getAliasesBox(keyStore);
        ComboBox<JSONObject> comboBox = getComboWithCertificates();
        comboBox.setWidthFull();

        Div infoText = new Div();
        infoText.getElement().setProperty("innerHTML", "<strong>Подтвердите подписание выбранных файлов<strong>");
        Button approveButton = new Button("Подписать файлы", VaadinIcon.FILE_PROCESS.create());
        approveButton.getElement().getStyle().set("color", "#5c995e");
        approveButton.addClickListener(event -> {
            if (comboBox.isEmpty()) {
                Notification.show("Пожалуйста, выберете сертификат!", 1000, Notification.Position.MIDDLE);
                comboBox.focus();
            } else {
                JSONObject selectedComboBoxValue = comboBox.getValue();

                //получаем отпечаток SHA1 (id) сертификата
                String certId = selectedComboBoxValue.get("id").toString();
                System.out.println("selectedComboBoxValue id: " + certId + " name: " + selectedComboBoxValue.get("name"));

                selection.getValue().forEach(fileDTO -> {
                    //формируем ссылки на исходный файл и подписанный
                    String rootPath = fileDTO.getPath();
                    System.out.println("rootPath: " + rootPath);
                    String stampedPath = rootPath
                            .replace("\\PDF\\", "/PDF/STAMPED/")
                            .replaceAll("\\.pdf", "_stamped.pdf");
                    System.out.println("stampedPath: " + stampedPath);
                    String outputPath = stampedPath
                            .replaceAll("/PDF/STAMPED/", "/PDF/SIGNED/")
                            .replaceAll("_stamped.pdf", "_signed.pdf");
                    System.out.println("outputPath: " + outputPath);

                    this.getElement().executeJs("return getCertInfo($0, $1)", /*sha1*/ certId, /*options*/ null)
                            .then(oInfo -> {
                                //Определяем алгоритм
                                String algorithm = getAlgorithm(oInfo);
                                System.out.println("got algorithm: " + algorithm);
                                try (FileOutputStream fos = new FileOutputStream(stampedPath)) {
                                    PdfReader reader = new PdfReader(rootPath);
                                    PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0');

                                    float padding = 10;
                                    //Определение координат для рамки
                                    //Получение ширины страницы
                                    float pageWidth = reader.getPageSize(1).getWidth();
                                    System.out.println("page width: " + pageWidth);
                                    float width = pageWidth / 2 + padding * 2; //Ширина рамки
                                    float x = pageWidth - width - padding * 2; //Положение по X
                                    float height = 12 * 4 * 2; //Высота рамки
                                    float y = height + padding * 2; //Положение по Y

                                    PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                                    appearance.setVisibleSignature(new Rectangle(x, y, x + width + padding * 2, y + height + padding * 2), 1, "signatureField");

                                    BaseFont bf = BaseFont.createFont("./resources/fonts/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                                    Font font = new Font(bf, 10);
                                    appearance.setLayer2Font(font);
                                    StringBuilder stampText = getStampTextBuilder(oInfo);
                                    appearance.setLayer2Text(stampText.toString());
                                    ExternalSignatureContainer external = new ExternalBlankSignatureContainer(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
                                    MakeSignature.signExternalContainer(appearance, external, 8192);

                                    stamper.close();
                                    reader.close();
                                } catch (IOException | DocumentException | GeneralSecurityException e) {
                                    System.err.println("exception ! ! " + ExceptionUtils.getMessage(e));
                                    throw new RuntimeException(e);
                                }

                                //Подписываем документ подписью
                                StringBuilder hashResult = new StringBuilder();
                                try {
                                    MessageDigest md;
                                    if (algorithm.contains("2012") && algorithm.contains("256")) {
                                        System.out.println("-> 2012_256");

                                        md = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_256_NAME);
                                    } else if (algorithm.contains("2012") && algorithm.contains("512")) {
                                        System.out.println("-> 2012_512");
                                        md = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_512_NAME);
                                    } else {
                                        System.out.println("-> 3411");
                                        md = MessageDigest.getInstance(JCP.GOST_DIGEST_NAME);
                                    }

                                    System.out.println("outputPath: " + stampedPath);
                                    Path path = Paths.get(stampedPath);
                                    byte[] pdfBytes = Files.readAllBytes(path);
                                    md.update(pdfBytes);

                                    StringBuilder hexString = new StringBuilder();
                                    // Вычисление хеша
                                    byte[] fileDigest = md.digest();
                                    // Преобразование хеша в шестнадцатеричную строку
                                    for (byte b : fileDigest) {
                                        String hex = Integer.toHexString(0xff & b);
                                        if (hex.length() == 1) hexString.append('0');
                                        hexString.append(hex);
                                    }
                                    System.out.println("Хеш документа на подпись: " + hexString);
                                    System.out.println("сохранен с контейнером на " + stampedPath);


                                    //На клиенте подписываем hash от сервера
                                    this.getElement().executeJs("return signHash($0, $1)", hexString.toString(), certId)
                                            .then(
                                            result -> {
                                                if (result instanceof JreJsonString) {
                                                    hashResult.append(result.asString());
                                                    System.out.println("Результат подписи хеша файла:\n" + hashResult);
                                                }


                                                try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                                                    PdfReader reader = new PdfReader(stampedPath);
                                                    PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0');

                                                    float padding = 10;
                                                    //Определение координат для рамки
                                                    //Получение ширины страницы
                                                    float pageWidth = reader.getPageSize(1).getWidth();
                                                    System.out.println("page width: " + pageWidth);
                                                    float width = pageWidth / 2 + padding * 2; //Ширина рамки
                                                    float x = pageWidth - width - padding * 2; //Положение по X
                                                    float height = 12 * 4 * 2; //Высота рамки
                                                    float y = height + padding * 2; //Положение по Y

                                                    PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                                                    appearance.setVisibleSignature(new Rectangle(x, y, x + width + padding * 2, y + height + padding * 2), 1, "signatureField");

                                                    BaseFont bf = BaseFont.createFont("./resources/fonts/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                                                    Font font = new Font(bf, 10);
                                                    appearance.setLayer2Font(font);
                                                    StringBuilder stampText = getStampTextBuilder(oInfo);
                                                    appearance.setLayer2Text(stampText.toString());

                                                    // Использование внешнего контейнера для подписи
                                                    ExternalSignatureContainer external = new MyExternalSignatureContainer(hashResult.toString().getBytes());
                                                    MakeSignature.signExternalContainer(appearance, external, 8192);
                                                    System.out.println("SIGNED!!!: " + outputPath);

                                                    stamper.close();
                                                    reader.close();
                                                } catch (IOException | DocumentException | GeneralSecurityException e) {
                                                    System.err.println("exception ! ! " + ExceptionUtils.getMessage(e));
                                                    throw new RuntimeException(e);
                                                }

                                            });
                                } catch (NoSuchAlgorithmException e) {
                                    throw new RuntimeException(e);
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                });
                dialog.close();
            }
        });

        approveLayout.setPadding(false);
        approveLayout.getElement().getStyle().set("margin-top", "20px");
        approveLayout.add(comboBox, infoText, approveButton);

        //group layouts
        dialog.add(cancelLayout, approveLayout);
        dialog.open();
    }

    private String getAlgorithm(JsonValue oInfo) {
        JreJsonObject jsonObject = (JreJsonObject) oInfo;
        System.out.println("oInfo: " + oInfo);
        JSONObject mainObject = new JSONObject(jsonObject);
        System.out.println("main object: " + mainObject);
        JSONObject object = mainObject.getJSONObject("object");
        String algorithm = object.getString("Algorithm");
        System.out.println("algorithm: " + algorithm);
        return algorithm;
    }

    private boolean isPdfHeader(String header) {
        // Проверка, начинается ли строка с "JVBERi0xLjQKJeLjz9MKMyAwIG9iago8"
        return header.startsWith("JVBERi0xLjQKJeLjz9MKMyAwIG9iago8");
    }

    private static void printInfo(PrivateKey privateKey,
                                  X509Certificate certificate) {

        System.out.println("Private key: " + privateKey);
        System.out.println("Certificate:\n   Sn - " +
                certificate.getSerialNumber().toString(16) +
                "\n   Subject - " + certificate.getSubjectDN() +
                "\n   Issuer - " + certificate.getIssuerDN());
    }

    private void checkProviders() {
        try {
            MessageDigest digest = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_512_NAME, JCP.PROVIDER_NAME);
            System.out.println("get algo: " + digest.getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        for (Provider provider : Security.getProviders()) {
            System.out.println("provider name: " + provider.getName());
            for (Provider.Service service : provider.getServices()) {
                if ("MessageDigest".equals(service.getType())) {
                    System.out.println("algorithm:  " + service.getAlgorithm());
                }
            }
        }
    }

    private void loadKeyStore() {
        System.setProperty("file.encoding", "UTF-8");

        KeyStore ks;
        try {
            ks = KeyStore.getInstance("HDIMAGE", "JCSP");
            ks.load(null, null);
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                Certificate cert = ks.getCertificate(aliases.nextElement());
                if (cert == null) {
                    continue;
                }
                if (!(cert instanceof X509Certificate)) {
                    continue;
                }
                X509Certificate curCert = (X509Certificate) cert;
                System.out.println("curCert: " + curCert);
            }

        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Grid<FileDTO> createGrid(List<FileDTO> elements) {
        Grid<FileDTO> grid = new Grid<>(FileDTO.class);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setItems(new ArrayList<>(elements));

        grid.getColumnByKey("name").setHeader("Имя");
        grid.getColumnByKey("description").setHeader("Описание");
        grid.getColumnByKey("size").setHeader("Размер");
        grid.removeColumnByKey("path");
//        grid.getColumnByKey("path").setHeader("Путь");

        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        return grid;
    }
}

class MyExternalSignatureContainer implements ExternalSignatureContainer {
    private final byte[] signedHash;

    public MyExternalSignatureContainer(byte[] signedHash) {
        this.signedHash = signedHash;
    }

    @Override
    public byte[] sign(InputStream data) {
        return signedHash; // Возвращаем подписанный хэш
    }

    @Override
    public void modifySigningDictionary(PdfDictionary signDic) {
        // Здесь можно модифицировать словарь подписи, если необходимо
    }
}