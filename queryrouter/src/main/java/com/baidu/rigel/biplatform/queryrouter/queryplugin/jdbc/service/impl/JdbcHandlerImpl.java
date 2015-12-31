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
package com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.service.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.queryrouter.handle.QueryRouterContext;
import com.baidu.rigel.biplatform.queryrouter.query.vo.Meta;
import com.baidu.rigel.biplatform.queryrouter.query.vo.SearchIndexResultRecord;
import com.baidu.rigel.biplatform.queryrouter.query.vo.SearchIndexResultSet;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.connection.DataSourceException;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.connection.DataSourcePoolService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.connection.SqlDataSourceWrap;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.service.JdbcHandler;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlColumn;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlQuery;
import com.google.common.collect.Lists;

/**
 * 
 * 处理Jdbc sql query请求
 * 
 * @author luowenlei
 *
 */
@Service("jdbcHandlerImpl")
@Scope("prototype")
public class JdbcHandlerImpl implements JdbcHandler {
    
    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /**
     * jdbcTemplate
     */
    private JdbcTemplate jdbcTemplate = null;
    
    /**
     * dataSourcePoolService
     */
    @Resource(name = "localDataSourcePoolServiceImpl")
    private DataSourcePoolService dataSourcePoolService;
    
    /**
     * dataSourceInfo
     */
    private DataSourceInfo dataSourceInfo;
    
    /**
     * 
     * initJdbcTemplate
     * 
     * @param dataSource
     *            dataSource
     */
    @Override
    public synchronized void initJdbcTemplate(DataSourceInfo dataSourceInfo) {
        long begin = System.currentTimeMillis();
        try {
            DataSource dataSource = (SqlDataSourceWrap) this.dataSourcePoolService
                    .getDataSourceByKey(dataSourceInfo);
            
            if (this.getJdbcTemplate() == null
                    || !this.getJdbcTemplate().getDataSource().equals(dataSource)) {
                this.setJdbcTemplate(new JdbcTemplate(dataSource));
            }
            this.dataSourceInfo = dataSourceInfo;
        } catch (DataSourceException e) {
            e.printStackTrace();
            logger.error("getDataSource error:" + e.getCause().getMessage());
        }
        logger.info("initJdbcTemplate cost:" + (System.currentTimeMillis() - begin) + "ms");
    }
    
    /**
     * 通过sql查询数据库中的数据
     * 
     * @param sqlQuery
     *            sql
     * @param dataSourceInfo
     *            dataSourceInfo
     * @return List<Map<String, Object>> formd tableresult data
     */
    public List<Map<String, Object>> queryForList(String sql, List<Object> whereValues) {
        long begin = System.currentTimeMillis();
        List<Map<String, Object>> result = null;
        try {
            logger.info("queryId:{} sql: {}", QueryRouterContext.getQueryId(),
                    this.toPrintString(sql, whereValues));
            result = this.jdbcTemplate.queryForList(sql, whereValues.toArray());
        } catch (Exception e) {
            logger.error("queryId:{} select sql error:{}", QueryRouterContext.getQueryId(), e
                    .getCause().getMessage());
            throw e;
        } finally {
            logger.info("queryId:{} select sql cost:{} ms resultsize:{}",
                    QueryRouterContext.getQueryId(), System.currentTimeMillis() - begin,
                    result == null ? null : result.size());
        }
        return result;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.baidu.rigel.biplatform.queryrouter.queryplugin.service.JdbcHandler
     * #queryForMeta(java.lang.String, java.util.List)
     */
    @Override
    public List<Map<String, Object>> queryForMeta(String sql, List<Object> whereValues) {
        // TODO Auto-generated method stub
        return this.queryForList(sql, whereValues);
    }
    
    /**
     * queryForInt
     * 
     * @param sql
     *            sql
     * @param whereValues
     *            whereValues
     * @param dataSourceInfo
     *            dataSourceInfo
     * @return int count
     */
    public int queryForInt(String sql, List<Object> whereValues) {
        long begin = System.currentTimeMillis();
        Map<String, Object> result = null;
        int count = 0;
        try {
            logger.info("queryId:{} count sql: {}", QueryRouterContext.getQueryId(),
                    this.toPrintString(sql, whereValues));
            result = this.jdbcTemplate.queryForMap(sql, whereValues.toArray());
            count = Integer.valueOf(result.values().toArray()[0].toString()).intValue();
        } catch (Exception e) {
            logger.error("queryId:{} select sql error:{}", QueryRouterContext.getQueryId(), e
                    .getCause().getMessage());
            throw e;
        } finally {
            logger.info("queryId:{} select count sql cost:{} ms, result: {}",
                    QueryRouterContext.getQueryId(), System.currentTimeMillis() - begin, count);
        }
        return count;
    }
    
    /**
     * querySqlList
     *
     * @param sqlQuery
     * @param groupByList
     * @param dataSourceInfo
     * @return
     */
    public SearchIndexResultSet querySqlList(SqlQuery sqlQuery, List<SqlColumn> groupByList) {
        // 此方法目前只能使用 preparesql = false
        sqlQuery.getWhere().setGeneratePrepareSql(false);
        long begin = System.currentTimeMillis();
        List<String> selectListOrder = Lists.newArrayList();
        for (SqlColumn sqlColumn : sqlQuery.getSelect().getSelectList()) {
            selectListOrder.add(sqlColumn.getName());
        }
        List<String> groupByListStr = Lists.newArrayList();
        for (SqlColumn sqlColumn : groupByList) {
            groupByListStr.add(sqlColumn.getName());
        }
        Meta meta = new Meta(selectListOrder.toArray(new String[0]));
        SearchIndexResultSet resultSet = new SearchIndexResultSet(meta, 1000000);
        
        final List<String> selectListOrderf = Lists.newArrayList(selectListOrder);
        jdbcTemplate.query(new PreparedStatementCreator() {
            
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                String sql = sqlQuery.toSql();
                logger.info("queryId:{} sql: {}", QueryRouterContext.getQueryId(), sql);
                PreparedStatement pstmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                if (con.getMetaData().getDriverName().toLowerCase().contains("mysql")) {
                    pstmt.setFetchSize(Integer.MIN_VALUE);
                }
                return pstmt;
            }
        }, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                List<Object> fieldValues = new ArrayList<Object>();
                String groupBy = "";
                for (String select : selectListOrderf) {
                    fieldValues.add(rs.getObject(select));
                    if (groupByListStr != null && groupByListStr.contains(select)) {
                        groupBy += rs.getString(select) + ",";
                    }
                }
                
                SearchIndexResultRecord record = new SearchIndexResultRecord(fieldValues
                        .toArray(new Serializable[0]), groupBy);
                resultSet.addRecord(record);
            }
        });
        logger.info("queryId:{} select sql cost:{} ms resultsize:{}",
                QueryRouterContext.getQueryId(), System.currentTimeMillis() - begin,
                resultSet == null ? null : resultSet.size());
        ;
        return resultSet;
    }
    
    /**
     * toPrintString
     * 
     * @param sql
     *            sql
     * @param objects
     *            objects
     * @return sql String
     */
    public String toPrintString(String sql, List<Object> objects) {
        if (CollectionUtils.isEmpty(objects)) {
            return sql;
        }
        String printSql = new String(sql);
        int valuesCount = 0;
        if (!StringUtils.isEmpty(printSql)) {
            for (Object value : objects) {
                valuesCount++;
                if (value instanceof String) {
                    printSql = StringUtils.replaceOnce(printSql, "?", "'" + value.toString() + "'");
                } else {
                    printSql = StringUtils.replaceOnce(printSql, "?", value.toString());
                }
                if (valuesCount > 2000) {
                    return printSql;
                }
            }
            return printSql;
        } else {
            return "";
        }
    }
    
    @Override
    public DataSourceInfo getDataSourceInfo() {
        // TODO Auto-generated method stub
        return this.dataSourceInfo;
    }
    
    public DataSourceInfo setDataSourceInfo(DataSourceInfo dataSourceInfo) {
        return this.dataSourceInfo = dataSourceInfo;
    }
    
    /**
     * default generate get jdbcTemplate
     * 
     * @return the jdbcTemplate
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
    
    /**
     * default generate set jdbcTemplate
     * 
     * @param jdbcTemplate
     *            the jdbcTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
}
