package com.smis.view;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.smis.dbservice.Dbservice;
import com.smis.entity.Block;
import com.smis.entity.Constituency;
import com.smis.entity.Installment;
import com.smis.entity.Scheme;
import com.smis.entity.Village;
import com.smis.entity.Work;
import com.smis.entity.Year;
import com.smis.util.ValidationUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;

public class WorkForm extends VerticalLayout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Dbservice service;
	//@Autowired
	
	private Work work;
	private Installment installment;
	Binder<Work> binder = new BeanValidationBinder<>(Work.class);
	ComboBox<Scheme> scheme = new ComboBox<>("Schemes");
	ComboBox<Year> year = new ComboBox<>("Financial Year");
	ComboBox<Constituency> constituency = new ComboBox<>("Assembly Constituency");
	ComboBox<Block> block = new ComboBox<>("Block/MB");
	ComboBox<Village> village = new ComboBox<>("Village");
	// TextField workName=new TextField("Work Name");
	TextArea workName=new TextArea("Work Name");
	ComboBox<String> workSelect = new ComboBox<>("Select From Previous Works");
	IntegerField noOfInstallments = new IntegerField("No Of Installments");
	// NumberField noOfInstallments=new NumberField("No Of Installments");
	// NumberField workAmount=new NumberField("Sanctioned Amount");
	BigDecimalField workAmount = new BigDecimalField("Sanctioned Amount");
	//TextField sanctionNo = new TextField("Sanction Number");
	ComboBox<String> sanctionNo = new ComboBox<>("Sanction No");
	DatePicker sanctionDate = new DatePicker("Sanction Date");
	Button save = new Button("Save");
	Button delete = new Button("Delete");
	Button close = new Button("Close");
	Button installsave = new Button("Save");
	Button installclose = new Button("Close");
	Button ucsave = new Button("Save");
	Button ucclose = new Button("Close");
	// ConfirmDialog cdialog;
	BigDecimalField installmentAmount = new BigDecimalField("Amount");
	// TextField installmentCheque =new TextField("Cheque Number");
	TextField ucletter = new TextField("UC Number");
	
	DatePicker ucDate = new DatePicker("UC Date");
	//Notification notify = new Notification();
	Accordion accordion = new Accordion();
	AccordionPanel workaccordion = new AccordionPanel();
	AccordionPanel installaccordion = new AccordionPanel();
	AccordionPanel ucaccordion = new AccordionPanel();
	// Label installmentmaster=new Label("");
	H3 installmentmaster = new H3("");
	H3 ucmaster = new H3("");
	// Label ucmaster=new Label("");
	boolean isAdmin;
	boolean isUser;
	
	public WorkForm(Dbservice service) {
		block.addValueChangeListener(e -> getVillages(e.getValue()));
		this.service = service;
		binder.bindInstanceFields(this);
		isAdmin = service.isAdmin();
		isUser = service.isUser();
		add(createFinalPanel());

	}

	public void getVillages(Block block) {
		village.setItems(service.getVillage(block));
		village.setItemLabelGenerator(village -> village.getVillageName());
	}

	public Component createFinalPanel() {

		// accordion.add("Work", new VerticalLayout(configureForm(),
		// createButtonsLayout()));
		workaccordion = accordion.add("Work", new VerticalLayout(configureForm(), createButtonsLayout()));
		installaccordion = accordion.add("Installments",
				new VerticalLayout(configureInstallmentForm(), createInstallButtons()));
		ucaccordion = accordion.add("Utilisation Certificate",
				new VerticalLayout(configureUcForm(), createUcButtons()));
		return accordion;
	}
	
	public Component configureForm() {
		ValidationUtil.applyTextAreaValidation(workName);
		ValidationUtil.applyValidation(ucletter);
		//addCustomValueSetListener(workSelect);
		//addCustomValueSetListener(sanctionNo);
		noOfInstallments.setStepButtonsVisible(true);
		noOfInstallments.setMin(1);
		noOfInstallments.setMax(3);
		// noOfInstallments.setAutocorrect(true);
		workName.setHeight("100px");
		// workName.setErrorMessage("Hello");
		// workName.val
		noOfInstallments.setValue(1);
		scheme.setItems(service.getAllSchemes());
		year.setItems(service.getAllYears());
		constituency.setItems(service.getAllConstituencies());
		block.setItems(service.getAllBlocks());
		scheme.setItemLabelGenerator(Scheme::getSchemeName);
		year.setItemLabelGenerator(Year::getYearName);
		constituency.setItemLabelGenerator(constituency -> constituency.getConstituencyNo() + "-"
				+ constituency.getConstituencyName() + "-" + constituency.getConstituencyMLA());
		block.setItemLabelGenerator(block -> block.getBlockName());
		workName.setMinLength(5);
		workName.setMaxLength(999);
		// Work work=new Work();
		workSelect.setItems(service.getWorkNames());
		workSelect.setAllowCustomValue(true);
		workSelect.addCustomValueSetListener(e -> {
			String workname = e.getDetail();
			//System.out.println(workname);
			workSelect.setItems(workname);
			workSelect.setValue(workname);
		});
		workSelect.addValueChangeListener(e->workName.setValue(workSelect.getValue()));
		//sanctionNo.setMinLength(2);
		//sanctionNo.setMaxLength(50);
		sanctionNo.setItems(service.getSanctionNos());
		sanctionNo.setAllowCustomValue(true);
		sanctionNo.addCustomValueSetListener(e->{
			String sancno=e.getDetail();
			sanctionNo.setItems(sancno);
			sanctionNo.setValue(sancno);
		});
		
		workName.addClassName("custom-combobox");
		
		// village.setItemLabelGenerator(village->village.getVillageName());
		FormLayout form1 = new FormLayout();
		form1.setWidth("100%");
		form1.add(scheme, 1);
		form1.add(year, 1);
		form1.add(constituency, 2);
		form1.add(block, 1);
		form1.add(village, 1);
		form1.add(workSelect, 2);
		form1.add(workName, 2);
		form1.add(workAmount, 1);
		form1.add(noOfInstallments, 1);
		form1.add(sanctionNo, 1);
		form1.add(sanctionDate, 1);
		// form1.add(createButtonsLayout(), 2);
		form1.setResponsiveSteps(new ResponsiveStep("0", 2),
				// Use two columns, if layout's width exceeds 500px
				new ResponsiveStep("500px", 2));
		return form1;
	}
	
	private Component createButtonsLayout() {
		
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		save.addClickShortcut(Key.ENTER);
		close.addClickShortcut(Key.ESCAPE);
		save.addClickListener(event -> validatandSave());
		// delete.addClickListener(event -> fireEvent(new DeleteEvent(this, work)));
		delete.addClickListener(event -> confirmDelete(work));
		close.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1 = new HorizontalLayout(save, delete, close);
		// return new HorizontalLayout(save, delete, close);
		hl1.setFlexGrow(1, save);
		hl1.setFlexGrow(1, delete);
		hl1.setFlexGrow(1, close);
		hl1.setWidthFull();
		return hl1;

	}

	public void confirmDelete(Work work) {
		ConfirmDialog dialog = new ConfirmDialog();
		if (work == null) {

		} else {
			dialog.setHeader("Delete??");
			dialog.setText("Are You sure you want to delete this item.");
			dialog.setCancelable(true);
			dialog.addCancelListener(event -> dialog.close());
			dialog.setRejectable(true);
			dialog.setRejectText("Discard");
			dialog.addRejectListener(event -> dialog.close());
			dialog.setConfirmText("Delete");
			dialog.addConfirmListener(event -> fireEvent(new DeleteEvent(this, work)));
			dialog.open();

		}
	}

	public Component configureInstallmentForm() {
		FormLayout form2 = new FormLayout();
		form2.setWidth("100%");
		form2.add(installmentmaster, 2);
		form2.add(installmentAmount, 2);
		// form2.add(installmentCheque, 2);
		form2.setResponsiveSteps(new ResponsiveStep("0", 2),
				// Use two columns, if layout's width exceeds 500px
				new ResponsiveStep("500px", 2));

		return form2;
	}

	private Component createInstallButtons() {
		//installsave.setEnabled(isUser);
		// installsave.setWidthFull();
		// installclose.setWidthFull();
		installsave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		installsave.addClickShortcut(Key.ENTER);
		installclose.addClickShortcut(Key.ESCAPE);
		installsave.addClickListener(event -> SaveInstallments());
		installclose.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1 = new HorizontalLayout(installsave, installclose);
		// return new HorizontalLayout(save, delete, close);
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
		//ucsave.setEnabled(isUser);
		// ucsave.setWidthFull();
		// ucclose.setWidthFull();
		ucsave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		ucsave.addClickShortcut(Key.ENTER);
		ucclose.addClickShortcut(Key.ESCAPE);
		ucsave.addClickListener(event -> saveUc());
		ucclose.addClickListener(event -> fireEvent(new CloseEvent(this)));
		HorizontalLayout hl1 = new HorizontalLayout(ucsave, ucclose);
		// return new HorizontalLayout(save, delete, close);
		hl1.setWidthFull();
		return hl1;
	}

	private void validatandSave() {
		if (work == null) {
			Notification.show("Unable To Identify The Work", 5000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else if (noOfInstallments.getValue() < 1 || noOfInstallments.getValue() > 5) {
			Notification.show("Failure: Number of Installments Should Be Between 1 and 5", 5000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else if (workAmount.getValue() == null || workAmount.getValue().compareTo(BigDecimal.ZERO) == -1
				|| workAmount.getValue().compareTo(BigDecimal.ZERO) == 0) {
			Notification.show("Failure: Amount  Must Be Entered .", 5000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else if (sanctionDate.getValue() == null) {
			Notification.show("Failure: Sanction Date Must Be Entered .", 5000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else if(!ValidationUtil.applyValidation(sanctionNo.getValue())) {
			Notification.show("Sanction No: Special Characters like *, ?, ^,%, $ ,#  are not allowed.", 5000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
			else {
			try {
				long singlework = work.getWorkCode();
				binder.writeBean(work);
				long newWorkCode = service.getWorkCode() + 1;
				if (singlework == 0) {
					work.setWorkCode(newWorkCode);
					work.setWorkStatus("Entered");
					work.setEnteredBy(service.getloggeduser());
					work.setEnteredOn(LocalDate.now());
				}
				work.setDistrict(service.getDistrict());
				fireEvent(new SaveEvent(this, work));
				// notify.show("New Work Entered Successfully with Work Code: "+newWorkCode,
				// 5000, Position.TOP_CENTER);

			} catch (Exception e) {
				Notification.show("Unable to Save Work. Please Enter All Mandatory Fields" + e, 5000, Position.TOP_CENTER)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);

			}
		}
	}
	
	public void setWork(Work work) {
		this.work = work;
		binder.readBean(work);
	}

	private void SaveInstallments() {
		int workinstallments = work.getNoOfInstallments();
		int tableinstallments = service.getInstallments(work).size();

		int toEnterInstallment = tableinstallments + 1;
		if (work == null) {
			Notification.show("Unable To Retrieve Work. Please Select Work From The Table");
		} else if (workinstallments == tableinstallments) {
			Notification.show("Failure: All Installments Have Been Entered For The Selected Work.", 5000,
					Position.TOP_CENTER);
		} else if (installmentAmount.getValue() == null || installmentAmount.getValue().compareTo(BigDecimal.ZERO) == 0
				|| installmentAmount.getValue().compareTo(BigDecimal.ZERO) == -1) {
			Notification.show("Failure:Please Enter A Valid Amount .", 5000, Position.TOP_CENTER);
		} else if (work.getWorkAmount().compareTo(installmentAmount.getValue()) == -1) {
			Notification.show("Failure: Please Check Released Amount. It Should Be less Or Equal To The Sanctioned Amount",
					5000, Position.TOP_CENTER);
		} else if ((calculateReleasedInstAmount(work).add(installmentAmount.getValue()))
				.compareTo(work.getWorkAmount()) == 1) {
			Notification.show("Failure: Released Amount:" + calculateReleasedInstAmount(work)
					+ " & Amount To Be Released has Exceeded The Sanctioned Amount:" + work.getWorkAmount() + "", 5000,
					Position.TOP_CENTER);
		} else {
			try {
				Installment installment = new Installment();
				installment.setInstallmentAmount(installmentAmount.getValue());
				installment.setInstallmentNo(service.getInstallments(work).size() + 1);
				installment.setInstallmentAmountPrev(calculateReleasedInstAmount(work));
				installment.setEnteredBy(service.getloggeduser());
				installment.setEnteredOn(LocalDate.now());
				installment.setWork(work);
				service.saveInstallment(installment);
				updateWork("Installment " + toEnterInstallment + "");
				//audit.saveAudit(work, "Save Installment");
				Notification.show("Installment No:" + toEnterInstallment + " Entered Sucessfully", 5000, Position.TOP_CENTER);
				// notify.show("Installment No:"+toEnterInstallment+" Entered Sucessfully",5000,
				// Position.TOP_CENTER);
			} catch (Exception e) {
				Notification.show("Unable to Save Work" + e);

			}
		}
	}

	public BigDecimal calculateReleasedInstAmount(Work work) {
		int tablecount = service.getInstallments(work).size();

		if (tablecount == 0) {
			return BigDecimal.ZERO;
		} else {
			BigDecimal totalamount = BigDecimal.ZERO;
			for (int i = 0; i < tablecount; i++) {
				BigDecimal amount = service.getInstallments(work).get(i).getInstallmentAmount();
				totalamount = totalamount.add(amount);
			}
			return totalamount;
		}

	}

	public void saveUc() {
		int tableinstallments = service.getInstallments(work).size();
		int toEnterInstallment = tableinstallments;
		int index = tableinstallments - 1;
		if (ucletter.getValue() == null || ucletter.equals("") || ucDate.getValue() == null
				|| ucDate.getValue().equals(null)) {
			Notification.show("Please Enter All Values", 5000, Position.TOP_CENTER);
		} else if (ucDate.getValue().isBefore(service.getInstallments(work).get(index).getInstallmentDate())) {
			Notification.show("UC Date Cannot Be Before Installment Release Date", 5000, Position.TOP_CENTER);
		} else {
			try {
				this.installment = service.getInstallments(work).get(index);
				installment.setUcDate(ucDate.getValue());
				installment.setUcLetter(ucletter.getValue());
				//audit=new Audit(service);
				//audit.saveAudit(work, "Save UC");
				service.saveInstallment(installment);
				if (work.getNoOfInstallments() == tableinstallments) {
					updateWork("Completed");
				} else {
					updateWork("UC " + toEnterInstallment + "");
				}
				
				Notification.show("UC:" + toEnterInstallment + " Entered Sucessfully", 5000, Position.TOP_CENTER);
			} catch (Exception e) {
				Notification.show("Unable to Save Work" + e);

			}
		}
	}

	private void updateWork(String text) {
		try {
			binder.writeBean(work);
			work.setWorkStatus(text);
			fireEvent(new SaveEvent(this, work));
		} catch (Exception e) {
			Notification.show("Unable to Save Work" + e);

		}

	}
	// Form Events for Save Delete

	public static abstract class WorkFormEvent extends ComponentEvent<WorkForm> {
		private Work work;

		protected WorkFormEvent(WorkForm source, Work work) {
			super(source, false);
			this.work = work;
		}

		public Work getWork() {
			return work;
		}
	}

	public static class SaveEvent extends WorkFormEvent {
		SaveEvent(WorkForm source, Work work) {
			super(source, work);
		}
	}

	public static class DeleteEvent extends WorkFormEvent {
		DeleteEvent(WorkForm source, Work work) {
			super(source, work);
		}

	}

	public static class CloseEvent extends WorkFormEvent {
		CloseEvent(WorkForm source) {
			super(source, null);
		}
	}

	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
			ComponentEventListener<T> listener) {
		return getEventBus().addListener(eventType, listener);
	}
}