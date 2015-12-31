package com.baidu.rigel.biplatform.tesseract.isservice.index.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeLevel;
import com.baidu.rigel.biplatform.ac.minicube.TimeDimension;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Measure;
import com.baidu.rigel.biplatform.ac.model.MeasureType;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.data.impl.SqlDataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.cache.StoreManager;
import com.baidu.rigel.biplatform.tesseract.config.IndexConfig;
import com.baidu.rigel.biplatform.tesseract.isservice.IndexTestBase;
import com.baidu.rigel.biplatform.tesseract.isservice.index.service.impl.IndexMetaServiceImpl;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.DataDescInfo;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexMeta;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexShard;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexState;
import com.baidu.rigel.biplatform.tesseract.node.meta.Node;
import com.baidu.rigel.biplatform.tesseract.node.service.IsNodeService;
import com.baidu.rigel.biplatform.tesseract.util.FileUtils;

/**
 * IndexMetaServiceTest
 * @author lijin
 *
 */
public class IndexMetaServiceTest extends IndexTestBase {
    @InjectMocks
    private IndexMetaServiceImpl indexMetaService;
    @Mock
    private StoreManager storeManager;
    @Mock
    private IndexConfig indexConfig;
    
    @Mock
    private IsNodeService isNodeService;
    
    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testInitMiniCubeIndexMetaBoundaryTest() throws Exception {
        List<Cube> cubeList = null;
        DataSourceInfo dataSourceInfo = new SqlDataSourceInfo("aaa");
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(dataSourceInfo);
        List<IndexMeta> result = null;
        // cubeList == null
        result = this.indexMetaService.initMiniCubeIndexMeta(cubeList, dataSourceInfoList);
        Assert.assertEquals(0, result.size());
        result = this.indexMetaService.initMiniCubeIndexMeta(cubeList, null);
        Assert.assertEquals(0, result.size());
        // cubeList.size() == 0
        cubeList = new ArrayList<Cube>();
        result = this.indexMetaService.initMiniCubeIndexMeta(cubeList, dataSourceInfoList);
        Assert.assertEquals(0, result.size());
        result = this.indexMetaService.initMiniCubeIndexMeta(cubeList, null);
        Assert.assertEquals(0, result.size());
        // dataSourceInfo == null        
        Assert.assertEquals(0, result.size());
        result = this.indexMetaService.initMiniCubeIndexMeta(cubeList, null);
        Assert.assertEquals(0, result.size());
        
    }
    
    @Test
    public void testInitMiniCubeIndexMeta() throws Exception {
        ConfigQuestionModel question = this.mockQuestion();
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(question.getCube());
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(question.getDataSourceInfo());
        List<IndexMeta> result = this.indexMetaService.initMiniCubeIndexMeta(cubes,
            dataSourceInfoList);
        Assert.assertEquals(1, result.size());
        IndexMeta idxMeta = result.get(0);
        MiniCube currCube = (MiniCube) question.getCube();
        Assert.assertTrue(idxMeta.getCubeIdSet().contains(question.getCubeId()));
        Assert.assertEquals(2, idxMeta.getDimSet().size());
        
        int measureCount = 0;
        for (String mKey : currCube.getMeasures().keySet()) {
            Measure measure = currCube.getMeasures().get(mKey);
            if (measure.getType().equals(MeasureType.COMMON)) {
                measureCount++;
            }
        }
        Assert.assertEquals(measureCount, idxMeta.getMeasureSet().size());
        
        // 维度中有时间维度
        currCube = (MiniCube) question.getCube();
        TimeDimension timeDim = new TimeDimension("the_date");
        MiniCubeLevel timeLevel = new MiniCubeLevel("the_date");
        timeLevel.setFactTableColumn("the_date");
        timeDim.addLevel(timeLevel);
        timeLevel.setDimension(timeDim);
        currCube.getDimensions().put("the_date", timeDim);
        result = this.indexMetaService.initMiniCubeIndexMeta(cubes, dataSourceInfoList);
        Assert.assertEquals(1, result.size());
        idxMeta = result.get(0);
        Assert.assertEquals("the_date", idxMeta.getShardDimBase());
        
        // 维度为空
        cubes.clear();
        question.getCube().getDimensions().clear();
        cubes.add(question.getCube());
        result = this.indexMetaService.initMiniCubeIndexMeta(cubes, dataSourceInfoList);
        Assert.assertEquals(1, result.size());
        idxMeta = result.get(0);
        currCube = (MiniCube) question.getCube();
        Assert.assertTrue(idxMeta.getCubeIdSet().contains(question.getCubeId()));
        Assert.assertEquals(0, idxMeta.getDimSet().size());
        measureCount = 0;
        for (String mKey : currCube.getMeasures().keySet()) {
            Measure measure = currCube.getMeasures().get(mKey);
            if (measure.getType().equals(MeasureType.COMMON)) {
                measureCount++;
            }
        }
        Assert.assertEquals(measureCount, idxMeta.getMeasureSet().size());
        
        // 指标为空
        cubes.clear();
        question.getCube().getMeasures().clear();
        cubes.add(question.getCube());
        result = this.indexMetaService.initMiniCubeIndexMeta(cubes, dataSourceInfoList);
        Assert.assertEquals(1, result.size());
        idxMeta = result.get(0);
        currCube = (MiniCube) question.getCube();
        Assert.assertTrue(idxMeta.getCubeIdSet().contains(question.getCubeId()));
        Assert.assertEquals(0, idxMeta.getDimSet().size());
        measureCount = 0;
        Assert.assertEquals(measureCount, idxMeta.getMeasureSet().size());
    }
    
    @Test
    public void testGetIndexMetasByDataSourceKeyBoundaryTest() {
        List<IndexMeta> result = null;
        try {
            result = this.indexMetaService.getIndexMetasByDataSourceKey(null);
        } catch (Exception e) {
            System.out.println("Exception occur");
        }
        Assert.assertNull(result);
    }
    
    /**
     * 默认集群名
     */
    private static final String STR_CLUSTER_NAME = "default";
    
    /**
     * 默认cube名
     */
    private static final String STR_CUBE_NAME1 = "cube_name_1";
    
    /**
     * DATA_SOURCE_INFO_UUID
     */
    private static final String DATA_SOURCE_INFO_UUID = "TEST_UUID";
    
    /**
     * 测试用storekey-2
     */
    private static final SqlDataSourceInfo DATA_SOURCE_INFO = new SqlDataSourceInfo(
            DATA_SOURCE_INFO_UUID);
    
    /**
     * static 初始化块
     */
    static {
        DATA_SOURCE_INFO.setInstanceName("zongheng");
        DATA_SOURCE_INFO.setDBProxy(true);
        DATA_SOURCE_INFO.setUsername("zongheng");
    }
    
    /**
     * STR_FACTTABLE_NAME1
     */
    private static final String STR_FACTTABLE_NAME1 = "data_click_psum";
    
    /**
     * STR_PRODUCT_LINE
     */
    private static final String STR_PRODUCT_LINE = "zongheng";
    
    /**
     * 测试用storekey-1
     */
    private static final String STR_STOREKEY_1 = "test_cluster1_test_prod1_xxkey";
    
    /**
     * STR_INDEXMETA_ID
     */
    private static final String STR_INDEXMETA_ID = "index_meta_id";
    
    /**
     * 
     * Mock cache
     * 
     * @return Cache 返回mock的cache
     */
    private Cache mockCache() {
        Cache cache = new ConcurrentMapCache(IndexMeta.getDataStoreName());
        List<IndexMeta> idxMetaList = new ArrayList<IndexMeta>();
        IndexMeta idxMeta1 = new IndexMeta();
        idxMeta1.setClusterName(STR_CLUSTER_NAME);
        Set<String> cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1);
        idxMeta1.setCubeIdSet(cubeNameSet);
        idxMeta1.setDataSourceInfo(DATA_SOURCE_INFO);
        DataDescInfo dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta1.setDataDescInfo(dataDescInfo1);
        idxMeta1.setProductLine(STR_PRODUCT_LINE);
        idxMeta1.setReplicaNum(2);
        idxMeta1.setIdxShardList(this.mockIndexShard(2));
        idxMeta1.setIndexMetaId(STR_INDEXMETA_ID);
        idxMeta1.setDimSet(this.mockDim(2));
        idxMeta1.setMeasureSet(this.mockMeasure(2));
        idxMetaList.add(idxMeta1);
        
        String storeKey = DATA_SOURCE_INFO_UUID;
        
        cache.put(storeKey, idxMetaList);
        
        return cache;
        
    }
    
    /**
     * mockIndexShard
     * 
     * @param num
     *            num
     * @return List<IndexShard>
     */
    private List<IndexShard> mockIndexShard(int num) {
        List<IndexShard> result = new ArrayList<IndexShard>();
        for (int i = 0; i < num; i++) {
            Node node = new Node("10.12.13." + System.currentTimeMillis(), 8080, STR_CLUSTER_NAME);
            
            IndexShard idxShard = new IndexShard("shard_" + System.currentTimeMillis(), node);
            idxShard.setShardId(Long.valueOf(i));
            
            result.add(idxShard);
        }
        return result;
    }
    
    /**
     * testGetIndexMetasByDataSourceKey
     */
    @Test
    public void testGetIndexMetasByDataSourceKey() {
        List<IndexMeta> result = null;
        String dataSourceKey = DATA_SOURCE_INFO_UUID;
        Mockito.when(this.storeManager.getDataStore(IndexMeta.getDataStoreName())).thenReturn(
                this.mockCache());
        result = this.indexMetaService.getIndexMetasByDataSourceKey(dataSourceKey);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(DATA_SOURCE_INFO, result.get(0).getDataSourceInfo());
        
    }
    
    /**
     * testGetIndexShardListByProductLineByNull
     */
    @Test
    public void testGetIndexShardListByProductLineByNull() {
        String productLine = null;
        String storeKey = null;
        
        // productLine=null && storeKey=null
        List<IndexShard> idxShardList = this.indexMetaService.getIndexShardListByProductLine(
            productLine, storeKey);
        Assert.assertNull(idxShardList);
        
        // productLine=null && storeKey=""
        storeKey = "";
        idxShardList = this.indexMetaService.getIndexShardListByProductLine(productLine, storeKey);
        Assert.assertNull(idxShardList);
        
        // productLine="" && storeKey=""
        productLine = "";
        idxShardList = this.indexMetaService.getIndexShardListByProductLine(productLine, storeKey);
        Assert.assertNull(idxShardList);
        
        // productLine="" && storeKey=null
        storeKey = null;
        idxShardList = this.indexMetaService.getIndexShardListByProductLine(productLine, storeKey);
        Assert.assertNull(idxShardList);
    }
    
    /**
     * testGetIndexShardListByProductLine
     * 入参不为空
     */
    @Test
    public void testGetIndexShardListByProductLine() {
        String productLine = null;
        String storeKey = null;
        
        // storeKey 不存在
        productLine = STR_PRODUCT_LINE;
        storeKey = STR_STOREKEY_1;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        List<IndexShard> idxShardList = this.indexMetaService.getIndexShardListByProductLine(
            productLine, storeKey);
        Assert.assertNull(idxShardList);
        
        // storeKey 存在
        productLine = STR_PRODUCT_LINE;
        storeKey = DATA_SOURCE_INFO_UUID;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        idxShardList = this.indexMetaService.getIndexShardListByProductLine(productLine, storeKey);
        Assert.assertNotNull(idxShardList);
        
    }
    
    /**
     * testGetIndexMetasByFactTableNameByNull
     * 入参为空
     */
    @Test
    public void testGetIndexMetasByFactTableNameByNull() {
        String factTableName = null;
        String storeKey = null;
        
        // factTableName=null && storeKey=null
        List<IndexMeta> idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(
            factTableName, storeKey);
        Assert.assertNull(idxMetaList);
        
        // factTableName=null && storeKey=""
        storeKey = "";
        idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(factTableName, storeKey);
        Assert.assertNull(idxMetaList);
        
        // factTableName="" && storeKey=""
        factTableName = "";
        idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(factTableName, storeKey);
        Assert.assertNull(idxMetaList);
        
        // factTableName="" && storeKey=null
        storeKey = null;
        idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(factTableName, storeKey);
        Assert.assertNull(idxMetaList);
    }
    
    /**
     * testGetIndexMetasByFactTableName
     * 入参不为空
     */
    @Test
    public void testGetIndexMetasByFactTableName() {
        String factTableName = null;
        String storeKey = null;
        
        // storeKey 不存在
        factTableName = STR_FACTTABLE_NAME1;
        storeKey = STR_STOREKEY_1;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        List<IndexMeta> idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(
            factTableName, storeKey);
        Assert.assertNull(idxMetaList);
        
        // storeKey 存在
        factTableName = STR_FACTTABLE_NAME1;
        storeKey = DATA_SOURCE_INFO_UUID;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        idxMetaList = this.indexMetaService.getIndexMetasByFactTableName(factTableName, storeKey);
        Assert.assertNotNull(idxMetaList);
        Assert.assertEquals(1, idxMetaList.size());
    }
    
    /**
     * testGetIndexMetaByIndexMetaIdByNull
     */
    @Test
    public void testGetIndexMetaByIndexMetaIdByNull() {
        IndexMeta result = this.indexMetaService.getIndexMetaByIndexMetaId(null, null);
        Assert.assertNull(result);
    }
    
    /**
     * testGetIndexMetaByIndexMetaId
     */
    @Test
    public void testGetIndexMetaByIndexMetaId() {
        String idxMetaId = STR_INDEXMETA_ID;
        String storeKey = DATA_SOURCE_INFO_UUID;
        
        // storeKey不存在
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        IndexMeta result = this.indexMetaService.getIndexMetaByIndexMetaId(STR_INDEXMETA_ID, "aaa");
        Assert.assertNull(result);
        
        // storeKey存在
        result = this.indexMetaService.getIndexMetaByIndexMetaId(idxMetaId, storeKey);
        Assert.assertNotNull(result);
        Assert.assertEquals(idxMetaId, result.getIndexMetaId());
        
        // storeKey存在，idxMetaId不存在
        result = this.indexMetaService.getIndexMetaByIndexMetaId("aaa", storeKey);
        Assert.assertNull(result);
    }
    
    /**
     * testGetIndexMetaByCubeNameByNull
     * 入参为空的情况
     */
    @Test
    public void testGetIndexMetaByCubeNameByNull() {
        String cubeName = null;
        String storeKey = null;
        
        // cubeName=null && storeKey=null
        IndexMeta idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, storeKey);
        Assert.assertNull(idxMeta);
        
        // cubeName=null && storeKey=""
        storeKey = "";
        idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, storeKey);
        Assert.assertNull(idxMeta);
        
        // cubeName="" && storeKey=""
        cubeName = "";
        idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, storeKey);
        Assert.assertNull(idxMeta);
        
        // cubeName="" && storeKey=null
        storeKey = null;
        idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, storeKey);
        Assert.assertNull(idxMeta);
    }
    
    /**
     * testGetIndexMetaByCubeName
     * 入参不为空
     */
    @Test
    public void testGetIndexMetaByCubeName() {
        String cubeName = null;
        String storeKey = null;
        
        // storeKey 不存在
        cubeName = STR_CUBE_NAME1;
        storeKey = DATA_SOURCE_INFO_UUID;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        IndexMeta idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, "aaa");
        Assert.assertNull(idxMeta);
        
        // storeKey 存在
        cubeName = STR_CUBE_NAME1;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        idxMeta = this.indexMetaService.getIndexMetaByCubeId(cubeName, storeKey);
        Assert.assertNotNull(idxMeta);
        Assert.assertEquals(storeKey, idxMeta.getStoreKey());
        
        idxMeta = this.indexMetaService.getIndexMetaByCubeId("aaa", storeKey);
        Assert.assertNull(idxMeta);
        
    }
    
    /**
     * testSaveOrUpdateIndexMetaByNull
     * 入参为空
     */
    @Test
    public void testSaveOrUpdateIndexMetaByNull() {
        IndexMeta idxMeta = null;
        boolean result = false;
        boolean exception = false;
        try {
            result = this.indexMetaService.saveOrUpdateIndexMeta(idxMeta);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertFalse(result);
        Assert.assertTrue(exception);
    }
    
    /**
     * testSaveOrUpdateIndexMeta
     * 入参不为空
     */
    @Test
    public void testSaveOrUpdateIndexMeta() {
        IndexMeta idxMeta = null;
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        boolean result = false;
        boolean exception = false;
        try {
            result = this.indexMetaService.saveOrUpdateIndexMeta(idxMeta);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertFalse(result);
        Assert.assertTrue(exception);
        
        // idxMeta--save
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        idxMeta.setProductLine("test_prod_2");
        Set<String> cubeNameSet = new HashSet<String>();
        cubeNameSet.add("testCube3");
        cubeNameSet.add("testCube4");
        idxMeta.setCubeIdSet(cubeNameSet);
        DataDescInfo dataDescInfo = new DataDescInfo();
        dataDescInfo.setProductLine("test_prod_2");
        dataDescInfo.setSplitTable(false);
        dataDescInfo.setTableName("test_table");
        idxMeta.setDataDescInfo(dataDescInfo);
        SqlDataSourceInfo dataSourceInfo = new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID);
        dataSourceInfo.setInstanceName("zongheng");
        idxMeta.setDataSourceInfo(dataSourceInfo);
        
        result = this.indexMetaService.saveOrUpdateIndexMeta(idxMeta);
        Assert.assertTrue(result);
        
        // idxMeta--update
        cubeNameSet = idxMeta.getCubeIdSet();
        cubeNameSet.add("testCube5");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setIdxShardList(this.mockIndexShard(2));
        result = this.indexMetaService.saveOrUpdateIndexMeta(idxMeta);
        Assert.assertTrue(result);
    }
    
    /**
     * testAssignIndexShardByNull
     * 入参为空
     */
    @Test
    public void testAssignIndexShardByNull() {
        IndexMeta idxMeta = null;
        IndexMeta result = null;
        boolean exception = false;
        try {
            result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertNull(result);
        Assert.assertTrue(exception);
        
        result = null;
        exception = false;
        idxMeta = new IndexMeta();
        idxMeta.setDataSourceInfo(Mockito.mock(DataSourceInfo.class));
        try {
            result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertNull(result);
        Assert.assertTrue(exception);
        
        idxMeta = new IndexMeta();
        idxMeta.setDataSourceInfo(null);
        result = null;
        exception = false;
        try {
            result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        } catch (Exception e) {
            exception = true;
        }
        Assert.assertNull(result);
        Assert.assertTrue(exception);
    }
    
    /**
     * 
     * mockAssignedNode
     * 
     * @return Map<Node, Integer>
     */
    private Map<Node, Integer> mockAssignedNode() {
        Map<Node, Integer> result = new HashMap<Node, Integer>();
        Node node = new Node("10.12.13.45", 8080, STR_CLUSTER_NAME);
        result.put(node, 2);
        return result;
    }
    
    private Set<String> mockDim(int dimNum) {
        Set<String> dimSet = new HashSet<String>();
        for (int i = 0; i < dimNum; i++) {
            dimSet.add("dim_name_" + i);
        }
        return dimSet;
    }
    
    /**
     * 
     * mockMeasureMap
     * 
     * @param mNum
     *            mNum
     * @return Map<String, Measure>
     */
    private Set<String> mockMeasure(int mNum) {
        Set<String> mSet = new HashSet<String>();
        for (int i = 0; i < mNum; i++) {
            mSet.add("m_name_" + i);
        }
        
        return mSet;
        
    }
    
    /**
     * STR_CUBE_NAME2
     */
    private static final String STR_CUBE_NAME2 = "cube_2";
    
    /**
     * STR_FACTTABLE_NAME3
     */
    private static final String STR_FACTTABLE_NAME3 = "facttable_3";
    
    /**
     * testAssignIndexShard
     * 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAssignIndexShard() {
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        IndexMeta idxMeta = null;
        idxMeta = new IndexMeta();
        // 集群相同，产品线不同，事实表不同
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        Set<String> cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        DataDescInfo dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE + "_notsame");
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_0");
        IndexMeta result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表不同
        // FIXME assign方法中暂时还没有考虑复本问题
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_1");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标不同，相似度为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIndexMetaId("idxMetaId_2");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标不同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIndexMetaId("idxMetaId_3");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标相同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIndexMetaId("idxMetaId_4");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表相同，维度不相同，指标相同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(3));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIndexMetaId("idxMetaId_5");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
        // 集群相同，产品线相同，事实表相同，维度不同，指标不同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(3));
        idxMeta.setMeasureSet(this.mockMeasure(3));
        idxMeta.setIndexMetaId("idxMetaId_6");
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
        
    }
    
    /**
     * testAssignIndexShardFull
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAssignIndexShardFull() {
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        IndexMeta idxMeta = null;
        idxMeta = new IndexMeta();
        // 集群相同，产品线不同，事实表不同
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        Set<String> cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        DataDescInfo dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE + "_notsame");
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_0");
        idxMeta.setIdxShardList(this.mockIndexShard(1));
        idxMeta.getIdxShardList().get(0).setFull(Boolean.TRUE);
        
        IndexMeta result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(3, result.getIdxShardList().size());
        
        idxMeta = null;
        idxMeta = new IndexMeta();
        // 集群相同，产品线不同，事实表不同
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE + "_notsame");
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_0");
        idxMeta.setIdxShardList(this.mockIndexShard(1));
        List<IndexShard> shardList = this.mockIndexShard(2);
        
        for (int i = shardList.size() - 1; i >= 0; i--) {
            IndexShard is = shardList.get(i);
            is.setShardId(is.getShardId() + 1);
            idxMeta.getIdxShardList().add(is);
        }
        
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(5, result.getIdxShardList().size());
        
        idxMeta = null;
        idxMeta = new IndexMeta();
        // 集群相同，产品线不同，事实表不同
        
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE + "_notsame");
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_0");
        idxMeta.setIdxShardList(this.mockIndexShard(2));
        Mockito.when(this.indexConfig.getIdxShardSize()).thenReturn(10000L);
        
        result = this.indexMetaService.assignIndexShard(idxMeta, STR_CLUSTER_NAME);
        Assert.assertEquals(2, result.getIdxShardList().size());
    }
    
    /**
     * testMergeIndexMeta
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMergeIndexMeta() {
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        IndexMeta idxMeta = null;
        idxMeta = new IndexMeta();
        // 集群相同，产品线不同，事实表不同
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        Set<String> cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        DataDescInfo dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE + "_notsame");
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_0");
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        IndexMeta result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdSet().size());
        
        // 集群相同，产品线相同，事实表不同
        // FIXME assign方法中暂时还没有考虑复本问题
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME2);
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME3);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setIndexMetaId("idxMetaId_1");
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdSet().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标不同，相似度为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(new SqlDataSourceInfo(DATA_SOURCE_INFO_UUID));
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(4));
        idxMeta.setIndexMetaId("idxMetaId_2");
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdSet().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标不同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(4));
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        idxMeta.setIndexMetaId("idxMetaId_3");
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdSet().size());
        Assert.assertEquals(1, result.getCubeIdMergeSet().size());
        
        // 集群相同，产品线相同，事实表相同，维度相同，指标相同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        idxMeta.setIndexMetaId("idxMetaId_4");
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(2, result.getCubeIdSet().size());
        
        // 集群相同，产品线相同，事实表相同，维度不相同，指标相同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(3));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        idxMeta.setIndexMetaId("idxMetaId_5");
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdSet().size());
        
        // 集群相同，产品线相同，事实表相同，维度不同，指标不同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(3));
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        idxMeta.setMeasureSet(this.mockMeasure(3));
        idxMeta.setIndexMetaId("idxMetaId_6");
        result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdMergeSet().size());
        
    }
    
    /**
     * testMergeIndexMetaLock
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMergeIndexMetaLock() {
        Cache cache = mockCache();
        List<IndexMeta> idxMetaList = (List<IndexMeta>) cache.get(DATA_SOURCE_INFO_UUID).get();
        idxMetaList.get(0).setLocked(Boolean.TRUE);
        idxMetaList.get(0).setIdxVersion(System.currentTimeMillis() - 100000);
        
        Mockito.when(this.indexConfig.getIndexInterval()).thenReturn(1000);
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(cache);
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                 this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        IndexMeta idxMeta = null;
        Set<String> cubeNameSet = null;
        DataDescInfo dataDescInfo1 = null;
        // 集群相同，产品线相同，事实表相同，维度相同，指标不同，相似度不为0
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(4));
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE);
        idxMeta.setIndexMetaId("idxMetaId_3");
        IndexMeta result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(1, result.getCubeIdMergeSet().size());
        Assert.assertFalse(result.getLocked());
        
    }
    
    /**
     * testMergeIndexMetaMerge
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMergeIndexMetaMerge() {
        Cache cache = mockCache();
        List<IndexMeta> idxMetaList = (List<IndexMeta>) cache.get(DATA_SOURCE_INFO_UUID).get();
        idxMetaList.get(0).setIdxState(IndexState.INDEX_AVAILABLE);
        
        Mockito.when(this.indexConfig.getIndexInterval()).thenReturn(1000);
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(cache);
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        IndexMeta idxMeta = null;
        Set<String> cubeNameSet = null;
        DataDescInfo dataDescInfo1 = null;
        // 集群相同，产品线相同，事实表相同，维度相同，指标相同，状态为available
        idxMeta = null;
        idxMeta = new IndexMeta();
        idxMeta.setClusterName(STR_CLUSTER_NAME);
        cubeNameSet = new HashSet<String>();
        cubeNameSet.add(STR_CUBE_NAME1 + "abc");
        idxMeta.setCubeIdSet(cubeNameSet);
        idxMeta.setDataSourceInfo(DATA_SOURCE_INFO);
        
        dataDescInfo1 = new DataDescInfo();
        dataDescInfo1.setSplitTable(false);
        dataDescInfo1.setTableName(STR_FACTTABLE_NAME1);
        idxMeta.setDataDescInfo(dataDescInfo1);
        idxMeta.setProductLine(STR_PRODUCT_LINE);
        idxMeta.setReplicaNum(2);
        idxMeta.setDimSet(this.mockDim(2));
        idxMeta.setMeasureSet(this.mockMeasure(2));
        
        idxMeta.setIndexMetaId("idxMetaId_3");
        IndexMeta result = this.indexMetaService.mergeIndexMeta(idxMeta);
        Assert.assertEquals(2, result.getCubeIdSet().size());
        Assert.assertEquals(IndexState.INDEX_AVAILABLE_MERGE, result.getIdxState());
        
    }
    
    /**
     * testSaveIndexMetaLocallyByNull
     * @throws Exception
     */
    @Test
    public void testSaveIndexMetaLocallyByNull() throws Exception {
        boolean result = this.indexMetaService.saveIndexMetaLocally(null);
        Assert.assertFalse(result);
    }
    
    /**
     * testSaveIndexMetaLocally
     * @throws Exception
     */
    @Test
    public void testSaveIndexMetaLocally() throws Exception {
        boolean result = false;
        ConfigQuestionModel question = this.mockQuestion();
        IndexMeta idxMeta = this.mockIndexMeta(question).get(0);
        idxMeta.setIndexMetaId("idxMeta_mock_id");
        
        Node mockNode = this.mockNode();
        Mockito.when(this.isNodeService.getCurrentNode()).thenReturn(mockNode);
        File idxMetaFileDir = new File(mockNode.getIndexBaseDir()
                + idxMeta.getIndexMetaFileDirPath());
        if (idxMetaFileDir.exists()) {
            FileUtils.deleteFile(idxMetaFileDir);
        }
        // 路径不存在
        result = this.indexMetaService.saveIndexMetaLocally(idxMeta);
        Assert.assertFalse(result);
        
        // 路径存在--第一次写入 --output .new .timg
        idxMetaFileDir.mkdirs();
        result = this.indexMetaService.saveIndexMetaLocally(idxMeta);
        Assert.assertTrue(result);
        // 第二次写入--output .new .timg .bak
        result = this.indexMetaService.saveIndexMetaLocally(idxMeta);
        Assert.assertTrue(result);
        // 第三次写入--output .new .timg .bak
        result = this.indexMetaService.saveIndexMetaLocally(idxMeta);
        Assert.assertTrue(result);
    }
    
    /**
     * testLoadIndexMetasLocalImageByNull
     */
    @Test
    public void testLoadIndexMetasLocalImageByNull() {
        List<IndexMeta> idxMetaList = this.indexMetaService.loadIndexMetasLocalImage(null, null,
                null);
        Assert.assertEquals(0, idxMetaList.size());
    }
    
    /**
     * testLoadIndexMetasLocalImage
     * @throws Exception
     */
    @Test
    public void testLoadIndexMetasLocalImage() throws Exception {
        // Mock目录及文件
        ConfigQuestionModel question = this.mockQuestion();
        IndexMeta idxMeta = this.mockIndexMeta(question).get(0);
        idxMeta.setIndexMetaId("idxMeta_mock_id");
        idxMeta.setIdxShardList(this.mockIndexShard(2));
        
        Node mockNode = this.mockNode();
        mockNode.setIndexBaseDir("idxbase/");
        Mockito.when(this.isNodeService.getCurrentNode()).thenReturn(mockNode);
        File idxMetaFileDir = new File(mockNode.getIndexBaseDir()
                + idxMeta.getIndexMetaFileDirPath());
        if (idxMetaFileDir.exists()) {
            FileUtils.deleteFile(idxMetaFileDir);
        }
        
        // 测试
        List<IndexMeta> idxMetaList = null;
        this.indexMetaService.saveIndexMetaLocally(idxMeta);
        idxMetaList = this.indexMetaService.loadIndexMetasLocalImage(mockNode.getIndexBaseDir(),
                mockNode.getNodeKey(), mockNode.getClusterName());
        Assert.assertEquals(0, idxMetaList.size());
        
        idxMetaFileDir.mkdirs();
        this.indexMetaService.saveIndexMetaLocally(idxMeta);
        idxMetaList = this.indexMetaService.loadIndexMetasLocalImage(mockNode.getIndexBaseDir(),
                mockNode.getNodeKey(), mockNode.getClusterName());
        Assert.assertEquals(1, idxMetaList.size());
        
    }
    
    /**
     * testRecoverLocalIndexMetaWithClusterByNull
     */
    @Test
    public void testRecoverLocalIndexMetaWithClusterByNull() {
        this.indexMetaService.recoverLocalIndexMetaWithCluster(null, null);
    }
    
    /**
     * testRecoverLocalIndexMetaWithCluster
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRecoverLocalIndexMetaWithCluster() throws Exception {
        ConfigQuestionModel question = this.mockQuestion();
        List<IndexShard> idxShardList = this.mockIndexShard(2);
        idxShardList.get(0).setClusterName(STR_CLUSTER_NAME);
        idxShardList.get(1).setClusterName(STR_CLUSTER_NAME);
        IndexMeta idxMeta = this.mockIndexMeta(question).get(0);
        idxMeta.setIndexMetaId("idxMeta_mock_id");
        idxMeta.setIdxShardList(idxShardList);
        Node mockNode = this.mockNode();
        mockNode.setIndexBaseDir("idxbase/");
        Mockito.when(this.isNodeService.getCurrentNode()).thenReturn(mockNode);
        File idxMetaFileDir = new File(mockNode.getIndexBaseDir()
                + idxMeta.getIndexMetaFileDirPath());
        if (idxMetaFileDir.exists()) {
            FileUtils.deleteFile(idxMetaFileDir);
        }
        
        List<IndexMeta> idxMetaList = null;
        idxMetaFileDir.mkdirs();
        this.indexMetaService.saveIndexMetaLocally(idxMeta);
        idxMetaList = this.indexMetaService.loadIndexMetasLocalImage(mockNode.getIndexBaseDir(),
                mockNode.getNodeKey(), mockNode.getClusterName());
        
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(mockCache());
        Mockito.when(this.isNodeService.assignFreeNode(Mockito.anyInt(), Mockito.anyString()))
                .thenReturn(mockAssignedNode());
        Mockito.when(
                this.isNodeService.assignFreeNodeByNodeList(Mockito.anyList(), Mockito.anyInt(),
                        Mockito.anyString())).thenReturn(mockAssignedNode());
        
        // Remote Indexmeta does not exist
        this.indexMetaService.recoverLocalIndexMetaWithCluster(idxMetaList,
                mockNode.getClusterName());
        
        // Remote indexmeta exist
        Cache cache = mockCache();
        List<IndexMeta> cacheIdxMetaList = (List<IndexMeta>) cache.get(DATA_SOURCE_INFO_UUID).get();
        cacheIdxMetaList.get(0).getCubeIdSet().add("05a26a5f79adf6c8b2283e4628d577cf");
        
        cache.put("test_test", cacheIdxMetaList);
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(cache);
        this.indexMetaService.recoverLocalIndexMetaWithCluster(idxMetaList,
                mockNode.getClusterName());
        
        cacheIdxMetaList.get(0).setIdxShardList(idxShardList);
        cacheIdxMetaList.get(0).getIdxShardList().get(1).setNodeKey("aaa");
        cache.put("test_test", cacheIdxMetaList);
        Mockito.when(storeManager.getDataStore(Mockito.anyString())).thenReturn(cache);
        this.indexMetaService.recoverLocalIndexMetaWithCluster(idxMetaList,
                mockNode.getClusterName());
        
    }
    
    
    
}
