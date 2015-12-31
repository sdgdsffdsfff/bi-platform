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
package com.baidu.rigel.biplatform.ma.resource.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeLevel;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMeasure;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeSchema;
import com.baidu.rigel.biplatform.ac.minicube.StandardDimension;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.Measure;
import com.baidu.rigel.biplatform.ac.query.data.DataModel;
import com.baidu.rigel.biplatform.ac.query.data.HeadField;
import com.baidu.rigel.biplatform.ac.query.data.TableData;
import com.baidu.rigel.biplatform.ac.query.data.TableData.Column;
import com.baidu.rigel.biplatform.ma.report.model.ExtendArea;
import com.baidu.rigel.biplatform.ma.report.model.ExtendAreaType;
import com.baidu.rigel.biplatform.ma.report.model.FormatModel;
import com.baidu.rigel.biplatform.ma.report.model.Item;
import com.baidu.rigel.biplatform.ma.report.model.LinkInfo;
import com.baidu.rigel.biplatform.ma.report.model.LogicModel;
import com.baidu.rigel.biplatform.ma.report.model.ReportDesignModel;
import com.baidu.rigel.biplatform.ma.report.query.QueryAction;
import com.baidu.rigel.biplatform.ma.report.query.QueryAction.OrderDesc;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.CellData;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.ColDefine;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.PivotTable;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.PlaneTable;
import com.baidu.rigel.biplatform.ma.report.utils.QueryUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * DataModel操作工具测试类
 *
 * @author david.wang
 * @version 1.0.0.1
 */
@RunWith(PowerMockRunner.class)
public class DataModelUtilsTest {

    /**
     * 
     */
    @Test
    public void testTransDataModel2PivotTableWithNullModel() {
        try {
            DataModelUtils.transDataModel2PivotTable(null, null, true, 100, true, null);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     */
    @Test
    @PrepareForTest(QueryUtils.class)
    public void testTransDataModel2PivotTableWithHideWhiteRow() throws Exception {
        PowerMockito.mockStatic(QueryUtils.class);

        DataModel dataModel = Mockito.mock(DataModel.class);
        Cube cube = Mockito.mock(Cube.class);
        PowerMockito.when(QueryUtils.transformCube(cube)).thenReturn(cube);
        long begin = System.currentTimeMillis();
        DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, true, null);
        long end = System.currentTimeMillis() - begin;
        begin = System.currentTimeMillis();
        DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
        long endAnohter = System.currentTimeMillis() - begin;
        Assert.assertTrue(end > endAnohter);
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testTransDataModel2PivotTableWithNullRowHeadFields() {
        try {
            DataModel dataModel = Mockito.mock(DataModel.class);
            dataModel.setRowHeadFields(null);
            Cube cube = Mockito.mock(Cube.class);
            PivotTable table = DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
            Assert.assertTrue(table.getRowHeadFields().size() == 0);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testTransDataModel2PivotTableWithEmptyRowHeadFields() {
        try {
            DataModel dataModel = Mockito.mock(DataModel.class);
            dataModel.setRowHeadFields(Lists.newArrayList());
            Cube cube = Mockito.mock(Cube.class);
            PivotTable table = DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
            Assert.assertTrue(table.getRowHeadFields().size() == 0);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testTransDataModel2PivotTableWithNullColHeadFields() {
        try {
            DataModel dataModel = Mockito.mock(DataModel.class);
            dataModel.setColumnHeadFields(null);
            Cube cube = Mockito.mock(Cube.class);
            PivotTable table = DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
            Assert.assertTrue(table.getColFields().size() == 0);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testTransDataModel2PivotTableWithEmptyColHeadFields() {
        try {
            DataModel dataModel = Mockito.mock(DataModel.class);
            dataModel.setColumnHeadFields(Lists.newArrayList());
            Cube cube = Mockito.mock(Cube.class);
            PivotTable table = DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
            Assert.assertTrue(table.getColFields().size() == 0);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     */
    @Test
    @PrepareForTest(QueryUtils.class)
    public void testTransDataModel2PivotTable() throws Exception {
        PowerMockito.mockStatic(QueryUtils.class);
        Cube mockCube = Mockito.mock(Cube.class);
        PowerMockito.when(QueryUtils.transformCube(Mockito.any())).thenReturn(mockCube);
        DataModel dataModel = new DataModel();
        List<HeadField> columnHeader = new ArrayList<HeadField>();
        // int c
        String[] trades = { "房屋出租", "房地产其他", "皮革", "服装", "鞋帽", "纺织辅料", "服装鞋帽其他", "工艺品", "礼品", "饰品" };

        String[] cols = { "消费", "点击", "访问人数", "人均消费", "平均价格", "总量", "分量", "单位产出", "月均订单数", "收入" };

        for (int i = 0; i < 10; ++i) {
            HeadField header = new HeadField(null);
            header.setCaption(cols[i]);
            header.setValue("[Measures].[column_" + i + "]");
            columnHeader.add(header);
        }
        dataModel.setColumnHeadFields(columnHeader);
        List<HeadField> rowHeader = new ArrayList<HeadField>();
        for (int i = 0; i < 10; ++i) {
            HeadField header = new HeadField(null);
            header.setCaption(trades[i]);
            header.setValue("[Dmension].[row_" + i + "]");
            rowHeader.add(header);
        }
        dataModel.setRowHeadFields(rowHeader);
        List<List<BigDecimal>> datas = new ArrayList<List<BigDecimal>>();

        for (int i = 0; i < 10; ++i) {
            List<BigDecimal> cellDatas = new ArrayList<BigDecimal>();
            for (int j = 0; j < 10; ++j) {
                Random random = new Random();
                cellDatas.add(BigDecimal.valueOf(random.nextInt(10000)));
            }
            datas.add(cellDatas);
        }
        dataModel.setColumnBaseData(datas);
        Cube cube = Mockito.mock(Cube.class);
        PivotTable pivotTable = DataModelUtils.transDataModel2PivotTable(cube, dataModel, true, 100, false, null);
        Assert.assertNotNull(pivotTable);
        Assert.assertEquals(1, pivotTable.getColFields().size());
        Assert.assertEquals(10, pivotTable.getRowHeadFields().size());
    }

    /**
     * 
     */
    @Test
    public void testDecorateTableWithNullTable() {
        FormatModel formatModel = new FormatModel();
        Map<String, String> dataFormat = formatModel.getDataFormat();
        dataFormat.put("a", "1");
        try {
            DataModelUtils.decorateTable(formatModel, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     */
    @Test
    public void testDecorateTableWithNullData() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = Mockito.mock(PivotTable.class);
        Mockito.doReturn(null).when(table).getColDefine();
        formatModel.getDataFormat().put("a", "1");
        try {
            DataModelUtils.decorateTable(formatModel, table);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     */
    @Test
    public void testDecorateTableWithEmptyFormate() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = Mockito.mock(PivotTable.class);
        Mockito.doReturn(Lists.newArrayList()).when(table).getColDefine();
        try {
            DataModelUtils.decorateTable(formatModel, table);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * 
     */
    @Test
    public void testDecorateTable() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = new PivotTable();
        List<ColDefine> newArrayList = Lists.newLinkedList();
        ColDefine colDefine = new ColDefine();
        colDefine.setCaption("test");
        colDefine.setUniqueName("[Measures].[a]");
        colDefine.setOlapElementId("testOlapElementId");
        newArrayList.add(colDefine);
        table.setColDefine(newArrayList);
        formatModel.getDataFormat().put("a", "abc");
        Map<String, LinkInfo> linkInfoMap = Maps.newHashMap();
        LinkInfo linkInfo = new LinkInfo();
        linkInfo.setPlaneTableId("testPlaneTableId");
        Map<String, String> paramMapping = Maps.newHashMap();
        paramMapping.put("aa", "bb");
        linkInfo.setParamMapping(paramMapping);
        linkInfoMap.put("testOlapElementId", linkInfo);
        formatModel.setLinkInfo(linkInfoMap);
        try {
            DataModelUtils.decorateTable(formatModel, table);
            Assert.assertEquals("abc", table.getColDefine().get(0).getFormat());
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    /**
     * 
     */
    @Test
    public void testDecorateTableIsShowZero() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = new PivotTable();
        table.setDataSourceColumnBased(parseCellDatas(buildDataModel().getColumnBaseData()));
        ExtendArea targetArea = Mockito.mock(ExtendArea.class);
        Mockito.doReturn(ExtendAreaType.TABLE).when(targetArea).getType();
        ReportDesignModel reportDesignModel = Mockito.mock(ReportDesignModel.class);
        try {
            DataModelUtils.decorateTable(formatModel, table, targetArea, reportDesignModel);
            BigDecimal actual = table.getDataSourceColumnBased().get(0).get(0).getV();
            Assert.assertEquals(BigDecimal.ZERO, actual);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * 
     */
    @Test
    public void testDecorateTableWithToolTips() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = new PivotTable();
        List<ColDefine> newArrayList = Lists.newLinkedList();
        ColDefine colDefine = new ColDefine();
        colDefine.setCaption("test");
        colDefine.setUniqueName("[Measures].[a]");
        newArrayList.add(colDefine);
        table.setColDefine(newArrayList);
        formatModel.getToolTips().put("a", "abc");
        try {
            DataModelUtils.decorateTable(formatModel, table);
            Assert.assertEquals("abc", table.getColDefine().get(0).getToolTip());
            Assert.assertNull(table.getColDefine().get(0).getFormat());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testDecorateTableWithEmptyToolTips() {
        FormatModel formatModel = new FormatModel();
        PivotTable table = new PivotTable();
        List<ColDefine> newArrayList = Lists.newLinkedList();
        ColDefine colDefine = new ColDefine();
        colDefine.setCaption("test");
        colDefine.setUniqueName("[Measures].[a]");
        newArrayList.add(colDefine);
        table.setColDefine(newArrayList);
        try {
            DataModelUtils.decorateTable(formatModel, table);
            Assert.assertEquals("a", table.getColDefine().get(0).getToolTip());
            Assert.assertNull(table.getColDefine().get(0).getFormat());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testDecorateTableWithDefaultFormat() {
        FormatModel formatModel = new FormatModel();
        formatModel.getDataFormat().put("defaultFormat", "abc");
        PivotTable table = new PivotTable();
        List<ColDefine> newArrayList = Lists.newLinkedList();
        ColDefine colDefine = new ColDefine();
        colDefine.setCaption("test");
        colDefine.setUniqueName("[Measures].[a]");
        newArrayList.add(colDefine);
        table.setColDefine(newArrayList);
        try {
            DataModelUtils.decorateTable(formatModel, table);
            Assert.assertEquals("a", table.getColDefine().get(0).getToolTip());
            Assert.assertEquals("abc", table.getColDefine().get(0).getFormat());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testMergedataModelWithRowNum() {
        DataModel dataModel = Mockito.mock(DataModel.class);
        DataModel otherDataModel = Mockito.mock(DataModel.class);

        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, -1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }

        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, Integer.MAX_VALUE);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, dataModel.getRowHeadFields().size());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testMerageDataModelWithNullModel() {
        DataModel dataModel = Mockito.mock(DataModel.class);
        DataModel otherDataModel = Mockito.mock(DataModel.class);
        try {
            DataModelUtils.merageDataModel(null, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        try {
            DataModelUtils.merageDataModel(dataModel, null, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testMerageDataModelWithNullChildren() {
        DataModel dataModel = Mockito.mock(DataModel.class);
        DataModel otherDataModel = Mockito.mock(DataModel.class);
        dataModel.setRowHeadFields(null);
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        dataModel = Mockito.mock(DataModel.class);
        otherDataModel.setRowHeadFields(null);
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testMerageDataModelWithEmptyChildren() {
        DataModel dataModel = Mockito.mock(DataModel.class);
        DataModel otherDataModel = Mockito.mock(DataModel.class);
        dataModel.setRowHeadFields(Lists.newArrayList());
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        dataModel = Mockito.mock(DataModel.class);
        otherDataModel.setRowHeadFields(Lists.newArrayList());
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testMergeDataModel() {
        DataModel dataModel = new DataModel();
        DataModel otherDataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        HeadField headField = new HeadField();
        rowHeadFields.add(headField);
        dataModel.setRowHeadFields(rowHeadFields);
        otherDataModel.setRowHeadFields(rowHeadFields);
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testMergeDataModelWithData() {
        DataModel dataModel = new DataModel();
        DataModel otherDataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        HeadField headField = new HeadField();
        rowHeadFields.add(headField);
        dataModel.setRowHeadFields(rowHeadFields);
        List<List<BigDecimal>> columnBaseData = Lists.newArrayList();
        List<BigDecimal> datas = Lists.newArrayList();
        datas.add(BigDecimal.ZERO);
        datas.add(BigDecimal.ONE);
        columnBaseData.add(datas);
        dataModel.setColumnBaseData(columnBaseData);
        otherDataModel.setRowHeadFields(rowHeadFields);
        otherDataModel.setColumnBaseData(columnBaseData);
        try {
            DataModel rs = DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.assertEquals(1, rs.getColumnBaseData().size());
            Assert.assertEquals(3, rs.getColumnBaseData().get(0).size());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testMergeDataModelEmptyData() {
        DataModel dataModel = new DataModel();
        DataModel otherDataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        dataModel.setRowHeadFields(rowHeadFields);
        otherDataModel.setRowHeadFields(rowHeadFields);
        try {
            DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testMergeDataModelEmptyRowHead() {
        DataModel dataModel = new DataModel();
        DataModel otherDataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        HeadField headField = new HeadField();
        headField.setCaption("a");
        HeadField child = new HeadField();
        child.setCaption("b");
        headField.getChildren().add(child);
        rowHeadFields.add(headField);
        dataModel.setRowHeadFields(rowHeadFields);
        otherDataModel.setRowHeadFields(rowHeadFields);
        try {
            DataModel rs = DataModelUtils.merageDataModel(dataModel, otherDataModel, 0);
            Assert.assertEquals(0, rs.getColumnBaseData().size());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * 
     */
    @Test
    public void testRmDataFromDataModelWithNull() {
        try {
            DataModelUtils.removeDataFromDataModel(null, 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 
     */
    @Test
    public void testRmDataFromDataModelWrongRow() {
        try {
            DataModelUtils.removeDataFromDataModel(new DataModel(), -1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
        try {
            DataModelUtils.removeDataFromDataModel(new DataModel(), Integer.MAX_VALUE);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testRmDataFromDataModelEmptyHeader() {
        try {
            DataModelUtils.removeDataFromDataModel(new DataModel(), 0);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testRmDataFromDataModelHeader() {
        DataModel dataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        HeadField headField = new HeadField();
        rowHeadFields.add(headField);
        dataModel.setRowHeadFields(rowHeadFields);
        try {
            DataModel rs = DataModelUtils.removeDataFromDataModel(dataModel, 0);
            Assert.assertEquals(0, rs.getColumnBaseData().size());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testRmDataFromDataModel() {
        DataModel dataModel = new DataModel();
        List<HeadField> rowHeadFields = Lists.newArrayList();
        HeadField headField = new HeadField();
        headField.getChildren().add(new HeadField());
        rowHeadFields.add(headField);
        dataModel.setRowHeadFields(rowHeadFields);
        try {
            List<List<BigDecimal>> columnBaseData = Lists.newArrayList();
            List<BigDecimal> datas = Lists.newArrayList();
            datas.add(BigDecimal.ZERO);
            datas.add(BigDecimal.ONE);
            columnBaseData.add(datas);
            dataModel.setColumnBaseData(columnBaseData);
            DataModel rs = DataModelUtils.removeDataFromDataModel(dataModel, 1);
            Assert.assertEquals(2, rs.getColumnBaseData().get(0).size());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testConvert2StringWithEmptyModel() {
        try {
            Cube cube = Mockito.mock(Cube.class);
            DataModelUtils.convertDataModel2CsvString(cube, null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testConvert2StrWithEmptyCube() {
        try {
            DataModel dataModel = new DataModel();
            List<HeadField> rowHeadFields = Lists.newArrayList();
            HeadField headField = new HeadField();
            headField.setCaption("test");
            headField.setNodeUniqueName("[test].[test]");
            dataModel.setRowHeadFields(rowHeadFields);
            String str = DataModelUtils.convertDataModel2CsvString(null, dataModel, null);
            Assert.assertEquals("", str);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testConvert2Str() {
        try {
            Cube cube = Mockito.mock(Cube.class);
            DataModel dataModel = new DataModel();
            List<HeadField> rowHeadFields = Lists.newArrayList();
            HeadField headField = new HeadField();
            headField.setCaption("a");
            headField.setValue("[dim].[a]");
            rowHeadFields.add(headField);
            dataModel.setRowHeadFields(rowHeadFields);
            List<List<BigDecimal>> columnBaseData = Lists.newArrayList();
            List<BigDecimal> datas = Lists.newArrayList();
            datas.add(BigDecimal.ZERO);
            columnBaseData.add(datas);
            dataModel.setColumnBaseData(columnBaseData);
            String str = DataModelUtils.convertDataModel2CsvString(cube, dataModel, null);
            Assert.assertNotNull (str);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    /**
     * 测试平面表转换
     */
    @Test
    public void testParseToPlaneTable() throws Exception {
        // 构建假定的DataModel
        DataModel dataModel = buildDataModel();
        LogicModel logicModel = new LogicModel();
        Item item1 = new Item();
        item1.setId("id1");
        item1.setOlapElementId("id1");
        Item item2 = new Item();
        item2.setId("id2");
        item2.setOlapElementId("id2");
        logicModel.addColumn(item1);
        logicModel.addColumn(item2);

        MiniCube cube = new MiniCube();
        Map<String, Dimension> dimensions = Maps.newHashMap();
        StandardDimension dimension = new StandardDimension("test_Dim");
        MiniCubeLevel l = new MiniCubeLevel();
        l.setDimTable("test");
        l.setName("Dim");
        dimension.addLevel(l);

        dimension.setId("id1");
        dimensions.put("test_Dim", dimension);

        Map<String, Measure> measures = Maps.newHashMap();
        MiniCubeMeasure measure = new MiniCubeMeasure("Measure");
        measure.setId("id2");
        measure.setDefine("Measure");
        measures.put("Measure", measure);

        cube.setId("testCubeId");
        cube.setSource("test");
        cube.setMeasures(measures);
        cube.setDimensions(dimensions);

        MiniCubeSchema schema = new MiniCubeSchema();
        Map<String, MiniCube> cubes = Maps.newHashMap();
        cubes.put("testCubeId", cube);
        schema.setCubes(cubes);
        cube.setSchema(schema);

        FormatModel formatModel = PowerMockito.mock(FormatModel.class);
        QueryAction queryAction = new QueryAction();
        OrderDesc orderDesc = new OrderDesc("Measure", "DESC", 500);
        queryAction.setOrderDesc(orderDesc);

        PlaneTable planeTable =
                DataModelUtils.transDataModel2PlaneTable(cube, dataModel, logicModel, formatModel, queryAction);

        Assert.assertNotNull(planeTable);
        Assert.assertNotNull(planeTable.getColDefines());
        Assert.assertNotNull(planeTable.getData());
        Assert.assertEquals(2, planeTable.getColDefines().size());
        Assert.assertEquals(1, planeTable.getData().size());

        // 测试其他null条件下，转换结果
        PlaneTable expectPlaneTable = new PlaneTable();
        Assert.assertEquals(expectPlaneTable.getData(),
                DataModelUtils.transDataModel2PlaneTable(cube, null, logicModel, formatModel, queryAction).getData());
        Assert.assertEquals(expectPlaneTable.getData(),
                DataModelUtils.transDataModel2PlaneTable(cube, dataModel, null, formatModel, queryAction).getData());
        Assert.assertEquals(expectPlaneTable.getData(),
                DataModelUtils.transDataModel2PlaneTable(cube, dataModel, logicModel, null, queryAction).getData());

        // 没有数据
        dataModel.setTableData(null);
        Assert.assertEquals(expectPlaneTable.getData(),
                DataModelUtils.transDataModel2PlaneTable(cube, dataModel, logicModel, formatModel, queryAction)
                        .getData());

    }

    /**
     * 测试
     * testDecoratePlaneTable
     */
    @Test
    public void testDecoratePlaneTable() {
        PlaneTable planeTable = this.buildPlaneTable();
        List<Map<String, String>> expect = this.buildTableData(new String[] {"key1", "key2"}, new String[] {"0",  "0"});
        DataModelUtils.decoratePlaneTable(planeTable, true);
        List<Map<String, String>> actual = planeTable.getData();
        Assert.assertEquals(expect, actual);
        
        
    }
    
    /**
     * 构建平面表
     * buildPlaneTable
     * @return
     */
    private PlaneTable buildPlaneTable() {
        PlaneTable planeTable = new PlaneTable();
        planeTable.setData(this.buildTableData(new String[] {"key1", "key2"}, new String[] {"",  "0"}));
        return planeTable;
    }
    
    /**
     * 构建平面表的测试tableData
     * buildTableData
     * @return
     */
    private List<Map<String, String>> buildTableData(String[] key, String[] value) {
        List<Map<String, String>> datas = Lists.newArrayList();
        Map<String, String> data = Maps.newHashMap();
        data.put(key[0], value[0]);
        data.put(key[1], value[1]);
        datas.add(data);
        return datas;
    }
    
    /**
     * 构建DataModel buildDataModel
     * 
     * @return
     */
    private DataModel buildDataModel() {
        DataModel dataModel = new DataModel();
        TableData tableData = new TableData();
        // 构建多维数据
        List<List<BigDecimal>> multiDatas = Lists.newArrayList();
        List<BigDecimal> multiData = Lists.newArrayList();
        multiData.add(null);
        multiData.add(null);
        multiDatas.add(multiData);
        dataModel.setColumnBaseData(multiDatas);
        
        // 构建平面表数据
        Column columnDim = new Column("test.Dim", "Dim", "captionDim", "varchar", "test");
        Column columnMeasure = new Column("test.Measure", "Measure", "captionMeasure", "varchar", "test");
        List<Column> columns = Lists.newArrayList();
        columns.add(columnDim);
        columns.add(columnMeasure);
        tableData.setColumns(columns);

        Map<String, List<String>> data = Maps.newHashMap();
        List<String> data1 = Lists.newArrayList();
        data1.add("Dim");
        data.put("test.Dim", data1);

        List<String> data2 = Lists.newArrayList();
        data2.add("Measure");
        data.put("test.Measure", data2);

        tableData.setColBaseDatas(data);
        dataModel.setTableData(tableData);
        return dataModel;
    }
    
    @Test
    public void testGetDimCaption() {
        String[] rs = DataModelUtils.getDimCaptions (null, null);
        Assert.assertEquals (0, rs.length);
        Cube cube = Mockito.mock (Cube.class);
        rs = DataModelUtils.getDimCaptions (cube, null);
        Assert.assertEquals (0, rs.length);
        LogicModel model = Mockito.mock (LogicModel.class);
        rs = DataModelUtils.getDimCaptions (cube, model);
        Assert.assertEquals (0, rs.length);
        Item[] items = new Item[1];
        items[0] = new Item();
        items[0].setOlapElementId ("1");
        Map<String, Dimension> dims = Maps.newHashMap ();
        Dimension dim = Mockito.mock (Dimension.class);
        Mockito.doReturn ("a").when (dim).getCaption ();
        dims.put ("1", dim);
        Mockito.doReturn (dims).when (cube).getDimensions ();
        Mockito.doReturn (items).when (model).getRows ();
        rs = DataModelUtils.getDimCaptions (cube, model);
        Assert.assertEquals (1, rs.length);
    }
    
    @Test
    public void testIsNumeric() {
        Assert.assertTrue(DataModelUtils.isNumeric("123123123.00000"));
        Assert.assertTrue(DataModelUtils.isNumeric("123123123.234234"));
        Assert.assertTrue(DataModelUtils.isNumeric("2134523453.3453450"));
        Assert.assertTrue(DataModelUtils.isNumeric("3.0"));
        Assert.assertTrue(DataModelUtils.isNumeric("00000.000000"));
        Assert.assertTrue(DataModelUtils.isNumeric("-00000.000000"));
        Assert.assertTrue(DataModelUtils.isNumeric("1000.0001000"));
        Assert.assertTrue(DataModelUtils.isNumeric("0.0001000"));
        Assert.assertTrue(DataModelUtils.isNumeric("0.0"));
        Assert.assertTrue(DataModelUtils.isNumeric("0"));
        Assert.assertTrue(DataModelUtils.isNumeric("1345345345345"));
        Assert.assertTrue(DataModelUtils.isNumeric("-3"));
        Assert.assertTrue(DataModelUtils.isNumeric("-36"));
        Assert.assertTrue(DataModelUtils.isNumeric("1"));
        Assert.assertTrue(DataModelUtils.isNumeric("9"));
        Assert.assertTrue(DataModelUtils.isNumeric("23"));
        Assert.assertTrue(DataModelUtils.isNumeric("-3.01"));
        
        Assert.assertTrue(!DataModelUtils.isNumeric(""));
        Assert.assertTrue(!DataModelUtils.isNumeric(" "));
        Assert.assertTrue(!DataModelUtils.isNumeric("a"));
        Assert.assertTrue(!DataModelUtils.isNumeric("-a"));
        Assert.assertTrue(!DataModelUtils.isNumeric("d3.0"));
        Assert.assertTrue(!DataModelUtils.isNumeric("3.0b"));
        Assert.assertTrue(!DataModelUtils.isNumeric("dfdfb"));
        Assert.assertTrue(!DataModelUtils.isNumeric("中文"));
        Assert.assertTrue(!DataModelUtils.isNumeric("中文.0"));
        Assert.assertTrue(!DataModelUtils.isNumeric("0.99.1"));
    }
    /**
     * 将数据转为cellData
     * parseCellDatas
     * @param source
     * @return
     */
    private static List<List<CellData>> parseCellDatas(List<List<BigDecimal>> source) {
        List<List<CellData>> cellDatas = Lists.newArrayList();
        for (List<BigDecimal> sourcePiece : source) {
            List<CellData> cellRow = Lists.newArrayList();
            for (BigDecimal data : sourcePiece) {
                cellRow.add(parseCellData(data));
            }
            cellDatas.add(cellRow);
        }
        return cellDatas;
    }

    /**
     * 将数据转为cellData
     * parseCellData
     * @param value
     * @return
     */
    private static CellData parseCellData(BigDecimal value) {
        CellData data = new CellData();
        data.setCellId("");
        data.setFormattedValue("I,III.DD");
        if (value != null) {
            value = value.setScale(8, RoundingMode.HALF_UP);
            data.setV(value);
        } else {
            data.setV(value);
        }
        return data;
    }
    
}
