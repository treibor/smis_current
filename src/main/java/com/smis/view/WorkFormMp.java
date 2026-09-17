package com.smis.view;

import com.smis.security.ErrorReferences;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.smis.dbservice.Dbservice;
import com.smis.dbservice.DbserviceMp;
import com.smis.entity.Block;
import com.smis.entity.Constituency;
import com.smis.entity.Constituencymp;
import com.smis.entity.District;
import com.smis.entity.Impldistrict;
import com.smis.entity.Installment;
import com.smis.entity.Installmentmp;
import com.smis.entity.Scheme;
import com.smis.entity.Work;
import com.smis.entity.Workmp;
import com.smis.entity.Year;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;

public class WorkFormMp extends VerticalLayout{
	DbserviceMp service;
	private Workmp work;
	private Installmentmp installment;
	Binder <Workmp> binder=new BeanValidationBinder<>(Workmp.class);
	IntegerField priority=new IntegerField("Priority");
	ComboBox<Year> year=new ComboBox("Financial Year");
	ComboBox<Constituencymp> constituencymp=new ComboBox("Parliamentary Constituency");
	ComboBox<Block> block=new ComboBox("Implementing Block/MB");
	ComboBox<Impldistrict> implDistrict=new ComboBox("Sanctioned To");
	TextField implAgency=new TextField("Implementing Agency");
	TextField implHead=new TextField("Office Head");
	TextField implAddress=new TextField("Office Address");
	TextField workLocation=new TextField();
	TextArea workName=new TextArea("Work Name");
	IntegerField noOfInstallments=new IntegerField("Installments");
	//NumberField noOfInstallments=new NumberField("No Of Installments");
	//NumberField workAmount=new NumberField("Sanctioned Amount");
	BigDecimalField workAmount= new BigDecimalField("Recommended Amount");
	TextField sanctionNo=new TextField("Recommendation Number");
	DatePicker sanctionDate=new DatePicker("Recommendation Date");
	Button save=new Button("Save");
	Button delete=new Button("Delete");
	Button close=new Button("Close");
	Button installsave=new Button("Save");
	Button installclose=new Button("Close");
	Button ucsave=new Button("Save");
	Button ucclose=new Button("Close");
	
	BigDecimalField installmentAmount=new BigDecimalField("Amount");
	//TextField installmentCheque =new TextField("Cheque Number");
	TextField ucletter=new TextField("UC Number");
	DatePicker ucDate=new DatePicker("UC Date");
	static Notification notify=new Notification();
	Accordion accordion=new Accordion();
	AccordionPanel workaccordion=new AccordionPanel();
	AccordionPanel implaccordion=new AccordionPanel();
	AccordionPanel installaccordion=new AccordionPanel();
	AccordionPanel ucaccordion=new AccordionPanel();
	H3 installmentmaster=new H3("");
	H3 ucmaster=new H3("");
	boolean isAdmin;
	boolean isUser;
	public WorkFormMp(DbserviceMp service) {
		this.service=service;
		binder.bindInstanceFields(this);
		
		isAdmin=service.isAdmin();
		isUser=service.isUser();
		add(createFinalPanel());
	}
	public Component createFinalPanel() {
		
		//accordion.add("Work", new VerticalLayout(configureForm(), createButtonsLayout()));
		workaccordion=accordion.add("Work", new VerticalLayout(configureForm(), createButtonsLayout()));
		
		workaccordion.addThemeVariants(DetailsVariant.SMALL);
		//implaccordion=accordion.add("Implementing District", new VerticalLayout(configureImplForm(),  createButtonsLayout()));
		installaccordion=accordion.add("Installments", new VerticalLayout(configureInstallmentForm(), createInstallButtons()));
		installaccordion.addThemeVariants(DetailsVariant.SMALL);
		ucaccordion=accordion.add("Utilisation Certificate", new VerticalLayout(configureUcForm(), createUcButtons()));
		ucaccordion.addThemeVariants(DetailsVariant.SMALL);
		return accordion;
	}
	public Component configureForm() {
		//noOfInstallments.setValue(2);
		//noOfInstallments.setHasControls(true);
		noOfInstallments.setMin(1);
		noOfInstallments.setMax(5);
		//priority.setHasControls(true);
		workName.setMaxHeight("100px");
		noOfInstallments.setValue(1);
		noOfInstallments.setEnabled(false);
		priority.setValue(1);
		year.setItems(service.getAllYears());
		constituencymp.setItems(service.getAllConstituencies());
		block.setItems(service.getAllBlocks());
		block.setItemLabelGenerator(block->block.getBlockName());
		year.setItemLabelGenerator(Year:: getYearName);
		implDistrict.setItems(service.getAllImplDistricts());
		constituencymp.setItemLabelGenerator(constituency ->constituency.getConstituencyType()+" - "+constituency.getConstituencyMp());
		implDistrict.setItemLabelGenerator(impldistrict-> impldistrict.getDistrictName());
		//
		FormLayout form1=new FormLayout();
		form1.setWidth("100%");
		form1.add(implDistrict,2);
		form1.add(noOfInstallments, 2);
		form1.add(priority, 2);
		form1.add(workAmount, 2);
		form1.add(sanctionNo, 2);
		form1.add(sanctionDate, 2);
		form1.add(year, 2);
		form1.add(constituencymp, 2);
		form1.add(workName, 4);
		form1.add(workLocation, 4);
		
		form1.add(block, 2);
		form1.add(implAgency, 2);
		form1.add(implHead, 2);
		form1.add(implAddress, 2);
		form1.setResponsiveSteps(
		        new ResponsiveStep("0", 4),
		        // Use two columns, if layout's width exceeds 500px
		        new ResponsiveStep("500px", 4)
		);
		workLocation.setPlaceholder("Work Location");
		//block.setVisible(false);
		block.setAllowCustomValue(true);
		block.addValueChangeListener(e->populateImplBlock());
		implDistrict.addValueChangeListener(e-> populateImplAgency());
		//implDistrict.addFocusListener(e->populateImplAgency());
		return form1;
	}
	
	private void populateImplBlock() {
		// TODO Auto-generated method stub
		if(block.getValue()!=null) {
			implAgency.setValue(block.getValue().getBlockLabel());
			implHead.setValue(block.getValue().getBlockDevelopmentOfficer());
			implAddress.setValue(block.getValue().getBlockName());
		}
	}
	private Component createButtonsLayout() {
		
		save.setWidthFull();
		delete.setWidthFull();
		close.setWidthFull();
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		save.addClickShortcut(Key.ENTER);
		close.addClickShortcut(Key.ESCAPE);
		save.addClickListener(event -> validatandSave());
		delete.addClickListener(event -> fireEvent(new DeleteEvent(this, work)));
		//delete.addClickListener(event -> deleteWorks(work));
		close.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1=new HorizontalLayout(save, delete, close);
		//return new HorizontalLayout(save, delete, close);
		hl1.setWidthFull();
		return hl1;
		
	}
	
	public void populateImplAgency() {
		if (implDistrict.getValue() != null) {
			noOfInstallments.setValue(1);
			priority.setValue(1);
			
			if (implDistrict.getValue().getDistrictCode() == service.getDistrict().getDistrictCode()) {
				implAgency.setValue("");
				implHead.setValue("");
				noOfInstallments.setEnabled(true);
				//implAgency.setEnabled(false);
				block.setEnabled(true);
				// implAddress.setEnabled(true);
				// implHead.setEnabled(true);
			} else {
				implAgency.setValue(implDistrict.getValue().getDistrictName());
				implHead.setValue(implDistrict.getValue().getDeputyCommissioner());
				implAddress.setValue(implDistrict.getValue().getDistrictHq());
				noOfInstallments.setEnabled(false);
				implAgency.setVisible(true);
				block.setEnabled(false);
				// implAgency.setEnabled(false);
				// implAddress.setEnabled(false);
				// implHead.setEnabled(false); //impl
			}
		}
	}
	
	public Component configureInstallmentForm() {
		FormLayout form2 = new FormLayout();
		form2.setWidth("100%");
		form2.add(installmentmaster, 2);
		form2.add(installmentAmount, 2);
		//form2.add(installmentCheque, 2);
		form2.setResponsiveSteps(new ResponsiveStep("0", 2),
				// Use two columns, if layout's width exceeds 500px
				new ResponsiveStep("500px", 2));

		return form2;
	}
	private Component createInstallButtons() {
		installsave.setEnabled(isUser);
		installsave.setWidthFull();
		installclose.setWidthFull();
		installsave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		installsave.addClickShortcut(Key.ENTER);
		installclose.addClickShortcut(Key.ESCAPE);
		installsave.addClickListener(event -> SaveInstallments());
		installclose.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1=new HorizontalLayout(installsave, installclose);
		//return new HorizontalLayout(save, delete, close);
		hl1.setWidthFull();
		return hl1;
	}
	
	public Component configureUcForm() {
		FormLayout form2 = new FormLayout();
		form2.setWidth("100%");
		form2.add(ucmaster, 2);
		form2.add(ucDate, 1);
		form2.add(ucletter, 2);
		form2.setResponsiveSteps(new ResponsiveStep("0", 2),
				// Use two columns, if layout's width exceeds 500px
				new ResponsiveStep("500px", 2));

		return form2;
	}
	private Component createUcButtons() {
		ucsave.setEnabled(isUser);
		ucsave.setWidthFull();
		ucclose.setWidthFull();
		ucsave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		ucsave.addClickShortcut(Key.ENTER);
		ucclose.addClickShortcut(Key.ESCAPE);
		ucsave.addClickListener(event -> saveUc());
		ucclose.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1=new HorizontalLayout(ucsave, ucclose);
		//return new HorizontalLayout(save, delete, close);
		hl1.setWidthFull();
		return hl1;
	}
	private void validatandSave() {
		if (work == null) {
			notify.show("Unable To Identify The Work", 5000, Position.TOP_CENTER);
		} else if(noOfInstallments.getValue()<1 || noOfInstallments.getValue()>5){
			notify.show("Failure: Number of Installments Should Be Between 1 and 5",5000, Position.TOP_CENTER);
		}else if(workAmount.getValue()==null||workAmount.getValue().compareTo(BigDecimal.ZERO)==-1 ||workAmount.getValue().compareTo(BigDecimal.ZERO)==0){
			notify.show("Failure: Amount  Must Be Entered .",5000, Position.TOP_CENTER);
		}else if(sanctionDate.getValue()==null){
			notify.show("Failure: Recommended Date Must Be Entered .",5000, Position.TOP_CENTER);
		}else if(implDistrict.getValue().getDistrictId()==service.getDistrict().getDistrictId() && block.getValue()==null){
			notify.show("Failure: Please Select The Implementing Agency.",5000, Position.TOP_CENTER);
		}else if(implDistrict.getValue().getDistrictId()!=service.getDistrict().getDistrictId() && implAgency.getValue()==null){
			notify.show("Failure: Please Enter The Implementing Agency.",5000, Position.TOP_CENTER);
		}else {
			try {
				long singlework=work.getWorkCode();
				binder.writeBean(work);
				if (singlework == 0) {
					work.setWorkCode(service.getWorkCode() + 1);
					work.setWorkStatus("Entered");
					work.setImplDistrict(implDistrict.getValue());
					work.setEnteredBy(service.getloggeduser());
					work.setEnteredOn(LocalDate.now());
				}
				/*if(implDistrict.getValue().getDistrictId()==service.getDistrict().getDistrictId()) {
					work.setImplAgency(block.getValue().getBlockLabel());
				}else {
					work.setImplAgency(implAgency.getValue());
				}*/
				work.setDistrict(service.getDistrict());
				fireEvent(new SaveEvent(this, work));
				
			}catch(Exception e) {
				Notification.show(ErrorReferences.userMessage(e), 5000, Position.TOP_CENTER);
				
			}
		}
	}
	public void setWork(Workmp work) {
        this.work = work; 
        binder.readBean(work); 
    }
	
	private void SaveInstallments() {
		int workinstallments=work.getNoOfInstallments();
		int tableinstallments=service.getInstallmentMps(work).size();
		
		int toEnterInstallment=tableinstallments+1;
		if (work == null) {
			notify.show("Unable To Retrieve Work. Please Select Work From The Table");
		} else if(workinstallments==tableinstallments){
			notify.show("Failure: All Installments Have Been Entered For The Selected Work.",5000, Position.TOP_CENTER);
		}else if(installmentAmount.getValue()==null ||installmentAmount.getValue().compareTo(BigDecimal.ZERO)==0||installmentAmount.getValue().compareTo(BigDecimal.ZERO)==-1){
			notify.show("Failure:Please Enter A Valid Amount .",5000, Position.TOP_CENTER);
		}else if(work.getWorkAmount().compareTo(installmentAmount.getValue())==-1){
			notify.show("Failure: Please Check Released Amount. It Should Be less Or Equal To The Recommended Amount",5000, Position.TOP_CENTER);
		}else if((calculateReleasedInstAmount(work).add(installmentAmount.getValue())).compareTo(work.getWorkAmount())==1 ){
			notify.show("Failure: Released Amount:"+calculateReleasedInstAmount(work)+" & Amount To Be Released has Exceeded The Recommended Amount:"+work.getWorkAmount()+"",5000, Position.TOP_CENTER);
		}else{
			try {
				Installmentmp installment=new Installmentmp();
				installment.setInstallmentAmount(installmentAmount.getValue());
				installment.setInstallmentNo(service.getInstallmentMps(work).size()+1);
				installment.setInstallmentAmountPrev(calculateReleasedInstAmount(work));
				installment.setWorkmp(work);
				service.saveInstallmentMp(installment);
				updateWork("Installment "+toEnterInstallment+"");
				notify.show("Installment No:"+toEnterInstallment+" Entered Sucessfully",5000, Position.TOP_CENTER);
				//notify.show("Installment No:"+toEnterInstallment+" Entered Sucessfully",5000, Position.TOP_CENTER);
			}catch(Exception e) {
				Notification.show(ErrorReferences.userMessage(e));
				
			}
		}
	}
	public BigDecimal calculateReleasedInstAmount(Workmp work) {
		int tablecount = service.getInstallmentMps(work).size();
	
		if (tablecount == 0) {
			return BigDecimal.ZERO;
		} else {
			BigDecimal totalamount=BigDecimal.ZERO;
			for (int i = 0; i < tablecount; i++) {
				BigDecimal amount = service.getInstallmentMps(work).get(i).getInstallmentAmount();
				totalamount = totalamount.add(amount);
			}
			return totalamount;
		}
		
	}
	
	public void saveUc() {
		int tableinstallments=service.getInstallmentMps(work).size();
		int toEnterInstallment=tableinstallments;
		int index=tableinstallments-1;
		if (ucletter.getValue() == null || ucletter.equals("") || ucDate.getValue() == null
				|| ucDate.getValue().equals(null)) {
			notify.show("Please Enter All Values", 5000, Position.TOP_CENTER);
		} else if(ucDate.getValue().isBefore(service.getInstallmentMps(work).get(index).getInstallmentDate())){
			notify.show("UC Date Cannot Be Before Installment Release Date", 5000, Position.TOP_CENTER);
		}else {
			try {
				this.installment=service.getInstallmentMps(work).get(index);
				installment.setUcDate(ucDate.getValue());
				installment.setUcLetter(ucletter.getValue());
				service.saveInstallmentMp(installment);
				if (work.getNoOfInstallments() == tableinstallments) {
					updateWork("Completed");
				} else {
					updateWork("UC " + toEnterInstallment + "");
				}
				notify.show("UC:" + toEnterInstallment + " Entered Sucessfully", 5000, Position.TOP_CENTER);
			} catch (Exception e) {
				Notification.show(ErrorReferences.userMessage(e));
				
			}
		}
	}

	private void updateWork(String text) {
		try {
			binder.writeBean(work);
			work.setWorkStatus(text);
			fireEvent(new SaveEvent(this, work));
		} catch (Exception e) {
			Notification.show(ErrorReferences.userMessage(e));
			
		}

	}
	//Form Events for Save Delete
	
	public static abstract class WorkFormMpEvent extends ComponentEvent<WorkFormMp>{
		private Workmp work;
		
		protected WorkFormMpEvent(WorkFormMp source, Workmp work) {
			super(source, false);
			this.work=work;
		}
		public Workmp getWork() {
			return work;
		}
	}

	public static class SaveEvent extends WorkFormMpEvent {
		SaveEvent(WorkFormMp source, Workmp work) {
			super(source, work);
		}
	}

	public static class DeleteEvent extends WorkFormMpEvent {
		DeleteEvent(WorkFormMp source, Workmp work) {
			super(source, work);
		}

	}

	public static class CloseEvent extends WorkFormMpEvent {
		CloseEvent(WorkFormMp source) {
			super(source, null);
		}
	}

	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,ComponentEventListener<T> listener) {
		return getEventBus().addListener(eventType, listener);
	}
}
	
	
	
	
	
	

