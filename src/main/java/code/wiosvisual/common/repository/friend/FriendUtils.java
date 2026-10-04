/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 */
package code.wiosvisual.common.repository.friend;

import code.wiosvisual.common.repository.friend.Friend;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class FriendUtils {
    public static final List<Friend> friends = new ArrayList<Friend>();

    public static void addFriend(PlayerEntity player) {
        FriendUtils.addFriend(player.getName().getString());
    }

    public static void addFriend(String name) {
        friends.add(new Friend(name));
    }

    public static void removeFriend(PlayerEntity player) {
        FriendUtils.removeFriend(player.getName().getString());
    }

    public static void removeFriend(String name) {
        friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
    }

    public static boolean isFriend(Entity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            return FriendUtils.isFriend(player.getName().getString());
        }
        return false;
    }

    public static boolean isFriend(String friend) {
        return friends.stream().anyMatch(isFriend -> isFriend.getName().equals(friend));
    }

    public static void clear() {
        friends.clear();
    }

    private FriendUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static List<Friend> getFriends() {
        return friends;
    }
}
