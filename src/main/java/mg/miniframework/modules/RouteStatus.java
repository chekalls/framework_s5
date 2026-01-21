package mg.miniframework.modules;

public enum RouteStatus {
    RETURN_STRING(200),
    RETURN_MODEL_VIEW(200),
    NOT_FOUND(404),
    RETURN_TYPE_UNKNOWN(500),
    RETURN_JSON(200);

    private final Integer httpCode;

    RouteStatus(Integer code){
        this.httpCode = code;
    }

    public Integer getCode(){
        return this.httpCode;
    }
}
