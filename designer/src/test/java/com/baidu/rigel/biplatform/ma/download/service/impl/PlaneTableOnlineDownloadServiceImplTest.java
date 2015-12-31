package com.baidu.rigel.biplatform.ma.download.service.impl;

import java.util.List;
import java.util.Map;

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
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.Measure;
import com.baidu.rigel.biplatform.ac.query.MiniCubeConnection;
import com.baidu.rigel.biplatform.ac.query.MiniCubeDriverManager;
import com.baidu.rigel.biplatform.ac.query.data.DataModel;
import com.baidu.rigel.biplatform.ac.query.data.TableData;
import com.baidu.rigel.biplatform.ac.query.data.TableData.Column;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.ma.download.DownloadType;
import com.baidu.rigel.biplatform.ma.download.service.DownloadServiceFactory;
import com.baidu.rigel.biplatform.ma.download.service.DownloadTableDataService;
import com.baidu.rigel.biplatform.ma.model.consts.Constants;
import com.baidu.rigel.biplatform.ma.report.model.Item;
import com.baidu.rigel.biplatform.ma.report.model.LogicModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 测试平面表在线下载
 * 
 * @author yichao.jiang 2015年5月29日 上午9:34:10
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MiniCubeDriverManager.class })
public class PlaneTableOnlineDownloadServiceImplTest {

    /**
     * 测试
     */
    @Test
    public void testDownloadForPlaneTableOnline() throws Exception {
        DownloadType downloadType = DownloadType.PLANE_TABLE_ONLINE;
        DownloadTableDataService downService = DownloadServiceFactory.getDownloadTableDataService(downloadType);

        // Mock一些数据对象
        LogicModel logicModel = PowerMockito.mock(LogicModel.class);
        ConfigQuestionModel questionModel = PowerMockito.mock(ConfigQuestionModel.class);
        MiniCubeConnection connection = PowerMockito.mock(MiniCubeConnection.class);
        DataModel dataModel = PowerMockito.mock(DataModel.class);
        // 假定条件
        PowerMockito.mockStatic(MiniCubeDriverManager.class);
        PowerMockito.when(MiniCubeDriverManager.getConnection(Mockito.anyObject())).thenReturn(connection);
        PowerMockito.when(connection.query(questionModel)).thenReturn(dataModel);

        // DataModel中的TableData没有数据
        String csvString = downService.downloadTableData(questionModel, logicModel);
        Assert.assertEquals("", csvString);

        DataModel dataModelNew = this.buildDataModel();
        logicModel = this.buildLogicModel();

        questionModel = new ConfigQuestionModel();
        questionModel.setCube(this.buildCube());
        PowerMockito.when(connection.query(questionModel)).thenReturn(dataModelNew);

        csvString = downService.downloadTableData(questionModel, logicModel);

        String expectCsvString = "\tcaptionDim,\tcaptionMeasure\r\n";
        expectCsvString = expectCsvString + "0,0\r\n";
        Assert.assertEquals(expectCsvString, csvString);
        Assert.assertNotNull(csvString);
        
        
        Map<String, Object> setting = Maps.newHashMap();
        setting.put(Constants.IS_SHOW_ZERO, "true");
        
        expectCsvString = "\tcaptionDim,\tcaptionMeasure\r\n";
        expectCsvString = expectCsvString + "0,0\r\n";
        
        csvString = downService.downloadTableData(questionModel, logicModel, setting);
        Assert.assertEquals(expectCsvString, csvString);
        Assert.assertNotNull(csvString);
    }

    /**
     * 构建数据模型 buildDataModel
     * 
     * @return
     */
    private DataModel buildDataModel() {
        DataModel dataModelNew = new DataModel();
        TableData tableData = new TableData();
        Column columnDim = new Column("test.Dim", "Dim", "captionDim", "varchar", "test");
        Column columnMeasure = new Column("test.Measure", "Measure", "captionMeasure", "varchar", "test");
        List<Column> columns = Lists.newArrayList();
        columns.add(columnDim);
        columns.add(columnMeasure);
        tableData.setColumns(columns);

        Map<String, List<String>> data = Maps.newHashMap();
        List<String> data1 = Lists.newArrayList();
        data1.add("");
        data.put("test.Dim", data1);

        List<String> data2 = Lists.newArrayList();
        data2.add("0");
        data.put("test.Measure", data2);

        tableData.setColBaseDatas(data);
        dataModelNew.setTableData(tableData);
        return dataModelNew;
    }

    /**
     * 构建LogicModel模型 buildLogicModel
     * 
     * @return
     */
    private LogicModel buildLogicModel() {
        LogicModel logicModel = new LogicModel();
        Item item1 = new Item();
        item1.setId("id1");
        item1.setOlapElementId("id1");
        Item item2 = new Item();
        item2.setId("id2");
        item2.setOlapElementId("id2");
        logicModel.addColumn(item1);
        logicModel.addColumn(item2);
        return logicModel;
    }

    /**
     * 构建Cube buildCube
     * 
     * @return
     */
    private MiniCube buildCube() {
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
        return cube;
    }
}
