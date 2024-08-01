package ru.suek.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import ru.suek.model.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route("/elements")
public class ElementsLayout extends VerticalLayout {
    private Grid<Element> grid;
    private TextField filterField;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;

    private List<Element> elements;

    private List<Element> getListElements() {
        List<Element> elements = new ArrayList<>();
        elements.add(new Element("Name1", "Description1", 120.5));
        elements.add(new Element("Name2", "Description2", 79.21));
        elements.add(new Element("Name3", "Description3", 79.21));
        elements.add(new Element("Name4", "Description4", 79.21));
        elements.add(new Element("Name5", "Description5", 79.21));

        return elements;
    }

    public ElementsLayout(List<Element> elements) {
        elements = getListElements();
        this.elements=elements;
        System.out.println("elements: " + elements);
        // Создание таблицы элементов
        grid = new Grid<>(Element.class);
        grid.setItems(elements);

        // Создание поля фильтра
        filterField = new TextField();
        filterField.setPlaceholder("Filter by name...");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.EAGER);
        filterField.addValueChangeListener(event -> updateGrid(event.getValue()));

        // Создание кнопок управления
        addButton = new Button("Add");
        editButton = new Button("Edit");
        deleteButton = new Button("Delete");

        // Создание макета
        HorizontalLayout buttonLayout = new HorizontalLayout(addButton, editButton, deleteButton);
        Div filterDiv = new Div(filterField);
        VerticalLayout mainLayout = new VerticalLayout(filterDiv, grid, buttonLayout);
        mainLayout.setSizeFull();
        add(mainLayout);
    }

    private void updateGrid(String filterText) {
        grid.setItems(elements.stream()
                .filter(element -> element.getName().toLowerCase().contains(filterText.toLowerCase()))
                .collect(Collectors.toList()));
    }
}