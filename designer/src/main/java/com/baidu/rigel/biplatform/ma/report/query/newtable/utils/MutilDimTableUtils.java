package com.baidu.rigel.biplatform.ma.report.query.newtable.utils;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.baidu.rigel.biplatform.ac.util.MetaNameUtil;
import com.baidu.rigel.biplatform.ma.report.model.FormatModel;
import com.baidu.rigel.biplatform.ma.report.model.LinkInfo;
import com.baidu.rigel.biplatform.ma.report.query.newtable.bo.IndDataDefine;
import com.baidu.rigel.biplatform.ma.report.query.newtable.bo.MutilDimTable;
import com.baidu.rigel.biplatform.ma.report.query.newtable.bo.OperationColumnDefine;
import com.baidu.rigel.biplatform.ma.resource.utils.OlapLinkUtils;
import com.google.common.collect.Lists;

public class MutilDimTableUtils {
    /**
     * 包装新的MutilDimTable
     * 
     * @param formatModel
     * @param table
     */
    public static void decorateTable(FormatModel formatModel, MutilDimTable table) {
        if (formatModel == null) {
            return;
        }
        Map<String, String> dataFormat = formatModel.getDataFormat();
        Map<String, String> toolTips = formatModel.getToolTips();
        Map<String, String> textAlignFormat = formatModel.getTextAlignFormat();
        Map<String, LinkInfo> linkInfoMap = formatModel.getLinkInfo();

        List<IndDataDefine> indDataDefineList = table.getInds();
        for (IndDataDefine define : indDataDefineList) {

            String linkBridgeId = define.getOlapElementId();
            if (!StringUtils.isEmpty(linkBridgeId)) {
                LinkInfo linkInfo = linkInfoMap.get(linkBridgeId);
                // 这里严格判断，只有当设置了明细跳转表，并且参数映射也已经设置完成，才在多维报表处展示超链接
                if (linkInfo != null && !StringUtils.isEmpty(linkInfo.getPlaneTableId())
                        && !MapUtils.isEmpty(linkInfo.getParamMapping())) {
                    define.setLinkBridge(linkBridgeId);
                }

            }
            String uniqueName = define.getId();
            // 目前只针对列上放指标进行设置，如果维度要放到列上来，该方法有严重问题
            uniqueName = uniqueName.replaceAll("\\{", "").replaceAll("\\}", "");
            uniqueName = MetaNameUtil.parseUnique2NameArray(uniqueName)[1];
            if (dataFormat != null) {
                String formatStr = dataFormat.get("defaultFormat");
                if (!StringUtils.isEmpty(dataFormat.get(uniqueName))) {
                    formatStr = dataFormat.get(uniqueName);
                }
                if (!StringUtils.isEmpty(formatStr)) {
                    define.setFormat(formatStr);
                }
            }
            if (toolTips != null) {
                String toolTip = toolTips.get(uniqueName);
                if (StringUtils.isEmpty(toolTip)) {
                    toolTip = uniqueName;
                }
                define.setToolTip(toolTip);
            }
            if (textAlignFormat != null) {
                String align = textAlignFormat.get(uniqueName);
                if (StringUtils.isEmpty(align)) {
                    align = "left";
                }
                define.setAlign(align);
            }

        }
        /**
         * 为table增加操作列属性, add by majun
         */
        table.setOperationColumns(generateOperationColumnList(linkInfoMap));

    }

    /**
     * 返回新多维表需要的操作列对象集合
     * 
     * @param linkInfoMap linkInfoMap
     * @return 返回新多维表需要的操作列对象集合
     */
    private static List<OperationColumnDefine> generateOperationColumnList(Map<String, LinkInfo> linkInfoMap) {
        List<OperationColumnDefine> operationColumns = Lists.newArrayList();
        List<LinkInfo> savedOperationLinkInfoList = OlapLinkUtils.getOperationColumKeys(linkInfoMap);
        if (!CollectionUtils.isEmpty(savedOperationLinkInfoList)) {
            for (LinkInfo linkInfo : savedOperationLinkInfoList) {
                OperationColumnDefine operationColumn = new OperationColumnDefine();
                operationColumn.setLinkBridge(linkInfo.getColunmSourceId());
                operationColumn.setName(linkInfo.getColunmSourceCaption());
                operationColumns.add(operationColumn);
            }
        }
        return operationColumns;
    }

}
