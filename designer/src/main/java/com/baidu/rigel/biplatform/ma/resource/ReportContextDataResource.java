package com.baidu.rigel.biplatform.ma.resource;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.SerializationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.rigel.biplatform.ac.minicube.CallbackLevel;
import com.baidu.rigel.biplatform.ac.minicube.CallbackMember;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMember;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.DimensionType;
import com.baidu.rigel.biplatform.ac.model.Level;
import com.baidu.rigel.biplatform.ac.model.Member;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.util.DeepcopyUtils;
import com.baidu.rigel.biplatform.ac.util.HttpRequest;
import com.baidu.rigel.biplatform.ac.util.MetaNameUtil;
import com.baidu.rigel.biplatform.api.client.service.FileService;
import com.baidu.rigel.biplatform.api.client.service.FileServiceException;
import com.baidu.rigel.biplatform.cache.util.ApplicationContextHelper;
import com.baidu.rigel.biplatform.ma.comm.util.ParamValidateUtils;
import com.baidu.rigel.biplatform.ma.ds.exception.DataSourceConnectionException;
import com.baidu.rigel.biplatform.ma.ds.exception.DataSourceOperationException;
import com.baidu.rigel.biplatform.ma.ds.service.DataSourceConnectionService;
import com.baidu.rigel.biplatform.ma.ds.service.DataSourceConnectionServiceFactory;
import com.baidu.rigel.biplatform.ma.ds.service.DataSourceService;
import com.baidu.rigel.biplatform.ma.model.consts.Constants;
import com.baidu.rigel.biplatform.ma.model.ds.DataSourceDefine;
import com.baidu.rigel.biplatform.ma.report.exception.CacheOperationException;
import com.baidu.rigel.biplatform.ma.report.model.ExtendArea;
import com.baidu.rigel.biplatform.ma.report.model.ExtendAreaContext;
import com.baidu.rigel.biplatform.ma.report.model.ExtendAreaType;
import com.baidu.rigel.biplatform.ma.report.model.Item;
import com.baidu.rigel.biplatform.ma.report.model.LinkParams;
import com.baidu.rigel.biplatform.ma.report.model.LiteOlapExtendArea;
import com.baidu.rigel.biplatform.ma.report.model.PlaneTableCondition;
import com.baidu.rigel.biplatform.ma.report.model.ReportDesignModel;
import com.baidu.rigel.biplatform.ma.report.query.ReportRuntimeModel;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.CellData;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.PivotTable;
import com.baidu.rigel.biplatform.ma.report.query.pivottable.RowHeadField;
import com.baidu.rigel.biplatform.ma.report.service.ReportDesignModelService;
import com.baidu.rigel.biplatform.ma.report.service.ReportModelQueryService;
import com.baidu.rigel.biplatform.ma.report.utils.QueryDataUtils;
import com.baidu.rigel.biplatform.ma.report.utils.QueryUtils;
import com.baidu.rigel.biplatform.ma.resource.cache.ReportModelCacheManager;
import com.baidu.rigel.biplatform.ma.resource.utils.PlaneTableUtils;
import com.baidu.rigel.biplatform.ma.resource.utils.ResourceUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 此接口提供与报表查询上下文交互相关的数据查询服务
 * 
 * @author majun04
 *
 */
@RestController
@RequestMapping("/silkroad/reports")
public class ReportContextDataResource extends BaseResource {
    /**
     * logger
     */
    private Logger logger = LoggerFactory.getLogger(ReportContextDataResource.class);
    /**
     * reportModelCacheManager
     */
    @Resource
    private ReportModelCacheManager reportModelCacheManager;

    /**
     * 报表数据查询服务
     */
    @Resource
    private ReportModelQueryService reportModelQueryService;

    /**
     * reportDesignModelService
     */
    @Resource(name = "reportDesignModelService")
    private ReportDesignModelService reportDesignModelService;

    @Resource(name = "fileService")
    private FileService fileService;

    /**
     * 初始化查询参数,初始化查询区域参数
     * 
     * @param reportId
     * @param request
     * @return ResponseResult
     */
    @RequestMapping(value = "/{reportId}/init_params", method = { RequestMethod.POST })
    public ResponseResult initParams(@PathVariable("reportId") String reportId, HttpServletRequest request) {
        long begin = System.currentTimeMillis();
        logger.info("[INFO]--- ---begin init params with report id {}", reportId);
        String areaIdList = request.getParameter("paramList");
        String[] areaIds = null;
        final ReportDesignModel model = getDesignModelFromRuntimeModel(reportId);
        if (!StringUtils.isEmpty(areaIdList)) {
            areaIds = areaIdList.split(",");
        }
        if (areaIds == null || areaIds.length == 0) {
            ResponseResult rs = new ResponseResult();
            rs.setStatus(0);
            logger.info("[INFO]--- --- not needed init global params");
            return rs;
        }
        final ReportRuntimeModel runtimeModel = reportModelCacheManager.getRuntimeModel(reportId);
        Map<String, Object> datas = Maps.newConcurrentMap();
        Map<String, String> params = Maps.newHashMap();
        runtimeModel.getContext().getParams().forEach((k, v) -> {
            params.put(k, v == null ? "" : v.toString());
        });

        for (final String areaId : areaIds) {
            ExtendArea area = model.getExtendById(areaId);
            Cube cube = null;
            if (area != null) {
                // 获取对应的cube
                cube = model.getSchema().getCubes().get(area.getCubeId());
            }
            // TODO 查询条件回填？
            if (area != null && isQueryComp(area.getType()) && !area.listAllItems().isEmpty()) {
                Item item = area.listAllItems().values().toArray(new Item[0])[0];
                Cube tmpCube = QueryUtils.transformCube(cube);
                String dimId = item.getOlapElementId();
                Dimension dim = cube.getDimensions().get(dimId);
                if (dim != null) {
                    List<Map<String, String>> values;
                    try {
                        values = Lists.newArrayList();
                        params.remove(dim.getId());
                        params.put(Constants.LEVEL_KEY, "1");
                        List<Member> members =
                                reportModelQueryService.getMembers(tmpCube, tmpCube.getDimensions().get(dim.getName()),
                                        params, securityKey).get(0);
                        members.forEach(m -> {
                            Map<String, String> tmp = Maps.newHashMap();
                            tmp.put("value", m.getUniqueName());
                            tmp.put("text", m.getCaption());
                            if (dim.getLevels().size() <= 1) {
                                tmp.put("isLeaf", "1");
                            }
                            MiniCubeMember realMember = (MiniCubeMember) m;
                            if (realMember.getParent() != null) {
                                tmp.put("parent", realMember.getParent().getUniqueName());
                            } else {
                                tmp.put("parent", "");
                            }
                            values.add(tmp);
                            List<Map<String, String>> children = getChildren(realMember, realMember.getChildren());
                            if (children != null && !children.isEmpty()) {
                                values.addAll(children);
                            }
                        });
                        // List<Map<String, String>> values =
                        // QueryUtils.getMembersWithChildrenValue(members, tmpCube, dsInfo, Maps.newHashMap());
                        Map<String, Object> datasource = Maps.newHashMap();
                        datasource.put("datasource", values);
                        QueryDataUtils.fillBackParamValues(runtimeModel, dim, datasource);
                        datas.put(areaId, datasource);
                    } catch (Exception e) {
                        logger.info(e.getMessage(), e);
                    }
                }
            }
        }
        ResponseResult rs = new ResponseResult();
        rs.setStatus(0);
        rs.setData(datas);
        rs.setStatusInfo("OK");
        logger.info("[INFO]--- --- successfully init params, cost {} ms", (System.currentTimeMillis() - begin));
        return rs;
    }

    /**
     * 查询条件数据获取服务
     */
    @RequestMapping(value = "/{reportId}/members/{areaId}", method = { RequestMethod.POST })
    public ResponseResult getMemberWithParent(@PathVariable("reportId") String reportId,
            @PathVariable("areaId") String areaId, HttpServletRequest request) {
        long begin = System.currentTimeMillis();
        logger.info("[INFO]--- ---begin init params with report id {}", reportId);
        String currentUniqueName = request.getParameter("uniqueName");
        // int level = MetaNameUtil.parseUnique2NameArray(currentUniqueName).length - 1;
        final ReportDesignModel model = getDesignModelFromRuntimeModel(reportId);
        final ReportRuntimeModel runtimeModel = reportModelCacheManager.getRuntimeModel(reportId);
        Map<String, Object> datas = Maps.newConcurrentMap();
        Map<String, String> params = Maps.newHashMap();
        runtimeModel.getContext().getParams().forEach((k, v) -> {
            params.put(k, v == null ? "" : v.toString());
        });
        ExtendArea area = model.getExtendById(areaId);
        if (area != null && isQueryComp(area.getType()) && !area.listAllItems().isEmpty()) {
            Item item = area.listAllItems().values().toArray(new Item[0])[0];
            Cube cube = model.getSchema().getCubes().get(area.getCubeId());
            Cube tmpCube = QueryUtils.transformCube(cube);
            String dimId = item.getOlapElementId();
            Dimension dim = cube.getDimensions().get(dimId);
            if (QueryDataUtils.isCallbackDim(dim)) {
                String paramsName = QueryDataUtils.getParamName(dim, model);
                String paramsValue = QueryDataUtils.getCallbackParamValue(paramsName, currentUniqueName);
                params.put(paramsName, paramsValue);
            }

            if (dim != null) {
                List<Map<String, String>> values;
                try {
                    values = Lists.newArrayList();
                    params.remove(dim.getId());
                    List<Member> members =
                            reportModelQueryService.getMembers(tmpCube, currentUniqueName, params, securityKey);
                    members.forEach(m -> {
                        Map<String, String> tmp = Maps.newHashMap();
                        int curLevel = MetaNameUtil.parseUnique2NameArray(m.getUniqueName()).length - 1;
                        tmp.put("value", m.getUniqueName());
                        tmp.put("text", m.getCaption());
                        if (QueryDataUtils.isCallbackDim(dim) && m instanceof CallbackMember) {
                            CallbackMember cm = (CallbackMember) m;
                            tmp.put("isLeaf", Boolean.toString(!cm.isHasChildren()));
                        } else {
                            tmp.put("isLeaf", Boolean.toString(curLevel == dim.getLevels().size()));
                        }

                        values.add(tmp);
                    });
                    Map<String, Object> datasource = Maps.newHashMap();
                    datasource.put("datasource", values);
                    if (area.getType() == ExtendAreaType.CASCADE_SELECT) {
                        QueryDataUtils.fillBackParamValues(runtimeModel, dim, datasource);
                    }
                    datas.put(areaId, datasource);
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                } // end catch
            } // end if dim != null
        } // end if area != null
        ResponseResult rs = new ResponseResult();
        rs.setStatus(0);
        rs.setData(datas);
        rs.setStatusInfo("OK");
        logger.info("[INFO]--- --- successfully query member, cost {} ms", (System.currentTimeMillis() - begin));
        return rs;
    }

    private List<Map<String, String>> getChildren(Member parent, List<Member> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }
        List<Map<String, String>> rs = Lists.newArrayList();
        MiniCubeMember tmp = null;
        for (Member m : children) {
            tmp = (MiniCubeMember) m;
            Map<String, String> map = Maps.newHashMap();
            map.put("value", tmp.getUniqueName());
            map.put("text", tmp.getCaption());
            map.put("parent", parent.getUniqueName());
            rs.add(map);
            if (!CollectionUtils.isEmpty(tmp.getChildren())) {
                rs.addAll(getChildren(tmp, tmp.getChildren()));
            }
        }
        return rs;
    }

    /**
     * @param reportId
     * @return ReportDesignModel
     */
    ReportDesignModel getDesignModelFromRuntimeModel(String reportId) {
        return reportModelCacheManager.getRuntimeModel(reportId).getModel();
    }

    /**
     * 
     * @param type 区域类型
     * @return boolean
     */
    private boolean isQueryComp(ExtendAreaType type) {
        return QueryUtils.isFilterArea(type);
    }

    /**
     * 
     * @param reportId
     * @param request
     * @return ResponseResult
     */
    @RequestMapping(value = "/{reportId}/report_id", method = { RequestMethod.GET })
    public ResponseResult getReport(@PathVariable("reportId") String reportId, HttpServletRequest request) {
        long begin = System.currentTimeMillis();
        logger.info("[INFO] --- --- begin query report model");
        ReportDesignModel model = null;
        try {
            model = this.getDesignModelFromRuntimeModel(reportId); // reportModelCacheManager.getReportModel(reportId);
        } catch (CacheOperationException e1) {
            logger.info("[INFO]--- --- can't not get report form cache", e1.getMessage());
            return ResourceUtils.getErrorResult(e1.getMessage(), ResponseResult.FAILED);
        }
        // reportModelCacheManager.loadReportModelToCache(reportId);
        ResponseResult rs = ResourceUtils.getCorrectResult("OK", model);
        logger.info("[INFO] --- --- query report model successuffly, cost {} ms", (System.currentTimeMillis() - begin));
        return rs;
    }

    /**
     * 
     * @param reportId
     * @param request
     * @param response
     * @return String
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{reportId}/report_vm", method = { RequestMethod.GET, RequestMethod.POST },
            produces = "text/html;charset=utf-8")
    public String queryVM(@PathVariable("reportId") String reportId, HttpServletRequest request,
            HttpServletResponse response) {
        long begin = System.currentTimeMillis();
        ReportDesignModel model = null;
        String reportPreview = request.getParameter("reportPreview");
        String imageId = request.getParameter("reportImageId");
        ReportRuntimeModel runtimeModel = null;
        try {
            if (StringUtils.isEmpty(imageId) || reportId.equals(imageId)) {
                if (!StringUtils.isEmpty(reportPreview) && Boolean.valueOf(reportPreview)) {
                    model = reportModelCacheManager.getReportModel(reportId);
                    if (model != null) {
                        model = DeepcopyUtils.deepCopy(model);
                    }
                } else if ((runtimeModel = reportModelCacheManager.getRuntimeModelUnsafety(reportId)) == null) {
                    model = reportDesignModelService.getModelByIdOrName(reportId, true);
                }
            }
        } catch (CacheOperationException e1) {
            logger.info("[INFO]--- ---Fail in loading release report model into cache. ", e1);
            // throw new IllegalStateException();
        }

        if (model != null) {
            runtimeModel = new ReportRuntimeModel(reportId);
            runtimeModel.init(model, true);
        } else if (runtimeModel == null) {
            try {
                String path = getSavedReportPath(request);
                String fileName = path + File.separator + reportId + File.separator + imageId;
                runtimeModel = (ReportRuntimeModel) SerializationUtils.deserialize(fileService.read(fileName));
                model = runtimeModel.getModel();
            } catch (FileServiceException e) {
                logger.info("[INFO]--- ---加载保存的报表失败 ", e);
            }
        } else {
            model = runtimeModel.getModel();
        }
        if (runtimeModel == null) {
            logger.info("[INFO]--- ---init runtime model failed ");
            throw new RuntimeException("初始化报表模型失败");
        }
        // modify by jiangyichao at 2014-10-10
        // 将url参数添加到全局上下文中
        Enumeration<String> params = request.getParameterNames();
        // 请求参数
        Map<String, String> requestParams = Maps.newHashMap();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            if (request.getParameter(paramName) != null) {
                runtimeModel.getContext().put(paramName, request.getParameter(paramName));
                requestParams.put(paramName, request.getParameter(paramName));
            }
        }
        // 添加cookie内容
        runtimeModel.getContext().put(HttpRequest.COOKIE_PARAM_NAME, request.getHeader("Cookie"));

        // 获取多维数据表的报表Id
        String fromReportId = request.getParameter("fromReportId");
        // 平面表id
        String toReportId = request.getParameter("toReportId");
        // 如果是由多维跳转到明细
        if (!StringUtils.isEmpty(fromReportId) && !StringUtils.isEmpty(toReportId)) {
            // 从cache中取得多维表的运行态模型
            ReportRuntimeModel fromRuntimeModel = reportModelCacheManager.getRuntimeModel(fromReportId);
            // 如果从cache中取不到多维表的运行态模型，则抛出异常
            if (fromRuntimeModel == null) {
                logger.info("[INFO]--- ---无法获取多维表运行态模型, id :", fromReportId);
                throw new IllegalStateException("[INFO]--- ---无法获取多维表运行态模型, id :" + fromReportId);
            }

            // 多维表cube
            Cube multiCube = null;
            ExtendArea[] multiExtendAreas = fromRuntimeModel.getModel().getExtendAreaList();
            // 获取多维表对应的因为此处仅考虑一个cube
            // fix by yichao.jiang 并非每个控件都存在cube，cube可能会取不到，必须保证该extendArea中有cubeId，还不能是查询过滤控件
            for (ExtendArea extendArea : multiExtendAreas) {
                if (extendArea != null && !StringUtils.isEmpty(extendArea.getCubeId())
                        && !QueryUtils.isFilterArea(extendArea.getType())) {
                    multiCube = fromRuntimeModel.getModel().getSchema().getCubes().get(extendArea.getCubeId());
                }
            }

            // 平面表cube
            Cube planeCube = null;
            ExtendArea[] planeExtendAreas = model.getExtendAreaList();
            for (ExtendArea extendArea : planeExtendAreas) {
                if (extendArea != null && extendArea.getType() == ExtendAreaType.PLANE_TABLE) {
                    planeCube = model.getSchema().getCubes().get(extendArea.getCubeId());
                }
            }

            Map<String, PlaneTableCondition> planeTableConditions = model.getPlaneTableConditions();
            Map<String, Object> fromParams = fromRuntimeModel.getContext().getParams();
            // runtimeModel.getContext().getParams().putAll(
            // PlaneTableUtils.handelTimeCondition(cube, fromParams));
            // 如果包含跳转参数
            if (fromParams != null && fromParams.containsKey("linkBridgeParams")) {
                Map<String, LinkParams> linkParams = (Map<String, LinkParams>) fromParams.get("linkBridgeParams");
                Map<String, String> planeTableCond = Maps.newHashMap();
                if (planeTableConditions == null || planeTableConditions.size() == 0) {
                    throw new RuntimeException("the plane table conditions is empty, its id is : " + toReportId);
                }
                planeTableConditions.forEach((k, v) -> {
                    // LinkParams linkParam = linkParams.get(v.getName());
                    // if (StringUtils.isEmpty(linkParam) ||
                    // StringUtils.isEmpty(linkParam.getOriginalDimValue()) ||
                    // StringUtils.isEmpty(linkParam.getUniqueName())) {
                    // throw new RuntimeException("the need params { " + v.getName() + " } is empty, please check!");
                    // }
                        planeTableCond.put(v.getName(), v.getElementId());
                        planeTableCond.put(v.getElementId(), v.getName());
                    });

                for (String key : linkParams.keySet()) {
                    LinkParams linkParam = linkParams.get(key);
                    if (StringUtils.isEmpty(linkParam.getOriginalDimValue())
                            || StringUtils.isEmpty(linkParam.getUniqueName())) {
                        continue;
                    }
                    String newValue = null;
                    String planeTableConditionKey = null;
                    try {
                        planeTableConditionKey = planeTableCond.get(linkParam.getParamName());
                        // for (String conditionKey : planeTableConditions.keySet()) {
                        // if (planeTableConditions.get(conditionKey).getName().
                        // equals(linkParam.getParamName())) {
                        // planeTableConditionKey = conditionKey;
                        // }
                        // }
                        if (linkParam.getOriginalDimValue() != null
                                && PlaneTableUtils.isTimeDim(planeCube, planeTableConditionKey)) {
                            // 如果是普通时间JSON字符串
                            if (PlaneTableUtils.isTimeJson(linkParam.getOriginalDimValue())) {
                                newValue = linkParam.getOriginalDimValue();
                            } else {
                                // 如果不是规范的时间JSON字符串，则需特殊处理
                                newValue =
                                        PlaneTableUtils.convert2TimeJson(linkParam.getOriginalDimValue(), fromParams);
                            }
                        } else {
                            if (MetaNameUtil.isUniqueName(linkParam.getUniqueName())) {
                                requestParams.put(HttpRequest.COOKIE_PARAM_NAME, request.getHeader("Cookie"));
                                newValue =
                                        this.handleReqParams4PlaneTable(multiCube, planeTableCond,
                                                linkParam.getUniqueName(), requestParams, securityKey);
                            } else {
                                newValue = linkParam.getOriginalDimValue();
                            }
                        }
                        logger.debug("the linkParam {" + linkParam.getParamName() + "}, and it's origin value is ["
                                + linkParam.getOriginalDimValue() + "], and it's new value are [" + newValue + "]");
                    } catch (Exception e) {
                        logger.error("处理平面表参数出错，请检查!");
                        throw new RuntimeException("处理平面表参数出错，请检查!");
                    }
                    if (newValue != null) {
                        runtimeModel.getContext().getParams().put(key, newValue);
                    }
                    if (planeTableConditionKey != null && newValue != null) {
                        runtimeModel.getContext().getParams().put(planeTableConditionKey, newValue);
                    }
                }
            }
        } else {
            /**
             * 依据查询请求，根据报表参数定义，增量添加报表区域模型参数
             */
            Map<String, Object> tmp = QueryUtils.resetContextParam(request, model);
            runtimeModel.getContext().getParams().putAll(tmp);
        }
        if (StringUtils.isEmpty(imageId) || reportId.equals(imageId)) {
            reportModelCacheManager.updateRunTimeModelToCache(reportId, runtimeModel);
        } else {
            reportModelCacheManager.updateRunTimeModelToCache(imageId, runtimeModel);
        }
        StringBuilder builder = buildVMString(reportId, request, response, model);
        logger.info("[INFO] query vm operation successfully, cost {} ms", (System.currentTimeMillis() - begin));
        // 如果请求中包含UID 信息，则将uid信息写入cookie中，方便后边查询请求应用
        String uid = request.getParameter(UID_KEY);
        if (uid != null) {
            Cookie cookie = new Cookie(UID_KEY, uid);
            cookie.setPath(Constants.COOKIE_PATH);
            response.addCookie(cookie);
        }
        if (request.getParameter("newPlatform") != null) {
            return "<!DOCTYPE html><html>" + "<head><meta charset=\"utf-8\"><title>报表平台-展示端</title>"
                    + "<meta name=\"description\" content=\"报表平台展示端\">"
                    + "<meta name=\"viewport\" content=\"width=device-width\">" + "</head>" + "<body>"
                    + "<script type=\"text/javascript\">" + "var seed = document.createElement('script');"
                    + "seed.src = '/silkroad/new-biplatform/asset/seed.js?action=display&t=' + (+new Date());"
                    + "document.getElementsByTagName('head')[0].appendChild(seed);" + "</script>" + "</body>"
                    + "</html>";
        }
        return builder.toString();
    }

    /**
     * 处理平面表跳转时的参数问题 handleReqParams4PlaneTable
     * 
     * @param cube
     * @param uniqueName
     * @param params
     * @return
     */
    private String handleReqParams4PlaneTable(Cube cube, Map<String, String> planeTableCond, String uniqueName,
            Map<String, String> params, String securityKey) throws DataSourceOperationException {

        if (!ParamValidateUtils.check("cube", cube)) {
            return null;
        }
        if (!ParamValidateUtils.check("planeTableCond", planeTableCond)) {
            return null;
        }
        if (!ParamValidateUtils.check("uniqueName", uniqueName)) {
            return null;
        }
        String dimName = MetaNameUtil.getDimNameFromUniqueName(uniqueName);
        Cube oriCube = QueryUtils.transformCube(cube);
        Dimension dim = oriCube.getDimensions().get(dimName);
        String[] tmp = MetaNameUtil.parseUnique2NameArray(uniqueName);
        Level tmpLevel = null;
        if (dim != null && dim.getLevels() != null) {
            tmpLevel = dim.getLevels().values().toArray(new Level[0])[0];
        }

        DataSourceDefine dsDefine = null;
        DataSourceInfo dsInfo = null;
        DataSourceService dataSourceService =
                (DataSourceService) ApplicationContextHelper.getContext().getBean("dsService");
        try {
            dsDefine = dataSourceService.getDsDefine(cube.getSchema().getDatasource());
            DataSourceConnectionService<?> dsConnService =
                    DataSourceConnectionServiceFactory.getDataSourceConnectionServiceInstance(dsDefine
                            .getDataSourceType().name());
            dsInfo = dsConnService.parseToDataSourceInfo(dsDefine, securityKey);
        } catch (DataSourceOperationException | DataSourceConnectionException e) {
            logger.error("Fail in parse datasource to datasourceInfo.", e);
            throw new DataSourceOperationException(e);
        }
        if (QueryDataUtils.isCallbackLevel(tmpLevel)) {
            // 处理callback
            CallbackLevel callbackLevel = (CallbackLevel) tmpLevel;
            Map<String, String> callbackParams = callbackLevel.getCallbackParams();
            String callbackParam = null;
            // TODO是否考虑多个参数问题
            for (String key : callbackParams.keySet()) {
                if (planeTableCond.containsKey(key)) {
                    callbackParam = key;
                    break;
                }
            }
            callbackLevel.getCallbackParams().put(callbackParam, tmp[tmp.length - 1]);
            List<Member> members = callbackLevel.getMembers(oriCube, dsInfo, params);
            for (Member member : members) {
                if (member.getUniqueName().equals(uniqueName)) {
                    MiniCubeMember miniCubeMember = (MiniCubeMember) member;
                    Set<String> queryNodes = miniCubeMember.getQueryNodes();
                    return queryNodes.stream().collect(Collectors.joining(","));
                }
            }
        } else {
            // 如果有孩子结点，则要取到孩子结点数值
            if ((dim.getLevels().size() > tmp.length - 1)) {
                Level level = dim.getLevels().values().toArray(new Level[0])[tmp.length - dim.getLevels().size()];
                if (MetaNameUtil.isAllMemberUniqueName(uniqueName)) {
                    level = dim.getLevels().values().toArray(new Level[0])[dim.getLevels().size() - 1];
                }
                return getChildMembersStrByParentAndUniqueName(level, oriCube, dsInfo, params, uniqueName);
            }
            // 如果当前维度是个维度组，并且传入参数为形如[行业维度].[交通运输].[All_交通运输s]，
            // 那么其实需要取一级行业对应的全部二级行业节点，主要用于级联下拉框控件 update by majun
            else if (dim.getType() == DimensionType.GROUP_DIMENSION && (dim.getLevels().size() == tmp.length - 1)
                    && MetaNameUtil.isAllMemberName(tmp[tmp.length - 1])) {
                // 这里需要注意，传入的level应该是指定层级的上一级
                Level level = dim.getLevels().values().toArray(new Level[0])[dim.getLevels().size() - 2];
                if (MetaNameUtil.isAllMemberName(tmp[tmp.length - 1])) {
                    uniqueName = uniqueName.substring(0, uniqueName.lastIndexOf("."));
                }
                return getChildMembersStrByParentAndUniqueName(level, oriCube, dsInfo, params, uniqueName);

            } else {
                // 如果没有孩子，则直接返回
                return tmp[tmp.length - 1];
            }
        }
        return null;
    }

    /**
     * 根据给定的level和uniqueName，查找指定条件下对应的child成员，并以以“,”连接返回
     * 
     * @param level level
     * @param oriCube oriCube
     * @param dsInfo dsInfo
     * @param params params
     * @param uniqueName uniqueName
     * @return 子member拼成的字符串，以“,”连接
     */
    private String getChildMembersStrByParentAndUniqueName(Level level, Cube oriCube, DataSourceInfo dsInfo,
            Map<String, String> params, String uniqueName) {
        List<Member> members = level.getMembers(oriCube, dsInfo, params);
        // 如果uniqueName是all节点，直接返回最底层节点的孩子成员即可 update by majun04
        if (!CollectionUtils.isEmpty(members) && MetaNameUtil.isAllMemberUniqueName(uniqueName)) {
            return members.stream().map(child -> child.getName()).collect(Collectors.joining(","));
        }
        for (Member member : members) {
            if (member.getUniqueName().equals(uniqueName)) {
                List<Member> childMembers = member.getChildMembers(oriCube, dsInfo, params);
                return childMembers.stream().map(child -> child.getName()).collect(Collectors.joining(","));
            }
        }
        return null;
    }

    /**
     * @param reportId
     * @param response
     * @param model
     * @return StringBuilder
     */
    private StringBuilder buildVMString(String reportId, HttpServletRequest request, HttpServletResponse response,
            ReportDesignModel model) {
        // TODO 临时方案，以后前端做
        String vm = model.getVmContent();
        String imageId = request.getParameter("reportImageId");
        String js =
                "<script type='text/javascript'>" + "\r\n" + "        (function(NS) {" + "\r\n"
                        + "            NS.xui.XView.start(" + "\r\n"
                        + "                'di.product.display.ui.LayoutPage'," + "\r\n" + "                {" + "\r\n"
                        + "                    externalParam: {" + "\r\n" + "                    'reportId':'"
                        + (StringUtils.isEmpty(imageId) ? reportId : imageId)
                        + "','phase':'dev'},"
                        + "\r\n"
                        + "                    globalType: 'PRODUCT',"
                        + "\r\n"
                        + "                    diAgent: '',"
                        + "\r\n"
                        + "                    reportId: '"
                        + (StringUtils.isEmpty(imageId) ? reportId : imageId)
                        + "',"
                        + "\r\n"
                        + "                    webRoot: '/silkroad',"
                        + "\r\n"
                        + "                    phase: 'dev',"
                        + "\r\n"
                        + "                    serverTime: ' "
                        + System.currentTimeMillis()
                        + "',"
                        + "\r\n"
                        + "                    funcAuth: null,"
                        + "\r\n"
                        + "                    extraOpt: (window.__$DI__NS$__ || {}).OPTIONS"
                        + "\r\n"
                        + "                }"
                        + "\r\n"
                        + "            );"
                        + "\r\n"
                        + "        })(window);"
                        + "\r\n"
                        + "    </script>" + "\r\n";
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>");
        builder.append("<html>");
        builder.append("<head>");
        builder.append("<title>" + model.getName() + "</title>");
        builder.append("<meta content='text/html' 'charset=UTF-8'>");
        final String theme = model.getTheme();
        builder.append("<link rel='stylesheet' href='/silkroad/asset/" + theme + "/css/-di-product-min.css'/>");
        builder.append("<script src='/silkroad/dep/jquery-1.11.1.min.js'/></script>");
        builder.append("</head>");
        builder.append("<body>");
        builder.append(vm);

        builder.append("<script src='/silkroad/asset/" + theme + "/-di-product-min.js'>");
        builder.append("</script>");
        builder.append(js);
        builder.append("</body>");
        builder.append("</html>");
        response.setCharacterEncoding("utf-8");
        return builder;
    }

    @RequestMapping(value = "/{reportId}/report_json", method = { RequestMethod.GET, RequestMethod.POST },
            produces = "text/plain;charset=utf-8")
    public String queryJson(@PathVariable("reportId") String reportId, HttpServletRequest request,
            HttpServletResponse response) {
        long begin = System.currentTimeMillis();
        ReportDesignModel model = null;
        String json = null;
        try {
            model = this.getDesignModelFromRuntimeModel(reportId);
            if (!CollectionUtils.isEmpty(model.getRegularTasks())) {
                json = this.setReportJson(model.getJsonContent(), "REGULAR");
            } else {
                json = this.setReportJson(model.getJsonContent(), "RTPL_VIRTUAL");
            }
        } catch (Exception e) {
            logger.info("[INFO]--- ---There are no such model in cache. Report Id: " + reportId, e);
            throw new IllegalStateException();
        }
        logger.info(json);
        response.setCharacterEncoding("utf-8");
        logger.info("[INFO] query json operation successfully, cost {} ms", (System.currentTimeMillis() - begin));
        return json;
    }

    /**
     * 设置报表的JSON
     * 
     * @param json
     * @param reportType
     * @return
     */
    private String setReportJson(String json, String reportType) {
        try {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has("entityDefs")) {
                JSONArray jsonArrays = jsonObj.getJSONArray("entityDefs");
                for (int i = 0; i < jsonArrays.length(); i++) {
                    JSONObject value = jsonArrays.getJSONObject(i);
                    if (value.has("clzKey") && value.get("clzKey") != null
                            && value.get("clzKey").toString().equals("DI_FORM")) {
                        value.put("reportType", reportType);
                        break;
                    }
                }
            }
            return jsonObj.toString();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return json;
    }

    /**
     * 
     * @param reportId
     * @param request
     * @return ResponseResult
     */
    @RequestMapping(value = "/{reportId}/runtime_model", method = { RequestMethod.POST })
    public ResponseResult initRunTimeModel(@PathVariable("reportId") String reportId, HttpServletRequest request,
            HttpServletResponse response) {
        long begin = System.currentTimeMillis();
        logger.info("[INFO]--- ---begin init runtime env");
        boolean edit = Boolean.valueOf(request.getParameter(Constants.IN_EDITOR));
        ReportDesignModel model = null;
        if (edit) {
            /**
             * 编辑报表
             */
            model = reportModelCacheManager.loadReportModelToCache(reportId);
            model.setPersStatus(false);
        } else {
            /**
             * 如果是新建的报表，从缓存中找
             */
            try {
                model = reportModelCacheManager.getReportModel(reportId);
                model.setPersStatus(false);
            } catch (CacheOperationException e) {
                logger.info("[INFO]There are no such model in cache. Report Id: " + reportId, e);
                return ResourceUtils.getErrorResult("缓存中不存在的报表！id: " + reportId, 1);
            }
        }
        ReportRuntimeModel runtimeModel = new ReportRuntimeModel(reportId);
        runtimeModel.init(model, true);
        for (String key : request.getParameterMap().keySet()) {
            String value = request.getParameter(key);
            if (value != null) {
                /**
                 * value 不能是null，但可以为空字符串，空字符串可能有含义
                 */
                runtimeModel.getContext().put(key, value);
            }

        }
        reportModelCacheManager.updateRunTimeModelToCache(reportId, runtimeModel);
        // reportModelCacheManager.updateReportModelToCache(reportId, model);
        ResponseResult rs = ResourceUtils.getCorrectResult("OK", "");
        logger.info("[INFO] successfully init runtime evn, cost {} ms", (System.currentTimeMillis() - begin));
        return rs;
    }

    /**
     * 
     * @param reportId
     * @param runTimeModel
     * @return ReportDesignModel
     */
    private ReportDesignModel getRealModel(String reportId, ReportRuntimeModel runTimeModel) {
        ReportDesignModel model = runTimeModel.getModel();
        return model;
    }

    /**
     * 更新上下文 将格式态的报表模型转化成运形态的报表模型存入缓存 或者依据用户查询逻辑更新运形态报表模型
     * 
     * @param reportId
     * @param areaId
     * @param request
     * @return
     */
    @RequestMapping(value = "/{reportId}/runtime/context", method = { RequestMethod.POST })
    public ResponseResult updateContext(@PathVariable("reportId") String reportId, HttpServletRequest request) {
        long begin = System.currentTimeMillis();
        logger.info("[INFO]------begin update global runtime context");
        Map<String, String[]> contextParams = request.getParameterMap();
        ReportRuntimeModel runTimeModel = reportModelCacheManager.getRuntimeModel(reportId);
        ReportDesignModel model = runTimeModel.getModel();
        model.getExtendAreas().forEach((k, v) -> {
            reportModelCacheManager.updateAreaContext(reportId, k, new ExtendAreaContext());
        });
        Map<String, String> params = Maps.newHashMap();
        if (model.getParams() != null) {
            model.getParams().forEach((k, v) -> {
                params.put(v.getElementId(), v.getName());
            });
        }
        // add by jiangyichao， 取出DesignModel中的平面表条件
        Map<String, String> condition = Maps.newHashMap();
        if (model.getPlaneTableConditions() != null) {
            model.getPlaneTableConditions().forEach((k, v) -> {
                condition.put(v.getElementId(), v.getName());
            });
        }
        // prepare4Test(model, runTimeModel, contextParams, params);
        runTimeModel =
                QueryDataUtils.modifyRuntimeModel4RuntimeContext(model, runTimeModel, contextParams, params, condition);

        reportModelCacheManager.updateRunTimeModelToCache(reportId, runTimeModel);
        logger.info("[INFO]current context params status {}", runTimeModel.getContext().getParams());
        logger.info("[INFO]successfully update global context, cost {} ms", (System.currentTimeMillis() - begin));
        // return initParams (reportId, request);
        ResponseResult rs = ResourceUtils.getResult("Success Getting VM of Report", "Fail Getting VM of Report", "");
        return rs;
    }

    /**
     * 当在设计端编辑报表的时候，每次设置操作完毕，需要通知后台刷新model里面的无用条件
     * 
     * @param reportId reportId
     * @param areaId areaId
     * @param request request
     * @return responseResult
     */
    @RequestMapping(value = "/{reportId}/runtime/extend_area/{areaId}/refresh4design", method = { RequestMethod.POST })
    public ResponseResult refresh4design(@PathVariable("reportId") String reportId,
            @PathVariable("areaId") String areaId, HttpServletRequest request) {
        ReportRuntimeModel runTimeModel = reportModelCacheManager.getRuntimeModel(reportId);
        ReportDesignModel designModel = getRealModel(reportId, runTimeModel);
        ExtendArea targetArea = designModel.getExtendById(areaId);
        // 如果是liteolap报表，需要取其中的table区域对象
        if (targetArea.getType() == ExtendAreaType.LITEOLAP) {
            LiteOlapExtendArea liteOlapExtendArea = (LiteOlapExtendArea) targetArea;
            targetArea = designModel.getExtendById(liteOlapExtendArea.getTableAreaId());
        }
        ExtendAreaContext areaContext = reportModelCacheManager.getAreaContext(reportId, targetArea.getId());
        // 处理上一次查询的遗留脏数据，后续可能还有更多清理动作
        if (areaContext.getCurBreadCrumPath() != null) {
            areaContext.getCurBreadCrumPath().clear();
        }
        reportModelCacheManager.updateAreaContext(reportId, targetArea.getId(), areaContext);
        ResponseResult result = new ResponseResult();
        result.setStatus(0);
        return result;
    }

    /**
     * 数据查询API，获取基于报表模型的数据
     * 
     * @param reportId
     * @param request
     * @return ResponseResult
     */
    @RequestMapping(value = "/{reportId}/data", method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseResult queryData(@PathVariable("reportId") String reportId, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        long begin = System.currentTimeMillis();
        queryVM(reportId, request, response);
        ResponseResult rs = updateContext(reportId, request);
        if (rs.getStatus() != 0) {
            return rs;
        }
        ReportRuntimeModel runtimeModel = reportModelCacheManager.getRuntimeModel(reportId);
        ReportDesignModel model = this.getRealModel(reportId, runtimeModel);
        rs = new ResponseResult();
        if (model == null) {
            rs.setStatus(1);
            rs.setStatusInfo("未找到相应数据模型");
            logger.info("cannot get report define in queryData");
        }
        if (model.getExtendAreas().size() != 1) {
            rs.setStatus(1);
            rs.setStatusInfo("数据区域个数大于2, 不能确定数据区域");
            logger.info("more than one data areas, return");
        } else {
            ExtendArea area = model.getExtendAreaList()[0];
            QueryDataResource queryDataResource =
                    (QueryDataResource) ApplicationContextHelper.getContext().getBean("QueryDataResource");
            // 通过action调用数据查询action的入口方法
            rs = queryDataResource.queryArea(reportId, area.getId(), request);
            if (rs.getStatus() != 0) {
                logger.info("unknown error!");
                return rs;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) rs.getData();
            Map<String, List<String>> datas = Maps.newHashMap();
            if (data.containsKey("pivottable")) {
                PivotTable pivotTable = (PivotTable) data.get("pivottable");
                pivotTable.getDataSourceColumnBased();
                List<List<RowHeadField>> rowHeadFields = pivotTable.getRowHeadFields();
                List<List<RowHeadField>> colFieldBaseRow = Lists.newArrayList();
                // 行列互转
                for (int i = 0; i < rowHeadFields.size(); ++i) {
                    for (int j = 0; j < rowHeadFields.get(i).size(); ++j) {
                        if (colFieldBaseRow.size() <= j) {
                            List<RowHeadField> tmp = Lists.newArrayList();
                            tmp.add(rowHeadFields.get(i).get(j));
                            colFieldBaseRow.add(tmp);
                        } else {
                            colFieldBaseRow.get(j).add(rowHeadFields.get(i).get(j));
                        }
                    }
                }
                colFieldBaseRow.forEach(list -> {
                    String key = list.get(0).getUniqueName();
                    key = MetaNameUtil.getDimNameFromUniqueName(key);
                    List<String> value = list.stream().map(f -> f.getV()).collect(Collectors.toList());
                    datas.put(key, value);
                });
                for (int i = 0; i < pivotTable.getColDefine().size(); ++i) {
                    String key = pivotTable.getColDefine().get(i).getUniqueName();
                    if (MetaNameUtil.isUniqueName(key)) {
                        key = MetaNameUtil.getNameFromMetaName(key);
                    }
                    List<CellData> v = pivotTable.getDataSourceColumnBased().get(i);
                    List<String> tmpV =
                            v.stream().map(cellData -> cellData.getV().toString()).collect(Collectors.toList());
                    datas.put(key, tmpV);
                }
            } else {
                // DoNothing 暂时不支持平面表
            }
            rs.setData(datas);
        }
        logger.info("successfully get data from report model, cost {} ms", (System.currentTimeMillis() - begin));
        return rs;
    }
}
