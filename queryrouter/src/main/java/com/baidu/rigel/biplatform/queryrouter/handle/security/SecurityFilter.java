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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.rigel.biplatform.ac.util.AesUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 安全过滤器
 *
 * @author 罗文磊
 * @version 1.0.0.1
 */
public class SecurityFilter implements Filter {
    
    /**
     * 日志纪录器
     */
    private Logger logger = LoggerFactory.getLogger(SecurityFilter.class);
    
    /**
     * 存储配置策略的内置map
     */
    public static final Map<String, JSONObject> REPOSITORY = Maps.newConcurrentMap();
    
    /**
     * request parameter “token”
     */
    public static final String TOKEN = "token";
    
    /**
     * key string in REPOSITORY
     */
    public static final String KEY = "key";
    
    public SecurityFilter() {
    }
    
    /**
     * init filter
     * 
     * @param filterConfig
     *            FilterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    /**
     * 
     * @param requestURI
     *            custom URL
     * @return boolean if the provide requestURI in exclude list return true
     *         else false
     */
    private boolean isExcludeUrl(String requestURI) {
        Set<String> excludeUrl = Sets.newHashSet ();
        excludeUrl.add ("/alive");
        excludeUrl.add ("/tianlurpc/");
        for (String url : excludeUrl) {
            if (requestURI.indexOf(url) == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * do filter
     * 
     * @param request
     *            ServletRequest
     * @param response
     *            ServletResponse
     * @param chain
     *            FilterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = null;
        httpRequest = (HttpServletRequest) request;
        // 不需要鉴权的请求
        String realRequestUri = httpRequest.getRequestURI().substring(1);
        String requestURI = realRequestUri.substring(realRequestUri.indexOf("/"), realRequestUri.length());
        if (isExcludeUrl (requestURI)) {
            chain.doFilter (request, response);
            return;
        }
        if (doAuth(httpRequest, (HttpServletResponse) response)) {
            chain.doFilter(httpRequest, response);
        }
    }
    
    /**
     *
     * 接入认证
     * 
     * @param request
     *            HttpServletRequest
     * @param response
     *            HttpServletResponse
     * @param productLine
     *            产品线
     * @param magicNum
     *            cookie key
     *
     */
    private boolean doAuth(HttpServletRequest request, HttpServletResponse response) {
        // 进行认证
        try {
            if (request.getParameter(TOKEN) == null) {
                return false;
            }
            String systemCodeEncrypt = request.getParameter(TOKEN).toString();
            ApiDesAuthService service = new ApiDesAuthService();
            String systemCode = AesUtil.getInstance().decodeAnddecrypt(systemCodeEncrypt);
            JSONObject systemKey = REPOSITORY.get(systemCode);
            if (systemKey == null) {
                throw new IllegalStateException("token:\"" + systemKey
                        + "\" is uncorrect, access to services prohibited.");
            }
            // 认证请求成功
            return service.auth(request, response, systemKey.getString(KEY));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new IllegalStateException("token is uncorrect, authentication failure.");
        }
    }
    
    /**
     *
     * destroy
     *
     */
    @Override
    public void destroy() {
    }
    
}
