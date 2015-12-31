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
package com.baidu.rigel.biplatform.queryrouter.query.udf.condition;


/**
 * Description: RateConditionProcessHandlerFactory
 * @author david.wang
 *
 */
public final class RateConditionProcessHandlerFactory {

    /**
     * 构造函数
     */
    private RateConditionProcessHandlerFactory() {
    }
    
    /**
     * 
     * @param strategy 计算方式
     * @return AbsRateConditionProcessHandler
     */
    public static RateConditionProcessHandler getInstance(RateCalStrategy strategy) {
        switch (strategy) {
            case SR_NUMERATOR:
                return new SrNumeratorConditionProcessHandler();
            case SR_DENOMINATOR:
                return new SrDenominatorConditionProcessHandler();
            case RR_NUMERATOR:
                return new RrNumberatorConditionProcessHandler();
            case RR_DENOMINATOR:
                return new RrDenominatorConditionProcessHandler();
            default:
                throw new IllegalArgumentException("不支持的计算类型：" + strategy.name());
        }
    }
}
