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
package com.baidu.rigel.biplatform.queryrouter.handle.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.rigel.biplatform.queryrouter.QueryRouterApplication4Enterprise;

/**
 * Api server to server 的认证
 * 
 * @author luowenlei
 *
 */
public class ApiDesAuthService {
    
    /**
     * 参数request question
     */
    private static final String PARAM_QUESTION = "question";
    
    /**
     * 参数request signature
     */
    private static final String PARAM_SIGNATURE = "signature";
    
    /**
     * logger
     */
    private static Logger logger = LoggerFactory.getLogger(QueryRouterApplication4Enterprise.class);
    
    /**
     * {@inheritDoc}
     */
    public boolean auth(HttpServletRequest request, HttpServletResponse response, String key)
            throws Exception {
        if (request.getParameterMap().get(PARAM_QUESTION) == null
                || request.getParameterMap().get(PARAM_SIGNATURE) == null) {
            return true;
        }
        String question = request.getParameterMap().get(PARAM_QUESTION)[0].toString();
        String signature = request.getParameterMap().get(PARAM_SIGNATURE)[0].toString();
        // 参数验证
        try {
//            if (Md5Util.encode(question, key).equals(signature)) {
//                request.setAttribute(PARAM_QUESTION, question);
//                return true;
//            } else {
//                return false;
//            }
            request.setAttribute(PARAM_QUESTION, question);
            return true;
        } catch (Exception e) {
            logger.error(
                    "occur error, request param can not distinguish:{},question:{},signature:{}", e
                            .getCause().getMessage(), question, signature);
            return false;
        }
    }
    
}
