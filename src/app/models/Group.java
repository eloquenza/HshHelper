package models;

import io.ebean.Model;
import models.finders.GroupFinder;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "groups")
public class Group extends BaseDomain {

    @Column(unique = true)
    public String name;
    @ManyToOne
    @JoinColumn(name = "owner", referencedColumnName = "id")
    public User owner;
    public boolean isAdminGroup;
    public boolean isAllGroup;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.REMOVE
    }, mappedBy = "groups")
    public Set<User> members = new HashSet<>();

    public Group(Long id, String name, User owner, boolean isAdminGroup) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = isAdminGroup;
    }

    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id)&&
            Objects.equals(name, group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}