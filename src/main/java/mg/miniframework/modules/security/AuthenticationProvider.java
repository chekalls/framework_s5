package mg.miniframework.modules.security;

public interface AuthenticationProvider {
    
    User authenticate(String username, String password);
    
    default User loadUserByUsername(String username) {
        return null;
    }
}
