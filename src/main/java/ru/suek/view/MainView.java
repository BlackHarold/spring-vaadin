package ru.suek.view;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.security.ExternalBlankSignatureContainer;
import com.itextpdf.text.pdf.security.ExternalSignatureContainer;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonArray;
import elemental.json.impl.JreJsonObject;
import elemental.json.impl.JreJsonString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.CryptoPro.JCP.JCP;
import ru.CryptoPro.JCSP.JCSP;
import ru.suek.model.FileDTO;
import ru.suek.util.Token;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.suek.event.SignContent.getStampTextBuilder;

@Route("/signature")
@JavaScript("./js-1.0/cadesplugin_api.js")
@JavaScript("./js-1.0/crypto_plugin.js")
@JavaScript("./js-1.0/create_sign.js")
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    private Grid<FileDTO> grid;

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

        comboBox.addValueChangeListener(event -> {
            JSONObject selectedComboBoxValue = event.getValue();
            //получаем отпечаток SHA1 (id) сертификата
            certId = selectedComboBoxValue.get("id").toString();
            System.out.println("selectedComboBoxValue id: " + certId + " name: " + selectedComboBoxValue.get("name"));
            algorithm = null;
            this.getElement().executeJs("return getCertInfo($0, $1)", /*sha1*/ certId, /*options*/ null)
                    .then(oInfo -> {
                        //Определяем алгоритм
                        algorithm = getAlgorithm(oInfo);
                        reason = "Документ подписан электронной подписью";
                        location = getLocation(oInfo);
                        contact = getContact(oInfo);
                        System.out.println("got algorithm: " + algorithm);
                        stampText = getStampTextBuilder(oInfo);
                    });

            //TODO Error: WARN if cert is not found!
//                Notification notification = new Notification("Сертификат с отпечатком "
//                        + certId
//                        + " в хранилище не обнаружен, файл не будет подписан корректно. Выберете другой сертификат или обратитесь за помощью");
//                notification.addThemeVariants(NotificationVariant.LUMO_ERROR); // Установите тему
//                notification.getElement().getStyle().set("color", "red"); // Задайте цвет шрифта
//                notification.setDuration(5000); // Длительность отображения
//                notification.setPosition(Notification.Position.MIDDLE);
//
//                notification.open();
        });

        return comboBox;
    }

    public MainView(List<File> files) {
        System.out.println("Token " + Token.getValue());
        if (Token.getValue() == null || !Token.getValue().equals("logon")) {
            UI.getCurrent().navigate(LoginView.class);
        } else {

            files = getListElements("resources/data/PDF");
            System.out.println("resource pdf size: " + files.size());

            List<FileDTO> dtoFiles = new ArrayList<>();
            for (File file : files) {

                long fileLength = file.length();
                long sizeMb = fileLength / 1024 / 1024;
                long sizeKb = fileLength / 1024;

                dtoFiles.add(new FileDTO(file.getName(),
                        "Описание для файла " + file.getName(),
                        file.getAbsolutePath(), sizeMb > 0 ? file.length() / 1024 / 1024 + " Мб" : sizeKb > 0 ? sizeKb + " Кб" : fileLength + " байт")
                );
            }

            System.out.println("file dto elements: " + dtoFiles);

            this.setSizeFull();
            this.setSpacing(true);
            this.setPadding(true);

            Button signButton = createSignButton(); //create button & set disable

            // Переход на другую страницу
            Button navigateButton = new Button(
                    "Перейти в на страницу просмотра",
                    event -> getUI().ifPresent(ui -> ui.navigate(PdfPreview.class))
            );

            Button logoffButton = new Button("", VaadinIcon.SIGN_OUT.create());
            logoffButton.addClickListener(event -> {
                Token.setValue(null);
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            });
            Icon icon = (Icon) logoffButton.getIcon();
            icon.setColor("red");

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

            HorizontalLayout header = new HorizontalLayout(div, logoffButton);
            header.setWidth("100%");
            header.setJustifyContentMode(JustifyContentMode.BETWEEN);
            HorizontalLayout toolbar = new HorizontalLayout(signButton);
            HorizontalLayout footer = new HorizontalLayout(navigateButton);
            grid = createGrid(dtoFiles);
            grid.addSelectionListener(event -> {
                signButton.setEnabled(event.getAllSelectedItems().size() > 0);
            });

            IFrame iFrame = new IFrame("html/footer.html");
            iFrame.setWidth("100%");
            iFrame.setHeight("33%");

            add(header, toolbar, grid, footer, iFrame);
        }
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

    //Unused
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

    //Unused
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

    private String algorithm;
    private String reason;
    private String location;
    private String contact;
    private StringBuilder stampText;
    private String certId;

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

        //cancel button
        HorizontalLayout cancelLayout = new HorizontalLayout();
        Button cancelButton = new Button("", VaadinIcon.CLOSE.create());
        Icon icon = (Icon) cancelButton.getIcon();
        icon.setColor("red");
        cancelButton.addClickListener(e -> dialog.close());
        cancelLayout.add(cancelButton);
        cancelLayout.setJustifyContentMode(JustifyContentMode.END);

        //approve
        VerticalLayout approveLayout = new VerticalLayout();

        ComboBox<JSONObject> comboBox = getComboWithCertificates();
        comboBox.setWidthFull();

        Div infoText = new Div();
        infoText.getElement().setProperty("innerHTML", "Подтвердите подписание выбранных файлов");
        Button approveButton = new Button("Подписать файлы", VaadinIcon.FILE_PROCESS.create());
        approveButton.setWidth("100%");
        approveButton.setHeight("60px");
        approveButton.getElement().getStyle().set("background-color", "#F0F0F0"); // Зеленый цвет
        approveButton.getElement().getStyle().set("color", "#2196F3"); // Цвет текста
        approveButton.addClickListener(event -> {
            if (comboBox.isEmpty()) {
                Notification.show("Пожалуйста, выберете сертификат!", 1000, Notification.Position.MIDDLE);
                comboBox.focus();
            } else {
                selection.getValue().forEach(fileDTO -> {
                    UUID uuid = UUID.randomUUID();
                    //формируем ссылки на исходный файл и подписанный
                    String rootPath = fileDTO.getPath();
                    System.out.println("rootPath: " + rootPath);
                    String stampedPath = rootPath
                            .replace("\\PDF\\", "/PDF/STAMPED/")
                            .replaceAll("\\.pdf", uuid + "_stamped.pdf");
                    System.out.println("stampedPath: " + stampedPath);
                    String outputPath = stampedPath
                            .replaceAll("/PDF/STAMPED/", "/PDF/SIGNED/")
                            .replaceAll(uuid.toString(), "")
                            .replaceAll("_stamped.pdf", "_signed.pdf");
                    System.out.println("outputPath: " + outputPath);

                    PdfSignatureAppearance appearance;
                    InputStream inputStream;
                    int sgnCnt;
                    String signFieldName;
                    try (FileOutputStream fos = new FileOutputStream(stampedPath)) {
                        PdfReader reader = new PdfReader(rootPath);
                        PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0', null, true);

                        //Определение количества подписей в документе
                        sgnCnt = reader.getAcroFields().getSignatureNames().size() + 1;
                        signFieldName = "signatureField" + sgnCnt;

                        //Определяем стиль шрифта с поддержкой кириллицы и содержимое подписи
                        BaseFont bf = BaseFont.createFont("./resources/fonts/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        Font font = new Font(bf, 12);

                        //Определение координат для рамки
                        float padding = 10;
                        float pageWidth = reader.getPageSize(1).getWidth();
                        float width = pageWidth / 2 + padding * 2; //Ширина рамки
                        float x = (pageWidth - width - padding * 2) - padding; //Положение по X * Кол-во подписей
                        float height = font.getSize() * 4 * 2; //Высота рамки
                        float y = (height + padding * 2) * sgnCnt; //Положение по Y * Кол - во подписей

                        appearance = stamper.getSignatureAppearance();
                        appearance.setReason(reason);
                        appearance.setLocation(location);
                        appearance.setContact(contact);
                        appearance.setVisibleSignature(new Rectangle(x, y, x + width + padding * 2, y + height + padding * 2), 1, signFieldName);
                        appearance.setLayer2Font(font);
                        appearance.setLayer2Text(stampText.toString());
                        ExternalSignatureContainer external = new ExternalBlankSignatureContainer(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
                        MakeSignature.signExternalContainer(appearance, external, 65536);

                        stamper.close();
                        reader.close();
                    } catch (IOException | DocumentException | GeneralSecurityException e) {
                        System.err.println("exception ! ! " + ExceptionUtils.getMessage(e));
                        throw new RuntimeException(e);
                    }

                    //Подписываем документ подписью
                    String hexString1;
                    String hexString2;
                    try {
                        MessageDigest messageDigest;
                        if (algorithm.contains("2012") && algorithm.contains("256")) {
                            System.out.println("-> 2012_256");
                            messageDigest = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_256_NAME, JCP.PROVIDER_NAME);
                        } else if (algorithm.contains("2012") && algorithm.contains("512")) {
                            System.out.println("-> 2012_512");
                            messageDigest = MessageDigest.getInstance(JCP.GOST_DIGEST_2012_512_NAME, JCP.PROVIDER_NAME);
                        } else {
                            System.out.println("-> 3411");
                            messageDigest = MessageDigest.getInstance(JCP.GOST_DIGEST_NAME, JCP.PROVIDER_NAME);
                        }

                        // Чтение данных из потока и вычисление хэша
                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        inputStream = appearance.getRangeStream();
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            messageDigest.update(buffer, 0, bytesRead);
                        }

                        byte[] hashBytes = messageDigest.digest();

                        hexString1 = getHexString1(hashBytes);
                        System.out.println("Хеш документа на подпись вариант 1: " + hexString1);
                        hexString2 = getHexString2(hashBytes);
                        System.out.println("Хеш документа на подпись вариант 2: " + hexString2);

                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchProviderException e) {
                        throw new RuntimeException(e);
                    }

                    String hashString = hexString1;
                    System.out.println("Хеш документа на подпись: " + hashString);

                    //На клиенте подписываем hash от сервера
                    this.getElement().executeJs("return signHash($0, $1)", hashString, certId)
                            .then(signedData -> {
                                String base64Pdf = null;
                                if (signedData instanceof JreJsonString) {
                                    base64Pdf = signedData.asString();
                                    base64Pdf = base64Pdf.replaceAll("[^A-Za-z0-9+/=]", "");
                                    System.out.println("Результат подписи хеша файла:\n" + base64Pdf);
                                }

                                try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                                    PdfReader reader = new PdfReader(stampedPath);
                                    byte[] decodedBytes = Base64.getDecoder().decode(base64Pdf.getBytes());

                                    // Использование внешнего контейнера для подписи
                                    ExternalSignatureContainer external = new MyExternalSignatureContainer(decodedBytes);
                                    MakeSignature.signDeferred(reader, signFieldName, fos, external);
                                    System.out.println(LocalDateTime.now() + " Файл подписан и сохранен: " + outputPath);

                                    fos.close();
                                    reader.close();
                                } catch (IOException | DocumentException |
                                         GeneralSecurityException e) {
                                    System.err.println("exception ! ! " + ExceptionUtils.getMessage(e));
                                    throw new RuntimeException(e);
                                }
                            });
                });

                dialog.close();

                String postfix = (selectedItems.size() > 1) ? "ы" : "";
                Notification notification = new Notification(String.format("Файл%s подписан%s успешно", postfix, postfix));
                // Установка стиля уведомления
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                notification.getElement().getStyle().set("color", "white");
                notification.setDuration(3000);
                notification.setPosition(Notification.Position.MIDDLE);

                // Отображение уведомления
                notification.open();
            }
        });

        approveLayout.setPadding(false);
        approveLayout.getElement().getStyle().set("margin-top", "20px");
        approveLayout.add(comboBox, infoText, approveButton);

        //group layouts
        dialog.add(cancelLayout, approveLayout);
        dialog.open();
    }

    // Преобразование хеша в шестнадцатеричную строку способ первый
    private String getHexString1(byte[] hashBytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexStringBuilder.append('0');
            hexStringBuilder.append(hex);
        }

        return hexStringBuilder.toString().toUpperCase();
    }

    // Преобразование хеша в шестнадцатеричную строку способ второй
    private String getHexString2(byte[] hashBytes) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        for (byte b : hashBytes) {
            formatter.format("%02x", b);
        }

        return sb.toString().toUpperCase();
    }

    private void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        System.out.println("Алгоритм установлен: " + algorithm);
    }

    private String getAlgorithm(JsonValue oInfo) {
        JreJsonObject jsonObject = (JreJsonObject) oInfo;
        JSONObject mainObject = new JSONObject(jsonObject);
        JSONObject object = mainObject.getJSONObject("object");
        return object.getString("Algorithm");
    }

    private String getLocation(JsonValue oInfo) {
        JreJsonObject jsonObject = (JreJsonObject) oInfo;
        JSONObject mainObject = new JSONObject(jsonObject);
        JSONObject object = mainObject.getJSONObject("object");
        JSONObject subject = object.getJSONObject("Subject");
        System.out.println("subject: " + subject);
        return subject.getString("L");
    }

    private String getContact(JsonValue oInfo) {
        JreJsonObject jsonObject = (JreJsonObject) oInfo;
        JSONObject mainObject = new JSONObject(jsonObject);
        JSONObject object = mainObject.getJSONObject("object");
        JSONObject subject = object.getJSONObject("Subject");
        System.out.println("subject: " + subject);
        return subject.getString("E");
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
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        return grid;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (!Token.isUserLoggedIn()) {
            // Переадресовываем на страницу логина
            beforeEnterEvent.rerouteTo(LoginView.class);
        }
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