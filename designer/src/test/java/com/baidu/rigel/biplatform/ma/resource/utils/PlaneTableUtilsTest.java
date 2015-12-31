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

import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ma.PrepareModelObject4Test;
import com.google.common.collect.Maps;

/**
 * PlaneTableUtil测试类
 * @author jiangjiangyichao
 * @version 2015年6月3日
 * @since jdk 1.8 or after
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "file:src/test/resources/applicationContext.xml")
public class PlaneTableUtilsTest {
    /**
     * 密钥key
     */
    @Value("${biplatform.ma.ser_key}")
    private String securityKey;
    
    /**
     * 日志对象
     */
    private static final Logger LOG = LoggerFactory.getLogger(PlaneTableUtilsTest.class);
    
    /**
     * 时间粒度信息
     */
    private static final String GRANULARITY = "granularity";
    
    /**
     * 时间粒度起始标志
     */
    private static final String START = "start";
    
    /**
     * 时间粒度结束标识
     */
    private static final String END = "end";
    
    /**
     * 测试检查输入SQL条件是否合理
     */
    @Test
    public void testCheckSQLCondition() {
        // SQL条件和参数值均为null
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition(null, null));
        // SQL条件正常，参数值为null
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition("EQ", null));
        // SQL条件正常，无具体参数值
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition("EQ", ","));

        // 等于条件，参数值为一个
        Assert.assertTrue(PlaneTableUtils.checkSQLCondition("EQ", "test"));
        // 等于条件，参数值为两个
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition("EQ", "test1,test2"));

        // 小于条件，参数值为一个
        Assert.assertTrue(PlaneTableUtils.checkSQLCondition("LT", "test"));
        // 小于条件，参数值为两个
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition("LT", "test1,test2"));

        // between and 条件，参数值为一个
        Assert.assertFalse(PlaneTableUtils.checkSQLCondition("BETWEEN_AND", "test"));
        // between and 条件，参数值为两个
        Assert.assertTrue(PlaneTableUtils.checkSQLCondition("BETWEEN_AND", "test1,test2"));

        // in 条件，参数值为一个
        Assert.assertTrue(PlaneTableUtils.checkSQLCondition("IN", "test"));
        // in 条件，参数值为两个
        Assert.assertTrue(PlaneTableUtils.checkSQLCondition("IN", "test1,test2"));

    }

    /**
     * 测试平面表中对时间条件的特殊处理 testHandleTimeContion
     */
    @Test
    public void testHandleTimeContion() {
        // 时间字符串
        String timeJson = null;
        // 时间字符串为null，原样返回
        Assert.assertEquals(timeJson, PlaneTableUtils.handleTimeCondition(timeJson));

        timeJson = "";
        // 时间字符串为空串，原样返回
        Assert.assertEquals(timeJson, PlaneTableUtils.handleTimeCondition(timeJson));

        timeJson = "timeJson";
        // 时间字符串不包含'start','end','granularity'关键字，原样返回
        Assert.assertEquals(timeJson, PlaneTableUtils.handleTimeCondition(timeJson));

        // 日粒度
        timeJson = "{'start':'2015-06-03','end':'2015-06-03','granularity':'D'}";
        String actualDayTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualDayTime);
        LOG.debug("日粒度:" + actualDayTime);

        // 日粒度, 起始时间大于截止时间
        timeJson = "{'start':'2015-06-03','end':'2015-06-01','granularity':'D'}";
        actualDayTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualDayTime);
        LOG.debug("日粒度:" + actualDayTime);

        // 周粒度
        timeJson = "{'start':'2015-06-01','end':'2015-06-01','granularity':'W'}";
        String actualWeekTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualWeekTime);
        LOG.debug("周粒度:" + actualWeekTime);

        // 月粒度
        timeJson = "{'start':'2015-06','end':'2015-06','granularity':'M'}";
        String actualMonthTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualMonthTime);
        LOG.debug("月粒度:" + actualMonthTime);

        // 季粒度
        timeJson = "{'start':'2015-Q2','end':'2015-Q2','granularity':'Q'}";
        String actualQuarterTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualQuarterTime);
        LOG.debug("季粒度:" + actualQuarterTime);

        // 年粒度
        timeJson = "{'start':'2015','end':'2015','granularity':'Y'}";
        String actualYearTime = PlaneTableUtils.handleTimeCondition(timeJson);
        Assert.assertNotNull(actualYearTime);
        LOG.debug("年粒度:" + actualYearTime);
    }
          
    /**
     * 
     * testHandleTimeCondition
     */
    @Test
    public void testHandleTimeConditionNull() {
        
        /**
         * 测试空cube
         */
        Assert.assertEquals(Maps.newHashMap(), PlaneTableUtils.handelTimeCondition(null, Maps.newHashMap()));
        
        // 空请求参数
        Cube cube = PrepareModelObject4Test.getCube();
        Assert.assertEquals(Maps.newHashMap(), PlaneTableUtils.handelTimeCondition(cube, null));
    }
    
    /**
     * 
     * testHandleTimeCondition
     */
    @Test
    public void testHandleTimeCondition() {       
        // 获取cube
        Cube cube = PrepareModelObject4Test.getCube();
        // 测试天粒度
        String dayId = "dasdkdsjklfjdsakljfd;adlsjfkasldjfk";       
        Map<String, Object> requestParams = Maps.newHashMap();
        String dayValue = "{'start':'2015-06-03','end':'2015-06-03','granularity':'D'}";        
        requestParams.put("testId", dayValue);        
        Map<String, Object> actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertTrue(actualParams.containsKey(dayId));      
    }
    
    /**
     * 
     * testHandleTimeWithNull
     */
    @Test
    public void testHandleTimeDimNull() {
        // 维度值为null
        Cube cube = PrepareModelObject4Test.getCube();
        ((MiniCube) cube).setDimensions(null);
        Map<String, Object> params = Maps.newHashMap();
        params.put("test1", "test1");
        params.put("test2", "test2");
        String dayValue = "{'start':'2015-06-03','end':'2015-06-03','granularity':'D'}";
        params.put("time", dayValue);
        Assert.assertEquals(params, PlaneTableUtils.handelTimeCondition(cube, params));
    }
    
    /**
     * cube中没有时间维度
     * testHanleNoTimeDim
     */
    @Test
    public void testHanleNoTimeDim() {
        Cube cube = PrepareModelObject4Test.getCube();
        Map<String, Dimension> dimensions = Maps.newHashMap();
        dimensions.put("dimKey", Mockito.mock(Dimension.class));
        ((MiniCube) cube).setDimensions(dimensions);
        Map<String, Object> params = Maps.newHashMap();
        params.put("test1", "test1");
        params.put("test2", "test2");
        String dayValue = "{'start':'2015-06-03','end':'2015-06-03','granularity':'D'}";
        params.put("time", dayValue);
        Assert.assertEquals(params, PlaneTableUtils.handelTimeCondition(cube, params));
    }
    
    /**
     * 
     * testHandleTimeWrongJson
     */
    @Test
    public void testHandleTimeWrongJson() {
        Cube cube = PrepareModelObject4Test.getCube();
        Map<String, Object> params = Maps.newHashMap();
        params.put("test1", "test1");
        params.put("test2", "test2");
        String dayValue = "{'start':'2015-06-03','end'2015-06-03','granularity':'D'}";
        params.put("time", dayValue);
        try {
            PlaneTableUtils.handelTimeCondition(cube, params);            
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }
    
    /**
     * 参数数值为数组
     * testHandleTimeWithParamsArray
     */
    @Test
    public void testHandleTimeWithParamsArray() {
        Cube cube = PrepareModelObject4Test.getCube();
        Map<String, Object> params = Maps.newHashMap();
        params.put("test1", new String[] {"test1_1", "test1_2"});
        params.put("test2", "test2");
        Assert.assertEquals(params, PlaneTableUtils.handelTimeCondition(cube, params));
    }
    
    /**
     * 
     * testHandleTimeWithDifferentGra
     */
    @Test
    public void testHandleTimeWithDifferentGra() {
        // 获取cube
        Cube cube = PrepareModelObject4Test.getCube();
        // 测试天粒度
        String dayId = "dasdkdsjklfjdsakljfd;adlsjfkasldjfk";       
        Map<String, Object> requestParams = Maps.newHashMap();
        String dayValue = "{'start':'2015-06-03','end':'2015-06-03','granularity':'D'}";        
        requestParams.put("testId", dayValue);        
        Map<String, Object> actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertTrue(actualParams.containsKey(dayId));  
             
        // 测试周粒度
        String weekId = "5cdf3831879c5e48ec25b529258b6ad6";
        String weekValue = "{'start':'2015-06-01','end':'2015-06-01','granularity':'W'}";
        requestParams.put("testId", weekValue);
        actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertTrue(actualParams.containsKey(weekId));
        
        // 测试月粒度
        String monthId = "ab2b863f9673567ea1e634742d910bd0";
        String monthValue = "{'start':'2015-06','end':'2015-06','granularity':'M'}";
        requestParams.put("testId", monthValue);
        actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertTrue(actualParams.containsKey(monthId));
        
        // 测试季粒度
        String quarterId = "9ac1ea7cc2ede6abe5c72217235f8434";
        String quarterValue = "{'start':'2015-Q2','end':'2015-Q2','granularity':'Q'}";
        requestParams.put("testId", quarterValue);
        actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertTrue(actualParams.containsKey(quarterId));
        
        // 测试年粒度
        String yearValue = "{'start':'2015','end':'2015','granularity':'Y'}";
        requestParams.put("testId", yearValue);
        actualParams = PlaneTableUtils.handelTimeCondition(cube, requestParams);
        Assert.assertNotNull(actualParams);
        Assert.assertEquals(requestParams, actualParams);
    }
    
    /**
     * 
     * testIsTimeDim
     */
    @Test
    public void testIsTimeDimNull() {
        // 空cube
        Assert.assertFalse(PlaneTableUtils.isTimeDim(null, "elementId"));
        Cube cube = PowerMockito.mock(Cube.class);
        // 空elementId
        Assert.assertFalse(PlaneTableUtils.isTimeDim(cube, null));
        // 获取cube对象
        cube = PrepareModelObject4Test.getCube();
        // 维度中没有此id
        Assert.assertFalse(PlaneTableUtils.isTimeDim(cube, "elementId"));
        // 设置维度条件为null
        ((MiniCube) cube).setDimensions(null);
        Assert.assertFalse(PlaneTableUtils.isTimeDim(cube, "elementId"));
        // 设置维度条件size为0
        ((MiniCube) cube).setDimensions(Maps.newHashMap());
        Assert.assertFalse(PlaneTableUtils.isTimeDim(cube, "elementId"));
    }
    
    /**
     * 测试是否为时间维度
     * testIsTimeDim
     */
    public void testIsTimeDim() {
        Cube cube = PrepareModelObject4Test.getCube();
        String elementId = "dasdkdsjklfjdsakljfd;adlsjfkasldjfk";
        Assert.assertTrue(PlaneTableUtils.isTimeDim(cube, elementId));
    }
    
    /**
     * 测试
     * testCov2TimeJsonNull
     */
    @Test
    public void testCov2TimeJsonNull() {
        // 待处理字符串为空
        Assert.assertNull(PlaneTableUtils.convert2TimeJson(null, Maps.newHashMap()));
        // 参数值为空
        Assert.assertNull(PlaneTableUtils.convert2TimeJson("20150627", null));
        // 参数中没有粒度信息
        Assert.assertNull(PlaneTableUtils.convert2TimeJson("20150627", Maps.newHashMap()));
    }
    
    /**
     * 测试不满足要求的时间字符串<br>
     * 日:20150627;周:20150627
     * 月:201506;季:201504;
     * 年:2015
     * testC2TimJsonWithNotStandStr
     */
    @Test
    public void testC2TimJsonWithNotStandStr() {
//        // 上下文请求参数
//        Map<String, Object> requestParams = Maps.newHashMap();
//        // 时间字符串
//        String value = "";
//        // 日
//        requestParams.put(GRANULARITY, "D");
//        value = "2015-06-27";
//        Assert.assertNull(PlaneTableUtils.convert2TimeJson(value, requestParams));
//        
//        // 周
//        requestParams.put(GRANULARITY, "W");
//        value = "2015-06-22";
//        Assert.assertNull(PlaneTableUtils.convert2TimeJson(value, requestParams));
//        
//        // 月
//        requestParams.put(GRANULARITY, "M");
//        value = "2015-06";
//        Assert.assertNull(PlaneTableUtils.convert2TimeJson(value, requestParams));
//        
//        // 季
//        requestParams.put(GRANULARITY, "Q");
//        value = "2015-06";
//        Assert.assertNull(PlaneTableUtils.convert2TimeJson(value, requestParams));
//        
//        // 年
//        requestParams.put(GRANULARITY, "Y");
//        value = "20-15";
//        Assert.assertNull(PlaneTableUtils.convert2TimeJson(value, requestParams));
//        
//        // 不满足条件的粒度，抛出异常
//        requestParams.put(GRANULARITY, "S");
//        value = "2015";
//        try {
//            PlaneTableUtils.convert2TimeJson(value, requestParams);            
//        } catch (Exception e) {
//            Assert.assertNotNull(e);
//        }
    }
    
    /**
     * 
     * testCov2TimeJson
     */
    @Test
    public void testCov2TimeJson() throws Exception {
        // 上下文请求参数
        Map<String, Object> requestParams = Maps.newHashMap();
        // 时间字符串
        String value = "";
        // 期望结果
        String expectValue = "";
        // 实际结果
        String actualValue = "";
        // 期望json
        JSONObject expectJson;
        // 实际json
        JSONObject actualJson;
        // 日
        expectValue = "{'start':'20151102','end':'20151102','granularity':'D'}";
        requestParams.put(GRANULARITY, "D");
        value = "20151102";
        actualValue = PlaneTableUtils.convert2TimeJson(value, requestParams);
        expectJson = new JSONObject(expectValue);
        actualJson = new JSONObject(actualValue);
        Assert.assertEquals(expectJson.get(START), actualJson.get(START));
        Assert.assertEquals(expectJson.get(END), actualJson.get(END));
        Assert.assertEquals(expectJson.get(GRANULARITY), actualJson.get(GRANULARITY));
        
        // 周
        expectValue = "{'start':'20150622','end':'20150628','granularity':'W'}";
        requestParams.put(GRANULARITY, "W");
        value = "20150622";
        actualValue = PlaneTableUtils.convert2TimeJson(value, requestParams);
        expectJson = new JSONObject(expectValue);
        actualJson = new JSONObject(actualValue);
        Assert.assertEquals(expectJson.get(START), actualJson.get(START));
        Assert.assertEquals(expectJson.get(END), actualJson.get(END));
        Assert.assertEquals(expectJson.get(GRANULARITY), actualJson.get(GRANULARITY));
        
        // 月
        expectValue = "{'start':'20151001','end':'20151031','granularity':'M'}";
        requestParams.put(GRANULARITY, "M");
        value = "201510";
        actualValue = PlaneTableUtils.convert2TimeJson(value, requestParams);
        expectJson = new JSONObject(expectValue);
        actualJson = new JSONObject(actualValue);
        Assert.assertEquals(expectJson.get(START), actualJson.get(START));
        Assert.assertEquals(expectJson.get(END), actualJson.get(END));
        Assert.assertEquals(expectJson.get(GRANULARITY), actualJson.get(GRANULARITY));
        
        // 季
        expectValue = "{'start':'20150101','end':'20150331','granularity':'Q'}";
        requestParams.put(GRANULARITY, "Q");
        value = "201501";
        actualValue = PlaneTableUtils.convert2TimeJson(value, requestParams);
        expectJson = new JSONObject(expectValue);
        actualJson = new JSONObject(actualValue);
        Assert.assertEquals(expectJson.get(START), actualJson.get(START));
        Assert.assertEquals(expectJson.get(END), actualJson.get(END));
        Assert.assertEquals(expectJson.get(GRANULARITY), actualJson.get(GRANULARITY));
                
        // 年
        expectValue = "{'start':'20140101','end':'20141231','granularity':'Y'}";
        requestParams.put(GRANULARITY, "Y");
        value = "2014";
        actualValue = PlaneTableUtils.convert2TimeJson(value, requestParams);
        expectJson = new JSONObject(expectValue);
        actualJson = new JSONObject(actualValue);
        Assert.assertEquals(expectJson.get(START), actualJson.get(START));
        Assert.assertEquals(expectJson.get(END), actualJson.get(END));
        Assert.assertEquals(expectJson.get(GRANULARITY), actualJson.get(GRANULARITY));
    }
}
