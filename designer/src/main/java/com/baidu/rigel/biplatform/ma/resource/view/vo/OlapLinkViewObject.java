package com.baidu.rigel.biplatform.ma.resource.view.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * olap跳转信息view类
 * 
 * @author majun04
 *
 */
public class OlapLinkViewObject implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -558746725378781626L;
    /**
     * 所有能被跳转到的平面报表信息列表
     */
    private List<Map<String, String>> planeTableList = new ArrayList<Map<String, String>>();
    /**
     * 所有多维报表上能添加链接的列信息列表
     */
    private List<Map<String, String>> columnDefine = new ArrayList<Map<String, String>>();
    /**
     * 操作列信息
     */
    private List<Map<String, String>> operationColumn = new ArrayList<Map<String, String>>();

    /**
     * 为planeTableList增加实例对象
     * 
     * @param text 平面报表名称
     * @param value 平面报表id
     * 
     */
    public void addPlaneTable(String text, String value) {
        Map<String, String> planeTableVo = new HashMap<String, String>();
        planeTableVo.put("text", text);
        planeTableVo.put("value", value);
        planeTableList.add(planeTableVo);
    }

    /**
     * 为colunmDefine增加实体
     * 
     * @param text 多维报表列名称
     * @param value 多维报表列标识
     * @param selectedTable 已经被选中要跳转的平面报表id
     */
    public void addColunmDefine(String text, String value, String selectedTable) {
        Map<String, String> colunmDefineMap = new HashMap<String, String>();
        colunmDefineMap.put("text", text);
        colunmDefineMap.put("value", value);
        colunmDefineMap.put("selectedTable", selectedTable);
        columnDefine.add(colunmDefineMap);
    }

    /**
     * 为OperationColumn增加实体
     * @param text 操作列名称
     * @param value 操作列id标识
     * @param selectedTable 被选中要跳转的平面报表id
     */
    public void addOperationColumn(String text, String value, String selectedTable) {
        Map<String, String> operationColumnMap = new HashMap<String, String>();
        operationColumnMap.put("text", text);
        operationColumnMap.put("value", value);
        operationColumnMap.put("selectedTable", selectedTable);
        operationColumn.add(operationColumnMap);
    }

    /**
     * @return the planeTableList
     */
    public List<Map<String, String>> getPlaneTableList() {
        return planeTableList;
    }

    /**
     * @param planeTableList the planeTableList to set
     */
    public void setPlaneTableList(List<Map<String, String>> planeTableList) {
        this.planeTableList = planeTableList;
    }

    /**
     * @return the columnDefine
     */
    public List<Map<String, String>> getColumnDefine() {
        return columnDefine;
    }

    /**
     * @param columnDefine the columnDefine to set
     */
    public void setColumnDefine(List<Map<String, String>> columnDefine) {
        this.columnDefine = columnDefine;
    }

    /**
     * @return the operationColumn
     */
    public List<Map<String, String>> getOperationColumn() {
        return operationColumn;
    }

    /**
     * @param operationColumn the operationColumn to set
     */
    public void setOperationColumn(List<Map<String, String>> operationColumn) {
        this.operationColumn = operationColumn;
    }

}
