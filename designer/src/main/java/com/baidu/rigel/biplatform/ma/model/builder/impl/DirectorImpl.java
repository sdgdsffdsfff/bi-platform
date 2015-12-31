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
package com.baidu.rigel.biplatform.ma.model.builder.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baidu.rigel.biplatform.ac.minicube.CallbackLevel;
import com.baidu.rigel.biplatform.ac.minicube.CallbackMeasure;
import com.baidu.rigel.biplatform.ac.minicube.ExtendMinicubeMeasure;
import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeDimension;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeLevel;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeSchema;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.DimensionType;
import com.baidu.rigel.biplatform.ac.model.Level;
import com.baidu.rigel.biplatform.ac.model.Measure;
import com.baidu.rigel.biplatform.ac.model.MeasureType;
import com.baidu.rigel.biplatform.ac.model.Schema;
import com.baidu.rigel.biplatform.ac.util.DeepcopyUtils;
import com.baidu.rigel.biplatform.ma.comm.util.ParamValidateUtils;
import com.baidu.rigel.biplatform.ma.model.builder.Director;
import com.baidu.rigel.biplatform.ma.model.meta.DimTableMetaDefine;
import com.baidu.rigel.biplatform.ma.model.meta.StarModel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * Only implementation of the <tt>Director</tt> interface.Implements all optional {@link Director} operations.
 * <p>
 * <hr/>
 * all know subclasses<br/>
 * : None
 * </p>
 * 
 * 
 * @see com.baidu.rigel.biplatform.ac.model.Schema
 * @see com.baidu.rigel.biplatform.ma.model.meta.StarModel
 * @since JDK1.8 or after
 * @version Silkroad 1.0.1
 * @author david.wang
 * 
 */
@Service
public class DirectorImpl implements Director {

    /**
     * the builder service of schema
     */
    private SchemaBuilder schemaBuilder = new SchemaBuilder();

    /**
     * logger
     */
    private Logger logger = LoggerFactory.getLogger(DirectorImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema(StarModel[] starModels) {
        // check the input validate or not
        if (!ParamValidateUtils.check("starModels", starModels)) {
            return null;
        }
        logger.info("begin generate schema with start models");
        // build schema with the star model reference datasource's id
        MiniCubeSchema schema = (MiniCubeSchema) buildSchema(starModels[0].getDsId());
        if (!ParamValidateUtils.check("schema", schema)) {
            return null;
        }
        // build cubes for the schema
        schema.setCubes(buildCubes(schema, starModels));
        return schema;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema modifySchemaWithNewModel(Schema schema, StarModel[] starModels) {
        // check input if invalidate return
        if (!ParamValidateUtils.check("starModels", starModels)) {
            return schema;
        }

        // make sure schema correct
        if (!ParamValidateUtils.check("schema", schema)) {
            throw new IllegalStateException("ori schema can not be null or must include cubes");
        }
        if (!ParamValidateUtils.check("cubes", schema.getCubes())) {
            throw new IllegalStateException("ori schema can not be null or must include cubes");
        }
        // create new map store the new cubes which generate from new star models
        Map<String, MiniCube> newCubes = Maps.newLinkedHashMap();

        // because the new star models lost some info, so create new schema and copy lost info to the schema
        MiniCubeSchema newSchema = new MiniCubeSchema();
        // copy lost info
        newSchema.setDatasource(schema.getDatasource());
        newSchema.setId(schema.getId());
        newSchema.setName(schema.getName());
        newSchema.setVisible(true);
        newSchema.setDescription(schema.getDescription());
        CubeBuilder builder = new CubeBuilder();

        for (StarModel model : starModels) {
            Cube cube = schema.getCubes().get(model.getCubeId());
            // maybe this is new cube
            if (cube == null) {
                cube = builder.buildCube(model);
            } else {
                cube = modifyCubeWithModel(builder, schema, model);
            }
            if (cube != null) {
                ((MiniCube) cube).setSchema(newSchema);
                newCubes.put(cube.getId(), (MiniCube) cube);
            }
        }
        ((MiniCubeSchema) newSchema).setCubes(newCubes);
        return newSchema;
    }

    /**
     * 
     * update {@link Cube} with {@link StarModel}
     * 
     * @param builder -- CubeBuilder
     * @param oriSchema -- original schema
     * @param starModel -- new star model
     * @return cube -- already update cube
     * @see com.baidu.rigel.biplatform.ma.model.builder.impl.CubeBuilder
     * @see com.baidu.rigel.biplatform.ac.model.Schema
     * 
     */
    private Cube modifyCubeWithModel(CubeBuilder builder, Schema oriSchema, StarModel starModel) {
        MiniCube oriCube = (MiniCube) oriSchema.getCubes().get(starModel.getCubeId());
        // StarModelBuilder modelBuilder = new StarModelBuilder();
        // StarModel oriModel = modelBuilder.buildModel((MiniCube) oriCube);
        // if true the star model not changed
        // if (oriModel.equals(starModel)) {
        // return oriCube;
        // }
        MiniCube cube = new MiniCube();
        cube.setCaption(oriCube.getCaption());
        cube.setId(oriCube.getId());
        cube.setMutilple(oriCube.isMutilple());
        cube.setSource(oriCube.getSource());
        cube.setName(oriCube.getName());
        cube.setVisible(oriCube.isVisible());
        cube.setDivideTableStrategyVo(oriCube.getDivideTableStrategyVo());
        Map<String, Measure> newMeasures = modifyMeasures(starModel, oriCube);
        cube.setMeasures(newMeasures);

        // store the newest dimension
        Map<String, Dimension> dims = new HashMap<String, Dimension>();
        Map<String, Dimension> oriDims = oriCube.getDimensions();
        DimensionBuilder dimBuilder = new DimensionBuilder();
        List<Dimension> newDimensions = Lists.newArrayList();

        for (DimTableMetaDefine dimTable : starModel.getDimTables()) {
            Dimension[] buildDims = dimBuilder.buildDimensions(dimTable, starModel.getFactTable());
            Collections.addAll(newDimensions, buildDims);
        }
        dims = addOrReplaceDims(oriDims, newDimensions);

        dims = modifyDimGroup(dims, DeepcopyUtils.deepCopy(oriDims));
        resetMeasures(dims, oriCube.getDimensions(), cube);
        cube.setDimensions(dims);
        return cube;
    }

    /**
     * 修正指标定义，将原来已经转换为维度的指标替换掉
     * 
     * @param dims
     * @param oriDims
     * @param cube
     */
    private void resetMeasures(Map<String, Dimension> dims, Map<String, Dimension> oriDims, MiniCube cube) {
        Iterator<String> it = cube.getMeasures().keySet().iterator();
        final Map<String, Dimension> tmp = Maps.newHashMap();
        oriDims.values().forEach(dim -> {
            if (dim.getType() != DimensionType.GROUP_DIMENSION && dim.getTableName().equals(cube.getSource())) {
                tmp.put(dim.getName(), dim);
            }
        });
        while (it.hasNext()) {
            Measure m = cube.getMeasures().get(it.next());
            if (tmp.containsKey(m.getName())) {
                Dimension dim = tmp.get(m.getName());
                dims.put(dim.getId(), dim);
                it.remove();
            }
        }
    }

    /**
     * 
     * modify {@link Dimension} group define
     * 
     * @param dims -- the newest dimensions which update through star model
     * @param oriDims -- original dimensions
     * @return the newest dimensions map, key is dimension's id
     * 
     */
    private Map<String, Dimension> modifyDimGroup(Map<String, Dimension> dims, Map<String, Dimension> oriDims) {
        Set<String> allLevelIds = getAllLevels(dims);
        Iterator<Entry<String, Dimension>> it = oriDims.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Dimension> tmpDim = it.next();
            it.remove();
            Dimension dim = tmpDim.getValue();
            if (dim.getType() == DimensionType.GROUP_DIMENSION) {
                Iterator<Map.Entry<String, Level>> levelIterator = dim.getLevels().entrySet().iterator();
                for (; levelIterator.hasNext();) {
                    Map.Entry<String, Level> tmp = levelIterator.next();
                    Level value = tmp.getValue();
                    String key = value.getFactTableColumn() + "_" + value.getDimTable() + "_" + value.getName();
                    if (!allLevelIds.contains(key)) {
                        levelIterator.remove();
                    }
                }
                if (dim.getLevels().size() > 0) {
                    dims.put(dim.getId(), dim);
                }
            }
        }
        return dims;
    }

    /**
     * 
     * get all dimensions's levels id list
     * 
     * @param dims -- dimension instance map
     * @return the set which contains id of dimension's levels
     * @see com.baidu.rigel.biplatform.ac.model.Dimension
     * 
     */
    private Set<String> getAllLevels(Map<String, Dimension> dims) {
        Set<String> levelKeys = new HashSet<String>();
        for (Map.Entry<String, Dimension> dim : dims.entrySet()) {
            String key = dim.getValue().getFacttableColumn() + "_" + dim.getValue().getTableName();
            dim.getValue().getLevels().values().forEach(level -> {
                levelKeys.add(key + "_" + level.getName());
            });
        }
        return levelKeys;
    }

    /**
     * 
     * add or update {@link Dimension} define through the dimensions which generate from new star model
     * 
     * @param oriDims -- original dimension which already defined in schema
     * @param buildDims -- new dimension which generate from new star model
     * @return the newest dimension map, key is dimension's id
     * 
     */
    private Map<String, Dimension> addOrReplaceDims(Map<String, Dimension> oriDims, List<Dimension> buildDims) {
        Map<String, Dimension> dims = new LinkedHashMap<String, Dimension>();
        // if (oriDims.isEmpty()) {
        // buildDims.forEach(dim -> {
        // oriDims.put(dim.getId(), dim);
        // });
        // return dims;
        // }
        final Map<String, Dimension> dimIdents = new LinkedHashMap<String, Dimension>();
        oriDims.values().forEach(dim -> {
            dimIdents.put(buildDimIdent(dim), dim);
        });
        buildDims.forEach(dim -> {
            String dimIdent = buildDimIdent(dim);
            if (dimIdents.containsKey(dimIdent)) {
                Dimension tmp = dimIdents.get(dimIdent);
                ((MiniCubeDimension) dim).setId(tmp.getId());
                dims.put(tmp.getId(), dim);
            } else {
                dims.put(dim.getId(), dim);
            }
        });
        return dims;
    }

    /**
     * 
     * generate dimensions's identification through {@link Dimension} instance
     * 
     * @param dim -- dimension instance
     * @return dimIdent -- dimension identification
     * 
     */
    private String buildDimIdent(Dimension dim) {
        String ident = dim.getTableName();
        Level level = dim.getLevels().values().toArray(new Level[0])[0];
        if (level instanceof MiniCubeLevel) {
            return ident + "_" + ((MiniCubeLevel) level).getSource();
        }
        return ident + "_" + ((CallbackLevel) level).getName();
    }

    /**
     * 
     * update the cube's measures: remove unused measures, add new measures, copy the calculate measures and no changed
     * measures
     * 
     * @param starModel -- star model
     * @param oriCube -- original cube
     * @return the new measures, key is measure id
     * @see com.baidu.rigel.biplatform.ac.model.Measure
     * 
     */
    private Map<String, Measure> modifyMeasures(StarModel starModel, MiniCube oriCube) {
        Map<String, Measure> newMeasures = new HashMap<String, Measure>();
        Map<String, Measure> oriMeasures = oriCube.getMeasures();
        // store new reference column info
        final Set<String> refCol = new HashSet<String>();
        // iterate all the dimension table and find the reference column
        starModel.getDimTables().forEach(dimTable -> {
            refCol.add(dimTable.getReference().getMajorColumn());
        });

        final Map<String, String> oriMeasureNameRep = new HashMap<String, String>();
        // remove all the measures which already convert to dimension
        oriMeasures.values().stream().filter(oriMeasure -> {
            return !refCol.contains(oriMeasure.getDefine()) && !StringUtils.isEmpty(oriMeasure.getName());
        }).map(oriMeasure -> {
            return oriMeasure.getName() + "&&" + oriMeasure.getId();
        }).distinct().forEach(str -> {
            String[] tmp = str.split("&&");
            oriMeasureNameRep.put(tmp[0], tmp[1]);
        });

        final MeasureBuilder measureBuilder = new MeasureBuilder();
        starModel.getFactTable().getColumnList().stream().forEach(col -> {
            // if true measure already convert to dimension
                if (!refCol.contains(col.getName()) && !StringUtils.isEmpty(col.getName())) {
                    // old measure
                    if (oriMeasureNameRep.containsKey(col.getName())) {
                        String id = oriMeasureNameRep.get(col.getName());
                        newMeasures.put(id, oriMeasures.get(id));
                    } else {
                        Measure m = measureBuilder.buildMeasure(col);
                        newMeasures.put(m.getId(), m);
                    }
                }
            });
        // 同环比、计算列处理
        oriCube.getMeasures().forEach((k, v) -> {
            if (v instanceof CallbackMeasure || v.getType() == MeasureType.SR || v.getType() == MeasureType.RR) {
                newMeasures.put(v.getId(), v);
            }
            // if (v.getType() == MeasureType.CAL || v.getType() == MeasureType.SR || v.getType() == MeasureType.RR) {
                if (v.getType() == MeasureType.CAL) {
                    ExtendMinicubeMeasure m = (ExtendMinicubeMeasure) v;
                    // if (checkRefMeasuer(m.getRefIndNames(), newMeasures)) {
                    // newMeasures.put(v.getId(), v);
                    // }
                    if (checkRefMeasuer(m, newMeasures)) {
                        newMeasures.put(v.getId(), v);
                    }
                }
            });

        return newMeasures;
    }

    /**
     * 检查计算列的基础指标在指标列表里还存不存在
     * @param m ExtendMinicubeMeasure
     * @param newMeasures newMeasures
     * @return  如果存在，返回true，不存在则返回false；
     */
    private boolean checkRefMeasuer(ExtendMinicubeMeasure m, Map<String, Measure> newMeasures) {
        if(StringUtils.isEmpty(m.getFormula())){
            return false;
        }
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(m.getFormula());
        Set<String> baseMeasureSet = Sets.newHashSet();
        while (matcher.find()) {
            baseMeasureSet.add(matcher.group(1));
            logger.info("the MinicubeMeasure :[" + m.getName() + "]'Formula :[" + m.getFormula()
                    + "] has baseMeasure :[" + matcher.group(1) + "]");
        }
        String[] measuerNames = newMeasures.values().stream().map(measure -> {
            return measure.getName();
        }).toArray(String[]::new);
        List<String> tmp = Lists.newArrayList();
        Collections.addAll(tmp, measuerNames);
        for (String ref : baseMeasureSet) {
            if (!tmp.contains(ref)) {
                return false;
            }
        }
        return true;
    }

//    /**
//     * 
//     * @param newMeasures
//     * @param refNames
//     * @return
//     */
//    private boolean checkRefMeasuer(Set<String> refNames, Map<String, Measure> newMeasures) {
//        boolean rs = true;
//        if (refNames == null || refNames.size() == 0) {
//            return rs;
//        }
//        String[] measuerNames = newMeasures.values().stream().map(m -> {
//            return m.getName();
//        }).toArray(String[]::new);
//        List<String> tmp = Lists.newArrayList();
//        Collections.addAll(tmp, measuerNames);
//        for (String ref : refNames) {
//            if (!tmp.contains(ref)) {
//                return false;
//            }
//        }
//        // for (String str : refNames) {
//        // if (refNames.contains(entry.getKey()) || refNames.contains(entry.getValue().getName())) {
//        // continue;
//        // }
//        // }
//        return rs;
//    }

    /**
     * 
     * {@inheritDoc}
     * 
     */
    @Override
    public StarModel[] getStarModel(Schema schema) {
        if (schema == null) {
            logger.error("can not create star model with null schema");
            return new StarModel[0];
        }
        Collection<? extends Cube> cubes = schema.getCubes().values();
        if (cubes == null || cubes.size() == 0) {
            logger.error("can not create star model with null cubes");
            return new StarModel[0];
        }
        List<StarModel> rs = new ArrayList<StarModel>();
        StarModelBuilder modelBuilder = new StarModelBuilder();
        for (Cube cube : cubes) {
            StarModel model = modelBuilder.buildModel((MiniCube) cube);
            if (model == null) {
                continue;
            }
            rs.add(model);
        }
        logger.info("create star model with schema successfully");
        return rs.toArray(new StarModel[0]);
    }

    /**
     * build cubes({@link Cube}'s Map) with star model({@link StarModel})
     * 
     * @param starModels -- star model array
     * @param schema -- schema instance
     * @return if success return map instance, include all cubes but empty, key is cube's id
     *
     */
    private Map<String, MiniCube> buildCubes(Schema schema, StarModel[] starModels) {
        Map<String, MiniCube> cubes = new HashMap<String, MiniCube>();
        logger.info("begin create starModel");
        // make sure star model is not empty
        if (starModels == null) {
            logger.info("star models is null");
            return cubes;
        }
        if (starModels.length <= 0) {
            logger.info("star models's size is 0");
            return cubes;
        }
        // create cube
        CubeBuilder builder = new CubeBuilder();
        for (StarModel model : starModels) {
            Cube cube = builder.buildCube(model);
            if (cube != null) {
                ((MiniCube) cube).setSchema(schema);
                cubes.put(cube.getId(), (MiniCube) cube);
            }
        }
        logger.info("create cube successfully");
        return cubes;
    }

    /**
     * 
     * build {@link Schema} with datasource's id
     * 
     * @param dsId -- datasource's id
     * @return schema -- schema instance
     * 
     */
    private Schema buildSchema(String dsId) {
        if (StringUtils.isEmpty(dsId)) {
            logger.error("datasource id can not be null");
            throw new IllegalStateException("star model's datasource id is null");
        }
        Schema schema = schemaBuilder.buildSchema(dsId);
        if (schema == null) {
            logger.error("can not be create schema with starModel");
            return null;
        }
        logger.info("transform model to schema successfully " + schema);
        return schema;
    }

}
