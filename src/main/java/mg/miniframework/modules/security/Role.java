package mg.miniframework.modules.security;

import java.util.HashSet;
import java.util.Set;

public class Role {
    private String name;
    private String description;
    private Set<Permission> permissions;

    public Role() {
        this.permissions = new HashSet<>();
    }

    public Role(String name) {
        this.name = name;
        this.permissions = new HashSet<>();
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }


    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }


    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(p -> p.getName().equals(permissionName));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name != null && name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Role{name='" + name + "', permissions=" + permissions.size() + "}";
    }
}
