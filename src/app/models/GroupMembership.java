package models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupMembership {

    public int groupId;
    public int userId;

    public GroupMembership() {

    }

    public GroupMembership(int groupId, int userId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    // note: only testcode for the first day, switch later to in memory
    // database h2
    private static List<GroupMembership> groupMemberships;

    static {
        groupMemberships = new ArrayList<GroupMembership>();
        groupMemberships.add(new GroupMembership(0, 0));
        groupMemberships.add(new GroupMembership(1, 0));
        groupMemberships.add(new GroupMembership(1, 1));
        groupMemberships.add(new GroupMembership(1, 2));
        groupMemberships.add(new GroupMembership(1, 3));
        groupMemberships.add(new GroupMembership(1, 4));
        groupMemberships.add(new GroupMembership(1, 5));
        groupMemberships.add(new GroupMembership(2, 0));
        groupMemberships.add(new GroupMembership(2, 1));
        groupMemberships.add(new GroupMembership(2, 2));
        groupMemberships.add(new GroupMembership(2, 3));
    }

    public static List<GroupMembership> findAll() {
        return new ArrayList<GroupMembership>(groupMemberships);
    }

    public static void add(GroupMembership gm) {
        groupMemberships.add(gm);
    }

    public static List<User> getGroupUsers(Group g) {
        Set<Integer> groupMembers = groupMemberships.stream().filter(x -> x.groupId == g.id).map(x->x.userId).collect(Collectors.toSet());
        List<User> users = User.findAll().stream().filter(x -> groupMembers.contains(x.id)).collect(Collectors.toList());
        return users;
    }

    public static void remove(Group g, User toBeDeleted) {
        groupMemberships.removeIf(x -> x.groupId == g.id && x.userId == toBeDeleted.id);
    }
}