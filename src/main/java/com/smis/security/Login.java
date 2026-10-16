package com.smis.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;

import com.smis.audit.Audit;
import com.smis.security.captcha.Captcha;
import com.smis.security.captcha.CapthaImpl;
import com.smis.view.HomeView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
@Route("login")
@AnonymousAllowed
@com.vaadin.flow.component.dependency.JsModule("./login-password-crypto.js")
@com.vaadin.flow.component.dependency.NpmPackage(value = "node-forge", version = "1.4.0")
public class Login extends VerticalLayout implements BeforeEnterObserver {

	@Autowired
	Audit audit;
	@Autowired
	SessionRegistry sr;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	SecurityContextRepository securityRepo;
	private final AuthenticatedUser authenticatedUser;
	HorizontalLayout captchacontainer = new HorizontalLayout();
	Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
	Captcha captcha;
	Image image;
	@Tag("vaadin-text-field")
	@com.vaadin.flow.component.dependency.JsModule("@vaadin/text-field/src/vaadin-text-field.js")
	static class BrowserTextField extends Component {}
	BrowserTextField captchatext = new BrowserTextField();
	BrowserTextField usernameField = new BrowserTextField();

	// No HasValue/AbstractField: plaintext must never be synchronized by Flow.
	@Tag("vaadin-password-field")
	@com.vaadin.flow.component.dependency.JsModule("@vaadin/password-field/src/vaadin-password-field.js")
	static class BrowserPasswordField extends Component {}
	BrowserPasswordField passwordField = new BrowserPasswordField();
	private final LoginPasswordCipher passwordCipher = new LoginPasswordCipher();
	Button button = new Button("Login");
	Span loginError = new Span();
	H3 title = new H3("Scheme MIS 2.0");
	H2 description = new H2("Government of Meghalaya");
	Anchor anchor = new Anchor();
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();


	@Autowired
	public Login(AuthenticatedUser authenticatedUser) {
		this(authenticatedUser, new CapthaImpl());
	}

	Login(AuthenticatedUser authenticatedUser, Captcha captcha) {
		this.authenticatedUser = authenticatedUser;
		this.captcha = captcha;

		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);
		setSizeFull();
		add(createPasswordForm());
		getStyle().set("background-color", "hsla(0, 0%, 95%, 0.69)");
	}

	private Component createPasswordForm() {
		usernameField.getElement().setProperty("label", "User Name");
		usernameField.getElement().setProperty("required", true);
		usernameField.getElement().setProperty("allowedCharPattern", "[0-9A-Za-z@]");
		usernameField.getElement().setProperty("minlength", 5);
		usernameField.getElement().setProperty("maxlength", 40);
		passwordField.getElement().setProperty("label", "Password");
		passwordField.getElement().setProperty("required", true);
		passwordField.getElement().setProperty("minlength", 5);
		passwordField.getElement().setProperty("maxlength", 40);
		captchatext.getElement().setProperty("placeholder", "CAPTCHA");
		captchatext.getElement().setProperty("required", true);
		captchatext.getElement().setAttribute("aria-label", "Enter the six-character CAPTCHA");
		captchatext.getElement().setProperty("maxlength", 6);
		captchatext.getElement().setProperty("minlength", 6);
		captchatext.getElement().setAttribute("theme", "align-center");
		captchatext.getElement().getStyle().set("max-width", "100px").set("height", "100%");
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		button.setAutofocus(true);
		anchor.setText("Forgot Password?");
		// anchor.getElement().addEventListener("click",e-> ForgotPassword());
		usernameField.getElement().setAttribute("autocomplete", "off");
		passwordField.getElement().setAttribute("autocomplete", "off");

		addAttachListener(event -> installEncryptedLogin());

		anchor.getStyle().set("color", "hsla(211, 100%, 50%, 0.90)").set("padding-bottom", "20px");
		var form = new FormLayout();
		// form.add(title, 1);
		// form.add(description, 1);
		form.add(usernameField, 1);
		form.add(passwordField, 1);

		form.add(getCaptcha(), 1);
		form.add(new Span(), 1);
		// form.add(captchatext, 1);
		// form.add(, 1);
		form.add(button, 1);
		loginError.getElement().setAttribute("role", "alert");
		loginError.getStyle().set("color", "var(--lumo-error-text-color)");
		form.add(loginError, 1);
		// form.add(anchor, 1);
		form.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("300px", 1));
		form.setWidth("320px");
		form.getStyle().set("padding", "20px");

		var header = new VerticalLayout();
		title.getStyle().set("color", "white");
		description.getStyle().set("color", "white");
		header.add(title, description);
		header.getStyle().set("background-color", "hsla(211, 100%, 50%, 0.90)");
		header.setAlignItems(Alignment.START);
		header.setJustifyContentMode(JustifyContentMode.END);
		header.setHeight("150px");
		header.getStyle().set("border-radius", "10px 10px 0 0");
		var container = new VerticalLayout();
		container.setSizeUndefined();
		container.getStyle().set("background-color", "white");
		// container.getStyle().set("border", "2px solid black");
		container.getStyle().set("border-radius", "10px");
		container.getStyle().set("padding", "0px");
		container.setAlignItems(Alignment.CENTER);
		container.setJustifyContentMode(JustifyContentMode.CENTER);
		container.add(header, form, anchor);
		/*
		 * var wrapper = new VerticalLayout(); wrapper.setSizeFull();
		 * wrapper.setAlignItems(Alignment.CENTER);
		 * wrapper.setJustifyContentMode(JustifyContentMode.CENTER);
		 * wrapper.add(title,description, container);
		 */
		return container;
	}

	@ClientCallable
	public void submitEncryptedLogin(String encryptedUsername, String encryptedPassword, String encryptedCaptcha) {
		String username = "";
		try {
			var credentials = passwordCipher.decrypt(encryptedUsername, encryptedPassword, encryptedCaptcha);
			username = credentials.username();
			authenticateSession(username, credentials.password(), credentials.captcha(), VaadinServletRequest.getCurrent().getHttpServletRequest(),
					VaadinServletResponse.getCurrent().getHttpServletResponse());
			
			audit.saveLoginAudit("Login Successfully", username);
			UI.getCurrent().navigate(HomeView.class);
		} catch (InvalidCaptchaException e) {
			loginError.setText("Invalid CAPTCHA. Please try again.");
			Notification.show("Invalid CAPTCHA. Please try again.")
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			clearFields();
		} catch (Exception e) {
			loginError.setText("Authentication failed. Check your user name and password and try again.");
			// Handle login failure
			audit.saveLoginAudit("Login Failure- Authentication", username);
			Notification.show("Authentication failed: Wrong User Name and Password")
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			clearFields();

		}
	}

	private void installEncryptedLogin() {
		getElement().setProperty("loginPublicKey", passwordCipher.publicKey());
		getElement().setProperty("loginChallenge", passwordCipher.challenge());
		getElement().executeJs("window.smisInstallEncryptedLogin(this,$0,$1,$2,$3,$4)",
				passwordField.getElement(), button.getElement(), usernameField.getElement(), loginError.getElement(), captchatext.getElement());
	}

	void authenticateSession(String username, String password, String captchaAnswer, HttpServletRequest request,
			HttpServletResponse response) {
		boolean validCaptcha = captcha.checkUserAnswer(captchaAnswer);
		// Consume every challenge before checking credentials, including failed attempts.
		regenerateCaptcha();
		if (!validCaptcha) {
			throw new InvalidCaptchaException();
		}
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(username, password));
		// Rotate the servlet ID while preserving Vaadin's session/UI attributes.
		request.getSession(true);
		String previousId = request.getSession().getId();
		request.changeSessionId();
		// Only a verified identity may replace that account's previous sessions.
		invalidatePreviousSessionsForUser(authentication.getName());
		sr.removeSessionInformation(previousId);
		sr.registerNewSession(request.getSession().getId(), authentication.getPrincipal());
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		securityRepo.saveContext(context, request, response);
		securityContextHolderStrategy.setContext(context);
	}

	static class InvalidCaptchaException extends org.springframework.security.authentication.BadCredentialsException {
		InvalidCaptchaException() { super("Invalid CAPTCHA"); }
	}

	public int getActiveSessionCountForUser(String username) {
		int count = 0;
		List<Object> principals = sr.getAllPrincipals();
		for (Object principal : principals) {
			if (principal instanceof UserDetails) {
				UserDetails userDetails = (UserDetails) principal;
				if (userDetails.getUsername().equals(username)) {
					List<SessionInformation> sessionInfoList = sr.getAllSessions(userDetails, true);
					count += sessionInfoList.size();
				}
			}
		}
		return count;
	}

	public void ForgotPassword() {
		Dialog aboutdialog = new Dialog();
		Button cancelButton = new Button("Cancel");
		H2 headline = new H2("Forgot Password?");
		// H3 header=new H3("Meghalaya Biodiversity Board");
		// H3 header2=new H3("People's Biodiversity Register (PBR): Version 2.0");
		H5 body = new H5("Please Enter Your Email Id");
		EmailField email = new EmailField();
		email.setPlaceholder("Email");
		email.setMaxLength(20);
		email.setMinLength(5);
		Button submitbutton = new Button("Submit");
		submitbutton.addClickListener(e -> Notification.show("To Be Implemented Using Email API. Public IP Required"));
		headline.getStyle().set("margin", "var(--lumo-space-m) 0 0 0").set("font-size", "1.5em")
				.set("font-weight", "bold").set("text-decoration", "underline");
		cancelButton.addClickListener(e -> aboutdialog.close());
		HorizontalLayout buttonLayout1 = new HorizontalLayout(submitbutton, cancelButton);
		buttonLayout1.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		VerticalLayout dialogLayout1 = new VerticalLayout(headline, body, email, buttonLayout1);
		dialogLayout1.setPadding(false);
		dialogLayout1.setAlignItems(FlexComponent.Alignment.STRETCH);
		dialogLayout1.getStyle().set("width", "300px").set("max-width", "100%");
		aboutdialog.add(dialogLayout1);
		aboutdialog.open();
	}

	public void invalidatePreviousSessionsForUser(String username) {
		// Get active session count for the user
		int activeSessionCount = getActiveSessionCountForUser(username);

		// If there are active sessions for the user, invalidate them
		if (activeSessionCount > 0) {
			List<Object> principals = sr.getAllPrincipals();
			for (Object principal : principals) {
				if (principal instanceof UserDetails) {
					UserDetails userDetails = (UserDetails) principal;
					if (userDetails.getUsername().equals(username)) {
						List<SessionInformation> sessionInfoList = sr.getAllSessions(userDetails, true); // Invalidate
																											// previous
																											// sessions
						for (SessionInformation sessionInfo : sessionInfoList) {
							sessionInfo.expireNow(); // Expire session information

						}
					}
				}
			}
		}
	}

	private void clearFields() {
		regenerateCaptcha();
		button.setEnabled(true);
		passwordField.getElement().setProperty("value", "");
		passwordCipher.renewChallenge();
		getElement().setProperty("loginChallenge", passwordCipher.challenge());
		usernameField.getElement().setProperty("value", "");
		captchatext.getElement().setProperty("value", "");
	}

	public Component getCaptcha() {
		if (image != null) {
			regenerateCaptcha();
			return captchacontainer;
		}
		image = captcha.getCaptchaImg();
		captchacontainer.add(image, refreshButton, captchatext);
		refreshButton.addClickListener(e -> regenerateCaptcha());
		refreshButton.setTooltipText("Generate Another Captcha");
		captchacontainer.setWidthFull();
		captchacontainer.setJustifyContentMode(JustifyContentMode.CENTER);
		captchacontainer.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-s)");
		return captchacontainer;
	}

	private void regenerateCaptcha() {
		// Only refresh after the image has been initialized.
		if (image == null) {
			return;
		}
		captchacontainer.remove(image);
		captchacontainer.remove(refreshButton);
		captchacontainer.remove(captchatext);
		image = captcha.getCaptchaImg();
		captchacontainer.add(image, refreshButton, captchatext);
		captchatext.getElement().setProperty("value", "");
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		// setCustomCookie();
		if (authenticatedUser.get().isPresent()) {
			// Already logged in
			// loginOverlay.setOpened(false);
			event.forwardTo("");
		}

		// loginOverlay.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
	}
}
