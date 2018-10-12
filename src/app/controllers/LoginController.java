package controllers;

import constants.CookieConstants;
import models.User;
import models.UserSession;
import models.dtos.UserLoginDto;
import org.joda.time.DateTime;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;


import javax.inject.Inject;
import java.util.Optional;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;

    @Inject
    public LoginController(FormFactory formFactory) {
        loginForm = formFactory.form(UserLoginDto.class);
    }

    public Result login() {
        return ok(views.html.Login.render(loginForm));
    }

    public Result loginUnauthorized() {
        return unauthorized("ungültiges passwort du lümmel");
    }

    public Result loginCommit() {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("userName", "password");
        if (boundForm.hasErrors()) {
            return redirect(routes.LoginController.loginUnauthorized());
        }
        UserLoginDto loginData = boundForm.get();


        if (User.authenticate(loginData.getUserName(), loginData.getPassword())) {


            User user = User.findByName(loginData.getUserName()).get();

            UserSession userSession = new UserSession();

            userSession.setSessionId(UserSession.sessionsCount());
            userSession.setUserId(user.id);
            userSession.setIssuedAt(DateTime.now());

            UserSession.add(userSession);

            session().put(CookieConstants.USER_SESSION_ID_NAME, userSession.getSessionId().toString());

            return redirect(routes.HelloWorldController.index());
        }
        return redirect(routes.LoginController.loginUnauthorized());
    }

    public Result logoutCommit() {

        if (session().containsKey(CookieConstants.USER_SESSION_ID_NAME)) {

            String sessionIdString = session().getOrDefault(CookieConstants.USER_SESSION_ID_NAME, null);
            session().remove(CookieConstants.USER_SESSION_ID_NAME);

            Integer sessionId = Integer.parseInt(sessionIdString);
            Optional<UserSession> session = UserSession.findById(sessionId);
            session.ifPresent(UserSession::remove);

        }
        return redirect(routes.HelloWorldController.index());

    }

}