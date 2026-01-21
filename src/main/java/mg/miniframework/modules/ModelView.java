package mg.miniframework.modules;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String,Object> dataMap;

    public ModelView(){
        this.dataMap = new HashMap<>();
    }

    public void setData(String dataKey,Object objectValue){
        dataMap.put(dataKey, objectValue);
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
