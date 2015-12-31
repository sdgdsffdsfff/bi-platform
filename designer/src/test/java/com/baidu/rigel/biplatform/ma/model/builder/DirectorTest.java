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
package com.baidu.rigel.biplatform.ma.model.builder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.rigel.biplatform.ac.minicube.ExtendMinicubeMeasure;
import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMeasure;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeSchema;
import com.baidu.rigel.biplatform.ac.minicube.StandardDimension;
import com.baidu.rigel.biplatform.ac.model.Aggregator;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.DimensionType;
import com.baidu.rigel.biplatform.ac.model.Measure;
import com.baidu.rigel.biplatform.ac.model.MeasureType;
import com.baidu.rigel.biplatform.ac.model.Schema;
import com.baidu.rigel.biplatform.ac.model.TimeType;
import com.baidu.rigel.biplatform.ma.model.builder.impl.DirectorImpl;
import com.baidu.rigel.biplatform.ma.model.meta.CallbackDimTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.ColumnMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.DimSourceType;
import com.baidu.rigel.biplatform.ma.model.meta.DimTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.FactTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.ReferenceDefine;
import com.baidu.rigel.biplatform.ma.model.meta.StandardDimTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.StarModel;
import com.baidu.rigel.biplatform.ma.model.meta.TimeDimTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.TimeDimType;
import com.baidu.rigel.biplatform.ma.model.meta.UserDefineDimTableMetaDefine;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * test class
 *
 * @author david.wang
 * @version 1.0.0.1
 */
public class DirectorTest {

    /**
     * director
     */
    DirectorImpl director = new DirectorImpl();

    /**
     * 使用null获取schema
     */
    @Test
    public void testGetSchemaWithNull() {
        Assert.assertNull(director.getSchema(null));
    }

    @Test
    public void testCheckRefMeasuer() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        ExtendMinicubeMeasure m = new ExtendMinicubeMeasure("R值环比");
        m.setFormula("(${cash}/${csm})/(dateData(${cash},(0-1))/dateData(${csm},0-1))-1");
        Map<String, Measure> newMeasures = new HashMap<String, Measure>();
        Measure measure1 = new MiniCubeMeasure("csm");
        newMeasures.put("measure1", measure1);
        @SuppressWarnings("rawtypes")
        Class[] classArg = new Class[2];
        classArg[0] = ExtendMinicubeMeasure.class;
        classArg[1] = Map.class;
        Object[] args = new Object[2];
        args[0] = m;
        args[1] = newMeasures;
        Method method = DirectorImpl.class.getDeclaredMethod("checkRefMeasuer", classArg);
        method.setAccessible(true);
        Object rs = method.invoke(director, args);
        boolean booleanRs = (Boolean) rs;
        Assert.assertTrue(!booleanRs);
    }

    /**
     * 使用空StarModel数组获取schema
     */
    @Test
    public void testGetSchemaWithEmptyArray() {
        Assert.assertNull(director.getSchema(new StarModel[0]));
    }

    /**
     * 不指定数据源id获取schema
     */
    @Test
    public void testGetSchemaWithNullDsId() {
        StarModel starModel = new StarModel();
        StarModel[] starModels = new StarModel[] { starModel };
        try {
            director.getSchema(starModels);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 使用数据源id获取schema
     */
    @Test
    public void testGetSchemaWithDsId() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        Assert.assertEquals(0, schema.getCubes().size());
    }

    /**
     * 事实表不含有列的情况下获取schema
     */
    @Test
    public void testGetSchemaWithFactableWithNullColumn() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        starModel.setFactTable(factTable);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(0, cube.getMeasures().size());
        Assert.assertEquals(0, cube.getDimensions().size());
    }

    /**
     * 事实表中含有一列的情况下，获取schema
     */
    @Test
    public void testGetSchemaWithFactableWithColumn() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        ColumnMetaDefine column = new ColumnMetaDefine();
        column.setName("abc");
        factTable.addColumn(column);
        starModel.setFactTable(factTable);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(1, cube.getMeasures().size());
        Assert.assertEquals(0, cube.getDimensions().size());

        Measure m = cube.getMeasures().values().toArray(new Measure[0])[0];
        Assert.assertEquals("abc", m.getName());
    }

    /**
     * starModel中不含有维表
     */
    @Test
    public void testGetSchemaWithNullDimTable() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        // 含有一个Cube，两个指标，0个维度
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(2, cube.getMeasures().size());
        Assert.assertEquals(0, cube.getDimensions().size());
    }

    /**
     * 维度为空
     */
    @Test
    public void testGetSchemaWithEmptyDimTable() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        // 标准维度
        DimTableMetaDefine dimTable = new StandardDimTableMetaDefine();
        dimTables.add(dimTable);
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        try {
            director.getSchema(starModels);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }

    }

    /**
     * 维表未指定引用关系
     */
    @Test
    public void testGetSchemaWithDimTableEmptyRef() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        DimTableMetaDefine dimTable = new StandardDimTableMetaDefine();
        dimTable.setName("dim");
        dimTables.add(dimTable);
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        try {
            director.getSchema(starModels);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 维表指定的引用关系不正确
     */
    @Test
    public void testGetSchemaWithDimTableNotCorrectRef() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        DimTableMetaDefine dimTable = new StandardDimTableMetaDefine();
        dimTable.setName("dim");
        ReferenceDefine reference = new ReferenceDefine();
        reference.setMajorColumn(null);
        reference.setMajorTable(null);
        reference.setSalveColumn(null);
        dimTable.setReference(reference);
        dimTables.add(dimTable);
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        try {
            director.getSchema(starModels);
            // Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 维表中不存在引用关系对应的列
     */
    @Test
    public void testGetSchemaWithDimTableCorrectRefNoneDimColumn() {
        StarModel starModel = new StarModel();
        starModel.setDsId("fact");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("test");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        DimTableMetaDefine dimTable = new StandardDimTableMetaDefine();
        dimTable.setName("dim");
        ReferenceDefine reference = new ReferenceDefine();
        reference.setMajorColumn("abc");
        reference.setMajorTable("fact");
        reference.setSalveColumn("aaa");
        dimTable.setReference(reference);
        dimTables.add(dimTable);
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(1, cube.getMeasures().size());
        Assert.assertEquals(0, cube.getDimensions().size());
    }

    /**
     * 维表中有正确的引用关系
     */
    @Test
    public void testGetSchemaWithDimTableCorrectRefDimColumn() {
        StarModel starModel = new StarModel();
        starModel.setDsId("fact");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("test");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("fact_abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("fact_def");
        factTable.addColumn(def);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = buildBaseDimTables();
        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        // Cube中含有一个指标、一个维度
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(1, cube.getMeasures().size());
        Assert.assertEquals(1, cube.getDimensions().size());
    }

    /**
     * 使用时间维度
     */
    @Test
    public void testGetSchemaWithTimeDimension() {
        StarModel starModel = new StarModel();
        starModel.setDsId("fact");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("test");
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("fact_abc");
        factTable.addColumn(abc);

        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("fact_def");
        factTable.addColumn(def);

        ColumnMetaDefine timeCol = new ColumnMetaDefine();
        timeCol.setName("fact_time_id");
        factTable.addColumn(timeCol);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = buildDimTablesWithTimeDim();

        starModel.setDimTables(dimTables);

        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(2, cube.getMeasures().size());
        Assert.assertEquals(1, cube.getDimensions().size());
    }

    /**
     * 使用CallBack维度
     */
    @Test
    public void testGetSchemaWithCallbackDim() {
        StarModel starModel = new StarModel();
        starModel.setDsId("fact");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("test");

        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("fact_abc");
        factTable.addColumn(abc);

        ColumnMetaDefine timeCol = new ColumnMetaDefine();
        timeCol.setName("fact_callback");
        factTable.addColumn(timeCol);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = this.buildCallbackDimTable();

        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = this.director.getSchema(starModels);

        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(1, cube.getMeasures().size());
        Assert.assertEquals(1, cube.getDimensions().size());
    }

    /**
     * 使用自定义维度获取Schema
     */
    @Test
    public void tewtGetSchemaWithUserDim() {
        StarModel starModel = new StarModel();
        starModel.setDsId("fact");
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("test");

        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("fact_abc");
        factTable.addColumn(abc);

        ColumnMetaDefine timeCol = new ColumnMetaDefine();
        timeCol.setName("fact_user");
        factTable.addColumn(timeCol);
        starModel.setFactTable(factTable);

        List<DimTableMetaDefine> dimTables = this.buildUserDefineDimTableMetaDefine();

        starModel.setDimTables(dimTables);
        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = this.director.getSchema(starModels);

        Assert.assertNotNull(schema);
        Assert.assertEquals(1, schema.getCubes().size());
        Cube cube = schema.getCubes().values().toArray(new Cube[0])[0];
        Assert.assertEquals(1, cube.getMeasures().size());
        Assert.assertEquals(1, cube.getDimensions().size());
    }

    /**
     * 
     */
    @Test
    public void testGetStarModel() {
        /**
         * 以空schema获取starModel
         */
        StarModel[] expectStarModel = new StarModel[0];
        Assert.assertArrayEquals(expectStarModel, this.director.getStarModel(null));

        /**
         * schema的cube为空
         */
        MiniCubeSchema miniCubeSchema = new MiniCubeSchema();
        miniCubeSchema.setCubes(Maps.newHashMap());
        Assert.assertArrayEquals(expectStarModel, this.director.getStarModel(miniCubeSchema));

        /**
         * schema的cube不为空
         */
        Schema schema = this.genSchema();
        StarModel[] starModels = this.director.getStarModel(schema);
        Assert.assertNotNull(starModels);
        Assert.assertEquals(1, starModels.length);
        StarModel starModel = starModels[0];

        // 有两个维度，普通维度和callback维度表、时间维度
        Assert.assertEquals(3, starModel.getDimTables().size());
        Assert.assertEquals("fact", starModel.getFactTable().getName());
    }

    /**
     * 根据星型模型修改Schema
     */
    @Test
    public void testModifySchemaWithNewModel() {
        Schema schema = this.genSchema();
        StarModel starModel = genStarModel();

        StarModel[] starModels = new StarModel[] { starModel };
        // 修改Cube对应的id，保证新的starModel同原来的starModel对应的Cube是同一个
        Map<String, ? extends Cube> cubes = schema.getCubes();
        for (Cube cube : cubes.values()) {
            starModel.setCubeId(cube.getId());
        }
        Schema newSchema = this.director.modifySchemaWithNewModel(schema, starModels);
        Assert.assertNotNull(newSchema);
        Assert.assertEquals(schema.getId(), newSchema.getId());
        Assert.assertEquals(schema.getName(), newSchema.getName());
        Assert.assertEquals(schema.getCubes().size(), newSchema.getCubes().size());
    }

    /**
     * 测试增加扩展指标
     */
    @Test
    public void testModifySchemaWithNewModelWithNewCube() {
        Schema schema = this.genSchema();
        StarModel starModel = genStarModel();

        StarModel[] starModels = new StarModel[] { starModel };
        // 修改Cube对应的id，保证新的starModel同原来的starModel对应的Cube是同一个
        Map<String, ? extends Cube> cubes = schema.getCubes();
        for (Cube cube : cubes.values()) {
            starModel.setCubeId(cube.getId());
        }
        // 增加一个扩展计算指标
        this.buildNewCube(cubes);
        Schema newSchema = this.director.modifySchemaWithNewModel(schema, starModels);

        Assert.assertNotNull(newSchema);
        Assert.assertEquals(schema.getId(), newSchema.getId());
        Assert.assertEquals(schema.getName(), newSchema.getName());
        Assert.assertEquals(schema.getCubes().size(), newSchema.getCubes().size());

        Map<String, ? extends Cube> actualCubes = newSchema.getCubes();
        Assert.assertEquals(1, actualCubes.size());
        for (Cube cube : actualCubes.values()) {
            MiniCube miniCube = (MiniCube) cube;
            Assert.assertEquals(1, miniCube.getMeasures().size());
            Assert.assertEquals(3, cube.getDimensions().size());
        }
    }

    /**
     * testModifySchemaWithNewModelDifferentCube
     */
    @Test
    public void testModifySchemaWithNewModelDifferentCube() {
        Schema schema = this.genSchema();
        StarModel starModel = genStarModel();
        StarModel[] starModels = new StarModel[] { starModel };
        Schema newSchema = this.director.modifySchemaWithNewModel(schema, starModels);
        Assert.assertNotNull(newSchema);
        Assert.assertEquals(schema.getId(), newSchema.getId());
        Assert.assertEquals(schema.getName(), newSchema.getName());
        Assert.assertEquals(schema.getCubes().size(), newSchema.getCubes().size());
    }

    /**
     * testModifySchemaWithNewModelNullCondition
     */
    @Test
    public void testModifySchemaWithNewModelNullCondition() {
        StarModel starModel = this.genStarModel();
        StarModel[] starModels = new StarModel[] { starModel };
        try {
            this.director.modifySchemaWithNewModel(null, starModels);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        Schema schema = this.genSchema();
        Assert.assertEquals(schema, this.director.modifySchemaWithNewModel(schema, null));
        MiniCubeSchema miniCubeSchema = (MiniCubeSchema) schema;
        miniCubeSchema.setCubes(null);
        try {
            this.director.modifySchemaWithNewModel(schema, starModels);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 产生schema对象
     * 
     * @return
     */
    private Schema genSchema() {
        StarModel starModel = genStarModel();

        StarModel[] starModels = new StarModel[] { starModel };
        Schema schema = director.getSchema(starModels);
        return schema;
    }

    /**
     * 构建星型模型
     * 
     * @return
     */
    private StarModel genStarModel() {
        StarModel starModel = new StarModel();
        starModel.setDsId("test");
        // 建立事实表
        FactTableMetaDefine factTable = new FactTableMetaDefine();
        factTable.setName("fact");
        // 建立事实表的第一列
        ColumnMetaDefine abc = new ColumnMetaDefine();
        abc.setName("fact_abc");
        factTable.addColumn(abc);
        // 建立事实表的第二列
        ColumnMetaDefine def = new ColumnMetaDefine();
        def.setName("fact_def");
        factTable.addColumn(def);

        // 建立callback回调列
        ColumnMetaDefine calback = new ColumnMetaDefine();
        calback.setName("fact_callback");
        factTable.addColumn(calback);
        // 建立时间列
        ColumnMetaDefine timeCol = new ColumnMetaDefine();
        timeCol.setName("fact_time_id");
        factTable.addColumn(timeCol);

        // // 建立自定义维度列
        // ColumnMetaDefine userCol = new ColumnMetaDefine();
        // userCol.setName("fact_user");
        // factTable.addColumn(userCol);

        // 设置starModel的事实表
        starModel.setFactTable(factTable);

        // 建立时间维度表
        List<DimTableMetaDefine> dimTables = buildDimTablesWithTimeDim();
        // 添加回调维度
        dimTables.addAll(this.buildCallbackDimTable());
        // 添加标准维度
        dimTables.addAll(this.buildBaseDimTables());
        // // 添加自定义维度
        // dimTables.add(buildUserDefineDimTableMetaDefine());
        // 设置维度表
        starModel.setDimTables(dimTables);

        // 设置星型模型的维度表
        starModel.setDimTables(dimTables);
        return starModel;
    }

    /**
     * 构建回调维度
     * 
     * @return
     */
    private List<DimTableMetaDefine> buildCallbackDimTable() {
        List<DimTableMetaDefine> callbackDimTable = Lists.newArrayList();
        // 建立callback回调维度
        CallbackDimTableMetaDefine callBackDim = new CallbackDimTableMetaDefine();
        callBackDim.setName("call_back");
        callBackDim.setUrl("http://host:port/callbackService");
        ColumnMetaDefine callBackCol = new ColumnMetaDefine();
        callBackCol.setName("fact_callback");
        callBackCol.setCaption("fact_callback");
        callBackDim.addColumn(callBackCol);
        // 构建回调维度对应的引用列
        ReferenceDefine callbackRef = new ReferenceDefine();
        callbackRef.setMajorColumn("fact_callback");
        callbackRef.setMajorTable("fact");
        callbackRef.setSalveColumn("gen_call_back_id");
        callBackDim.setReference(callbackRef);
        callbackDimTable.add(callBackDim);
        return callbackDimTable;
    }

    /**
     * 构建时间维度表
     * 
     * @return
     */
    private List<DimTableMetaDefine> buildDimTablesWithTimeDim() {
        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        // 时间维度表
        TimeDimTableMetaDefine timeDimTable = new TimeDimTableMetaDefine(TimeDimType.STANDARD_TIME);
        ColumnMetaDefine timeId = new ColumnMetaDefine();
        timeId.setName(TimeType.TimeMonth.toString());
        timeId.setCaption(TimeType.TimeMonth.toString());
        timeDimTable.addColumn(timeId);
        timeDimTable.setName("time");
        // 构建时间维度对应的事实表引用列
        ReferenceDefine timeReference = new ReferenceDefine();
        timeReference.setMajorColumn("fact_time_id");
        timeReference.setMajorTable("fact");
        timeReference.setSalveColumn(TimeType.TimeMonth.toString());
        timeDimTable.setReference(timeReference);
        dimTables.add(timeDimTable);
        return dimTables;
    }

    /**
     * 构建基本维度表
     * 
     * @return
     */
    private List<DimTableMetaDefine> buildBaseDimTables() {
        List<DimTableMetaDefine> dimTables = Lists.newArrayList();
        DimTableMetaDefine dimTable = new StandardDimTableMetaDefine();
        dimTable.setName("dim");
        ColumnMetaDefine dimColumn = new ColumnMetaDefine();
        dimColumn.setName("dim_abc");
        dimColumn.setCaption("dim_abc");
        dimTable.addColumn(dimColumn);
        ColumnMetaDefine anotherDimCol = new ColumnMetaDefine();
        anotherDimCol.setName("dim_def");
        anotherDimCol.setCaption("dim_def");
        dimTable.addColumn(anotherDimCol);
        ReferenceDefine reference = new ReferenceDefine();
        reference.setMajorColumn("fact_abc");
        reference.setMajorTable("fact");
        reference.setSalveColumn("dim_abc");
        dimTable.setReference(reference);
        dimTables.add(dimTable);
        return dimTables;
    }

    /**
     * 构建用户自定义维度表
     * 
     * @return
     */
    private List<DimTableMetaDefine> buildUserDefineDimTableMetaDefine() {
        List<DimTableMetaDefine> dimTable = Lists.newArrayList();
        UserDefineDimTableMetaDefine ud = new UserDefineDimTableMetaDefine();
        ColumnMetaDefine dimColumn = new ColumnMetaDefine();
        dimColumn.setName("dim_user");
        dimColumn.setCaption("dim_user");
        ud.setSourceType(DimSourceType.SQL);
        ud.addColumn(dimColumn);
        ReferenceDefine reference = new ReferenceDefine();
        reference.setMajorColumn("fact_user");
        reference.setMajorTable("fact");
        reference.setSalveColumn("dim_user");
        ud.setReference(reference);
        ud.setParams(Maps.newHashMap());
        dimTable.add(ud);
        return dimTable;
    }

    /**
     * 建立新的Cube
     */
    private void buildNewCube(Map<String, ? extends Cube> cubes) {
        for (Cube cube : cubes.values()) {
            MiniCube miniCube = (MiniCube) cube;
            Map<String, Measure> measures = miniCube.getMeasures();
            for (Measure measure : measures.values()) {

                MiniCubeMeasure miniMeasure = (MiniCubeMeasure) measure;
                miniMeasure.setAggregator(Aggregator.COUNT);
            }

            // 增加扩展指标
            ExtendMinicubeMeasure extendMeasure = new ExtendMinicubeMeasure("CAL");
            extendMeasure.setAggregator(Aggregator.COUNT);
            extendMeasure.setId("id");
            extendMeasure.setCaption("CAL");
            extendMeasure.setType(MeasureType.CAL);
            extendMeasure.setVisible(true);
            extendMeasure.setFormula("${cash}/${csm}");
            Set<String> refIndName = Sets.newHashSet();
            refIndName.add("fact_def");
            extendMeasure.setRefIndNames(refIndName);
            measures.put("id", extendMeasure);
            miniCube.setMeasures(measures);

            // 修改维度为维度组
            Map<String, Dimension> dimensions = miniCube.getDimensions();
            for (Dimension dimension : dimensions.values()) {
                switch (dimension.getType()) {
                    case STANDARD_DIMENSION:
                        StandardDimension standardDimension = (StandardDimension) dimension;
                        standardDimension.setType(DimensionType.GROUP_DIMENSION);
                        break;
                    case TIME_DIMENSION:
                    case CALLBACK:
                    case GROUP_DIMENSION:
                        break;
                    default:
                        break;
                }
            }
            miniCube.setDimensions(dimensions);
        }
    }
}
