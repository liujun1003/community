package com.example.community.controller;

import com.example.community.service.DataService;
import com.example.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    @RequestMapping(path = "/data/UV", method = RequestMethod.POST)
    @ResponseBody
    public String UV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                     @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (start == null || end ==null) {
            return CommunityUtil.getJSONString(1, "请选择日期范围！");
        }
        if (start.after(end)) {
            return CommunityUtil.getJSONString(1, "日期范围有误！");
        }

        // 统计指定日期范围内的UV
        long UVPeriod = dataService.getUVPeriod(start, end);

        // 返回参数
        Map<String, Object> map = new HashMap<>();
        map.put("UVPeriod", UVPeriod);

        return CommunityUtil.getJSONString(0, null, map);
    }

    @RequestMapping(path = "/data/DAU", method = RequestMethod.POST)
    @ResponseBody
    public String DAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                     @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {
        if (start == null || end == null) {
            return CommunityUtil.getJSONString(1, "请选择日期范围！");
        }
        if (start.after(end)) {
            return CommunityUtil.getJSONString(1, "日期范围有误！");
        }

        // 统计指定日期范围内的DAU
        long DAUPeriod = dataService.getDAUPeriod(start, end);

        // 返回参数
        Map<String, Object> map = new HashMap<>();
        map.put("DAUPeriod", DAUPeriod);

        return CommunityUtil.getJSONString(0, null, map);
    }
}
