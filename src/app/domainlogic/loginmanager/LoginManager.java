package domainlogic.loginmanager;

import extension.HashHelper;
import extension.RecaptchaHelper;
import models.User;
import play.mvc.Http;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;
import policy.session.SessionManager;

import javax.inject.Inject;

public class LoginManager {

    private HashHelper hashHelper;
    private Firewall loginFirewall;
    private SessionManager sessionManager;

    @Inject
    public LoginManager(HashHelper hashHelper, Firewall loginFirewall, SessionManager sessionManager){
        this.hashHelper = hashHelper;
        this.loginFirewall = loginFirewall;
        this.sessionManager = sessionManager;
    }

    private User authenticate(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidLoginException {
        Authentification.Result auth = Authentification.Perform(
            username,
            password
        );

        Long uid = null;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        }

        Instance fw = loginFirewall.get(Http.Context.current().request().remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            throw new InvalidLoginException();
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!RecaptchaHelper.IsValidResponse(captchaToken, Http.Context.current().request().remoteAddress())) {
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            throw new InvalidLoginException();
        }

        return auth.user();
    }

    public void login(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidLoginException, PasswordChangeRequiredException {
        User authenticatedUser = this.authenticate(username, password, captchaToken);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            throw new PasswordChangeRequiredException();
        }

        sessionManager.startNewSession(authenticatedUser);
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken) throws InvalidLoginException, CaptchaRequiredException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken);

        authenticatedUser.setIsPasswordResetRequired(false);
        authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
        authenticatedUser.save();
    }

    public void logout() {
        sessionManager.destroyCurrentSession();
    }
}