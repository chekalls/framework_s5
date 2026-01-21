package mg.miniframework.modules.security;

import java.util.HashSet;
import java.util.Set;

public class User {
    private String username;
    private String password;
    private boolean authenticated;
    private Set<Role> roles;
    private Object principal; 

    public User() {
        this.roles = new HashSet<>();
        this.authenticated = false;
    }
    public void setRoles(Role[] roles) {
        this.roles.clear();
    
        if (roles == null) {
            return;
        }
    
        for (Role role : roles) {
            if (role != null) {
                this.roles.add(role);
            }
        }
    }
    

    public User(String username) {
        this.username = username;
        this.roles = new HashSet<>();
        this.authenticated = false;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }


    public void addRole(String roleName) {
        this.roles.add(new Role(roleName));
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }

    public boolean hasAnyRole(String... roleNames) {
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllRoles(String... roleNames) {
        for (String roleName : roleNames) {
            if (!hasRole(roleName)) {
                return false;
            }
        }
        return true;
    }


    public boolean hasPermission(String permissionName) {
        return roles.stream()
                .anyMatch(r -> r.hasPermission(permissionName));
    }

    public boolean hasAnyPermission(String... permissionNames) {
        for (String permissionName : permissionNames) {
            if (hasPermission(permissionName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllPermissions(String... permissionNames) {
        for (String permissionName : permissionNames) {
            if (!hasPermission(permissionName)) {
                return false;
            }
        }
        return true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Object getPrincipal() {
        return principal;
    }

    public void setPrincipal(Object principal) {
        this.principal = principal;
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', authenticated=" + authenticated + 
               ", roles=" + roles.size() + "}";
    }
}