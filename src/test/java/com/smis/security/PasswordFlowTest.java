package com.smis.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.smis.dbservice.Dbservice;
import com.smis.entity.District;
import com.smis.entity.State;
import com.smis.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

class PasswordFlowTest {
    private UI ui;
    @AfterEach void cleanup() { UI.setCurrent(null); ui = null; }

    @ParameterizedTest
    @ValueSource(strings = {"Aa1!", "             Aa1!             "})
    void bothCreateAndChangeRejectShortPasswordBeforeAnyPersistence(String password) {
        ui = new UI();
        UI.setCurrent(ui);
        // Exercise the real handlers with client-side field validation bypassed.
        MainLayout layout = mock(MainLayout.class, CALLS_REAL_METHODS);
        Dbservice service = mock(Dbservice.class);
        ReflectionTestUtils.setField(layout, "service", service);
        PasswordField newPassword = new PasswordField();
        PasswordField confirmation = new PasswordField();
        PasswordField oldPassword = new PasswordField();
        newPassword.setValue(password);
        confirmation.setValue(password);
        oldPassword.setValue("old-password");
        ReflectionTestUtils.setField(layout, "newpwd", newPassword);
        ReflectionTestUtils.setField(layout, "confirmpwd", confirmation);
        ReflectionTestUtils.setField(layout, "oldpwd", oldPassword);

        var state = new State();
        var district = new District();
        ComboBox<State> states = new ComboBox<>();
        states.setItems(state);
        states.setValue(state);
        ComboBox<District> districts = new ComboBox<>();
        districts.setItems(district);
        districts.setValue(district);
        ComboBox<String> roles = new ComboBox<>();
        roles.setItems("USER");
        roles.setValue("USER");
        TextField username = new TextField();
        username.setValue("new-user");
        ReflectionTestUtils.setField(layout, "state", states);
        ReflectionTestUtils.setField(layout, "district", districts);
        ReflectionTestUtils.setField(layout, "usertype", roles);
        ReflectionTestUtils.setField(layout, "userName", username);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(layout, "saveNewUser"));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(layout, "changePassword"));
        verifyNoInteractions(service);
    }
}
