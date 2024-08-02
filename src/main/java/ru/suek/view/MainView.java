package ru.suek.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ru.suek.model.MqlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Route("/main")
public class MainView extends VerticalLayout {
    private Logger log = LoggerFactory.getLogger(MainView.class);

    private Grid<MqlObject> grid = new Grid<>(MqlObject.class);

    public MainView() {
        grid.setItems(getMqlObjects());
        VerticalLayout todosList = new VerticalLayout();
        TextField taskField = new TextField();
        Button addButton = new Button("Add");
        addButton.addClickShortcut(Key.ENTER);


        CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
        checkboxGroup.add("Item 1");

        ComboBox<Checkbox> comboBox = new ComboBox<>();
        comboBox.setItems(List.of(new Checkbox("Item 1"), new Checkbox("Item 2"), new Checkbox("Item 3")));

        addButton.addClickListener(click -> {
            comboBox.getListDataView().getItems().forEach(e->log.info("element getAttribute: " + e));
            comboBox.getListDataView().getItems().forEach(e->log.info("element getProperty: " + e.getElement().getProperty("selected")));
        });

        add(
                new H1("Vaadin Todo"),
                todosList,
                new HorizontalLayout(
                        taskField,
                        addButton
                )
//                , grid
                , comboBox
        );
    }

    private List<MqlObject> getMqlObjects() {
        List<MqlObject> mqlObjects = new ArrayList<>();
        mqlObjects.add(new MqlObject("id1", "code1", "description1"));
        mqlObjects.add(new MqlObject("id2", "code2", "description2"));
        mqlObjects.add(new MqlObject("id3", "code3", "description3"));
        return mqlObjects;
    }
}
