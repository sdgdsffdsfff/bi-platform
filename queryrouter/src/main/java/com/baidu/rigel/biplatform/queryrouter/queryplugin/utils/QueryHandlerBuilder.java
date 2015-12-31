package com.baidu.rigel.biplatform.queryrouter.queryplugin.utils;

import java.util.Map;

import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.data.impl.SqlDataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.ac.query.model.MetaCondition;
import com.baidu.rigel.biplatform.ac.query.model.PageInfo;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.ac.query.model.SortRecord;
import com.baidu.rigel.biplatform.cache.util.ApplicationContextHelper;
import com.baidu.rigel.biplatform.queryrouter.handle.model.QueryHandler;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.convert.PlaneTableUtils;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.convert.SqlColumnUtils;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.meta.TableMetaService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.plugins.palo.sql.PaloSqlExpression;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.service.JdbcHandler;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.SqlExpression;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.PlaneTableQuestionModel;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QueryMeta;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QuestionModelTransformationException;

/**
 * 类QueryRequestBuilder.java的实现描述：TODO 类实现描述
 * 
 * @author luowenlei 2015年11月25日 下午4:32:08
 */
public class QueryHandlerBuilder {
    
    /**
     * 通过QuestionModel构建QueryHandler,此方法默认设置了needColumns
     *
     * @param questionModel
     * @param tableMetaService
     *            可以为null，这样获取不到数据类型
     * @param queryHandler
     *            可以为null，这样方法会通过数据源自动获取。
     * @return
     * @throws QuestionModelTransformationException
     */
    public static QueryHandler buildQueryHandler(QuestionModel questionModel,
            TableMetaService tableMetaService, JdbcHandler queryHandler)
            throws QuestionModelTransformationException {
        ConfigQuestionModel configQuestionModel = (ConfigQuestionModel) questionModel;
        PlaneTableQuestionModel planeTableQuestionModel = PlaneTableUtils
                .convertConfigQuestionModel2PtQuestionModel(configQuestionModel);
        QueryHandler newQueryRequest = buildQueryHandler(configQuestionModel.getCube(),
                configQuestionModel.getDataSourceInfo(),
                planeTableQuestionModel.getQueryConditions(), configQuestionModel.getPageInfo(),
                configQuestionModel.getSortRecord(), tableMetaService, queryHandler);
        newQueryRequest.getSqlExpression().setNeedColums(
                SqlColumnUtils.getNeedColumns(newQueryRequest.getSqlExpression().getQueryMeta(),
                        planeTableQuestionModel.getSelection()));
        return newQueryRequest;
    }
    
    /**
     * 通过QuestionModel构建QueryHandler
     *
     * @param cube
     *            minicube
     * @param dataSourceInfo
     *            不能为空
     * @return
     * @throws QuestionModelTransformationException
     */
    public static QueryHandler buildQueryHandler(Cube cube, DataSourceInfo dataSourceInfo)
            throws QuestionModelTransformationException {
        return buildQueryHandler(cube, dataSourceInfo, null, null, null, null, null);
    }
    
    /**
     * 通过QuestionModel构建buildQueryHandler
     *
     * @param cube
     *            minicube
     * @param dataSourceInfo
     *            不能为空
     * @param queryConditions
     *            查询数据,可以为空，为空后没有条件信息，也可以后行设置
     * @param pageInfo
     *            分页数据 可以为空，为空后，没有分页信息
     * @param sortRecord
     *            排序数据 可以为空，为空后，没有排序信息
     * @param tableMetaService
     *            可以为null，这样获取不到数据类型
     * @param queryHandler
     *            可以为null，这样方法会通过数据源自动获取。
     * @return
     * @throws QuestionModelTransformationException
     */
    public static QueryHandler buildQueryHandler(Cube cube, DataSourceInfo dataSourceInfo,
            Map<String, MetaCondition> queryConditions, PageInfo pageInfo, SortRecord sortRecord,
            TableMetaService tableMetaService, JdbcHandler jdbcHandler)
            throws QuestionModelTransformationException {
        if (dataSourceInfo == null || cube == null) {
            throw new QuestionModelTransformationException(
                    "dataSourceInfo or cube can not be null.");
        }
        String source = ((MiniCube) cube).getSource();
        QueryMeta queryMeta = SqlColumnUtils.getAllColumns(
                PlaneTableUtils.getAllColumns(cube, sortRecord), source, queryConditions);
        SqlDataSourceInfo sqlDataSourceInfo = (SqlDataSourceInfo) dataSourceInfo;
        SqlExpression sqlExpression = null;
        switch (sqlDataSourceInfo.getDataBase()) {
            case MYSQL:
                sqlExpression = new SqlExpression(sqlDataSourceInfo.getDataBase().getDriver());
                if (jdbcHandler == null) {
                    jdbcHandler = (JdbcHandler) ApplicationContextHelper.getContext().getBean(
                            "jdbcHandlerImpl");
                }
                if (tableMetaService == null) {
                    tableMetaService = (TableMetaService) ApplicationContextHelper.getContext()
                            .getBean("tableMetaServiceImpl");
                }
                break;
            case PALO:
                sqlExpression = new PaloSqlExpression(sqlDataSourceInfo.getDataBase().getDriver());
                if (jdbcHandler == null) {
                    jdbcHandler = (JdbcHandler) ApplicationContextHelper.getContext().getBean(
                            "jdbcHandlerImpl");
                }
                if (tableMetaService == null) {
                    tableMetaService = (TableMetaService) ApplicationContextHelper.getContext()
                            .getBean("tableMetaServiceImpl");
                }
                break;
//            case DRUID:
//                sqlExpression = new DruidSqlExpression(sqlDataSourceInfo.getDataBase().getDriver());
//                if (jdbcHandler == null) {
//                    jdbcHandler = (JdbcHandler) ApplicationContextHelper.getContext().getBean(
//                            "druidJdbcHandlerImpl");
//                }
//                if (tableMetaService == null) {
//                    tableMetaService = (TableMetaService) ApplicationContextHelper.getContext()
//                            .getBean("druidTableMetaServiceImpl");
//                }
//                break;
            default:
                throw new QuestionModelTransformationException(sqlDataSourceInfo.getDataBase()
                        .getDriver() + " can not handle.");
        }
        jdbcHandler.initJdbcTemplate(dataSourceInfo);
        
        if (tableMetaService != null) {
            // 1.设置mysql表 meta的信息，生成allColumns里面的dataType
            tableMetaService.generateColumnDataType(queryMeta.getAllColumns(), dataSourceInfo,
                    jdbcHandler);
        }
        sqlExpression.setQueryMeta(queryMeta);
        sqlExpression.setNeedColums(null);
        sqlExpression.setTableName(source);
        QueryHandler queryHandler = new QueryHandler(sqlExpression, jdbcHandler);
        
        return queryHandler;
    }
}
