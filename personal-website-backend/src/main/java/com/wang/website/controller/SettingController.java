package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.common.Result;
import com.wang.website.entity.SiteSetting;
import com.wang.website.mapper.SiteSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/setting")
@CrossOrigin
public class SettingController {

    @Autowired
    private SiteSettingMapper siteSettingMapper;

    /** 读取全部设置（key -> value），公开读 */
    @GetMapping("/all")
    public Result<Map<String, String>> all() {
        try {
            List<SiteSetting> list = siteSettingMapper.selectList(null);
            Map<String, String> m = new HashMap<>();
            for (SiteSetting s : list) m.put(s.getSettingKey(), s.getSettingValue());
            return Result.success(m);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("读取设置失败");
        }
    }

    /** 批量保存（管理员） */
    @PostMapping("/save")
    public Result<String> save(@RequestBody Map<String, String> body) {
        try {
            for (Map.Entry<String, String> e : body.entrySet()) {
                QueryWrapper<SiteSetting> w = new QueryWrapper<SiteSetting>().eq("setting_key", e.getKey());
                SiteSetting exist = siteSettingMapper.selectOne(w);
                if (exist == null) {
                    SiteSetting s = new SiteSetting();
                    s.setSettingKey(e.getKey());
                    s.setSettingValue(e.getValue());
                    siteSettingMapper.insert(s);
                } else {
                    exist.setSettingValue(e.getValue());
                    siteSettingMapper.updateById(exist);
                }
            }
            return Result.success("保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败");
        }
    }
}
