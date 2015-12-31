package com.baidu.rigel.biplatform.queryrouter.queryplugin.convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.baidu.rigel.biplatform.ac.util.DataModelUtils;
import com.baidu.rigel.biplatform.queryrouter.query.vo.QueryObject;
import com.baidu.rigel.biplatform.queryrouter.query.vo.QueryRequest;
import com.baidu.rigel.biplatform.queryrouter.query.vo.sql.Expression;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QueryMeta;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlColumn;


/**
 * PlantTableUtils
 * 
 * @author luowenlei
 *
 */
public class WhereDataUtils {
    
    /**
     * 
     * transQueryRequestAndList2Map:analyze andList of queryRequest ,trans
     * andList into Map<String,List<String>>
     * 
     * @param query
     *            queryRequest
     * @return Map<String,List<String>> the result map,whose key is property and
     *         value is leafvalues
     */
    public static Map<String, List<Object>> transQueryRequestAndWhereList2Map(
            QueryRequest query, String tableName, QueryMeta queryMeta) {
        Map<String, List<Object>> resultMap = new HashMap<String, List<Object>>();
        for (Expression expression : query.getWhere().getAndList()) {
            String fieldName = expression.getProperties();
            if (StringUtils.isEmpty(fieldName) || StringUtils.isEmpty(tableName)) {
                continue;
            }
            SqlColumn sqlColumn = queryMeta.getSqlColumn(tableName, fieldName);
            boolean isChar = true;
            if (sqlColumn != null) {
                isChar = DataModelUtils.isChar(sqlColumn.getDataType());
            }
            List<Object> valueList = new ArrayList<Object>();
            for (QueryObject qo : expression.getQueryValues()) {
                for (String value : qo.getLeafValues()) {
                    if (StringUtils.isEmpty(sqlColumn.getDataType())) {
                    // by default
                        valueList.add(value);
                    } else if (isChar) {
                        valueList.add(value);
                    } else {
                        valueList.add(new BigDecimal(value));
                    }
                }
            }
            resultMap.put(QueryMeta.getSqlColumnKey(tableName, fieldName), valueList);
        }
        return resultMap;
    }
}
