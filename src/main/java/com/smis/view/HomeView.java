package com.smis.view;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

import com.smis.dbservice.DashboardService;
import com.smis.dbservice.Dbservice;
import com.storedobject.chart.BarChart;
import com.storedobject.chart.CategoryData;
import com.storedobject.chart.Data;
import com.storedobject.chart.DataType;
import com.storedobject.chart.NightingaleRoseChart;
import com.storedobject.chart.Position;
import com.storedobject.chart.RectangularCoordinate;
import com.storedobject.chart.SOChart;
import com.storedobject.chart.Title;
import com.storedobject.chart.Toolbox;
import com.storedobject.chart.XAxis;
import com.storedobject.chart.YAxis;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
@PageTitle("Home")
@Route(value="", layout=MainLayout.class)
@PermitAll
public class HomeView extends VerticalLayout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Dbservice service;
	DashboardService dservice;
    public HomeView(Dbservice service, DashboardService dservice) {
    	this.service=service;
    	this.dservice=dservice;
    	add(getCards());
        setWidthFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		getStyle().set("text-align", "center");

    }
    private Component getCards() {
		VerticalLayout card1 = createCard(1);
		VerticalLayout card2 = createCard(2);
		VerticalLayout card3 = createCard(3);
		VerticalLayout card4 = createCard(4);
		
		return new HorizontalLayout(card1, card2, card3, card4);
	}
    private VerticalLayout createCard(int type) {
		VerticalLayout card = new VerticalLayout();
		card.addClassName("card");

		Icon icon;
        String title;
        String description;


		// Define content based on card type
		switch (type) {
		case 1:
			 icon = VaadinIcon.CHECK.create();
			long title1=dservice.getWorksCount();
			title = ""+title1;
			description = "Total Works Entered";
			break;
		case 2:
			 icon = VaadinIcon.ADJUST.create();
			long title2=dservice.getInstallmentCount();
			title = ""+title2;
			description = "Total Installments Released";
			break;
		case 3:
			 icon = VaadinIcon.CALENDAR.create();
			long title3=dservice.getCurrentMonthData();
			title = ""+title3;
			description = "Works Entered In The Current Month";
			break;
		case 4:
			 icon = VaadinIcon.CALENDAR_CLOCK.create();
			long title4=dservice.getMonthData();
			title = ""+title4;
			description = "Works Entered In The Previous Month";
			break;
		default:
			 icon = VaadinIcon.BARCODE.create();
			title = "Default Card Title";
			description = "This is a default card description.";
			break;
		}
		

		// Create image component
		 icon.addClassName("card-icon");

		// Create title component
		Span titleLabel = new Span(title);
		titleLabel.addClassName("card-title");

		// Create description component
		Span descriptionLabel = new Span(description);
		descriptionLabel.addClassName("card-description");

		// Create a button
		Button actionButton = new Button("Learn More");
		actionButton.addClassName("card-button");

		// Add components to the card layout
		card.add(icon, titleLabel, descriptionLabel);

		return card;
	}
    
    public Component getCharts() {
    	SOChart soChart = new SOChart();
    	SOChart soChart2 = new SOChart();
    	CategoryData labels = new CategoryData();
    	Data data = new Data();
        //soChart.setSizeFull();
        //soChart2.setSizeFull();
        int i=service.getAllConstituencies().size();
        for(int index=0; index<i; index++) {
        	
        	labels.add(service.getAllConstituencies().get(index).getConstituencyName());
        	//service.getWorkCount(null)
        	data.add(service.getWorkCount(service.getAllConstituencies().get(index)));
        }
        BarChart bc = new BarChart(labels, data);
        RectangularCoordinate rc;
        rc  = new RectangularCoordinate(new XAxis(DataType.CATEGORY), new YAxis(DataType.NUMBER));
        Position p = new Position();
        //p.setBottom(Size.percentage(55));
        //rc.setPosition(p); // Position it leaving 55% space at the bottom
        bc.plotOn(rc); // Bar chart needs to be plotted on a coordinate system
        bc.setName("Works");
        Toolbox toolbox = new Toolbox();
        toolbox.addButton(new Toolbox.Download(), new Toolbox.Zoom());
        // Let's add some titles.
        Title title = new Title("Constituency Wise Works");
        //title.setSubtext("Please Test");
        // We are going to create a couple of charts. So, each chart should be positioned
        // appropriately.
        // Create a self-positioning chart.
        NightingaleRoseChart nc = new NightingaleRoseChart(labels, data);
        nc.setName("Works");
        //Position p = new Position();
        //p.setTop(Size.percentage(50));
        nc.setPosition(p); // Position it leaving 50% space at the top
        //soChart.s
        soChart.add(nc, toolbox);
        soChart2.add(bc, toolbox, title);
        HorizontalLayout getCharts=new HorizontalLayout();
        
        getCharts.addClassName("chartsLayout1");
        getCharts.setWidthFull();
        //getCharts.setHeight("100px");
       soChart2.setWidthFull();
        getCharts.add(soChart2);
        return getCharts;
    }
    
    public Component getCharts2() {
    	SOChart soChart = new SOChart();
    	SOChart soChart2 = new SOChart();
    	CategoryData labels = new CategoryData();
    	Data data = new Data();
        //soChart.setSizeFull();
        //soChart2.setSizeFull();
        int i=service.getAllSchemes().size();
        for(int index=0; index<i; index++) {
        	
        	labels.add(service.getAllSchemesWIthNotInUse().get(index).getSchemeName());
        	//service.getWorkCount(null)
        	data.add(service.getWorkCount(service.getAllSchemes().get(index)));
        }
        BarChart bc = new BarChart(labels, data);
        bc.setName("Works");
        RectangularCoordinate rc;
        rc  = new RectangularCoordinate(new XAxis(DataType.CATEGORY), new YAxis(DataType.NUMBER));
        Position p = new Position();
        //p.setBottom(Size.percentage(55));
        //rc.setPosition(p); // Position it leaving 55% space at the bottom
        bc.plotOn(rc); // Bar chart needs to be plotted on a coordinate system
        Toolbox toolbox = new Toolbox();
        toolbox.addButton(new Toolbox.Download(), new Toolbox.Zoom());
        
        // Let's add some titles.
        Title title = new Title("Scheme Wise Works");
        //title.setSubtext("Please Test");
        // We are going to create a couple of charts. So, each chart should be positioned
        // appropriately.
        // Create a self-positioning chart.
        NightingaleRoseChart nc = new NightingaleRoseChart(labels, data);
        nc.setName("Works");
        //Position p = new Position();
        //p.setTop(Size.percentage(50));
        nc.setPosition(p); // Position it leaving 50% space at the top
        //soChart.s
        soChart.add(nc, toolbox);
        soChart2.add(bc, toolbox, title);
        HorizontalLayout getCharts=new HorizontalLayout();
        getCharts.add( getCards(),soChart);
        
        getCharts.addClassName("chartsLayout1");
        getCharts.setWidthFull();
        getCharts.setJustifyContentMode(JustifyContentMode.CENTER);
		//getCharts.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		getStyle().set("text-align", "center");
        return getCharts;
    }

}
