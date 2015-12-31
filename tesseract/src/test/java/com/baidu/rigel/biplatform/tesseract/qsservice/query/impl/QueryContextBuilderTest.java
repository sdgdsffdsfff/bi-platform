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
package com.baidu.rigel.biplatform.tesseract.qsservice.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeLevel;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMember;
import com.baidu.rigel.biplatform.ac.minicube.StandardDimension;
import com.baidu.rigel.biplatform.ac.model.DimensionType;
import com.baidu.rigel.biplatform.ac.model.LevelType;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.model.DimensionCondition;
import com.baidu.rigel.biplatform.ac.query.model.QueryData;
import com.baidu.rigel.biplatform.ac.query.model.QuestionModel;
import com.baidu.rigel.biplatform.tesseract.meta.MetaDataService;
import com.baidu.rigel.biplatform.tesseract.qsservice.query.QueryContextBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *Description:
 * @author david.wang
 *
 */
public class QueryContextBuilderTest {
    
    @Test
    public void testGetRequestParams() {
        try {
            QueryContextBuilder.getRequestParams (null, null);
        } catch (Exception e) {
            Assert.assertNotNull (e);
        }
        
        QuestionModel model = new QuestionModel ();
        Map<String, String> params = Maps.newHashMap ();
        params.put ("a", "b");
        model.setRequestParams (params);
        Map<String, String> rs = QueryContextBuilder.getRequestParams (model, null);
        Assert.assertEquals (2, rs.size ());
        
        DimensionCondition cond = new DimensionCondition ("test");
        model.getQueryConditions ().put ("test", cond);
        
        try {
            QueryContextBuilder.getRequestParams (model, null);
        } catch (Exception e) {
            Assert.assertNotNull (e);
        }
        
        MiniCube cube = new MiniCube ();
        cube.setSource ("b");
        StandardDimension dim = new StandardDimension ();
        dim.setName ("test");
        dim.setTableName ("a");
        cube.getDimensions ().put (dim.getName (), dim);
        rs = QueryContextBuilder.getRequestParams (model, cube);
        Assert.assertEquals (2, rs.size ());
        
        cube.setSource ("a");
        rs = QueryContextBuilder.getRequestParams (model, cube);
        Assert.assertEquals (3, rs.size ());
        
        QueryData q = new QueryData("q");
        q.setUniqueName ("[test].[q]");
        cond.getQueryDataNodes ().add (q);
        model.getQueryConditions ().put ("test", cond);
        rs = QueryContextBuilder.getRequestParams (model, cube);
        Assert.assertEquals (3, rs.size ());
    }
    
    @Test
    public void testBuildFilterConditionIllegal () throws Exception {
        QueryContextBuilder builder = new QueryContextBuilder ();
        try {
            builder.buildFilterCondition (null, null, null, null);
        } catch (Exception e) {
            Assert.assertNotNull (e);
        }
        
        DimensionCondition dimCondition = new DimensionCondition ();
        Assert.assertNull (builder.buildFilterCondition (null, null, dimCondition, null));
        
        QueryData q = new QueryData ("[test].[test]");
        dimCondition.getQueryDataNodes ().add (q);
        
        try {
            builder.buildFilterCondition (null, null, dimCondition, null);
        } catch (Exception e) {
            Assert.assertNotNull (e);
        }
        
    }
    
    @Test
    public void testBuildFilterCondition() throws Exception {
        QueryContextBuilder builder = new QueryContextBuilder ();
        DimensionCondition dimCondition = new DimensionCondition ("test");
        QueryData q = new QueryData ("[test].[test]");
        dimCondition.getQueryDataNodes ().add (q);
        MiniCube cube = new MiniCube ("test");
        cube.setSource ("test");
        StandardDimension dim = new StandardDimension("test");
        dim.setType (DimensionType.CALLBACK);
        try {
            builder.buildFilterCondition (null, cube, dimCondition, null);
        } catch (Exception e) {
            Assert.assertNotNull (e);
        }
        q = new QueryData ("[test].[All_tests]");
        dimCondition.getQueryDataNodes ().add (q);
        MiniCubeLevel level = new MiniCubeLevel ();
        level.setType (LevelType.CALL_BACK);
        cube.getDimensions ().put ("test", dim);
        dim.getLevels ().put ("test", level);
        Assert.assertNull (builder.buildFilterCondition (null, cube, dimCondition, null));
        
        dim = new StandardDimension ("test");
        level = new MiniCubeLevel ();
        level.setType (LevelType.REGULAR);
        cube.getDimensions ().put ("test", dim);
        dim.getLevels ().put ("test", level);
        MiniCubeLevel level2 = new MiniCubeLevel ("l2");
        level2.setDimTable ("test");
        dim.getLevels ().put ("l2", level2);
        builder.buildFilterCondition (null, cube, dimCondition, null);
        level.setDimTable ("tt");
        builder.buildFilterCondition (null, cube, dimCondition, null);
        q = new QueryData("[test].[test]");
        dimCondition.getQueryDataNodes ().clear ();
        dimCondition.getQueryDataNodes ().add (q);
        level.setDimTable ("test");
        MetaDataService service = Mockito.mock (MetaDataService.class);
        builder.setMetaDataService (service);
        List<MiniCubeMember> rs = Lists.newArrayList ();
        DataSourceInfo info = Mockito.mock (DataSourceInfo.class);
        ArrayList<String> uniqueNameList = Lists.newArrayList ("[test].[test]");
        Mockito.doReturn (rs).when (service).lookUp (info, cube, uniqueNameList, null);
        Assert.assertEquals (1, builder.buildFilterCondition (info, cube, dimCondition, null).size ());
        MiniCubeMember m1 = new MiniCubeMember ("test1");
        rs.add (m1);
        MiniCubeMember m2 = new MiniCubeMember ("test2");
        m2.setQueryNodes (Sets.newHashSet ("a"));
        rs.add (m2);
        level.setFactTableColumn ("test");
        Mockito.doReturn (rs).when (service).lookUp (info, cube, uniqueNameList, null);
        Assert.assertNull (builder.buildFilterCondition (info, cube, dimCondition, null));
        m1.setLevel (level);
        Assert.assertEquals (1, builder.buildFilterCondition (info, cube, dimCondition, null).size ());
    }
}
