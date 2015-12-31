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
package com.baidu.rigel.biplatform.queryrouter.queryplugin;

import com.baidu.rigel.biplatform.ac.query.data.DataModel;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.QuestionModelTransformationException;

/**
 *
 * 查询接口
 * 
 * Created by luowenlei.
 *
 */
public interface QueryPlugin {
    
    /**
     *
     * 查询接口
     * 
     * @param QuestionModel
     *            问题模型
     * @return DataModel 数据模型
     *
     */
    DataModel query(QuestionModel questionModel) throws QuestionModelTransformationException;
    
    /**
     *
     * 判断是否匹配
     * 
     * @param QuestionModel
     *            问题模型
     * @return boolean true匹配成功，false不匹配
     *
     */
    boolean isSuitable(QuestionModel questionModel) throws QuestionModelTransformationException;
    
}
