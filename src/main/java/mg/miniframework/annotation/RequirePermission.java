package mg.miniframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour spécifier les permissions requises pour accéder à une route.
 * L'utilisateur doit avoir AU MOINS UNE des permissions spécifiées.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequirePermission {
    /**
     * Liste des permissions autorisées.
     * L'utilisateur doit avoir au moins une de ces permissions.
     */
    String[] value();
    
    /**
     * Si true, l'utilisateur doit avoir TOUTES les permissions.
     * Si false (défaut), l'utilisateur doit avoir AU MOINS UNE permission.
     */
    boolean requireAll() default false;
}
