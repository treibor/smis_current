package com.smis.view;

import com.smis.security.ErrorReferences;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.smis.dbservice.Dbservice;
import com.smis.entity.District;
import com.smis.entity.UsersRoles;
import com.smis.entity.State;
import com.smis.entity.Users;
import com.smis.security.SecurityService;
import com.smis.security.PasswordPolicy;
import com.smis.security.noauth.NoAuthTestUser;
import com.smis.view.processflow.WorkViewNew;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Dbservice service;
	@Autowired
	SecurityService securityService;
	Dialog dialog;
	Dialog userdialog;
	Dialog aboutdialog;
	//Notification notify;
	PasswordField oldpwd;
	PasswordField newpwd;
	PasswordField confirmpwd;
	Button cancelButton = new Button("Cancel");
	Button saveButton = new Button("Save");
	TextField userName = new TextField("User Name");
	String userType;
	ComboBox<State> state = new ComboBox<>("State");
	ComboBox<District> district = new ComboBox<>("District");
	final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	ComboBox<String> usertype = new ComboBox<>("Role");
	private Users user;
	boolean isUser;
	boolean isAdmin;
	boolean isSuper;
	//private boolean isPasswordExpired;
	Anchor anchor = new Anchor("", "SMIS 2.0");
	private final boolean noAuthTestMode;

	public MainLayout(Dbservice dbservice, Environment environment) {
		this.service = dbservice;
		this.noAuthTestMode = environment.matchesProfiles(NoAuthTestUser.PROFILE);
		usertype.setItems("ADMIN", "USER");
		isAdmin = service.isAdmin();
		isSuper = service.isSuperAdmin();
		isUser = service.isUser();
		createHeader();
		createDrawer();
		checkPasswordExpiry();
		// addToDrawer(createHeader(), createDrawer());

		// setPrimarySection(Section.DRAWER);
	}

	

	private void checkPasswordExpiry() {
		user = service.getLoggedUser();
		LocalDate expiryDate = user.getPwdChangedDate();
		LocalDate expiryDatePlus180Days = expiryDate.plus(180, ChronoUnit.DAYS);
		LocalDate today = LocalDate.now();
		boolean isExpiryDateValid = expiryDatePlus180Days.isAfter(today);
		if (!isExpiryDateValid) {
			openMandatoryPasswordDialog();
		}
	}

	

	public void populateDistricts() {
		district.setItems(service.getAllDistricts(state.getValue()));
	}

	private void createDrawer() {

		VerticalLayout drawerContent = new VerticalLayout();

		// Add navigation items with helper text
		SideNavItemWithHelperText home = new SideNavItemWithHelperText("", "Home", HomeView.class,
				LineAwesomeIcon.HOME_SOLID.create());
		SideNavItemWithHelperText mla = new SideNavItemWithHelperText("", "Works", WorkView.class,
				LineAwesomeIcon.PEOPLE_CARRY_SOLID.create());
		SideNavItemWithHelperText releaseorder = new SideNavItemWithHelperText("", "Release Order", PrintView.class,
				LineAwesomeIcon.DONATE_SOLID.create());
		SideNavItemWithHelperText master = new SideNavItemWithHelperText("", "Master", MasterView.class,
				LineAwesomeIcon.BALANCE_SCALE_LEFT_SOLID.create());
		SideNavItemWithHelperText schemeprocess = new SideNavItemWithHelperText("", "Scheme Process", SchemeProcessView.class,
				LineAwesomeIcon.BALANCE_SCALE_LEFT_SOLID.create());
		SideNavItemWithHelperText distmaster = new SideNavItemWithHelperText("", "District Master", DistView.class,
				LineAwesomeIcon.BALANCE_SCALE_RIGHT_SOLID.create());
		SideNavItemWithHelperText report = new SideNavItemWithHelperText("", "Reports", ReportView.class,
				LineAwesomeIcon.CALCULATOR_SOLID.create());
		SideNavItemWithHelperText audit = new SideNavItemWithHelperText("", "Audit Trail", AuditView.class,
				LineAwesomeIcon.CALENDAR.create());
		SideNavItemWithHelperText users = new SideNavItemWithHelperText("", "Users", UsersView.class,
				LineAwesomeIcon.USER.create());
		SideNavItemWithHelperText newWorks = new SideNavItemWithHelperText("", "New Works", WorkViewTest.class,
				LineAwesomeIcon.PEOPLE_CARRY_SOLID.create());
		master.setVisible(isAdmin);
		distmaster.setVisible(isSuper);
		releaseorder.setVisible(isUser);
		audit.setVisible(isAdmin);
		users.setVisible(isAdmin);
		// nav.setWidth("5%");
		// getElement().getStyle().set("--_vaadin-app-layout-drawer-width", "2px");
		// addToDrawer(new VerticalLayout(nav));
		// addToDrawer(nav);
		//drawerContent.add(home, mla, releaseorder, master, distmaster, report, audit, users,schemeprocess, newWorks);
		drawerContent.add(home, mla, releaseorder, master, distmaster, report, audit, users);
		addToDrawer(drawerContent);
	}

	private void createHeader() {

		Avatar avatarImage = new Avatar(service.getloggeduser());
		avatarImage.setColorIndex(2);
		// avatarImage.addThemeVariants(AvatarVariant.LUMO_LARGE);
		MenuBar menuBar = new MenuBar();
		menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
		MenuItem item = menuBar.addItem(avatarImage);
		SubMenu subMenu = item.getSubMenu();
		subMenu.addItem("About", e -> openAboutDialog());
		subMenu.addItem("Change Password", e -> openPasswordDialog());
		subMenu.addItem("Create User", e -> createUser()).setVisible(isAdmin);
		subMenu.addItem("Logout", e -> securityService.logout());
		// SubMenu shareSubMenu = share.getSubMenu();
		// anchor.setTarget("/");
		H3 logo = new H3("SMIS 2.0  || " + service.getDistrict().getDistrictName().toUpperCase());
		// logo.addClassNames("text-s", "m-m");
		HorizontalLayout header = new HorizontalLayout();
		header.add(new DrawerToggle());
		if (noAuthTestMode) {
			Span testModeBanner = new Span("TEST MODE – AUTHENTICATION DISABLED");
			testModeBanner.getElement().setAttribute("role", "alert");
			testModeBanner.addClassName("no-auth-test-banner");
			testModeBanner.getStyle().set("background", "#b00020").set("color", "white")
					.set("font-weight", "700").set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
					.set("border-radius", "var(--lumo-border-radius-m)");
			header.add(testModeBanner);
		}
		header.add(logo, menuBar);
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(logo);
		header.setWidthFull();
		header.addClassNames("py-0", "px-m");
		addToNavbar(header);
	}

	private void openAboutDialog() {
		if (aboutdialog != null) {
			aboutdialog = null;
		}
		aboutdialog = new Dialog();
		VerticalLayout dialogLayout1 = createAboutDialog(aboutdialog);
		aboutdialog.add(dialogLayout1);
		aboutdialog.open();
	}

	private VerticalLayout createAboutDialog(Dialog dialog2) {
		H2 headline = new H2("About");
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0").set("font-size", "1.5em").set("font-weight",
				"bold");
		// Label text1=new Label("Designed and Developed By NIC, Meghalaya");
		// Label text2=new Label("Contact: aiban.m@nic.in");
		H3 text1 = new H3("Designed and Developed By NIC, Meghalaya");
		H3 text2 = new H3("Contact: aiban.m@nic.in");
		Button close = new Button("Close");
		close.addClickListener(e -> dialog2.close());
		VerticalLayout dialogLayout1 = new VerticalLayout(headline, text1, text2, close);
		dialogLayout1.setPadding(false);
		dialogLayout1.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout1.getStyle().set("width", "300px").set("max-width", "100%");
		// clearDialog();
		return dialogLayout1;
	}

	private void openPasswordDialog() {
		if (dialog != null) {
			dialog = null;
		}
		dialog = new Dialog();
		dialog.setModal(true);
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		VerticalLayout dialogLayout = createDialogLayout(dialog);
		dialog.add(dialogLayout);
		dialog.open();
	}
	private void openMandatoryPasswordDialog() {
		if (dialog != null) {
			dialog = null;
		}
		dialog = new Dialog();
		dialog.setModal(true);
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		//VerticalLayout dialogLayout = createDialogLayout(dialog);
		H2 headline = new H2("Password Expired - Change Password");
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0").set("font-size", "1.5em").set("font-weight",
				"bold");

		oldpwd = new PasswordField("Old Password");
		newpwd = new PasswordField("New Password");
		confirmpwd = new PasswordField("Confirm New Password");
		configurePasswordFields();
		oldpwd.setRevealButtonVisible(false);
		newpwd.setRevealButtonVisible(false);
		confirmpwd.setRevealButtonVisible(false);
		// oldpwd.setValue("");
		//cancelButton.setText(//userType);
		cancelButton.addClickListener(e -> securityService.logout());
		saveButton.addClickListener(e -> changePassword());
		VerticalLayout fieldLayout = new VerticalLayout(oldpwd, newpwd, confirmpwd);
		fieldLayout.setSpacing(false);
		fieldLayout.setPadding(false);
		fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
		buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		VerticalLayout dialogLayout = new VerticalLayout(headline, fieldLayout, buttonLayout);
		dialogLayout.setPadding(false);
		dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");
		clearDialog();
		dialog.add(dialogLayout);
		dialog.open();
	}
	private VerticalLayout createDialogLayout(Dialog dialog) {
		H2 headline = new H2("Change Password");
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0").set("font-size", "1.5em").set("font-weight",
				"bold");

		oldpwd = new PasswordField("Old Password");
		newpwd = new PasswordField("New Password");
		confirmpwd = new PasswordField("Confirm New Password");
		configurePasswordFields();
		oldpwd.setRevealButtonVisible(false);
		newpwd.setRevealButtonVisible(false);
		confirmpwd.setRevealButtonVisible(false);
		// oldpwd.setValue("");
		cancelButton.addClickListener(e -> dialog.close());
		saveButton.addClickListener(e -> changePassword());
		VerticalLayout fieldLayout = new VerticalLayout(oldpwd, newpwd, confirmpwd);
		fieldLayout.setSpacing(false);
		fieldLayout.setPadding(false);
		fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
		buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		VerticalLayout dialogLayout = new VerticalLayout(headline, fieldLayout, buttonLayout);
		dialogLayout.setPadding(false);
		dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");
		clearDialog();
		
		return dialogLayout;
	}

	public void clearDialog() {
		oldpwd.setValue("");
		confirmpwd.setValue("");
		newpwd.setValue("");
	}

	private void changePassword() {
		// notify.show("Under Development", 3000, Position.TOP_CENTER);
		if (oldpwd.isEmpty() || newpwd.isEmpty() || confirmpwd.isEmpty()) {
			Notification.show("Error: Enter All Values, Please", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		}  else if(!PasswordPolicy.isValid(newpwd.getValue())){
			Notification.show(PasswordPolicy.DESCRIPTION).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else {
			if (newpwd.getValue().trim().equals(confirmpwd.getValue().trim())) {
				String pwd = oldpwd.getValue();

				if (passwordEncoder.matches(pwd, service.getLoggedUser().getPassword())) {
					user = service.getLoggedUser();
					user.setPassword(passwordEncoder.encode(newpwd.getValue().trim()));
					user.setPwdChangedDate(LocalDate.now());
					service.saveUser(user);
					showConfirmationDialog();

				} else {

					Notification.show("Unauthorised User", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
				}
			} else {
				Notification.show("Please check and confirm your passwords", 3000, Position.TOP_CENTER)
						.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		}
	}
	private void showConfirmationDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Password Changed");
        dialog.setText("Password has been successfully changed. You will be now be logged out. Please Login again with your new Password");
        
        dialog.setConfirmText("OK");
        dialog.addConfirmListener(event -> {
            securityService.logout(); // Call the logout method
            //getUI().ifPresent(ui -> ui.navigate("login")); // Redirect to the login page
        });

        dialog.open(); // Open the dialog
    }
	private void createUser() {
		// TODO Auto-generated method stub

		if (isAdmin == true || isSuper == true) {
			userdialog = new Dialog();
			VerticalLayout dialogLayout1 = createUserDialog(userdialog);
			userdialog.add(dialogLayout1);
			userdialog.open();

		} else {
			Notification.show("Please Contact Your Administrator", 3000, Position.TOP_CENTER);
		}
	}

	private VerticalLayout createUserDialog(Dialog userdialog) {
		H2 headline = new H2("Create User");
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0").set("font-size", "1.5em").set("font-weight",
				"bold");
		state.setItems(service.getAllStates());
		state.addValueChangeListener(e -> populateDistricts());

		if (isSuper) {
			usertype.setEnabled(true);
			// state.setEnabled(true);
		} else if (isAdmin) {

			state.setValue(service.getLoggedUser().getDistrict().getState());
			district.setValue(service.getLoggedUser().getDistrict());
			// state.setValue(null);
			usertype.setValue("USER");
			usertype.setEnabled(false);
		}
		state.setEnabled(isSuper);
		district.setEnabled(isSuper);
		state.setItemLabelGenerator(State::getStateName);
		district.setItemLabelGenerator(District::getDistrictName);
		cancelButton.addClickListener(e -> userdialog.close());
		saveButton.addClickListener(e -> saveNewUser());
		newpwd = new PasswordField("Password");
		confirmpwd = new PasswordField("Confirm Password");
		configurePasswordFields();
		VerticalLayout fieldLayout1 = new VerticalLayout(state, district, userName, newpwd, confirmpwd, usertype);
		fieldLayout1.setSpacing(false);
		fieldLayout1.setPadding(false);
		fieldLayout1.setAlignItems(FlexComponent.Alignment.STRETCH);

		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttonLayout1 = new HorizontalLayout(cancelButton, saveButton);
		buttonLayout1.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		VerticalLayout dialogLayout1 = new VerticalLayout(headline, fieldLayout1, buttonLayout1);
		dialogLayout1.setPadding(false);
		dialogLayout1.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout1.getStyle().set("width", "300px").set("max-width", "100%");
		// clearDialog();
		return dialogLayout1;
	}
	private void configurePasswordFields() {
		newpwd.setMinLength(PasswordPolicy.MIN_LENGTH);
		newpwd.setMaxLength(PasswordPolicy.MAX_LENGTH);
		newpwd.setHelperText(PasswordPolicy.DESCRIPTION);
		confirmpwd.setMinLength(PasswordPolicy.MIN_LENGTH);
		confirmpwd.setMaxLength(PasswordPolicy.MAX_LENGTH);
	}
	private void saveNewUser() {
		// TODO Auto-generated method stub
		if (district.isEmpty()  || state.isEmpty()  || usertype.isEmpty() 
				|| userName.isEmpty() || newpwd.isEmpty() || confirmpwd.getValue().isEmpty()) {
			Notification.show(" Enter All Values, Please", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
		else if (userName.getValue().trim().length() < 7) {
			Notification.show("User name is too short", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
		} else if(!PasswordPolicy.isValid(newpwd.getValue())){
			Notification.show(PasswordPolicy.DESCRIPTION).addThemeVariants(NotificationVariant.LUMO_WARNING);
		}  else {
			if (!newpwd.getValue().equals(confirmpwd.getValue())) {
				Notification.show("Check Your Passwords, Please", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
			} else {
				try {
					if (service.findUser(userName.getValue()) == null) {
						Users users = new Users();
						UsersRoles role=new UsersRoles();
						users.setDistrict(district.getValue());
						users.setUserName(userName.getValue());
						users.setPassword(passwordEncoder.encode(newpwd.getValue().trim()));
						users.setEnteredBy(service.getloggeduser());
						users.setEnteredOn(LocalDate.now());
						users.setPwdChangedDate(LocalDate.now());
						users.setEnabled(true);
						service.saveUser(users);
						role.setUser(users);
						role.setRoleName(usertype.getValue().toString());
						service.saveRole(role);
						clearUserFields();
						Notification.show("User Created Successfully", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
						userdialog.close();
					} else {
						Notification.show("Username Already Taken. Enter Another Username", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
						userName.setValue("");
						userName.focus();
					}
					
				} catch (Exception e) {
					Notification.show(ErrorReferences.userMessage(e)).addThemeVariants(NotificationVariant.LUMO_ERROR);
				}
			}
		}

	}

	public void clearUserFields() {
		district.setValue(null);
		state.setValue(null);
		userName.setValue("");
		newpwd.setValue("");
		confirmpwd.setValue("");
	}
}
