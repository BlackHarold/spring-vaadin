package ru.suek.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.router.Route;
import ru.suek.model.Element;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route("/toolbar")
public class ElementsWithToolbarLayout extends VerticalLayout {
    private Grid<Element> grid;
    private TextField filterField;
    private Button editButton;
    private Button deleteButton;

    private List<Element> elements;

    private List<Certificate> certificateList;

    private List<Element> getListElements() {
        List<Element> elements = new ArrayList<>();
        elements.add(new Element("1", "PKS2.D.P000.5.0UJA99GML10&.052.DC.0001.S-MJB0001.pdf", "Reactor Building (50UJA). Pipelines and Equipment of GML System in Foundation Slab at Elevation -19.300", 120.5));
        elements.add(new Element("2", "PKS2.D.P000.5.0UJA99GML10&.052.DC.0001.S-MJH0001.pdf", "ϥ𥷥�쥲⠨ 嬮⠪𮫿 쮭򠦭ûõ 񢫵 񮥤譥�/ List of methods and scope of testing of field welded joints", 79.21));
        elements.add(new Element("3", "PKS2.D.P000.5.0UJA99GML10&.052.DC.0001.S-MJH0001.pdf", "ϥ\uD857\uDDE5�\uDA57\uDD6D跥\uD96A\uDE35 õఠ겥\uD863\uDC72誠\uDA83\uDCE1\uEBF0\uE8AE䮢/ List of technical characteristics of pipelines\"", 79.21));
        elements.add(new Element("4", "PKS2.D.P000.5.0UJA99GML10&.052.DC.0001.E-MJZ0003.pdf", "À걮�岰跥񪠿 쮭򠦭࿠񵥬࠯ Axonometric installation diagram", 54.21));
        elements.add(new Element("5", "PKS2.D.P000.5.0UJA99GML10&.052.DC.0001.E-MZY0001.pdf", "https://msk-dmz2-esbapp004.niaepnn.ru:8443/metamodel-service/api/getFile?esbTransactionId=618ba5b6-b5d8-4889-bf8f-9fc1821aa9c5&remove=true", 79.21));

        return elements;
    }

    public ElementsWithToolbarLayout(List<Element> elements) {
        this.certificateList = new ArrayList<>();
        elements = getListElements();
        this.elements = elements;
        System.out.println("elements: " + elements);

        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);

        HorizontalLayout toolbar = createToolbar(List.of(createAddButton()));
        grid = createGrid(elements);

        add(toolbar, grid);
    }


    private Button createAddButton() {
        Button addButton = new Button("Добавить", VaadinIcon.PLUS.create());
        addButton.addClickListener(e -> showDialog());

        return addButton;
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
            processCertificate(inputStream, fileName);
            Notification.show("Сертификат " + fileName + " загружен успешно!");

            dialog.close();
        });

        dialog.add(upload);
        dialog.open();
    }

    private void processCertificate(InputStream inputStream, String fileName) {
        // Пример обработки сертификата: добавление данных в список
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            // Здесь вы можете добавить логику для разбора содержимого сертификата
            // Например, добавление данных в список certificateList
//            certificateList.add(new Certificate(certificateList.size() + 1, fileName, "2025-12-31"));
//            grid.setItems(certificateList); // Обновление таблицы
        } catch (Exception e) {
            Notification.show("Ошибка при обработке сертификата: " + e.getMessage());
        }
    }

    private HorizontalLayout createToolbar(List<Component> elements) {

        Button btn = (Button) elements.get(0);
//        elements.forEach(toolbar::add);
        HorizontalLayout toolbar = new HorizontalLayout(btn);
        toolbar.setWidth("100%");
        toolbar.setVerticalComponentAlignment(Alignment.END, btn);

        return toolbar;
    }

    private void showDialog() {
        MultiSelect<Grid<Element>, Element> selection = grid.asMultiSelect();
        Notification.show(selection.getValue().parallelStream().map(e -> e.getId()).collect(Collectors.joining(",")));

        Dialog dialog = new Dialog();

        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);

        TextField nameField = new TextField("Имя");
        TextField emailField = new TextField("Email");
        Button uploadCertificateButton = createUploadButton();

        Button saveButton = new Button("Подписать файлы", VaadinIcon.FILE_PROCESS.create());
        saveButton.addClickListener(e -> {
            Element el = new Element();
            el.setName(nameField.getValue());
            el.setDescription(emailField.getValue());

            grid.getDataProvider().refreshAll();
            List<Element> fetchedElements = grid
                    .getDataProvider()
                    .fetch(new Query<>())
                    .collect(Collectors.toList()
                    );
            fetchedElements.add(el);
            grid.setItems(fetchedElements);
            dialog.close();
        });

        Button cancelButton = new Button("", VaadinIcon.CLOSE.create());
        Icon icon = (Icon) cancelButton.getIcon();
        icon.setColor("red");
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        dialogLayout.add(nameField, emailField, uploadCertificateButton, buttonLayout);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private Grid<Element> createGrid(List<Element> elements) {
        Grid<Element> grid = new Grid<>(Element.class);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setItems(new ArrayList<>(elements));

        grid.removeColumnByKey("id");
        grid.removeColumnByKey("price");
        grid.getColumnByKey("name").setHeader("Имя");
        grid.getColumnByKey("description").setHeader("Описание");
//        grid.addColumn(Element::getName, "name");
//        grid.addColumn(Element::getDescription, "description");

        grid.addItemClickListener(event -> {
            Notification.show("ид " + event.getItem().getId());
        });

//        grid.setColumns("name", "description");
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        return grid;
    }

    private void updateGrid(String filterText) {
        grid.setItems(elements.stream()
                .filter(element -> element.getName().toLowerCase().contains(filterText.toLowerCase()))
                .collect(Collectors.toList()));
    }
}