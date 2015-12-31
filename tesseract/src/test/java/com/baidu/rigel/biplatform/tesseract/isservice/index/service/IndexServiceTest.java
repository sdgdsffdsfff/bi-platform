/**
 * 
 */
package com.baidu.rigel.biplatform.tesseract.isservice.index.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.query.model.ConfigQuestionModel;
import com.baidu.rigel.biplatform.cache.StoreManager;
import com.baidu.rigel.biplatform.tesseract.config.IndexConfig;
import com.baidu.rigel.biplatform.tesseract.dataquery.service.DataQueryService;
import com.baidu.rigel.biplatform.tesseract.datasource.DataSourcePoolService;
import com.baidu.rigel.biplatform.tesseract.datasource.impl.SqlDataSourceWrap;
import com.baidu.rigel.biplatform.tesseract.isservice.IndexTestBase;
import com.baidu.rigel.biplatform.tesseract.isservice.exception.IndexAndSearchException;
import com.baidu.rigel.biplatform.tesseract.isservice.index.service.impl.IndexServiceImpl;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexMeta;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexShard;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexShardState;
import com.baidu.rigel.biplatform.tesseract.isservice.meta.IndexState;
import com.baidu.rigel.biplatform.tesseract.netty.message.isservice.IndexMessage;
import com.baidu.rigel.biplatform.tesseract.node.service.IndexAndSearchClient;
import com.baidu.rigel.biplatform.tesseract.node.service.IsNodeService;
import com.baidu.rigel.biplatform.tesseract.resultset.isservice.IndexDataResultSet;

/**
 * IndexService
 * 
 * @author lijin
 *
 */
public class IndexServiceTest extends IndexTestBase {
    
    @InjectMocks
    private IndexServiceImpl indexService;
    
    @Mock
    private IndexMetaService indexMetaService;
    
    @Mock
    private DataSourcePoolService dataSourcePoolService;
    
    @Mock
    private DataQueryService dataQueryService;
    
    @Mock
    private IndexAndSearchClient isClient;
    
    @Mock
    private IsNodeService isNodeService;
    
    @Mock
    private IndexConfig indexConfig;
    
    @Mock
    private StoreManager storeManager;
    
    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testInitMiniCubeIndexWithNull() throws IndexAndSearchException {
        List<Cube> cubeList = null;
        DataSourceInfo dataSourceInfo = null;
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(dataSourceInfo);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(new ArrayList<IndexMeta>());
        boolean result = this.indexService
            .initMiniCubeIndex(cubeList, dataSourceInfoList, false, false);
        Assert.assertFalse(result);
        List<IndexMeta>  mockIndexMetaList=new ArrayList<IndexMeta>();
        IndexMeta idxMeta=new IndexMeta();
        mockIndexMetaList.add(idxMeta);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(mockIndexMetaList);
        Mockito.when(this.indexMetaService.mergeIndexMeta(Mockito.any())).thenReturn(idxMeta);
        result = this.indexService
            .initMiniCubeIndex(cubeList, dataSourceInfoList, false, false);
        Assert.assertTrue(result);
        
    }
    
    
    /**
     * testInitMiniCubeIndex_1 
     * 建索引时：IndexState.INDEX_UNINIT
     * @throws Exception
     */
    @Test
    public void testInitMiniCubeIndex_1() throws Exception {
    	
    	ConfigQuestionModel question=this.mockQuestion();         
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(question.getCube());
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(question.getDataSourceInfo());
        List<IndexMeta> mockIndexMetaList=this.mockIndexMeta(question);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(mockIndexMetaList);
        Mockito.when(this.indexMetaService.mergeIndexMeta(Mockito.any())).thenReturn(mockIndexMetaList.get(0));
        boolean result = this.indexService.initMiniCubeIndex(cubes, dataSourceInfoList, false,
                false);
        Assert.assertTrue(result);
        
        //doIndex
        //-checkIndexMetaBeforeIndex
        IndexMeta idxMeta=mockIndexMetaList.get(0);
        Mockito.when(this.indexMetaService.getIndexMetaByIndexMetaId(Mockito.anyString(), Mockito.anyString())).thenReturn(idxMeta);
        Mockito.when(this.indexMetaService.saveOrUpdateIndexMeta(Mockito.any())).thenReturn(Boolean.TRUE);
        SqlDataSourceWrap sqlDataSourceWrap=this.mockSqlDataSourceWrap();
        //Mockito.when(this.dataSourcePoolService.getDataSourceByKey(Mockito.anyString())).thenReturn(sqlDataSourceWrap);
        Mockito.when(this.dataSourcePoolService.getDataSourceByKey(Mockito.any(DataSourceInfo.class))).thenReturn(sqlDataSourceWrap);
        //-getTotalCountBeforeIndex        
        
        Mockito.when(this.dataQueryService.queryForLongWithSql(Mockito.anyString(), Mockito.any())).thenReturn(new Long(10));
        //-getDataForIndex
       // IndexDataResultSet data=this.mockIndexDataResultSet();
        Mockito.when(this.dataQueryService.queryForDocListWithSQLQuery(Mockito.any(), Mockito.any(), Mockito.anyLong(),  Mockito.anyLong())).thenReturn(this.mockIndexDataResultSet());
        
        idxMeta=this.mockAssignIndexShard(idxMeta);
        
        Mockito.when(this.indexMetaService.assignIndexShard(Mockito.any(), Mockito.any())).thenReturn(idxMeta);
        Mockito.when(this.isNodeService.getCurrentNode()).thenReturn(this.mockNode());
        
        
        //-writeindex
        Mockito.when(this.isNodeService.getNodeByNodeKey(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(this.mockNode());
        
        IndexShard idxShard=idxMeta.getIdxShardList().get(0);
        IndexDataResultSet data=this.mockIndexDataResultSet();
        data.getDataList().clear();
        IndexMessage message=this.mockIndexMessage(idxShard, data, this.mockNode());
        
        Mockito.when(this.isClient.index(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(message);  
        Mockito.when(this.indexConfig.getShardReplicaNum()).thenReturn(1);
        
        
        result = this.indexService.initMiniCubeIndex(cubes, dataSourceInfoList, true, false);
        Assert.assertTrue(result);
        
    }
    
    /**
     * testInitMiniCubeIndex_2 
     * 建索引时：IndexState.INDEX_AVAILABLE_MERGE
     * @throws Exception
     */
    @Test
    public void testInitMiniCubeIndex_2() throws Exception {
    	
    	ConfigQuestionModel question=this.mockQuestion();         
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(question.getCube());
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(question.getDataSourceInfo());
        List<IndexMeta> mockIndexMetaList=this.mockIndexMeta(question);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(mockIndexMetaList);
        mockIndexMetaList.get(0).setIdxState(IndexState.INDEX_AVAILABLE_MERGE);
        Mockito.when(this.indexMetaService.mergeIndexMeta(Mockito.any())).thenReturn(mockIndexMetaList.get(0));
        boolean result = this.indexService
                .initMiniCubeIndex(cubes, dataSourceInfoList, true, false);
        Assert.assertTrue(result);
        
    }
    
    /**
     * testInitMiniCubeIndex_3
     * 建索引时：IndexState.INDEX_AVAILABLE_NEEDMERGE
     * @throws Exception
     */
    @Test
    public void testInitMiniCubeIndex_3() throws Exception {
    	
    	ConfigQuestionModel question=this.mockQuestion();         
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(question.getCube());
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(question.getDataSourceInfo());
        List<IndexMeta> mockIndexMetaList=this.mockIndexMeta(question);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(mockIndexMetaList);
        IndexMeta idxMeta=mockIndexMetaList.get(0);
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE_NEEDMERGE);
        idxMeta.getCubeIdMergeSet().add("aaa");
        idxMeta.getMeasureInfoMergeSet().add("click");
        idxMeta.getDataDescInfo().getMaxDataIdMap().put(idxMeta.getDataDescInfo().getTableName(), new BigDecimal(10));
        this.mockAssignIndexShard(idxMeta);
        for(IndexShard idxShard:idxMeta.getIdxShardList()){
        	idxShard.setIdxShardState(IndexShardState.INDEXSHARD_INDEXED);
        	idxShard.setIdxState(IndexState.INDEX_AVAILABLE);
        }
        Mockito.when(this.indexMetaService.mergeIndexMeta(Mockito.any())).thenReturn(idxMeta);
        
        
        //doIndex
        //-checkIndexMetaBeforeIndex
        Mockito.when(this.indexMetaService.getIndexMetaByIndexMetaId(Mockito.anyString(), Mockito.anyString())).thenReturn(idxMeta);
        Mockito.when(this.indexMetaService.saveOrUpdateIndexMeta(Mockito.any())).thenReturn(Boolean.TRUE);
        SqlDataSourceWrap sqlDataSourceWrap=this.mockSqlDataSourceWrap();
        //Mockito.when(this.dataSourcePoolService.getDataSourceByKey(Mockito.anyString())).thenReturn(sqlDataSourceWrap);
        Mockito.when(this.dataSourcePoolService.getDataSourceByKey(Mockito.any(DataSourceInfo.class))).thenReturn(sqlDataSourceWrap);
        //-getTotalCountBeforeIndex        
        
        Mockito.when(this.dataQueryService.queryForLongWithSql(Mockito.anyString(), Mockito.any())).thenReturn(new Long(10));
        //-getDataForIndex
       // IndexDataResultSet data=this.mockIndexDataResultSet();
        Mockito.when(this.dataQueryService.queryForDocListWithSQLQuery(Mockito.any(), Mockito.any(), Mockito.anyLong(),  Mockito.anyLong())).thenReturn(this.mockIndexDataResultSet());
        
        idxMeta=this.mockAssignIndexShard(idxMeta);
        
        Mockito.when(this.indexMetaService.assignIndexShard(Mockito.any(), Mockito.any())).thenReturn(idxMeta);
        Mockito.when(this.isNodeService.getCurrentNode()).thenReturn(this.mockNode());
        
        
        //-writeindex
        Mockito.when(this.isNodeService.getNodeByNodeKey(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(this.mockNode());
        
        IndexShard idxShard = idxMeta.getIdxShardList().get(0);
        IndexDataResultSet data=this.mockIndexDataResultSet();
        data.getDataList().clear();
        IndexMessage message=this.mockIndexMessage(idxShard, data, this.mockNode());
        
        Mockito.when(this.isClient.index(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(message);  
        Mockito.when(this.indexConfig.getShardReplicaNum()).thenReturn(1);
        
        
        
        
        
        boolean result = this.indexService
                .initMiniCubeIndex(cubes, dataSourceInfoList, true, false);
        Assert.assertTrue(result);
        
       
    }
    
    /**
     * testInitMiniCubeIndex_4 
     * 建索引时：内部异常
     * @throws Exception
     */
    @Test
    public void testInitMiniCubeIndex_4() throws Exception {
    	
    	ConfigQuestionModel question=this.mockQuestion();         
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(question.getCube());
        List<DataSourceInfo> dataSourceInfoList = new ArrayList<DataSourceInfo>();
        dataSourceInfoList.add(question.getDataSourceInfo());
        List<IndexMeta> mockIndexMetaList=this.mockIndexMeta(question);
        Mockito.when(this.indexMetaService.initMiniCubeIndexMeta(Mockito.anyList(), Mockito.any())).thenReturn(mockIndexMetaList);
        IndexMeta idxMeta=mockIndexMetaList.get(0);
        idxMeta.setIdxState(IndexState.INDEX_AVAILABLE_NEEDMERGE);
        idxMeta.getCubeIdMergeSet().add("aaa");
        idxMeta.getMeasureInfoMergeSet().add("click");
        idxMeta.getDataDescInfo().getMaxDataIdMap().put(idxMeta.getDataDescInfo().getTableName(), new BigDecimal(10));
        this.mockAssignIndexShard(idxMeta);
        for(IndexShard idxShard:idxMeta.getIdxShardList()){
        	idxShard.setIdxShardState(IndexShardState.INDEXSHARD_INDEXED);
        	idxShard.setIdxState(IndexState.INDEX_AVAILABLE);
        }
        Mockito.when(this.indexMetaService.mergeIndexMeta(Mockito.any())).thenReturn(idxMeta);
        
        
        //doIndex
        //-checkIndexMetaBeforeIndex
        Mockito.when(this.indexMetaService.getIndexMetaByIndexMetaId(Mockito.anyString(), Mockito.anyString())).thenReturn(idxMeta);
        Mockito.when(this.indexMetaService.saveOrUpdateIndexMeta(Mockito.any())).thenReturn(Boolean.TRUE);       
        boolean result = this.indexService
                .initMiniCubeIndex(cubes, dataSourceInfoList, true, false);
        Assert.assertFalse(result);
        
    }
    
    
   
    
}
