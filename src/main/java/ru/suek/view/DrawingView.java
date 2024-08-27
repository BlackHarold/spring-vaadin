package ru.suek.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("/draw")
@JavaScript("./js-1.0/coordinates.js")
public class DrawingView extends VerticalLayout {
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    private static int clickCounter;

    public DrawingView() {
        Button openDialogButton = new Button("Открыть диалог для указания рамки", event -> openDialog());

        add(openDialogButton);
    }

    private void openDialog() {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setHeight("400px");

        // Создаем область для рисования
        Div drawingArea = new Div();
        drawingArea.setWidth("100%");
        drawingArea.setHeight("100%");
        drawingArea.getStyle().set("border", "1px solid black");
        drawingArea.getStyle().set("position", "relative");

        // Добавляем обработчик клика

        drawingArea.addClickListener(event -> {
            drawingArea.getElement().executeJs(
                    "this.addEventListener('mousedown', function(event) {" +
                            "   this.startX = event.offsetX;" +
                            "   this.startY = event.offsetY;" +
                            "   this.isDrawing = true;" +
                            "});" +
                            "this.addEventListener('mousemove', function(event) {" +
                            "   if (this.isDrawing) {" +
                            "       const ctx = this.getContext('2d');" +
                            "       ctx.clearRect(0, 0, this.width, this.height);" +
                            "       ctx.beginPath();" +
                            "       ctx.rect(this.startX, this.startY, event.offsetX - this.startX, event.offsetY - this.startY);" +
                            "       ctx.stroke();" +
                            "   }" +
                            "});" +
                            "this.addEventListener('mouseup', function(event) {" +
                            "   this.isDrawing = false;" +
                            "   const width = event.offsetX - this.startX;" +
                            "   const height = event.offsetY - this.startY;" +
                            "   console.log('Координаты: (' + this.startX + ', ' + this.startY + '), Размеры: ' + width + 'x' + height);" +
                            "});"
            );

        });

        dialog.add(drawingArea);
        dialog.open();
    }

    public void setCoordinates(double x, double y) {
        // Если начальная точка не установлена, устанавливаем её
        if (startX == 0 && startY == 0) {
            startX = x;
            startY = y;
            Notification.show("Начальная точка установлена: (" + startX + ", " + startY + ")");
        } else {
            // Устанавливаем конечную точку и обрабатываем рамку
            endX = x;
            endY = y;
            Notification.show("Конечная точка установлена: (" + endX + ", " + endY + ")");
            processRectangle();
        }
    }

    private void processRectangle() {
        // Здесь вы можете обработать размеры рамки
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        Notification.show("Размеры рамки: Ширина = " + width + ", Высота = " + height);

        // Сброс координат для следующего использования
        startX = 0;
        startY = 0;
        endX = 0;
        endY = 0;
    }
}
