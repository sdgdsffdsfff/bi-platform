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
package com.baidu.rigel.biplatform.ac.util;

import org.junit.Assert;
import org.junit.Test;


/**
 *Description:
 * @author david.wang
 *
 */
public class DataModelUtilsTest {
    @Test
    public void testIsChar() {
        Assert.assertTrue(DataModelUtils.isChar("varchar"));
        Assert.assertTrue(DataModelUtils.isChar("varchar2"));
        Assert.assertTrue(DataModelUtils.isChar("char"));
        Assert.assertTrue(DataModelUtils.isChar("date"));
        Assert.assertTrue(DataModelUtils.isChar("datetime"));
        Assert.assertTrue(DataModelUtils.isChar("timestamp"));
        Assert.assertTrue(DataModelUtils.isChar("blob"));
        Assert.assertTrue(DataModelUtils.isChar("text"));
        Assert.assertTrue(DataModelUtils.isChar("longtext"));
        Assert.assertTrue(DataModelUtils.isChar("mediumblob"));
        Assert.assertTrue(DataModelUtils.isChar("mediumtext"));
        Assert.assertTrue(DataModelUtils.isChar("time"));
        
        Assert.assertFalse(DataModelUtils.isChar("int"));
        Assert.assertFalse(DataModelUtils.isChar("bigint"));
        Assert.assertFalse(DataModelUtils.isChar("decimal"));
        Assert.assertFalse(DataModelUtils.isChar("bigint"));
        Assert.assertFalse(DataModelUtils.isChar("double"));
        Assert.assertFalse(DataModelUtils.isChar("float"));
        Assert.assertFalse(DataModelUtils.isChar("mediumint"));
        Assert.assertFalse(DataModelUtils.isChar("numeric"));
        Assert.assertFalse(DataModelUtils.isChar("tinyint"));
        Assert.assertFalse(DataModelUtils.isChar("bigint"));
        Assert.assertFalse(DataModelUtils.isChar("long"));
        Assert.assertFalse(DataModelUtils.isChar("integer"));
    }
}
