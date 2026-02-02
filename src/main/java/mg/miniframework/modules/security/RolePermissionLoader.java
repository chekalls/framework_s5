package mg.miniframework.modules.security;

import java.util.*;

import mg.miniframework.modules.LogManager;
import mg.miniframework.modules.LogManager.LogStatus;

public class RolePermissionLoader {

    private LogManager logManager;
    private Map<String, Role> rolesCache;
    private Map<String, Permission> permissionsCache;

    public RolePermissionLoader() {
        this.logManager = new LogManager();
        this.rolesCache = new HashMap<>();
        this.permissionsCache = new HashMap<>();
    }

    public void loadFromConfig(Map<String, String> config) {
        try {
            loadPermissions(config);
            loadRoles(config);

            logManager.insertLog("Security configuration loaded: " +
                    permissionsCache.size() + " permissions, " +
                    rolesCache.size() + " roles", LogStatus.INFO);

        } catch (Exception e) {
            try {
                logManager.insertLog("Error loading security config: " + e.getMessage(),
                        LogStatus.ERROR);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void loadPermissions(Map<String, String> config) throws Exception {
        String permissionsStr = config.get("security.permissions");

        if (permissionsStr == null || permissionsStr.trim().isEmpty()) {
            logManager.insertLog("No permissions defined in config, using defaults",
                    LogStatus.WARN);
            permissionsCache.put("READ", new Permission("READ", "Lecture"));
            permissionsCache.put("WRITE", new Permission("WRITE", "Écriture"));
            permissionsCache.put("DELETE", new Permission("DELETE", "Suppression"));
            return;
        }

        String[] permissionNames = permissionsStr.split(",");
        for (String permName : permissionNames) {
            permName = permName.trim();
            if (!permName.isEmpty()) {
                Permission permission = new Permission(permName);
                permissionsCache.put(permName, permission);
                logManager.insertLog("Permission loaded: " + permName, LogStatus.DEBUG);
            }
        }
    }

    private void loadRoles(Map<String, String> config) throws Exception {
        String rolePrefix = "security.role.";

        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith(rolePrefix)) {
                String roleName = key.substring(rolePrefix.length()).trim();
                String permissionsStr = entry.getValue();

                if (roleName.isEmpty()) {
                    continue;
                }

                Role role = new Role(roleName);

                if (permissionsStr != null && !permissionsStr.trim().isEmpty()) {
                    String[] permissionNames = permissionsStr.split(",");

                    for (String permName : permissionNames) {
                        permName = permName.trim();

                        if (!permName.isEmpty()) {
                            Permission permission = permissionsCache.get(permName);

                            if (permission != null) {
                                role.addPermission(permission);
                            } else {
                                logManager.insertLog("Warning: Permission '" + permName +
                                        "' not found for role '" + roleName + "'", LogStatus.WARN);
                            }
                        }
                    }
                }

                rolesCache.put(roleName, role);
                logManager.insertLog("Role loaded: " + roleName + " with " +
                        role.getPermissions().size() + " permissions", LogStatus.DEBUG);
            }
        }

        if (rolesCache.isEmpty()) {
            createDefaultRoles();
        }
    }

    private void createDefaultRoles() throws Exception {
        logManager.insertLog("No roles defined in config, creating defaults", LogStatus.WARN);

        Role admin = new Role("ADMIN", "Administrateur");
        for (Permission perm : permissionsCache.values()) {
            admin.addPermission(perm);
        }
        rolesCache.put("ADMIN", admin);

        Role user = new Role("USER", "Utilisateur");
        Permission readPerm = permissionsCache.get("READ");
        if (readPerm != null) {
            user.addPermission(readPerm);
        }
        rolesCache.put("USER", user);
    }

    public Role getRole(String roleName) {
        return rolesCache.get(roleName);
    }

    public Permission getPermission(String permissionName) {
        return permissionsCache.get(permissionName);
    }


    public User createUserWithRoles(String username, String... roleNames) {
        User user = new User(username);

        for (String roleName : roleNames) {
            Role role = rolesCache.get(roleName);
            if (role != null) {
                Role roleCopy = new Role(role.getName(), role.getDescription());
                for (Permission perm : role.getPermissions()) {
                    roleCopy.addPermission(perm);
                }
                user.addRole(roleCopy);
            }
        }

        return user;
    }

    public boolean hasRole(String roleName) {
        return rolesCache.containsKey(roleName);
    }

    public boolean hasPermission(String permissionName) {
        return permissionsCache.containsKey(permissionName);
    }


    public Map<String, Role> getAllRoles() {
        return new HashMap<>(rolesCache);
    }

    public Map<String, Permission> getAllPermissions() {
        return new HashMap<>(permissionsCache);
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }
}
