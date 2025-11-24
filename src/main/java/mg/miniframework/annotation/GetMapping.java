package mg.miniframework.annotation;

public @interface GetMapping {
    String path() default "/";
}
