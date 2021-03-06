package models;

import io.ebean.Model;
import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static extension.StringHelper.empty;

@Entity
@Table(name = "users")
public class User extends Model {

    @Id
    public Long userId;

    @Column(unique = true)
    private String username;

    @Constraints.Email
    private String email;

    private String passwordHash;

    private boolean passwordResetRequired;

    private Long quotaLimit;

    private String twoFactorAuthSecret;

    @OneToMany(
        mappedBy = "owner",
        cascade = CascadeType.ALL
    )
    private List<Group> ownerOf = new ArrayList<>();

    @OneToMany(
            mappedBy = "owner",
            cascade = CascadeType.ALL
    )
    private List<File> ownedFiles = new ArrayList<>();

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.REMOVE,
            CascadeType.MERGE
    })
    @JoinTable(name = "groupmembers",
            joinColumns = @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "fk_group_id", referencedColumnName = "group_id")
    )
    @OrderBy("name")
    private List<Group> groups = new ArrayList<>();


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserPermission> userPermissions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @OrderBy("dateTime")
    private List<LoginAttempt> loginAttempts = new ArrayList<>();


    private int sessionTimeoutInMinutes;

    private byte[] cryptoSalt;
    private byte[] initializationVectorCredentialKey;
    private byte[] credentialKeyCipherText;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<NetServiceCredential> netServiceCredentials = new ArrayList<>();


    public User(
            String username,
            String email,
            String passwordHash,
            boolean passwordResetRequired,
            Long quotaLimit) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordResetRequired = passwordResetRequired;
        this.quotaLimit = quotaLimit;
        this.sessionTimeoutInMinutes = ConstraintValues.MIN_SESSION_TIMEOUT_MINUTES;
    }

    public String getTwoFactorAuthSecret() {
        return twoFactorAuthSecret;
    }

    public void setTwoFactorAuthSecret(String twoFactorAuthSecret) {
        this.twoFactorAuthSecret = twoFactorAuthSecret;
    }

    public List<File> getOwnedFiles() {
        return ownedFiles;
    }

    public List<LoginAttempt> getLoginAttempts() {
        return loginAttempts;
    }

    public void setLoginAttempts(List<LoginAttempt> loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    public boolean isAdmin() {
        return this.groups.stream().anyMatch(Group::getIsAdminGroup);
    }

    public boolean has2FA() {
        return !empty(getTwoFactorAuthSecret());
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean getIsPasswordResetRequired() {
        return passwordResetRequired;
    }

    public void setIsPasswordResetRequired(boolean passwordResetRequired) {
        this.passwordResetRequired = passwordResetRequired;
    }

    public Long getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public List<Group> getOwnerOf() {
        return ownerOf;
    }

    public void setOwnerOf(List<Group> ownerOf) {
        this.ownerOf = ownerOf;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<UserPermission> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(List<UserPermission> userPermissions) {
        this.userPermissions = userPermissions;
    }


    public int getSessionTimeoutInMinutes() {
        return sessionTimeoutInMinutes;
    }

    public void setSessionTimeoutInMinutes(int sessionTimeoutInMinutes) {
        this.sessionTimeoutInMinutes = sessionTimeoutInMinutes;
    }

    public byte[] getCryptoSalt() {
        return cryptoSalt;
    }

    public void setCryptoSalt(byte[] cryptoSalt) {
        this.cryptoSalt = cryptoSalt;
    }

    public byte[] getCredentialKeyCipherText() {
        return credentialKeyCipherText;
    }

    public void setCredentialKeyCipherText(byte[] credentialKeyCipherText) {
        this.credentialKeyCipherText = credentialKeyCipherText;
    }

    public byte[] getInitializationVectorCredentialKey() {
        return initializationVectorCredentialKey;
    }

    public void setInitializationVectorCredentialKey(byte[] initializationVectorCredentialKey) {
        this.initializationVectorCredentialKey = initializationVectorCredentialKey;
    }

    public List<NetServiceCredential> getNetServiceCredentials() {
        return netServiceCredentials;
    }

    public void setNetServiceCredentials(List<NetServiceCredential> netServiceCredentials) {
        this.netServiceCredentials = netServiceCredentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // A situation could occur where users have not been added to the DB yet but still need
        // to be comparable. During a test, for example.
        if (user.userId == null) {
            return Objects.equals(username, user.username);
        }
        return Objects.equals(userId, user.userId) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return username + " (id: " + userId + ")";
    }
}
