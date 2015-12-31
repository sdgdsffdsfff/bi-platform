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
package com.baidu.rigel.biplatform.queryrouter.handle;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.rigel.biplatform.ac.query.data.DataModel;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.ac.util.AnswerCoreConstant;
import com.baidu.rigel.biplatform.ac.util.ResponseResult;
import com.baidu.rigel.biplatform.ac.util.ResponseResultUtils;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.QueryPlugin;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.QueryPluginFactory;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QuestionModelTransformationException;
import com.google.gson.JsonSyntaxException;

/**
 * queryrouter对外接口
 * 
 * @author luowenlei
 * 
 *         2015-05-07
 */
@RestController
@RequestMapping("/queryrouter")
@Service
public class QueryRouterResource {
    
    /**
     * logger
     */
    private Logger logger = LoggerFactory.getLogger(QueryRouterResource.class);
    
    /**
     * log4j最长打印字符串长度
     */
    private static final int MAX_PRINT_LENGTH = 5000;
    
    /**
     * 出参参的参数“成功”
     */
    private static final String SUCCESS = "success";
    
    /**
     * 入参参的参数“问题模型参数”
     */
    private static final String PRAMA_QUESTION = "question";
    
    /**
     * queryPluginFactory
     */
    @Resource
    private QueryPluginFactory queryPluginFactory;
    
    /**
     * 查询一个报表中，某个区域的数据
     * 
     * @param reportId
     * @param areaId
     * @param request
     * @return
     */
    @RequestMapping(value = "/query", method = { RequestMethod.POST })
    public ResponseResult query(HttpServletRequest request) {
        long begin = System.currentTimeMillis();
        if (request.getAttribute(PRAMA_QUESTION) == null) {
            return ResponseResultUtils.getErrorResult("question is null", 100);
        }
        String questionStr = request.getAttribute(PRAMA_QUESTION).toString();
        // convert json to QuestionModel
        ConfigQuestionModel questionModel = AnswerCoreConstant.GSON.fromJson(questionStr,
                ConfigQuestionModel.class);
        questionModel.setQueryId(questionModel.getDataSourceInfo().getProductLine() + "-"
                + questionModel.getQueryId());
        QueryRouterContext.setQueryInfo(questionModel.getQueryId());
        logger.info("queryId:{} query current handle size:{} , begin to handle this queryId.",
                questionModel.getQueryId(), QueryRouterContext.getQueryCurrentHandleSize());
        // 限制日志输出
        if (questionStr.length() > MAX_PRINT_LENGTH) {
            logger.info("queryId:{} request questionmodel json:{}", questionModel.getQueryId(),
                    QueryRouterContext.getQueryCurrentHandleSize(),
                    questionStr.substring(0, MAX_PRINT_LENGTH));
            logger.debug("queryId:{} request questionmodel json:{}", questionModel.getQueryId(),
                    questionStr);
        } else {
            logger.info("queryId:{} request questionmodel json:{}", questionModel.getQueryId(),
                    questionStr);
        }
        // get DataModel
        try {
            DataModel dataModel = this.query(questionModel);
            if (dataModel == null) {
                return ResponseResultUtils.getErrorResult("tesseract occur an error", 1);
            }
            String dataModelJson = AnswerCoreConstant.GSON.toJson(dataModel);
            // 限制日志输出
            if (dataModelJson.length() > MAX_PRINT_LENGTH) {
                logger.debug("queryId:{} response modeldata json:{}", questionModel.getQueryId(),
                        dataModelJson);
                logger.info("queryId:{} response modeldata json:{}...", questionModel.getQueryId(),
                        dataModelJson.substring(0, MAX_PRINT_LENGTH));
            } else {
                logger.info("queryId:{} response modeldata json:{}", questionModel.getQueryId(),
                        dataModelJson);
            }
            logger.info("queryId:{} response query toal cost:{} ms", questionModel.getQueryId(),
                    System.currentTimeMillis() - begin);
            return ResponseResultUtils.getCorrectResult(SUCCESS, dataModelJson);
        } catch (JsonSyntaxException e) {
            logger.error("queryId:{} error msg:{}", questionModel.getQueryId(), e.getMessage());
            // 说明模型参数传入有问题
            return ResponseResultUtils.getErrorResult(
                    "json syntax exception,json is not well formed.", 100);
        } catch (QuestionModelTransformationException e) {
            logger.error("queryId:{} error msg:{}", questionModel.getQueryId(), e.getMessage());
            // 说明模型参数传入有问题
            return ResponseResultUtils.getErrorResult(
                    "question model exception, questionmodel is incorrect." + "reason:"
                            + e.getMessage(), 100);
        } finally {
            logger.info("queryId:{} query current handle size:{} , end to handle this queryId.",
                    questionModel.getQueryId(), QueryRouterContext.getQueryCurrentHandleSize());
            QueryRouterContext.removeQueryInfo();
        }
    }
    
    /**
     * 将传入的questionModel通过dispatch后分发到相应的Plugin，然后转换成DataModel对象
     * 
     * @param questionStr
     *            request questionStr
     * @return DataModel DataModel
     */
    private DataModel query(QuestionModel questionModel) {
        long begin = System.currentTimeMillis();
        QueryPlugin queryPlugin = queryPluginFactory.getPlugin(questionModel);
        logger.info("queryId:{} getQueryPlugin cost:{}", questionModel.getQueryId(),
                System.currentTimeMillis() - begin);
        return queryPlugin.query(questionModel);
    }
    
    /**
     * 将传入的questionModel通过dispatch后分发到相应的Plugin，然后转换成DataModel对象,下载中心查询用
     * 
     * @param questionStr
     *            request questionStr
     * @return DataModel DataModel
     */
    public DataModel queryAndLog(QuestionModel questionModel) {
        long begin = System.currentTimeMillis();
        QueryRouterContext.setQueryInfo(questionModel.getQueryId());
        logger.info("queryId:{} query current handle size:{} , begin to handle this queryId.",
                questionModel.getQueryId(), QueryRouterContext.getQueryCurrentHandleSize());
        try {
            long beginGet = System.currentTimeMillis();
            QueryPlugin queryPlugin = queryPluginFactory.getPlugin(questionModel);
            logger.info("queryId:{} getQueryPlugin cost:{}", questionModel.getQueryId(),
                    System.currentTimeMillis() - beginGet);
            DataModel dataModel = queryPlugin.query(questionModel);
            logger.info("queryId:{} response query toal cost:{} ms", questionModel.getQueryId(),
                    System.currentTimeMillis() - begin);
            return dataModel;
        } catch (Exception e) {
            logger.error("queryId:{} occur error, cost:{}ms, cause:{}", questionModel.getQueryId(),
                    System.currentTimeMillis() - begin, e.getCause().getMessage());
            return null;
        } finally {
            logger.info("queryId:{} query current handle size:{} , end to handle this queryId.",
                    questionModel.getQueryId(), QueryRouterContext.getQueryCurrentHandleSize());
            QueryRouterContext.removeQueryInfo();
        }
    }
    
    /**
     * 判断服务是否存活
     * 
     * @param request
     * @return ResponseResult status string
     */
    @RequestMapping(value = "/alive", method = { RequestMethod.GET })
    public ResponseResult checkAlive(HttpServletRequest request) {
        return ResponseResultUtils.getCorrectResult("OK", "OK");
    }
}
