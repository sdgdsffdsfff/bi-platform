package com.baidu.rigel.biplatform.ma.report.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 多维报表跳转平面报表的映射关系对象
 * 
 * @author majun04
 *
 */
public class LinkInfo implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4146274201672309214L;
    /**
     * 跳转列源id
     */
    private String colunmSourceId;
    /**
     * 跳转列源名称
     */
    private String colunmSourceCaption;
    /**
     * 跳转到的平面报表id
     */
    private String planeTableId;
    /**
     * key为平面报表参数名称，value为多维报表对应维度对象
     */
    private Map<String, String> paramMapping = new HashMap<String, String>();

    /**
     * @return the planeTableId
     */
    public String getPlaneTableId() {
        return planeTableId;
    }

    /**
     * @param planeTableId the planeTableId to set
     */
    public void setPlaneTableId(String planeTableId) {
        this.planeTableId = planeTableId;
    }

    /**
     * @return the paramMapping
     */
    public Map<String, String> getParamMapping() {
        return paramMapping;
    }

    /**
     * @param paramMapping the paramMapping to set
     */
    public void setParamMapping(Map<String, String> paramMapping) {
        this.paramMapping = paramMapping;
    }

    /**
     * @return the colunmSourceId
     */
    public String getColunmSourceId() {
        return colunmSourceId;
    }

    /**
     * @param colunmSourceId the colunmSourceId to set
     */
    public void setColunmSourceId(String colunmSourceId) {
        this.colunmSourceId = colunmSourceId;
    }

    /**
     * @return the colunmSourceCaption
     */
    public String getColunmSourceCaption() {
        return colunmSourceCaption;
    }

    /**
     * @param colunmSourceCaption the colunmSourceCaption to set
     */
    public void setColunmSourceCaption(String colunmSourceCaption) {
        this.colunmSourceCaption = colunmSourceCaption;
    }

    

}
