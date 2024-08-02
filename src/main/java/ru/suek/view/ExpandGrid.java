package ru.suek.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("/grid")
public class ExpandGrid extends HorizontalLayout {

    public ExpandGrid() {
        List<String> dataList = List.of("firstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirs\ntfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirstf\nirstfirstfirstfirstfirstfirstfirstfirstfirstfirstfirst", "second", "third");
        Grid<String> grid = new Grid<>(String.class);
        grid.setItems(dataList);

        grid.addColumn(new ComponentRenderer<>(data -> {
            Div textDiv = new Div();
            textDiv.setText(data);
            textDiv.setClassName("text-content");

            Button expandButton = new Button("Expand");
            expandButton.addClickListener(event -> {
                textDiv.getElement().executeJs(
                        "this.classList.toggle('expanded');"
                );
                expandButton.setText(
                        textDiv.getClassNames().contains("expanded") ? "Collapse" : "Expand"
                );
            });

            VerticalLayout layout = new VerticalLayout(textDiv, expandButton);
            layout.setAlignItems(Alignment.START);
            return layout;
        })).setHeader("Text");

        add(grid);
    }
}
