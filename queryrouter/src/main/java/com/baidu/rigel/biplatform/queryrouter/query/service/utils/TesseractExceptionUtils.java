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
/**
 * 
 */
package com.baidu.rigel.biplatform.queryrouter.query.service.utils;

import org.springframework.util.StringUtils;

import com.baidu.rigel.biplatform.queryrouter.query.exception.IndexAndSearchExceptionType;

/**
 * TesseractExceptionUtils
 * 
 * @author lijin
 *
 */
public class TesseractExceptionUtils {
    
    /**
     * 
     * getExceptionMessage
     * 
     * @param message
     *            message
     * @param qType
     *            qType
     * @return String
     */
    public static String getExceptionMessage(String message, IndexAndSearchExceptionType qType) {
        String result = "";
        if (!StringUtils.isEmpty(message) && qType != null) {
            result = String.format(message, qType.getTypeName());
        }
        return result;
        
    }
    
}
