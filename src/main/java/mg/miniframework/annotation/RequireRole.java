package mg.miniframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour spécifier les rôles requis pour accéder à une route.
 * L'utilisateur doit avoir AU MOINS UN des rôles spécifiés.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    /**
     * Liste des rôles autorisés.
     * L'utilisateur doit avoir au moins un de ces rôles.
     */
    String[] value();
    
    /**
     * Si true, l'utilisateur doit avoir TOUS les rôles.
     * Si false (défaut), l'utilisateur doit avoir AU MOINS UN rôle.
     */
    boolean requireAll() default false;
}
