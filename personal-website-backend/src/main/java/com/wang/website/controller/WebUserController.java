package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.common.Result;
import com.wang.website.entity.Message;
import com.wang.website.entity.User;
import com.wang.website.entity.WebUser;
import com.wang.website.mapper.MessageMapper;
import com.wang.website.mapper.OperationLogMapper;
import com.wang.website.mapper.UserMapper;
import com.wang.website.mapper.WebUserMapper;
import com.wang.website.service.TokenService;
import com.wang.website.util.RateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 站点访客身份接口（极简无门槛：选形象 + 填昵称 = 身份同步）。
 *
 * 设计要点：
 *   - 没有密码、验证码、邮箱，昵称就是唯一标识；
 *   - 昵称已存在则视为「同一身份」直接同步进来（更新形象/登录记录）；
 *   - 每次同步都记录 IP、UA、次数，后台可查可管（禁言/改名/删除）。
 */
@RestController
@RequestMapping("/api/webuser")
public class WebUserController {

    @Autowired
    private WebUserMapper webUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private TokenService tokenService;

    /** 昵称长度 2~20，允许中英文、数字、下划线、连字符与空格（不含首尾） */
    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;
    /** 保留昵称：防止冒充站长或后台账号 */
    private static final Set<String> RESERVED = Set.of(
            "admin", "administrator", "root", "system", "yu翔", "yuxiang",
            "站长", "管理员", "官方", "客服");
    /** 允许的预置形象 key（与前端 utils/avatar.js 保持一致） */
    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "preset:orange", "preset:black", "preset:yellow",
            "preset:purple", "preset:teal");
    private static final String DEFAULT_AVATAR = "preset:orange";

    // ==================== 公开接口 ====================

    /**
     * 身份同步并登录：昵称存在则同步该身份，不存在则创建。
     * 请求体：{ nickname: "张三", avatar: "preset:teal" }
     */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync(@RequestBody Map<String, String> params,
                                            HttpServletRequest request) {
        String ip = clientIp(request);
        // 防刷：同 IP 每小时最多 20 次身份同步（正常用户远低于此）
        if (!RateLimitUtil.hit("sync:" + ip, 20, 3600_000L)) {
            return Result.error("操作过于频繁，请稍后再试");
        }

        String nickname = normalize(params.get("nickname"));
        String avatar = params.get("avatar");

        String invalid = validateNickname(nickname);
        if (invalid != null) return Result.error(invalid);
        if (avatar == null || !ALLOWED_AVATARS.contains(avatar)) avatar = DEFAULT_AVATAR;

        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 250) ua = ua.substring(0, 250);

        WebUser user = findByNickname(nickname);
        boolean isNew = false;
        if (user == null) {
            user = new WebUser();
            user.setNickname(nickname);
            user.setUsername(nickname); // 历史列保持同值
            user.setAvatar(avatar);
            user.setEmail("");
            user.setStatus("normal");
            user.setLoginCount(1);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(ip);
            user.setLastLoginUa(ua);
            webUserMapper.insert(user);
            isNew = true;
            user = webUserMapper.selectById(user.getId()); // 取回库端默认值
            OperationLogController.record(operationLogMapper, nickname, "身份创建", "新访客同步身份", ip);
        } else {
            if (!"normal".equals(user.getStatus())) {
                return Result.error("该昵称已被限制使用，如有疑问请联系站长");
            }
            if (!avatar.equals(user.getAvatar())) user.setAvatar(avatar);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(ip);
            user.setLastLoginUa(ua);
            user.setLoginCount(user.getLoginCount() == null ? 1 : user.getLoginCount() + 1);
            webUserMapper.updateById(user);
        }

        String token = tokenService.create(TokenService.TYPE_USER, user.getId(), user.getNickname());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("profile", safeProfile(user));
        data.put("isNew", isNew);
        return Result.success(data);
    }

    /** 当前身份资料（前端回源校验身份是否仍有效） */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        WebUser user = currentUser(request);
        if (user == null) return Result.error("身份已失效，请重新同步");
        return Result.success(safeProfile(user));
    }

    /** 退出：吊销当前令牌（不删除身份，下次填同一昵称即可回来） */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return Result.success("已退出登录");
    }

    // ==================== 管理员接口（仅管理员 Token 可达，由拦截器保证） ====================

    /** 身份分页列表（附留言数统计） */
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String keyword) {
        QueryWrapper<WebUser> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("nickname", keyword).or().like("last_login_ip", keyword));
        }
        wrapper.orderByDesc("last_login_time").orderByDesc("create_time");
        IPage<WebUser> users = webUserMapper.selectPage(new Page<>(page, pageSize), wrapper);

        IPage<Map<String, Object>> result = new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        List<Map<String, Object>> records = users.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>(safeProfile(u));
            Long msgCount = messageMapper.selectCount(new QueryWrapper<Message>()
                    .eq("user_id", u.getId()).ne("status", "deleted"));
            m.put("messageCount", msgCount);
            return m;
        }).toList();
        result.setRecords(records);
        return Result.success(result);
    }

    /** 禁言 / 解禁（同时吊销令牌，立即生效） */
    @PostMapping("/status")
    public Result<String> updateStatus(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer id = toInt(params.get("id"));
        String status = String.valueOf(params.get("status"));
        WebUser user = id == null ? null : webUserMapper.selectById(id);
        if (user == null) return Result.error("该身份不存在");
        if (!"normal".equals(status) && !"banned".equals(status)) return Result.error("非法状态");
        user.setStatus(status);
        webUserMapper.updateById(user);
        if ("banned".equals(status)) {
            tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        }
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"),
                "banned".equals(status) ? "禁言身份" : "解禁身份",
                "昵称 " + user.getNickname(), clientIp(request));
        return Result.success("banned".equals(status) ? "已禁言" : "已解禁");
    }

    /** 管理员改名（纠正违规/冒充昵称；该身份的令牌一并失效，需用新昵称重新同步） */
    @PostMapping("/rename")
    public Result<String> rename(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer id = toInt(params.get("id"));
        String nickname = normalize((String) params.get("nickname"));
        WebUser user = id == null ? null : webUserMapper.selectById(id);
        if (user == null) return Result.error("该身份不存在");
        String invalid = validateNickname(nickname);
        if (invalid != null) return Result.error(invalid);
        WebUser dup = findByNickname(nickname);
        if (dup != null && !dup.getId().equals(user.getId())) {
            return Result.error("该昵称已被占用");
        }
        String old = user.getNickname();
        user.setNickname(nickname);
        user.setUsername(nickname); // 历史列同值
        webUserMapper.updateById(user);
        tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"), "身份改名",
                old + " → " + nickname, clientIp(request));
        return Result.success("已改名为「" + nickname + "」");
    }

    /** 删除身份：令牌吊销，历史留言保留但匿名化 */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id, HttpServletRequest request) {
        WebUser user = webUserMapper.selectById(id);
        if (user == null) return Result.error("该身份不存在");
        tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        // 注意：updateById 会跳过 null 字段，匿名化必须用 UpdateWrapper.set 显式置空
        long msgCount = messageMapper.selectCount(new QueryWrapper<Message>().eq("user_id", id));
        messageMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Message>()
                .eq("user_id", id).set("user_id", null));
        webUserMapper.deleteById(id);
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"), "删除身份",
                "昵称 " + user.getNickname() + "，留言已匿名化 " + msgCount + " 条", clientIp(request));
        return Result.success("已删除，该身份的 " + msgCount + " 条留言已转为匿名");
    }

    // ==================== 内部工具 ====================

    private WebUser findByNickname(String nickname) {
        if (nickname == null) return null;
        return webUserMapper.selectOne(new QueryWrapper<WebUser>().eq("nickname", nickname));
    }

    /** 昵称校验：返回错误信息，通过则返回 null */
    private String validateNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) return "请填写昵称";
        if (nickname.length() < NICKNAME_MIN) return "昵称至少 " + NICKNAME_MIN + " 个字符";
        if (nickname.length() > NICKNAME_MAX) return "昵称最长 " + NICKNAME_MAX + " 个字符";
        if (RESERVED.contains(nickname.toLowerCase())) return "该昵称不可使用，请换一个";
        // 与后台管理员重名同样拒绝，避免身份混淆
        Long adminDup = userMapper.selectCount(new QueryWrapper<User>().eq("username", nickname));
        if (adminDup > 0) return "该昵称不可使用，请换一个";
        return null;
    }

    /** 去首尾空白 + 压缩连续空格，避免「张 三」与「张  三」被当成不同身份 */
    private String normalize(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private WebUser currentUser(HttpServletRequest request) {
        Object id = request.getAttribute("authUserId");
        if (id == null) return null;
        WebUser user = webUserMapper.selectById((Integer) id);
        return (user != null && "normal".equals(user.getStatus())) ? user : null;
    }

    private Map<String, Object> safeProfile(WebUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("nickname", u.getNickname());
        m.put("avatar", u.getAvatar());
        m.put("status", u.getStatus());
        m.put("loginCount", u.getLoginCount());
        m.put("lastLoginTime", u.getLastLoginTime());
        m.put("lastLoginIp", u.getLastLoginIp());
        m.put("lastLoginUa", u.getLastLoginUa());
        m.put("createTime", u.getCreateTime());
        return m;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }
}
