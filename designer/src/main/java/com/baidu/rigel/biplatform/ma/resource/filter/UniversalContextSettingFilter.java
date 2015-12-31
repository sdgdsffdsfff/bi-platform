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
package com.baidu.rigel.biplatform.ma.resource.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.CookieGenerator;

import com.baidu.rigel.biplatform.ac.util.AesUtil;
import com.baidu.rigel.biplatform.ma.model.consts.Constants;
import com.baidu.rigel.biplatform.ma.model.utils.UuidGeneratorUtils;
import com.baidu.rigel.biplatform.ma.report.utils.ContextManager;
import com.baidu.rigel.biplatform.ma.resource.utils.ResourceUtils;
import com.google.common.collect.Lists;

/**
 * 
 * 过滤器
 * @author zhongyi
 *
 *         2014-8-13
 */
public class UniversalContextSettingFilter implements Filter {
    
    /**
     * LOG
     */
    private static final Logger LOG = LoggerFactory.getLogger(UniversalContextSettingFilter.class);

    /**
     * securityKey
     */
    private String securityKey;
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (StringUtils.isEmpty(securityKey)) {
            WebApplicationContext context = 
                WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
            securityKey = ((ConfigurableApplicationContext) context)
                .getBeanFactory().resolveEmbeddedValue("${biplatform.ma.ser_key}");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request; 
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            LOG.info("current request url : " + httpRequest.getRequestURI());
            request.setCharacterEncoding("utf-8");
            response.setCharacterEncoding("utf-8");
            String sessionId = null;
            String productLine = null;
            if (httpRequest.getCookies() != null && httpRequest.getCookies().length > 0) {
                List<Cookie> cookies = Lists.newArrayList();
                Collections.addAll(cookies, httpRequest.getCookies());
                productLine = getProductLine(cookies);                
                sessionId = getSessionId(cookies);
            }
            LOG.info("productLine in cookie is " + productLine + " and sessionId is " + sessionId);
            if (StringUtils.isEmpty(productLine) 
                && !StringUtils.isEmpty(request.getParameter(Constants.TOKEN))) {
                productLine = decryptProductLine(httpRequest, httpResponse);
                if (StringUtils.isEmpty(sessionId)) {
                    sessionId = generateSessionId(httpResponse);
                }
            }
            LOG.info("productLine in token is " + productLine + " and sessionId is " + sessionId);
            if ((httpRequest.getRequestURI().endsWith("index.html") 
                    || httpRequest.getRequestURI().endsWith("newIndex.html")
                    || httpRequest.getRequestURI().endsWith("report.html"))
                    && StringUtils.isEmpty(productLine)) {
                httpResponse.addCookie (new Cookie("prevReq", httpRequest.getRequestURI ()));
                httpResponse.sendRedirect("home.html");
            }
            setSessionInfoIntoThread(httpRequest, httpResponse, chain, productLine, sessionId);
        } catch(Exception e) {
            throw new RuntimeException("productline encrypt happened exception, message:" + e, e);
        }
    }
    
    /**
     * 初始化用户会话id
     * @param response HttpServletResponse
     * @return String
     */
    private String generateSessionId(HttpServletResponse response) {
        String sessionId;
        sessionId = UuidGeneratorUtils.generate();
        Cookie sessionIdCookie = new Cookie(Constants.SESSION_ID, sessionId);
        sessionIdCookie.setPath(Constants.COOKIE_PATH);
        response.addCookie(sessionIdCookie);
        return sessionId;
    }

    /**
     * 初始化用户产品线信息
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return String
     * @throws Exception
     * 
     */
    private String decryptProductLine(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String productLine = null;
        productLine = request.getParameter(Constants.TOKEN);
        if (StringUtils.hasText(productLine)) {
            Cookie productLineCookie = new Cookie(Constants.BIPLATFORM_PRODUCTLINE, productLine);
            productLineCookie.setPath(Constants.COOKIE_PATH);
            ((HttpServletResponse) response).addCookie(productLineCookie);
            // 对productLine进行重新解密，以便放到ContextManager中
            try {
                productLine = AesUtil.getInstance().decodeAnddecrypt (productLine, securityKey);
            } catch (Exception e) {
                productLine = AesUtil.getInstance().decrypt (productLine, securityKey);
            }
        }
        return productLine;
    }

    /**
     * 从cookie中查找用户的会话信息
     * @param cookies
     * @return String
     */
    private String getSessionId(List<Cookie> cookies) {
        String sessionId = null;
        Object[] tmp;
        tmp = cookies.stream().filter(cookie -> {
            return Constants.SESSION_ID.equals(cookie.getName());
        }).map(genSessionId).toArray();
        
        if (tmp != null && tmp.length > 0) {
            sessionId = tmp[0].toString();
        }
        return sessionId;
    }

    /**
     * 从cookie中查找用户的产品线信息
     * @param cookies request cookies
     * @return String
     */
    private String getProductLine(List<Cookie> cookies) {
        String productLine = null;
        Object[] tmp = cookies.stream().filter(cookie -> {
           return Constants.BIPLATFORM_PRODUCTLINE.equals(cookie.getName()); 
        }).map(encryptProductLine).toArray();
        if (tmp != null && tmp.length > 0) {
            productLine = tmp[0].toString();
        }
        return productLine;
    }
    
    /**
     * 
     * 将运行时变量放到当前线程
     * @param request HttpServletRequest
     * @param response HttpServletRequest
     * @param chain FilterChain
     * @param productLine 产品线信息
     * @param sessionId 会话id
     * @throws Exception
     * 
     */
    private void setSessionInfoIntoThread(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain,
            String productLine, String sessionId) throws Exception {
        try {
            if (!StringUtils.isEmpty(sessionId)) {
                ContextManager.setSessionId(sessionId);
            } else {
                // 如果发现当前sessionId为空，那么可能是在编辑端进行操作，这时直接取每个客户端的标识uniqueFlag即可 update by majun
                String fakeSessionId = ResourceUtils.getCookieValueFromRequest(request, "uniqueFlag");
                if (StringUtils.isEmpty(fakeSessionId)) {
                    fakeSessionId = UuidGeneratorUtils.generate();
                }
                ContextManager.setSessionId(fakeSessionId);
                
                CookieGenerator cookieGenerator = new CookieGenerator();
                cookieGenerator.setCookieName("uniqueFlag");
                cookieGenerator.addCookie(response, fakeSessionId);
            }
            
            if (!StringUtils.isEmpty(productLine)) {
                ContextManager.setProductLine(productLine);
            }
            chain.doFilter(request, response);
        } finally {
            ContextManager.cleanSessionId();
            ContextManager.cleanProductLine();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig chain) throws ServletException {
    }
    
    // 对产品线cookie的处理
    private Function<Cookie, String> encryptProductLine = cookie -> {
        String innerProductLine = cookie.getValue();
        if (StringUtils.hasText(innerProductLine)) {
            // 调用解密算法，对productLine进行解密
            try {
                innerProductLine = AesUtil.getInstance().decodeAnddecrypt(innerProductLine, securityKey);
            } catch (Exception e) {
                try {
                    innerProductLine = AesUtil.getInstance ().decrypt (innerProductLine, securityKey);
                } catch (Exception e1) {
                    LOG.error (e1.getMessage (), e1);
                }
//                LOG.error(e.getMessage(),e);
                //e.printStackTrace();
            }
        } // 产品线value不为空
        return innerProductLine;
    };
    
    // 对sessionId的cookie的处理
    private Function<Cookie, String> genSessionId = cookie -> {
        return cookie.getValue();
    };  
}