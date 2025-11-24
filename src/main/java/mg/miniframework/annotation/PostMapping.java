package mg.miniframework.annotation;

public @interface PostMapping{
    String path() default "/";
}