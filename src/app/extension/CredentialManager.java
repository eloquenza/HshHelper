package extension;

import extension.Crypto.*;
import io.ebean.EbeanServer;
import models.User;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;

public class CredentialManager {
    private final SessionManager sessionManager;
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;
    private final RandomDataGenerator randomDataGenerator;
    private final EbeanServer ebeanServer;

    @Inject
    public CredentialManager(SessionManager sessionManager, KeyGenerator keyGenerator, Cipher cipher, RandomDataGenerator randomDataGenerator, EbeanServer ebeanServer) {
        this.sessionManager = sessionManager;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
        this.randomDataGenerator = randomDataGenerator;
        this.ebeanServer = ebeanServer;
    }

    public byte[] getCredentialPlaintext(String password) {
        User currentUser = sessionManager.currentUser();
        return getCredentialPlaintext(currentUser, password);
    }

    public byte[] getCredentialPlaintext(User user, String password) {
        CryptoKey key = keyGenerator.generate(password, user.getCryptoSalt());
        return cipher.decrypt(key, user.getInitializationVectorCredentialKey(), user.getCredentialKeyCipherText());
    }

    public void updateCredentialPassword(String oldPassword, String newPassword) {
        byte[] currentCredentialPlaintext = getCredentialPlaintext(oldPassword);

        byte[] salt = keyGenerator.generateSalt();
        CryptoKey key = keyGenerator.generate(newPassword, salt);
        CryptoResult result = cipher.encrypt(key, currentCredentialPlaintext);

        User currentUser = sessionManager.currentUser();
        currentUser.setCryptoSalt(salt);
        currentUser.setInitializationVectorCredentialKey(result.getInitializationVector());
        currentUser.setCredentialKeyCipherText(result.getCiphertext());
        ebeanServer.save(currentUser);
    }

    public void resetCredential(User user, String newPassword) {
        byte[] salt = keyGenerator.generateSalt();
        CryptoKey key = keyGenerator.generate(newPassword, salt);

        byte[] credentialKey = randomDataGenerator.generateBytes(CryptoConstants.GENERATED_KEY_BYTE);
        CryptoResult cryptoResult = cipher.encrypt(key, credentialKey);

        user.setCryptoSalt(salt);
        user.setInitializationVectorCredentialKey(
            cryptoResult.getInitializationVector()
        );
        user.setCredentialKeyCipherText(
            cryptoResult.getCiphertext()
        );

        ebeanServer.save(user);
        ebeanServer.deleteAll(user.getNetServiceCredentials());
    }
}