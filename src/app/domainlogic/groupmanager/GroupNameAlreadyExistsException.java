package domainlogic.groupmanager;

public class GroupNameAlreadyExistsException extends Exception {
    public GroupNameAlreadyExistsException(String message) {
        super(message);
    }
}
