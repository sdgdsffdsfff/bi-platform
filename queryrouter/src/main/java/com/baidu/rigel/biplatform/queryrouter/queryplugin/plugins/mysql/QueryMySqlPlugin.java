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
package com.baidu.rigel.biplatform.queryrouter.queryplugin.plugins.mysql;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baidu.rigel.biplatform.ac.query.data.DataModel;
import com.baidu.rigel.biplatform.ac.query.data.impl.SqlDataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.data.impl.SqlDataSourceInfo.DataBase;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.queryrouter.calculate.operator.utils.OperatorUtils;
import com.baidu.rigel.biplatform.queryrouter.handle.model.QueryHandler;
import com.baidu.rigel.biplatform.queryrouter.query.service.impl.QueryServiceImpl;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.QueryPlugin;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.convert.DataModelConvertService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.convert.PlaneTableUtils;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.service.impl.JdbcCountNumServiceImpl;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.meta.TableExistCheckService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.meta.impl.TableMetaServiceImpl;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.service.JdbcHandler;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.SqlExpression;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.PlaneTableQuestionModel;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QuestionModelTransformationException;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlColumn;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.utils.QueryHandlerBuilder;

/**
 * mysql查询的插件
 * 
 * @author luowenlei
 *
 */
@Service("querySqlPlugin")
@Scope("prototype")
public class QueryMySqlPlugin implements QueryPlugin {
    
    /**
     * logger
     */
    private static Logger logger = LoggerFactory.getLogger(QueryMySqlPlugin.class);
    
    /**
     * dataModelConvertService
     */
    @Resource(name = "dataModelConvertService")
    private DataModelConvertService dataModelConvertService;
    
    /**
     * jdbcCountNumServiceImpl
     */
    @Resource(name = "jdbcCountNumServiceImpl")
    private JdbcCountNumServiceImpl jdbcCountNumService;
    
    /**
     * TableExistCheck
     */
    @Resource(name = "jdbcTableExistCheckServiceImpl")
    private TableExistCheckService tableExistCheckService;
    
    /**
     * tableMetaService
     */
    @Resource(name = "tableMetaServiceImpl")
    private TableMetaServiceImpl tableMetaService;
    
    /**
     * JdbcConnectionPool
     */
    @Resource(name = "jdbcHandlerImpl")
    private JdbcHandler queryHandler;
    
    @Resource(name = "queryService")
    private QueryServiceImpl queryService;
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.baidu.rigel.biplatform.querybus.queryplugin.QueryPlugin#query(com
     * .baidu.rigel.biplatform.ac.query.model.QuestionModel)
     */
    @Override
    public DataModel query(QuestionModel questionModel) throws QuestionModelTransformationException {
        PlaneTableQuestionModel planeTableQuestionModel = PlaneTableUtils
                .convertConfigQuestionModel2PtQuestionModel((ConfigQuestionModel) questionModel);
        QueryHandler newQueryRequest = QueryHandlerBuilder.buildQueryHandler(questionModel,
                tableMetaService, queryHandler);
        List<SqlColumn> needColumns = newQueryRequest.getSqlExpression().getNeedColums();
        SqlExpression sqlExpression = newQueryRequest.getSqlExpression();
        
        if (OperatorUtils.isAggQuery(needColumns)) {
            newQueryRequest.getSqlExpression().getSqlQuery().getWhere()
                    .setGeneratePrepareSql(false);
            sqlExpression.setHasAlias(false);
            
            return queryService.query(questionModel, null, newQueryRequest);
            
            // tesseract all
            // return this.queryTesseract(questionModel);
        } else {
            // 2.检验cube.getSource中的事实表是否在数据库中存在，并过滤不存在的数据表
            sqlExpression.setHasAlias(true);
            String tableNames = tableExistCheckService.getExistTableList(
                    sqlExpression.getTableName(), newQueryRequest.getJdbcHandler());
            
            if (StringUtils.isEmpty(tableNames) || CollectionUtils.isEmpty(needColumns)) {
                // 如果获取的cube的数据为空
                logger.info("queryId:{} QuerySqlPlugin find no tables in Db.",
                        questionModel.getQueryId());
                return dataModelConvertService.getEmptyDataModel(needColumns);
            }
            // 3.生成sql
            newQueryRequest.getSqlExpression().setTableName(tableNames);
            newQueryRequest.getSqlExpression().generateSql(questionModel);
            
            // 4.execute sql
            String sql = newQueryRequest.getSqlExpression().getSqlQuery().toSql();
            List<Object> values = newQueryRequest.getSqlExpression().getSqlQuery().getWhere()
                    .getValues();
            List<Map<String, Object>> rowBasedList = queryHandler.queryForList(sql, values);
            // 5.convert data to datamodel
            DataModel dataModel = dataModelConvertService.convert(needColumns, rowBasedList);
            
            // 6.生成pageSize
            if (planeTableQuestionModel.isGenerateTotalSize()) {
                dataModel.setRecordSize(jdbcCountNumService.getTotalRecordSize(
                        planeTableQuestionModel, newQueryRequest));
            }
            return dataModel;
        }
    }
    
    @Override
    public boolean isSuitable(QuestionModel questionModel)
            throws QuestionModelTransformationException {
        if (questionModel instanceof ConfigQuestionModel) {
            ConfigQuestionModel configQuestionModel = (ConfigQuestionModel) questionModel;
            SqlDataSourceInfo sqlDataSourceInfo = ((SqlDataSourceInfo) configQuestionModel
                    .getDataSourceInfo());
            if (sqlDataSourceInfo.getDataBase() == DataBase.PALO
                    || sqlDataSourceInfo.getDataBase() == DataBase.MYSQL) {
                logger.info("queryId:{} QuerySqlPlugin find this questionModel isSuitable!",
                        questionModel.getQueryId());
                return true;
            }
        }
        return false;
    }
    //
    // public DataModel queryTesseract(QuestionModel questionModel) throws
    // MiniCubeQueryException {
    // // TODO Auto-generated method stub
    // long current = System.currentTimeMillis();
    // Map<String, String> params = new HashMap<String, String>();
    // ConfigQuestionModel configQuestionModel = (ConfigQuestionModel)
    // questionModel;
    // /** palo数据源不需要使用索引 **/
    // if (configQuestionModel.getDataSourceInfo() != null
    // && configQuestionModel.getDataSourceInfo() instanceof SqlDataSourceInfo)
    // {
    // SqlDataSourceInfo sqlDataSourceInfo = (SqlDataSourceInfo)
    // configQuestionModel
    // .getDataSourceInfo();
    // if (sqlDataSourceInfo.getDataBase() == DataBase.PALO) {
    // configQuestionModel.setUseIndex(false);
    // }
    // }
    // params.put(TesseractHttpConstants.QUESTIONMODEL_PARAM_KEY,
    // AnswerCoreConstant.GSON.toJson(configQuestionModel));
    // Map<String, String> headerParams = new HashMap<String, String>();
    // headerParams.put(TesseractHttpConstants.BIPLATFORM_QUERY_ROUTER_SERVER_TARGET_PARAM,
    // TesseractHttpConstants.TESSERACT_TYPE);
    // headerParams.put(TesseractHttpConstants.BIPLATFORM_PRODUCTLINE_PARAM,
    // configQuestionModel
    // .getDataSourceInfo().getProductLine());
    // long curr = System.currentTimeMillis();
    // logger.info("begin execute query with tesseract ");
    // String tesseractHost = "";
    // tesseractHost = PropertiesFileUtils.getPropertiesKey(
    // TesseractHttpConstants.TESSERACT_CONNECTION_FILE,
    // TesseractHttpConstants.TESSERACT_CONNECTION);
    // try {
    // String response = HttpRequest.sendPost(tesseractHost + "/query", params,
    // headerParams);
    // logger.info("queryId:{} execute query with tesseract cost {} ms",
    // questionModel.getQueryId(), System.currentTimeMillis() - curr);
    // ResponseResult responseResult =
    // AnswerCoreConstant.GSON.fromJson(response,
    // ResponseResult.class);
    // if (StringUtils.isEmpty(responseResult.getData()) &&
    // responseResult.getStatus() != 0) {
    // logger.error("queryId:{} execute query with tesseract occur an Error!"
    // + "pls check tesseract log; tesseract msg:{}",
    // questionModel.getQueryId(),
    // responseResult.getStatusInfo());
    // throw new
    // MiniCubeQueryException("execute query with tesseract occur an Error");
    // } else {
    // String dataModelJson = responseResult.getData();
    // DataModel dataModel =
    // JsonUnSeriallizableUtils.dataModelFromJson(dataModelJson);
    // logger.info("queryId:{} execute query questionModel cost {} ms",
    // questionModel.getQueryId(), System.currentTimeMillis() - current);
    // dataModel.setOthers(responseResult.getStatusInfo());
    // return dataModel;
    // }
    // } catch (Exception e) {
    // logger.error("queryId:{} execute query with tesseract occur an Error!"
    // + "pls check tesseract log; tesseract msg:{}",
    // questionModel.getQueryId(),
    // e.getMessage());
    // e.printStackTrace();
    // throw new MiniCubeQueryException(e);
    // }
    // }
}
