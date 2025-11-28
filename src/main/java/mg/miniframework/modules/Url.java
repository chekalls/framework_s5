package mg.miniframework.modules;

public class Url {
    private String urlPath;
    private Method method;

    public Url(){}

    public Url(String path){
        this.urlPath = path;
    }

    public Url(String path,Method method){
        urlPath = path;
        this.method = method;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public static enum Method {
        GET,
        POST,
        PUT,
        DELETE
    }

    public Method getMethod() {
        return method;
    }
}
