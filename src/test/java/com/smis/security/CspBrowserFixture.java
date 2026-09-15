package com.smis.security;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.smis.security.richtext.SafeRichTextEditor;
import com.storedobject.chart.*;

/** Test-classpath only. No database records or production route are introduced. */
@Route("security-fixture")
@AnonymousAllowed
public class CspBrowserFixture extends VerticalLayout {
    @Override protected void onAttach(com.vaadin.flow.component.AttachEvent event) {
        super.onAttach(event);
        event.getUI().getPage().addJavaScript("VAADIN/security-probes.js");
        try (var input = getClass().getResourceAsStream("/csp-chunk-keys.txt")) {
            for (String key : new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
                if (key.matches("[a-f0-9]{64}")) event.getUI().getPage().executeJs("return window.Vaadin.Flow.loadOnDemand('"+key+"');");
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    public CspBrowserFixture() {
        add(new H2("CSP component fixture"));
        var probes = new Button("Run CSP probes");
        probes.setId("run-csp-probes");
        var results = new com.vaadin.flow.component.html.Div();
        results.setId("csp-probe-results");
        add(probes, results);
        var shortcut = new Button("Shortcut test", event -> results.setText("Shortcut works"));
        shortcut.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
        add(shortcut);
        var combo = new ComboBox<String>("Choice");
        combo.setItems("Alpha", "Beta");
        add(combo, new DatePicker("Date"));
        var grid = new Grid<String>();
        grid.addColumn(value -> value).setHeader("Item").setSortable(true);
        grid.setItems("Alpha", "Beta");
        grid.setHeight("160px");
        add(grid);
        add(new Button("Open dialog", event -> {
            var dialog = new Dialog();
            dialog.add(new H2("Dialog works"), new Button("Close dialog", e -> dialog.close()));
            dialog.open();
        }));
        var editor = new SafeRichTextEditor();
        editor.setValue("<p>Safe <strong>formatted</strong> content</p>");
        add(editor, new Button("Read editor", event -> Notification.show(editor.getValue())));
        var chart = new SOChart();
        chart.setWidth("500px"); chart.setHeight("300px");
        var bars = new BarChart(new CategoryData("Alpha", "Beta"), new Data(2, 4));
        bars.plotOn(new RectangularCoordinate(new XAxis(DataType.CATEGORY), new YAxis(DataType.NUMBER)));
        chart.add(bars);
        add(chart);
    }
}
