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
package com.baidu.rigel.biplatform.queryrouter.query.exception;

/**
 * NotSupportedDateFormatException
 * @author lijin
 *
 */
public class NotSupportedDateFormatException extends Exception {
    
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -6668397362063894117L;

    /**
     * 构造函数
     */
    public NotSupportedDateFormatException() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     *  构造函数
     * @param message 消息
     */
    public NotSupportedDateFormatException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * 构造函数
     * @param cause 原因
     */
    public NotSupportedDateFormatException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * 构造函数
     * @param message 消息
     * @param cause 原因
     */
    public NotSupportedDateFormatException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
    
    /**
     * 构造函数
     * @param message 消息
     * @param cause 原因
     * @param enableSuppression  enableSuppression
     * @param writableStackTrace writableStackTrace
     */
    public NotSupportedDateFormatException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }
    
}
