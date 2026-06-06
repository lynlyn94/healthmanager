package com.rehab.infrastructure;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> GROUP_ID = new ThreadLocal<>();

    public static void set(Long userId, String username, String role, Long groupId) {
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLE.set(role);
        GROUP_ID.set(groupId);
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
        GROUP_ID.remove();
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static Long getGroupId() {
        return GROUP_ID.get();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(ROLE.get());
    }

    public static boolean isDoctor() {
        return "DOCTOR".equals(ROLE.get());
    }
}
