/*-
 * <<
 * sag
 * ==
 * Copyright (C) 2019 sia
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */


package com.creditease.gateway.admin.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.creditease.gateway.admin.controller.base.BaseAdminController;
import com.creditease.gateway.admin.service.SettingService;
import com.creditease.gateway.constant.GatewayConstant;
import com.creditease.gateway.discovery.DiscoveryService;
import com.creditease.gateway.helper.StringHelper;
import com.creditease.gateway.message.Message;
import com.creditease.gateway.message.ZuulHandler;

/**
 * 拓扑管理功能
 * 
 * @author peihua
 */

@RestController
public class TopoAdminController extends BaseAdminController {

    public static final Logger LOGGER = LoggerFactory.getLogger(TopoAdminController.class);

    @Autowired
    SettingService settingService;

    @Autowired
    private DiscoveryService zuuldisc;

    @Value("${spring.application.name}")
    private String appName;

    private static final String APIGATEWAYSERVICEPOST = "-GATEWAY-SERVICE";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    ZuulHandler handler;

     /**
      * 获取网络拓扑图
      */
    @RequestMapping(value = "/getGatewayTopo", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getGatewayTopo(@RequestBody Map<String, String> request) {
        String groupName = request.get("groupName");
        if(StringUtils.isEmpty(groupName)){
            return null;
        }
        JSONObject json = new JSONObject();

        //暂时写死
        JSONArray clientNodes = new JSONArray();
        clientNodes.add("应用:127.0.0.1:8095");
        json.put("clientNodes",clientNodes);

        //组成网关数据
        List<ServiceInstance> instancesZuuls = discoveryClient.getInstances(groupName);
        JSONArray zuulNodes = new JSONArray();
        JSONArray linkArray = new JSONArray();
        boolean flag = false;
        if(!CollectionUtils.isEmpty(instancesZuuls)) {
            flag = true;
            JSONObject link = null;
            for (ServiceInstance instancesZuul : instancesZuuls) {
                link = new JSONObject();
                link.put("dest",instancesZuul.getHost() + ":" + instancesZuul.getPort());
                link.put("source","应用:127.0.0.1:8095");
                link.put("state","RED");
                link.put("lastRequestTime","1561712559390");
                linkArray.add(link);
                zuulNodes.add(instancesZuul.getHost()+ ":" + instancesZuul.getPort());
            }
        }
        json.put("zuulNodes",zuulNodes);

        //封装后端服务
        JSONArray upstreamNodes = new JSONArray();
        List<String> services = discoveryClient.getServices();
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (String service : services) {
            if(!service.equalsIgnoreCase(groupName)){
                serviceInstances.addAll(discoveryClient.getInstances(service));
            }
        }
        for (ServiceInstance serviceInstance : serviceInstances) {
            upstreamNodes.add(serviceInstance.getHost() + ":" + serviceInstance.getPort());
            if(flag) {
                JSONObject upstreamLink = null;
                for (ServiceInstance instancesZuul : instancesZuuls) {
                    upstreamLink = new JSONObject();
                    upstreamLink.put("dest",serviceInstance.getHost() + ":" + serviceInstance.getPort());
                    upstreamLink.put("source",instancesZuul.getHost() + ":" + instancesZuul.getPort());
                    upstreamLink.put("state","RED");
                    upstreamLink.put("lastRequestTime","1561712559390");
                    linkArray.add(upstreamLink);
                }
            }
        }
        json.put("upstreamNodes",upstreamNodes);
        json.put("link",linkArray);
        //封装连线
        JSONObject jsonObject= new JSONObject();
        jsonObject.put("response",json);
        jsonObject.put("code",200);
        return jsonObject.toString();


//        String groupName = request.get("groupName");
//        String result = null;
//
//        if (StringHelper.isEmpty(groupName)) {
//            LOGGER.info("groupName is null!");
//            return null;
//        }
//
//        String apiGatewayService = appName.substring(0, appName.indexOf("-")) + APIGATEWAYSERVICEPOST;
//
//        try {
//            LOGGER.info("apiGatewayService:" + apiGatewayService);
//
//            List<String> ipports = zuuldisc.getServiceList(apiGatewayService);
//
//            if (null == ipports || ipports.size() < 0) {
//                LOGGER.info("groupName is null!");
//                Message resp = new Message("list is emptry", 500);
//                ObjectMapper mapper = new ObjectMapper();
//                return mapper.writeValueAsString(resp);
//            }
//
//            for (String ipport : ipports) {
//
//                try {
//                    Map<String, String> parmeter = new HashMap<String, String>(8);
//                    parmeter.put(GatewayConstant.GWGROUPNAME, groupName);
//
//                    Message msg = new Message(parmeter);
//                    String url = "http://" + ipport + "/getGatewayTopo";
//                    result = handler.executeHttpCmd(url, msg);
//                    break;
//                }
//                catch (Exception e) {
//                    LOGGER.info(">Exeption is :" + e.getMessage());
//                    continue;
//                }
//            }
//        }
//        catch (Exception e) {
//            LOGGER.error(e.getMessage());
//        }
//        return result;
    }

      /**
       * 获取路由拓扑图
      */
    @RequestMapping(value = "/getRouteTopo", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRouteTopo(@RequestBody Map<String, String> request) {
        //重写的代码
        String serviceId = request.get("apiName");
        JSONObject json = new JSONObject();

        //暂时写死
        JSONArray clientNodes = new JSONArray();
        clientNodes.add("应用:127.0.0.1:8095");
        json.put("clientNodes",clientNodes);

        //组成网关数据
        List<ServiceInstance> instancesZuuls = discoveryClient.getInstances(request.get("groupName"));
        JSONArray zuulNodes = new JSONArray();
        JSONArray linkArray = new JSONArray();
        boolean flag = false;
        if(!CollectionUtils.isEmpty(instancesZuuls)) {
            flag = true;
            JSONObject link = null;
            for (ServiceInstance instancesZuul : instancesZuuls) {
                link = new JSONObject();
                link.put("dest",instancesZuul.getHost() + ":" + instancesZuul.getPort());
                link.put("source","应用:127.0.0.1:8095");
                link.put("state","GREEN");
                link.put("lastRequestTime","1561712559390");
                linkArray.add(link);
                zuulNodes.add(instancesZuul.getHost()+ ":" + instancesZuul.getPort());
            }
        }
        json.put("zuulNodes",zuulNodes);
        //封装后端服务
        JSONArray upstreamNodes = new JSONArray();
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if(!CollectionUtils.isEmpty(instances)) {
            for (ServiceInstance serviceInstance : instances) {
                upstreamNodes.add(serviceInstance.getHost() + ":" + serviceInstance.getPort());
                if(flag) {
                    JSONObject upstreamLink = null;
                    for (ServiceInstance instancesZuul : instancesZuuls) {
                        upstreamLink = new JSONObject();
                        upstreamLink.put("dest",serviceInstance.getHost() + ":" + serviceInstance.getPort());
                        upstreamLink.put("source",instancesZuul.getHost() + ":" + instancesZuul.getPort());
                        upstreamLink.put("state","GREEN");
                        upstreamLink.put("lastRequestTime","1561712559390");
                        linkArray.add(upstreamLink);
                    }
                }
            }
        }
        json.put("upstreamNodes",upstreamNodes);
        json.put("link",linkArray);
        //封装连线
        JSONObject jsonObject= new JSONObject();
        jsonObject.put("response",json);
        return jsonObject.toString();



//        String groupName = request.get("groupName");
//        String routeid = request.get("routeid");
//        String result = null;
//        LOGGER.info("> getRouteTopo,groupName:{}", groupName);
//        LOGGER.info("> getRouteTopo,routeid:{}", routeid);
//
//        try {
//            LOGGER.info("groupName is appName:" + appName);
//
//            String apiGatewayService = appName.substring(0, appName.indexOf("-")) + APIGATEWAYSERVICEPOST;
//
//            List<String> ipports = zuuldisc.getServiceList(apiGatewayService);
//
//            if (null == ipports || ipports.size() < 0) {
//                LOGGER.info("groupName is null!");
//                Message resp = new Message("list is emptry", 500);
//                ObjectMapper mapper = new ObjectMapper();
//                return mapper.writeValueAsString(resp);
//            }
//
//            for (String ipport : ipports) {
//                try {
//                    LOGGER.info("groupName is ipport:" + ipport);
//
//                    Map<String, String> parmeter = new HashMap<String, String>(8);
//
//                    parmeter.put(GatewayConstant.GWGROUPNAME, groupName);
//                    parmeter.put(GatewayConstant.ROUTENAME, routeid);
//                    Message msg = new Message(parmeter);
//
//                    String url = "http://" + ipport + "/getRouteTopo";
//                    result = handler.executeHttpCmd(url, msg);
//                    LOGGER.info(">getRouteTopo result:" + result);
//
//                    break;
//                }
//                catch (Exception e) {
//                    LOGGER.info(">getRouteTopo Exeption is :" + e.getMessage());
//                    continue;
//                }
//            }
//        }
//        catch (Exception e) {
//
//            LOGGER.error(e.getMessage());
//        }

        //return "result";

    }
}
