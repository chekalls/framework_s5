package mg.miniframework.modules.security;

import jakarta.servlet.http.HttpServletRequest;


public class SecurityContext {
    private static final String USER_ATTRIBUTE = "mg.miniframework.security.User";
    
    private final HttpServletRequest request;

    public SecurityContext(HttpServletRequest request) {
        this.request = request;
    }


    public void setUser(User user) {
        request.setAttribute(USER_ATTRIBUTE, user);
    }

    public User getUser() {
        return (User) request.getAttribute(USER_ATTRIBUTE);
    }

    public boolean isAuthenticated() {
        User user = getUser();
        return user != null && user.isAuthenticated();
    }


    public boolean hasRole(String roleName) {
        User user = getUser();
        return user != null && user.hasRole(roleName);
    }

    public boolean hasAnyRole(String... roleNames) {
        User user = getUser();
        return user != null && user.hasAnyRole(roleNames);
    }

    public boolean hasAllRoles(String... roleNames) {
        User user = getUser();
        return user != null && user.hasAllRoles(roleNames);
    }

    public boolean hasPermission(String permissionName) {
        User user = getUser();
        return user != null && user.hasPermission(permissionName);
    }

    public boolean hasAnyPermission(String... permissionNames) {
        User user = getUser();
        return user != null && user.hasAnyPermission(permissionNames);
    }

    public boolean hasAllPermissions(String... permissionNames) {
        User user = getUser();
        return user != null && user.hasAllPermissions(permissionNames);
    }

    public Object getPrincipal() {
        User user = getUser();
        return user != null ? user.getPrincipal() : null;
    }
}