package models.finders;

import io.ebean.Finder;
import models.User;

import java.util.Optional;

public class UserFinder extends Finder<Long, User> {

    public UserFinder() {
        super(User.class);
    }
    public UserFinder(String serverName) {
        super(User.class, serverName);
    }

    public Optional<User> byIdOptional(Long id) {
        User u = this.byId(id);
        if (u == null) {
            return Optional.empty();
        }
        return Optional.of(u);
    }

    public Optional<User> byName(String username) {
        return this.query().where().eq("username", username).findOneOrEmpty();
    }

    public Optional<User> byEmail(String email, UserFinderQueryOptions queryOptions) {
        switch(queryOptions) {
            case CaseInsensitive:
                return this.query().where().ieq("email", email).findOneOrEmpty();
            default:
                return this.query().where().eq("email", email).findOneOrEmpty();
        }
    }
}