package ru.suek.view;

import com.vaadin.componentfactory.pdfviewer.PdfViewer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Route("/preview")
public class PdfPreview extends VerticalLayout {
    List<File> files = getListElements("resources/data/PDF/SIGNED");
    private Grid<File> grid = new Grid<>(File.class);
//    private Image pdfPreview = new Image();

    private PdfViewer pdfViewer = new PdfViewer();

    public PdfPreview() {
        setSizeFull();
        add(new H1("Таблица подписанных файлов"));

        // Настройка таблицы
        grid.setItems(files);
        grid.setColumns("Имя");
        grid.addSelectionListener(event -> {
            File selectedFile = event.getFirstSelectedItem().orElse(null);
            if (selectedFile != null) {
                showPdfPreview(selectedFile);
            }
        });

        // Добавление компонентов на страницу
        add(/*upload,*/ grid, pdfViewer);
    }

    private void showPdfPreview(File file) {
        try {
            byte[] pdfBytes = Files.readAllBytes(file.toPath());
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            pdfViewer.setSrc("data:application/pdf;base64," + pdfBase64);
            pdfViewer.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<File> getListElements(String directoryPath) {
        File folder = new File(directoryPath);
        List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf")));
        return files;
    }

}
