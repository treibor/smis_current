package com.smis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;

public class ValidationUtil {

    public static void applyValidation(TextField textField) {
        textField.setAllowedCharPattern("[0-9A-Za-z.()@/'&\\s-]");
        textField.setMinLength(0);
        textField.setMaxLength(50);
    }
    public static void applyTextOnly(TextField textField) {
        textField.setAllowedCharPattern("[0-9A-Za-z ]");
        textField.setMinLength(0);
        textField.setMaxLength(50);
    }
    public static void applyTextAreaValidation(TextArea textArea) {
    	//textArea.setAllowedCharPattern("[0-9A-Za-z.,()@/-'& ]");
    	//textArea.setAllowedCharPattern("[0-9A-Za-z.()@/'&\\s-]");
    	textArea.setAllowedCharPattern("[^<>]");
    	textArea.setMinLength(0);
    	textArea.setMaxLength(50);
    }
    public static boolean applyValidation(String string) {
    	//textArea.setAllowedCharPattern("[0-9A-Za-z.,()@/-'& ]");
    	if (string.matches("[0-9A-Za-z.()@/'&\\s-]*")){
    		return true;
    	}
    	return false;    	
    }
}