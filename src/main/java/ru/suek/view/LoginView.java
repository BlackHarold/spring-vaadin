package ru.suek.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class LoginView extends VerticalLayout {
    public LoginView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        add(new H1("Логин"));

        TextField usernameField = new TextField("Логин");
        PasswordField passwordField = new PasswordField("Пароль");
        Button loginButton = new Button("Войти");

        loginButton.addClickListener(event -> {
            String username = usernameField.getValue();
            String password = passwordField.getValue();

            // Здесь можно добавить логику проверки логина и пароля
            if (username.equals("admin") && password.equals("password")) {
                // Успешная авторизация
                // Можно перенаправить на другую страницу или показать сообщение
                usernameField.clear();
                passwordField.clear();
                UI.getCurrent().navigate("signature");
            } else {
                // Неверный логин или пароль
                // Можно показать сообщение об ошибке
            }
        });

        add(usernameField, passwordField, loginButton);
    }
}
