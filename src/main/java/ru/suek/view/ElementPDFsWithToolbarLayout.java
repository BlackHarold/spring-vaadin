package ru.suek.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import ru.CryptoPro.JCP.JCP;
import ru.CryptoPro.JCSP.JCSP;
import ru.suek.model.FileDTO;
import ru.suek.pdf.SignVerifyPDFExample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Route("")
public class ElementPDFsWithToolbarLayout extends VerticalLayout {
    /**
     * уникальное имя записываемого сертификата
     */
    private static final String ALIAS_2012_256 = "38812c44a-2c90-2ff0-1402-0523552191f";

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

    public ElementPDFsWithToolbarLayout(List<File> files) {

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
                    FileDTO dto = new FileDTO(file.getName(), descriptions.get(counter.getAndIncrement()), file.getAbsolutePath());
                    return dto;
                })
                .toList();

        System.out.println("file dto elements: " + dtoFiles);

        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);

        Button signButton = createSignButton(); //create button & set disable
        HorizontalLayout toolbar = new HorizontalLayout(signButton);
        grid = createGrid(dtoFiles);
        grid.addSelectionListener(event -> {
            signButton.setEnabled(event.getAllSelectedItems().size() > 0);
        });

        add(toolbar, grid);
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

    private void showDialog() {
        MultiSelect<Grid<FileDTO>, FileDTO> selection = grid.asMultiSelect();
        Notification.show(selection.getValue().parallelStream().map(FileDTO::getName).collect(Collectors.joining(",")));

        Set<FileDTO> selectedItems = selection.getSelectedItems();
        Iterator<FileDTO> it = selectedItems.iterator();
        while (it.hasNext()) {
            FileDTO fileDTO = it.next();
            System.out.println("result fileDTO: " + fileDTO);
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
        Div infoText = new Div();
        infoText.getElement().setProperty("innerHTML", "<strong>Подтвердите подписание выбранных файлов<strong>");
        Button approveButton = new Button("Подписать файлы", VaadinIcon.FILE_PROCESS.create());
        approveButton.getElement().getStyle().set("color", "#5c995e");
        approveButton.addClickListener(event -> {
            selection.getValue().stream().forEach(fileDTO -> {
                String rootPath = Paths.get(fileDTO.getPath()).toUri().getPath();
                String outputPath = rootPath
                        .replaceAll("/PDF/", "/PDF/SIGNED/")
                        .replaceAll(".pdf", "_signed.pdf");

                PrivateKey privateKey = null;
                Certificate[] certificateChain = null;
                try {
                    KeyStore keyStore = KeyStore.getInstance(JCSP.HD_STORE_NAME,
                            JCSP.PROVIDER_NAME);

                    keyStore.load(null, null);
                    System.out.println("key store type: " + keyStore.getType());
                    Enumeration<String> aliases;
                    aliases = keyStore.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        System.out.println("Alias: " + alias);
                    }

                    privateKey = (PrivateKey) keyStore.getKey(ALIAS_2012_256, "".toCharArray());
                    printInfo(privateKey, (X509Certificate) keyStore.getCertificate(ALIAS_2012_256));

                    // Получение PrivateKey
                    PublicKey publicKey = keyStore.getCertificate(ALIAS_2012_256).getPublicKey();

                    System.out.println("publicKey: " + publicKey);
                    System.out.println("privateKey: " + privateKey);

                    certificateChain = keyStore.getCertificateChain(ALIAS_2012_256);
                    for (int i = 0; i < certificateChain.length; i++) {
                        X509Certificate cert = (X509Certificate) certificateChain[i];
                        System.out.println("certificate: " + cert);
                        String algName = cert.getSigAlgName();
                        System.out.println("algName: " + algName);
                    }

                } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ke) {
                    System.out.println("Key store exception: " + ExceptionUtils.getMessage(ke));
                } catch (NoSuchProviderException e) {
                    System.out.println("No such provider exception: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                } catch (UnrecoverableKeyException e) {
                    System.out.println("Unrecoverable key: " + ExceptionUtils.getMessage(e));
                    throw new RuntimeException(e);
                }

                String location = "Российская федерация";
                String reason = "Тестовая подпись (ГОСТ 2012-256)";

                try {
                    SignVerifyPDFExample.sign(
                            privateKey,
                            JCP.GOST_DIGEST_2012_256_NAME,
                            JCP.PROVIDER_NAME,
                            certificateChain,
                            fileDTO.getPath(),
                            outputPath,
                            location,
                            reason,
                            true
                    );
                    SignVerifyPDFExample.verify(outputPath, null, null, JCSP.PROVIDER_NAME);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Notification.show("Подпись подтверждена");
            dialog.close();
        });
        approveLayout.setPadding(false);
        approveLayout.getElement().getStyle().set("margin-top", "20px");
        approveLayout.add(infoText, approveButton);

        //group layouts
        dialog.add(cancelLayout, approveLayout);
        dialog.open();
    }

    private static void printInfo(PrivateKey privateKey,
                                  X509Certificate certificate) {

        System.out.println("Private key: " + privateKey);
        System.out.println("Certificate:\n\tSn - " +
                certificate.getSerialNumber().toString(16) +
                "\n\tSubject - " + certificate.getSubjectDN() +
                "\n\tIssuer - " + certificate.getIssuerDN());

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
        grid.removeColumnByKey("path");
//        grid.getColumnByKey("path").setHeader("Путь");

        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        return grid;
    }
}