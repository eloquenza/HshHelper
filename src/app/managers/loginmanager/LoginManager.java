package managers.loginmanager;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import extension.HashHelper;
import extension.RecaptchaHelper;
import io.ebean.EbeanServer;
import models.LoginAttempt;
import models.User;
import models.finders.LoginAttemptFinder;
import org.joda.time.DateTime;
import play.Logger;
import play.mvc.Http;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;
import ua_parser.Client;
import ua_parser.Parser;

import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static policyenforcement.ConstraintValues.SUCCESSFUL_LOGIN_STORAGE_DURATION_DAYS;

public class LoginManager {

    private final EbeanServer ebeanSever;
    private Authentification authentification;
    private Firewall loginFirewall;
    private SessionManager sessionManager;
    private HashHelper hashHelper;
    private LoginAttemptFinder loginAttemptFinder;
    private final RecaptchaHelper recaptchaHelper;
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;

    private static final Logger.ALogger logger = Logger.of(LoginManager.class);

    @Inject
    public LoginManager(
            Authentification authentification,
            Firewall loginFirewall,
            SessionManager sessionManager,
            HashHelper hashHelper,
            EbeanServer ebeanServer,
            LoginAttemptFinder loginAttemptFinder,
            RecaptchaHelper recaptchaHelper, KeyGenerator keyGenerator, Cipher cipher)
    {
        this.loginAttemptFinder = loginAttemptFinder;
        this.ebeanSever = ebeanServer;
        this.authentification = authentification;
        this.loginFirewall = loginFirewall;
        this.sessionManager = sessionManager;
        this.hashHelper = hashHelper;
        this.recaptchaHelper = recaptchaHelper;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
    }

    private User authenticate(String username, String password, String captchaToken, Http.Request request, Integer twoFactorPin) throws CaptchaRequiredException, InvalidLoginException, IOException, GeneralSecurityException {
        Authentification.Result auth = authentification.Perform(username, password, twoFactorPin);
        Long uid;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        } else {
            // Negativen Zahlenraum für "virtuelle" UIDs (Hashmapping) nutzen
            Long fakeUid = hashHelper.insecureStringHash(username);
            if(fakeUid > 0) {
                fakeUid = fakeUid * -1;
            }
            uid = fakeUid;
        }

        Instance fw = loginFirewall.get(request.remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            logger.error(request.remoteAddress() + " is blocked from logging in.");
            throw new InvalidLoginException();
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!recaptchaHelper.IsValidResponse(captchaToken, request.remoteAddress())) {
                logger.error(request.remoteAddress() + " has tried to login in without a valid reCAPTCHA.");
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            logger.error(request.remoteAddress() + " failed to login on user " + uid);
            throw new InvalidLoginException();
        }


        Optional<String> userAgentString = request.getHeaders().get("User-Agent");
        if(!userAgentString.isPresent()) {
            throw new InvalidLoginException();
        }

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUser(auth.user());
        attempt.setAddress(request.remoteAddress());
        attempt.setClientName(this.getUserAgentDisplayString(userAgentString.get()));
        attempt.setDateTime(DateTime.now());
        this.ebeanSever.save(attempt);

        return auth.user();
    }

    private String getUserAgentDisplayString(String userAgentString) throws IOException {
        Parser uaParser = new Parser();
        Client c = uaParser.parse(userAgentString);
        return String.format("%s: %s (%s)", c.device.family, c.userAgent.family, c.userAgent.major);
    }

    public void login(String username, String password, String captchaToken, Http.Request request, Integer twoFactorPin) throws CaptchaRequiredException, InvalidLoginException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = this.authenticate(username, password, captchaToken, request, twoFactorPin);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            logger.error(authenticatedUser + " needs to change his password.");
            throw new PasswordChangeRequiredException();
        }

        CryptoKey key = keyGenerator.generate(password, authenticatedUser.getCryptoSalt());
        byte[] credentialKeyPlaintext = cipher.decrypt(key, authenticatedUser.getInitializationVectorCredentialKey(), authenticatedUser.getCredentialKeyCipherText());

        sessionManager.startNewSession(authenticatedUser, credentialKeyPlaintext);
        logger.info(authenticatedUser + " has logged in.");
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken, Http.Request request, Integer twoFactorPin) throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken, request, twoFactorPin);

        authenticatedUser.setIsPasswordResetRequired(false);
        authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
        this.ebeanSever.save(authenticatedUser);
        logger.info(authenticatedUser + " changed his password.");
    }

    public void logout() {
        sessionManager.destroyCurrentSession();
    }

    public void deleteOldLoginRecords() {
        int deletedSessions = loginAttemptFinder.query().where()
                .lt("dateTime",DateTime.now().minusDays(SUCCESSFUL_LOGIN_STORAGE_DURATION_DAYS)).delete();

        Logger.info("Deleted "+deletedSessions+" Login-Logs");
    }
}
