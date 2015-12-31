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

import java.util.ArrayList;
import java.util.List;

import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;

/**
 * 查询插件获取类
 * 
 * @author luowenlei
 *
 */
public class QueryPluginFactory {
    
    private List<QueryPlugin> activePluginList;
    
    /**
     * 获取查询插件
     *
     * @param QuestionModel
     * @return QueryPlugin 查询插件
     */
    public QueryPlugin getPlugin(QuestionModel questionModel) {
        List<QueryPlugin> suitablePlugins = new ArrayList<QueryPlugin>();
        activePluginList.forEach((queryPlugin) -> {
            if (queryPlugin.isSuitable(questionModel)) {
                suitablePlugins.add(queryPlugin);
            }
        });
        if (suitablePlugins.isEmpty()) {
            return null;
        } else {
            return suitablePlugins.get(0);
        }
    }
    
    /**
     * default generate set activePluginList
     * 
     * @param activePluginList
     *            the activePluginList to set
     */
    public void setActivePluginList(List<QueryPlugin> activePluginList) {
        this.activePluginList = activePluginList;
    }
}
