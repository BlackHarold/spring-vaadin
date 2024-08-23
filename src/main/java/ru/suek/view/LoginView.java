package ru.suek.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ru.suek.util.Token;

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
            if (username.equals("admin") && password.equals("password")) {
                Token.setValue("logon");
                UI.getCurrent().navigate("signature");
            } else {
                Token.setValue(null);
            }
        });

        add(usernameField, passwordField, loginButton);
    }
}
