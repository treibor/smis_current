package com.smis.view;

import com.smis.security.ErrorReferences;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.smis.dbservice.DbserviceMp;
import com.smis.entity.Constituencymp;
import com.smis.entity.Impldistrict;
import com.smis.entity.Installment;
import com.smis.entity.Installmentmp;
import com.smis.entity.Workmp;
import com.smis.entity.Year;
import com.vaadin.componentfactory.pdfviewer.PdfViewer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
@PageTitle("MP Release Order")
@Route(value="releaseordermp", layout=MainLayout.class)
@RolesAllowed({"USER","SUPER"})
public class PrintViewMp extends VerticalLayout{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	DbserviceMp service;
	Grid<Installmentmp> grid=new Grid<>(Installmentmp.class);
	Set<Installment> selectedPersons;
	IntegerField instNo=new IntegerField("Installment No:");
	FormLayout layout=new FormLayout();
	ComboBox<Impldistrict> implDistrict = new ComboBox<>("Implementing District");
	ComboBox<Year> year=new ComboBox<>("Financial Year");
	ComboBox<Constituencymp> constituency=new ComboBox<>("Parliamentary Constituency");
	//ComboBox<Block> block=new ComboBox("Block");
	TextField instletter=new TextField("Release Letter No.");
	//TextField installmentcheque=new TextField("Cheque No.");
	DatePicker instdate=new DatePicker("Release Date");
	DatePicker compldate=new DatePicker("Completion Date");
	TextField copyTo=new TextField("Copy To:");
	TextField note=new TextField("Note:");
	Button printButton=new Button("Print");
	HorizontalLayout hl4=new HorizontalLayout();
	//HorizontalLayout middleLayout=new HorizontalLayout();
	Notification notify=new Notification();
	boolean isAdmin;
	public PrintViewMp(DbserviceMp service) {
		//binder.bindInstanceFields(this);
		this.service=service;
		setSizeFull();
		configureGrid();
		populateAllFields();
		printButton.setEnabled(false);
		isAdmin=service.isAdmin();
		add(configureTopLayout(), configueMiddleLayout(),  configureBottomLayout());
	}
	
	
	public Component configureTopLayout() {
		FormLayout layout=new FormLayout();
		layout.setWidth("100%");
		//instNo.setHasControls(true);
		instNo.setMin(1);
		instNo.setMax(5);
		instNo.setValue(1);
		implDistrict.addValueChangeListener(e->populateGrid());
		constituency.addValueChangeListener(e->populateGrid());
		//block.addValueChangeListener(e->populateGrid());
		year.addValueChangeListener(e->populateGrid());
		instNo.addValueChangeListener(e->populateGrid());
		layout.add(implDistrict, 2);
		layout.add(instNo, 1);
		layout.add(year, 1);
		
		//layout.add(block, 2);
		layout.add(constituency, 3);
		//layout.setWidth("30em");
		layout.setResponsiveSteps(
		        new ResponsiveStep("100px", 4),
		        // Use two columns, if layout's width exceeds 500px
		        new ResponsiveStep("1000px", 9)
		);
		HorizontalLayout hl1=new HorizontalLayout(layout);
		hl1.setWidthFull();
		return layout;
	}
	public Component configureBottomLayout() {
		FormLayout blayout=new FormLayout();
		//populateAllFields();
		compldate.setHelperText("As Per Scheme Duration");
		printButton.addClickListener(e->printReport());
		layout.setWidthFull();
		blayout.add(instletter, 1);
		blayout.add(instdate, 1);
		blayout.add(compldate, 1);
		//blayout.add(installmentcheque, 1);
		blayout.add(copyTo, 2);
		blayout.add(note, 2);
		blayout.add(printButton, 1);
		//blayout.setWidth("30em");
		blayout.setResponsiveSteps(
		        new ResponsiveStep("0", 8),
		        // Use two columns, if layout's width exceeds 500px
		        new ResponsiveStep("500px", 8)
		);
		return blayout;
	}

	public Component configueMiddleLayout() {
		HorizontalLayout middleLayout=new HorizontalLayout(grid, hl4);
		middleLayout.setFlexGrow(1, grid);
		middleLayout.setFlexGrow(1, hl4);
		middleLayout.setSizeFull();
		return middleLayout;
	}
	

	private void printReport() {
		
		int installno=instNo.getValue();
		//int index=installno-1;
		if (instletter.getValue() == "" || instdate.getValue() == null || compldate.getValue()==null){
			notify.show( "Release Letter, Release Date  and Completion  Date Cannot Be Empty", 5000, Position.TOP_CENTER);
		} else {
			Set<Installmentmp> installmentset = grid.getSelectedItems();
			List<Installmentmp> installments=new ArrayList<>(installmentset);
			if (installments.get(0).getWorkmp().getSanctionDate().isAfter(instdate.getValue())) {
				notify.show("Release Date  Cannot be before the sanction Date", 5000, Position.TOP_CENTER);
			}else if (installments.get(0).getWorkmp().getSanctionDate().isAfter(compldate.getValue())||instdate.getValue().isAfter(compldate.getValue() )) {
				notify.show("Invalid Completion Date. Please do not Modify Completion date unless its is Really Necessary", 5000, Position.TOP_CENTER);
			}else if (installno >1 && service.getInstallmentByWorkAndNo(installments.get(0).getInstallmentNo()-1, installments.get(0).getWorkmp()).getUcDate().isAfter(instdate.getValue())) {
				notify.show("Invalid Release  Date. Release Date Has to Be After the UC date of Previous Installment", 5000, Position.TOP_CENTER);
			} else {
				try {
					int selecteditems = installments.size();
					String yearlabel=changeAmp(installments.get(0).getWorkmp().getYear().getYearLabel());
					String sanctionNo=changeAmp(installments.get(0).getWorkmp().getSanctionNo());
					String implAgency=changeAmp(installments.get(0).getWorkmp().getImplAgency());
					LocalDate completion=compldate.getValue();
					int installNo;
					if(instNo.getValue()<3) {
						installNo=instNo.getValue();
					}else {
						installNo=3;
					}
					BigDecimal totalamount=BigDecimal.ZERO;
					for (int i = 0; i < selecteditems; i++) {
						totalamount=installments.get(i).getInstallmentAmount().add(totalamount);
						Installmentmp singleinstallment = installments.get(i);
						singleinstallment.setInstallmentDate(instdate.getValue());
						singleinstallment.setInstallmentLetter(instletter.getValue());
						//singleinstallment.setInstallmentCheque(installmentcheque.getValue());
						service.saveInstallmentMp(singleinstallment);
						Workmp singlework = singleinstallment.getWorkmp();
						if (singleinstallment.getUcLetter() == null) {
							singlework.setWorkStatus("Release Order " + singleinstallment.getInstallmentNo());
						}
						service.saveWork(singlework);
					}
					String totalAmountwords=convertToIndianCurrency(totalamount+"");
					String totalAmountnumbers = totalamount.stripTrailingZeros().toPlainString();
					populateGrid();
					
					Resource resource=null;
					//URL res =null;
					Map<String, Object> parameters = new HashMap<>();
					if(implDistrict.getValue().getDistrictId()!=service.getDistrict().getDistrictId()) {
					//Resource resource = new ClassPathResource("report/Release"+reportType+""+installNo+".jrxml"); // removePdfViewer();
						resource = new ClassPathResource("report/Releasemp11.jrxml"); // removePdfViewer();
						if(copyTo.getValue()=="") {
							parameters.put("CopyTo", "");
						}else {
							parameters.put("CopyTo", "4. "+copyTo.getValue());
						}
					}else {
						resource = new ClassPathResource("report/Releasemp2"+installNo+".jrxml");
						if(copyTo.getValue()=="") {
							parameters.put("CopyTo", "");
						}else {
							parameters.put("CopyTo", "8. "+copyTo.getValue());
						}
					}
					URL res = getClass().getClassLoader().getResource("report/Releasemp11.jrxml");
					File file = Paths.get(res.toURI()).toFile();
					String absolutePath = file.getAbsolutePath();
					String reportPath=absolutePath.substring(0, absolutePath.length()-17);
					//String reportPath = "D:";
					InputStream employeeReportStream = resource.getInputStream();
					JasperReport jasperReport = JasperCompileManager.compileReport(employeeReportStream);
					JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(
							installments);
					
					parameters.put("Note", note.getValue());
					parameters.put("implAgency", implAgency);
					parameters.put("ComplDate", completion);
					parameters.put("year", yearlabel);
					parameters.put("sanctionNo", sanctionNo);
					parameters.put("amount",  "(Rupees "+totalAmountwords+" only.)");
										
					JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters,
							jrBeanCollectionDataSource);
					JasperExportManager.exportReportToPdfFile(jasperPrint, reportPath+"//releaseordermp.pdf");
					File a = new File(reportPath+"//releaseordermp.pdf");
					StreamResource resourcerange = new StreamResource("ReleaseOrder.pdf", () -> createResource(a));
					PdfViewer pdfViewerrange = new PdfViewer();
					pdfViewerrange.setSrc(resourcerange);
					hl4.setVisible(true);
					hl4.setSizeFull();
					hl4.add(pdfViewerrange);

				} catch (Exception e) {
					Notification.show(ErrorReferences.userMessage(e), 5000, Position.TOP_CENTER);
					// Position.TOP_CENTER);
					

				}
			}
		}
	}	
	public String changeAmp(String label) {
		if(label.contains("&")) {
			String replacedstring =label.replace("&", "&amp;");
			return replacedstring;
		}else {
			return label;
		}
	}
	
	private InputStream createResource(File path) {// get generated pdf file and create Resource
		try {
			return FileUtils.openInputStream(path);
		} catch (Exception ex) {
		}
		return null;
	}
	private void populateAllFields() {
		year.setItems(service.getAllYears());
		year.setItemLabelGenerator(year->year.getYearName());
		implDistrict.setItems(service.getAllImplDistricts());
		implDistrict.setItemLabelGenerator(district-> district.getDistrictName());
		//block.setItems(service.getAllBlocks());
		//block.setItemLabelGenerator(block-> block.getBlockName());
		constituency.setItems(service.getAllConstituencies());
		constituency.setItemLabelGenerator(constituency->constituency.getConstituencyType()+" - "+constituency.getConstituencyMp());
	}
	
	public void configureGrid() {
		grid.setSizeFull();
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.setColumns("installmentAmount");
		grid.addColumn(installment-> installment.getWorkmp().getWorkCode()+"-"+installment.getWorkmp().getWorkName()).setHeader("Work");
		grid.addColumn(installment-> installment.getWorkmp().getSanctionNo()).setHeader("Recomm Letter");
		grid.addColumn(installment-> installment.getWorkmp().getSanctionDate()).setHeader("Recomm. Date");
		grid.addColumn(installment-> installment.getWorkmp().getNoOfInstallments()).setHeader("No of Inst");
		grid.addColumn(installment-> installment.getInstallmentLetter()).setHeader("Release No");
		grid.addColumn(installment-> installment.getInstallmentDate()).setHeader("Release Date");
		//grid.addColumn(installment-> installment.getInstallmentCheque()).setHeader("Cheque No.");
		grid.addColumn(installment-> installment.getWorkmp().getWorkStatus()).setHeader("Status");
		grid.getColumns().forEach(col-> col.setAutoWidth(true));
		grid.addSelectionListener(event ->doSomething(event));
	}
	
	public void doSomething(SelectionEvent e) {
		LocalDate complDate = null;
		if (e.getAllSelectedItems().size() > 0) {
			hl4.setVisible(false);
			printButton.setEnabled(true);
			Set<Installmentmp> selected = grid.getSelectedItems();
			List<Installmentmp> installs = new ArrayList<>(selected);
			Installmentmp installsingle = installs.get(0);
			int schemeduration = installsingle.getWorkmp().getConstituencymp().getSchemeDuration();
			LocalDate sancDate = installsingle.getWorkmp().getSanctionDate();
			complDate = sancDate.plusMonths(schemeduration);
			compldate.setValue(complDate);
			instdate.setValue(installsingle.getInstallmentDate());
			
			//String letterNo=installsingle.getInstallmentLetter()+"";
			try {
				instletter.setValue(installsingle.getInstallmentLetter());
				//installmentcheque.setValue(installsingle.getInstallmentCheque());
			}catch(NullPointerException npe) {
				instletter.setValue("");
				//installmentcheque.setValue("");
			}
		} else {
			printButton.setEnabled(false);
			compldate.setValue(null);
			instletter.setValue("");
			instdate.setValue(null);
			//installmentcheque.setValue("");
		}
	}


	public void populateGrid() {
		try {
			int instno = instNo.getValue();
			if (year.getValue() != null || constituency.getValue() != null
					 || instno > 0 || instno <= 5 ||implDistrict.getValue()!=null) {
				if (hl4 != null) {
					hl4.removeAll();
				}
				
				if(implDistrict.getValue().getDistrictCode()==service.getDistrict().getDistrictCode()) {
					instNo.setEnabled(true);
				}else {
					instNo.setValue(1);
					instNo.setEnabled(false);
					
				}
				grid.setItems(service.getFilteredInstallmentMps(constituency.getValue(), year.getValue(), instno, implDistrict.getValue()));
				configureGrid();
				
			}
		} catch (Exception e) {
			
		}
	}

	public static String convertToIndianCurrency(String num) {
        BigDecimal bd = new BigDecimal(num);
        long number = bd.longValue();
        long no = bd.longValue();
        int decimal = (int) (bd.remainder(BigDecimal.ONE).doubleValue() * 100);
        int digits_length = String.valueOf(no).length();
        int i = 0;
        ArrayList<String> str = new ArrayList<>();
        HashMap<Integer, String> words = new HashMap<>();
        words.put(0, "");
        words.put(1, "One");
        words.put(2, "Two");
        words.put(3, "Three");
        words.put(4, "Four");
        words.put(5, "Five");
        words.put(6, "Six");
        words.put(7, "Seven");
        words.put(8, "Eight");
        words.put(9, "Nine");
        words.put(10, "Ten");
        words.put(11, "Eleven");
        words.put(12, "Twelve");
        words.put(13, "Thirteen");
        words.put(14, "Fourteen");
        words.put(15, "Fifteen");
        words.put(16, "Sixteen");
        words.put(17, "Seventeen");
        words.put(18, "Eighteen");
        words.put(19, "Nineteen");
        words.put(20, "Twenty");
        words.put(30, "Thirty");
        words.put(40, "Forty");
        words.put(50, "Fifty");
        words.put(60, "Sixty");
        words.put(70, "Seventy");
        words.put(80, "Eighty");
        words.put(90, "Ninety");
        String digits[] = {"", "Hundred", "Thousand", "Lakh", "Crore"};
        while (i < digits_length) {
            int divider = (i == 2) ? 10 : 100;
            number = no % divider;
            no = no / divider;
            i += divider == 10 ? 1 : 2;
            if (number > 0) {
                int counter = str.size();
                String plural = (counter > 0 && number > 9) ? "" : "";
                String tmp = (number < 21) ? words.get(Integer.valueOf((int) number)) + " " + digits[counter] + plural : words.get(Integer.valueOf((int) Math.floor(number / 10) * 10)) + " " + words.get(Integer.valueOf((int) (number % 10))) + " " + digits[counter] + plural;                
                str.add(tmp);
            } else {
                str.add("");
            }
        }
 
        Collections.reverse(str);
        String Rupees = String.join(" ", str).trim();
 
        String paise = (decimal) > 0 ? " And Paise " + words.get(Integer.valueOf((int) (decimal - decimal % 10))) + " " + words.get(Integer.valueOf((int) (decimal % 10))) : "";
        return  Rupees + paise;
    }
	
}
