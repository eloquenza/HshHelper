package controllers;

import domainlogic.loginmanager.*;
import extension.HashHelper;
import extension.RecaptchaHelper;
import models.User;
import models.dtos.ChangePasswordAfterResetDto;
import models.dtos.UserLoginDto;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;
import policy.session.Authentication;
import policy.session.SessionManager;


import javax.inject.Inject;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;
    private Form<ChangePasswordAfterResetDto> changePasswordForm;
    private LoginManager loginManager;

    @Inject
    public LoginController(
            FormFactory formFactory, LoginManager loginManager) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.changePasswordForm = formFactory.form(ChangePasswordAfterResetDto.class);
        this.loginManager = loginManager;
    }

    @Authentication.NotAllowed
    public Result showLoginForm() {
        return ok(views.html.Login.render(loginForm, false));
    }

    @Authentication.NotAllowed
    public Result login() {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("username", "password", "recaptcha");
        if (boundForm.hasErrors()) {
            return redirect(routes.LoginController.login());
        }

        UserLoginDto loginData = boundForm.get();

        try {
            loginManager.login(
                    loginData.getUsername(),
                    loginData.getPassword(),
                    loginData.getRecaptcha()
            );
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the Captcha!");
            return badRequest(views.html.Login.render(boundForm, true));
        } catch (InvalidUsernameOrPasswordException e) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            return badRequest(views.html.Login.render(boundForm, false));
        } catch (PasswordChangeRequiredException e) {
            return redirect(routes.LoginController.changePasswordAfterReset());
        }

        return redirect(routes.HomeController.index());
    }

    @Authentication.NotAllowed
    public Result showChangePasswordAfterResetForm() {
        return ok(views.html.ChangePasswordAfterReset.render(this.changePasswordForm, false));
    }

    // TODO: Sollte man nach password reset eingelogged sein?!
    @Authentication.NotAllowed
    public Result changePasswordAfterReset() {
        Form<ChangePasswordAfterResetDto> boundForm = this.changePasswordForm.bindFromRequest("username", "currentPassword", "password", "passwordRepeat", "recaptcha");
        if (boundForm.hasErrors()) {
            return ok(views.html.ChangePasswordAfterReset.render(boundForm, false));
        }

        ChangePasswordAfterResetDto changePasswordData = boundForm.get();

        try {
            loginManager.changePassword(
                    changePasswordData.getUsername(),
                    changePasswordData.getCurrentPassword(),
                    changePasswordData.getPassword(),
                    changePasswordData.getRecaptcha()
            );
        } catch (InvalidUsernameOrPasswordException e) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            return badRequest(views.html.ChangePasswordAfterReset.render(boundForm, false));
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the Captcha!");
            return badRequest(views.html.ChangePasswordAfterReset.render(boundForm, true));
        }

        return redirect(routes.LoginController.login());
    }

    @Authentication.Required
    public Result logout() {
        loginManager.logout();
        return redirect(routes.LoginController.login());
    }
}
