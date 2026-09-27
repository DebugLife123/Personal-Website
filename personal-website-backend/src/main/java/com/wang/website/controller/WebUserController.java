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
import com.wang.website.service.CaptchaService;
import com.wang.website.service.TokenService;
import com.wang.website.util.RateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 站点注册用户接口：注册、登录、个人资料 + 管理员的用户管理。
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
    @Autowired
    private CaptchaService captchaService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Pattern USERNAME_RE = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_RE = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    /** 保留名：防止冒充站长或后台账号 */
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "root", "system", "yu翔", "yuxiang", "站长", "管理员");

    // ==================== 公开接口 ====================

    /** 算术验证码（注册用），5 分钟有效，一次性 */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha(HttpServletRequest request) {
        if (!RateLimitUtil.hit("captcha:" + clientIp(request), 30, 3600_000L)) {
            return Result.error("请求过于频繁，请稍后再试");
        }
        return Result.success(captchaService.generate());
    }

    /** 注册：用户名+密码即可，昵称/邮箱可选，注册即登录 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params,
                                                HttpServletRequest request) {
        String ip = clientIp(request);
        if (!RateLimitUtil.hit("register:" + ip, 5, 3600_000L)) {
            return Result.error("操作过于频繁，请一小时后再试");
        }

        String username = trim(params.get("username"));
        String password = params.get("password");
        String nickname = trim(params.get("nickname"));
        String email = trim(params.get("email"));

        if (!captchaService.check(params.get("captchaId"), params.get("captchaAnswer"))) {
            return Result.error("验证码错误或已过期");
        }
        if (username == null || !USERNAME_RE.matcher(username).matches()) {
            return Result.error("用户名需为 3-20 位字母、数字或下划线");
        }
        if (RESERVED.contains(username.toLowerCase())) {
            return Result.error("该用户名为保留名，请换一个");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            return Result.error("密码长度需为 6-64 位");
        }
        if (nickname == null || nickname.isEmpty()) nickname = username;
        if (nickname.length() > 20) return Result.error("昵称最长 20 个字符");
        if (email != null && !email.isEmpty() && !EMAIL_RE.matcher(email).matches()) {
            return Result.error("邮箱格式不正确");
        }
        // 与管理员账号重名直接拒绝，避免身份混淆
        Long adminDup = userMapper.selectCount(new QueryWrapper<User>().eq("username", username));
        if (adminDup > 0) return Result.error("该用户名已被占用");
        Long dup = webUserMapper.selectCount(new QueryWrapper<WebUser>().eq("username", username));
        if (dup > 0) return Result.error("该用户名已被注册，可直接登录");

        WebUser user = new WebUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email == null ? "" : email);
        user.setAvatar("");
        user.setStatus("normal");
        webUserMapper.insert(user);
        // 重新读取，补齐数据库默认值（create_time 等）
        user = webUserMapper.selectById(user.getId());

        OperationLogController.record(operationLogMapper, username, "用户注册", "新注册用户", ip);

        String token = tokenService.create(TokenService.TYPE_USER, user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("profile", safeProfile(user));
        return Result.success(data);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params,
                                             HttpServletRequest request) {
        String ip = clientIp(request);
        String username = trim(params.get("username"));
        String password = params.get("password");
        if (username == null || password == null) {
            return Result.error("请输入用户名和密码");
        }
        String failKey = "login:" + ip + ":" + username.toLowerCase();
        if (!RateLimitUtil.hit(failKey, 8, 900_000L)) {
            return Result.error("尝试次数过多，请 15 分钟后再试");
        }

        WebUser user = webUserMapper.selectOne(new QueryWrapper<WebUser>().eq("username", username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (!"normal".equals(user.getStatus())) {
            return Result.error("账号已被限制使用，如有疑问请联系站长");
        }

        RateLimitUtil.reset(failKey);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        webUserMapper.updateById(user);

        String token = tokenService.create(TokenService.TYPE_USER, user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("profile", safeProfile(user));
        return Result.success(data);
    }

    /** 当前登录用户资料（前端回源校验登录态） */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        WebUser user = currentUser(request);
        if (user == null) return Result.error("未登录");
        return Result.success(safeProfile(user));
    }

    /** 修改自己的密码，改完全部令牌失效 */
    @PostMapping("/changePassword")
    public Result<String> changePassword(@RequestBody Map<String, String> params,
                                         HttpServletRequest request) {
        WebUser user = currentUser(request);
        if (user == null) return Result.error("未登录");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            return Result.error("新密码长度需为 6-64 位");
        }
        if (!passwordEncoder.matches(oldPassword == null ? "" : oldPassword, user.getPassword())) {
            return Result.error("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        webUserMapper.updateById(user);
        tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        return Result.success("密码已修改，请重新登录");
    }

    /** 退出登录：吊销当前令牌 */
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return Result.success("已退出登录");
    }

    // ==================== 管理员接口（仅管理员 Token 可达，由拦截器保证） ====================

    /** 用户分页列表（附留言数统计） */
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String keyword) {
        QueryWrapper<WebUser> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("username", keyword)
                    .or().like("nickname", keyword)
                    .or().like("email", keyword));
        }
        wrapper.orderByDesc("create_time");
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
        if (user == null) return Result.error("用户不存在");
        if (!"normal".equals(status) && !"banned".equals(status)) return Result.error("非法状态");
        user.setStatus(status);
        webUserMapper.updateById(user);
        if ("banned".equals(status)) {
            tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        }
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"),
                "banned".equals(status) ? "禁言用户" : "解禁用户",
                "用户 " + user.getUsername(), clientIp(request));
        return Result.success("banned".equals(status) ? "已禁言" : "已解禁");
    }

    /** 管理员重置用户密码（重置后用户全部令牌失效） */
    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer id = toInt(params.get("id"));
        String newPassword = (String) params.get("newPassword");
        WebUser user = id == null ? null : webUserMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 64) {
            return Result.error("新密码长度需为 6-64 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        webUserMapper.updateById(user);
        tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"), "重置用户密码",
                "用户 " + user.getUsername(), clientIp(request));
        return Result.success("密码已重置");
    }

    /** 删除用户：令牌吊销，历史留言保留但匿名化 */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id, HttpServletRequest request) {
        WebUser user = webUserMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        tokenService.revokeAll(TokenService.TYPE_USER, user.getId());
        // 留言匿名化而非连删，保住对话上下文
        // 注意：updateById 会跳过 null 字段，匿名化必须用 UpdateWrapper.set 显式置空
        long msgCount = messageMapper.selectCount(new QueryWrapper<Message>().eq("user_id", id));
        messageMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Message>()
                .eq("user_id", id).set("user_id", null));
        webUserMapper.deleteById(id);
        OperationLogController.record(operationLogMapper,
                (String) request.getAttribute("adminUser"), "删除用户",
                "用户 " + user.getUsername() + "，留言已匿名化 " + msgCount + " 条", clientIp(request));
        return Result.success("已删除，该用户的 " + msgCount + " 条留言已转为匿名");
    }

    // ==================== 内部工具 ====================

    private WebUser currentUser(HttpServletRequest request) {
        Object id = request.getAttribute("authUserId");
        if (id == null) return null;
        WebUser user = webUserMapper.selectById((Integer) id);
        return (user != null && "normal".equals(user.getStatus())) ? user : null;
    }

    private Map<String, Object> safeProfile(WebUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        m.put("avatar", u.getAvatar());
        m.put("status", u.getStatus());
        m.put("lastLoginTime", u.getLastLoginTime());
        m.put("lastLoginIp", u.getLastLoginIp());
        m.put("createTime", u.getCreateTime());
        return m;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }
}
