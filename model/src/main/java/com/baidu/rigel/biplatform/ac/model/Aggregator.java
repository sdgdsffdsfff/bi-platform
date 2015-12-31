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
package com.baidu.rigel.biplatform.ac.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指标汇总类型
 * 
 * @author xiaoming.chen
 *
 */
public enum Aggregator {
    /**
     * SUM
     */
    SUM // 加和

    {
        @Override
        public Serializable aggregate(Serializable src1, Serializable src2) {
            if(src2 == null) {
                return src1; 
            }
            if(src1 == null) {
                return src2;
            }
            BigDecimal arg1 = src1 instanceof BigDecimal ? (BigDecimal) src1 : new BigDecimal(src1.toString());
            BigDecimal arg2 = src2 instanceof BigDecimal ? (BigDecimal) src2 : new BigDecimal(src2.toString());
            return arg1.add(arg2);

        }
    },
    /**
     * COUNT
     */
    COUNT // 计数

    {
        @Override
        public Serializable aggregate(Serializable src1, Serializable src2) {
            if(src1 == null ) {
                return 1;
            }else {
                return Integer.parseInt(src1.toString()) + 1;
            }

        }
    },
    /**
     * DISTINCT_COUNT
     */
    DISTINCT_COUNT {
        @Override
        public Serializable aggregate(Serializable src1, Serializable src2) {
            return 0;
        }
        
    }, // 去重计数
    /**
     * CALCULATED
     */
    CALCULATED // 计算类型指标
    {
        @Override
        public Serializable aggregate(Serializable src1, Serializable src2) {
            throw new UnsupportedOperationException("unsupported aggregator:" + CALCULATED);
            
        }
    },
    
    /**
     * NONE
     */
    NONE // 无计算指标
    {
        @Override
        public Serializable aggregate(Serializable src1, Serializable src2) {
            throw new UnsupportedOperationException("unsupported aggregator:" + NONE);
            
        }
    };

    // /**
    // * 平均值
    // */
    // AVERAGE;


    public abstract Serializable aggregate(Serializable src1, Serializable src2);
}
