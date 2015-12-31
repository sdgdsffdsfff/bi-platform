/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.rigel.biplatform.queryrouter.queryplugin.meta.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.queryrouter.handle.QueryRouterContext;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.meta.TableMetaService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.service.JdbcHandler;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.ColumnType;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlColumn;

/**
 * 
 * Description: 查询数据库中的表的元数据信息
 * 
 * @author 罗文磊
 *
 */
@Service("tableMetaServiceImpl")
@Scope("prototype")
public class TableMetaServiceImpl implements TableMetaService {
    /**
     * logger
     */
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /**
     * meta sql tablename
     */
    public static final String TABLE_NAME = "[TABLENAME]";
    
    /**
     * meta sql columnname
     */
    public static final String COLUMN_NAME = "[COLUMNNAME]";
    
    /**
     * sql
     */
    private String sql;
    
    /**
     * tableName
     */
    private String tableName;
    
    /**
     * dataType
     */
    private String dataType;
    
    /**
     * columnName
     */
    private String columnName;

    /**
     * 查询所有字段在数据库中的数据类型并添入到sqlColumnMap中
     * 
     * @param sqlColumnMap
     *            所有的字段
     * @param SqlDataSourceInfo
     *            dataSourceInfo
     * @return List<String> 存在的表
     */
    public void generateColumnDataType(Collection<SqlColumn> sqlColumns,
            DataSourceInfo dataSourceInfo, JdbcHandler queryHandler) {
        setTableMetaServiceInfo();
        Set<String> tableNameSet = new HashSet<String>();
        Set<String> columnNameSet = new HashSet<String>();
        for (SqlColumn sqlColumn : sqlColumns) {
            String tableNameTmp = "";
            String columnNameTmp = "";
            if (sqlColumn.getType() == ColumnType.GROUP
                    || sqlColumn.getType() == ColumnType.CAL) {
                continue;
            }
            if (sqlColumn.getType() == ColumnType.TIME) {
                tableNameTmp = sqlColumn.getSourceTableName();
                columnNameTmp = sqlColumn.getFactTableFieldName();
            } else if (sqlColumn.getType() == ColumnType.CALLBACK) {
                tableNameTmp = sqlColumn.getSourceTableName();
                columnNameTmp = sqlColumn.getFactTableFieldName();
            } else {
                tableNameTmp = sqlColumn.getTableName();
                columnNameTmp = sqlColumn.getTableFieldName();
            }
            if (StringUtils.isNotEmpty(tableNameTmp) && tableNameTmp.indexOf(",") >= 0) {
                // 如果为多事实表，那么取一个表的数据类型
                tableNameTmp = tableNameTmp.substring(0, tableNameTmp.indexOf(","));
            }
            if (StringUtils.isNotEmpty(tableNameTmp)
                    && StringUtils.isNotEmpty(columnNameTmp)) {
                tableNameSet.add(tableNameTmp);
                columnNameSet.add(columnNameTmp);
            }
        }
        String tableNameStrs = convertSet2InString(tableNameSet);
        String columnNameStrs = convertSet2InString(columnNameSet);
        
        this.setQuerySql(tableNameSet, columnNameSet);
        
        List<Map<String, Object>> datas = queryHandler.queryForMeta(this.getSql(),
                new ArrayList<Object>());
        if (CollectionUtils.isEmpty(datas)) {
            logger.warn("queryId:{} can not found tablenames:" + tableNameStrs
                    + " and columnNames:" + columnNameStrs + " in database.",
                    QueryRouterContext.getQueryId());
            return;
        }
        for (SqlColumn sqlColumn : sqlColumns) {
            String tableNameTmp1 = "";
            String columnNameTmp1 = "";
            if (sqlColumn.getType() == ColumnType.GROUP
                    || sqlColumn.getType() == ColumnType.CAL) {
                continue;
            }
            if (sqlColumn.getType() == ColumnType.TIME) {
                tableNameTmp1 = sqlColumn.getSourceTableName();
                columnNameTmp1 = sqlColumn.getFactTableFieldName();
            } else if (sqlColumn.getType() == ColumnType.CALLBACK) {
                tableNameTmp1 = sqlColumn.getSourceTableName();
                columnNameTmp1 = sqlColumn.getFactTableFieldName();
            } else {
                tableNameTmp1 = sqlColumn.getTableName();
                columnNameTmp1 = sqlColumn.getTableFieldName();
            }
            if (StringUtils.isNotEmpty(tableNameTmp1) && tableNameTmp1.indexOf(",") >= 0) {
                // 为多事实表的情况
                tableNameTmp1 = tableNameTmp1.substring(0, tableNameTmp1.indexOf(","));
            }
            for (Map<String, Object> data : datas) {
                String columnNameTmp = data.get(this.getColumnName()).toString();
                String dataTypeTmp = data.get(this.getDataType()).toString();
                if (tableNameSet.size() == 1) {
                // 如果只有一个表名，不需要check 表名相等
                    if (columnNameTmp.equals(columnNameTmp1)) {
                        sqlColumn.setDataType(dataTypeTmp);
                        break;
                    }
                } else {
                    String tableNameTmp = data.get(this.getTableName()).toString();
                    if (tableNameTmp1.equals(tableNameTmp) && columnNameTmp1.equals(columnNameTmp)) {
                        sqlColumn.setDataType(dataTypeTmp);
                        break;
                    }
                }
            }
        }
        logger.info(
                "queryId:{} found table column datatype is:{} in tablenames:{} and columnNames:{}",
                QueryRouterContext.getQueryId(), dataType, tableNameStrs, columnNameStrs);
    }

    /**
     * setTableMetaServiceInfo
     *
     * @return
     */
    public void setTableMetaServiceInfo() {
        this.setSql("select COLUMN_NAME,TABLE_NAME,DATA_TYPE from information_schema.`columns` "
                + " where TABLE_NAME in ([TABLENAME])"
                + " and COLUMN_NAME in ([COLUMNNAME])");
        this.setTableName("TABLE_NAME");
        this.setColumnName("COLUMN_NAME");
        this.setDataType("DATA_TYPE");
    }
    
    /**
     * setQuerySql
     *
     * @param tableNameSet
     * @param columnNameSet
     */
    public void setQuerySql(Set<String> tableNameSet, Set<String> columnNameSet) {
        String tableNameStrs = convertSet2InString(tableNameSet);
        String columnNameStrs = convertSet2InString(columnNameSet);
        if (this.getSql().contains(TABLE_NAME)) {
            this.setSql(StringUtils.replace(this.getSql(), TABLE_NAME, tableNameStrs));
        }
        if (this.getSql().contains(COLUMN_NAME)) {
            this.setSql(StringUtils.replace(this.getSql(), COLUMN_NAME, columnNameStrs));
        }
    }
    
    private static String convertSet2InString(Set<String> set) {
        String result = "";
        for (String name : set) {
            result = result + "'" + name + "',";
        }
        if (StringUtils.isEmpty(result)) {
            return "";
        } else {
            return result.substring(0, result.lastIndexOf(","));
        }
    }

    /**
     * default generate get tableName
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * default generate set tableName
     * @param tableName the tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * default generate get dataType
     * @return the dataType
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * default generate set dataType
     * @param dataType the dataType to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * default generate get columnName
     * @return the columnName
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * default generate set columnName
     * @param columnName the columnName to set
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * default generate get sql
     * @return the sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * default generate set sql
     * @param sql the sql to set
     */
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    
}