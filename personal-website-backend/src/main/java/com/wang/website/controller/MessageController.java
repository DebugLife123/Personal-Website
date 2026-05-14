package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.website.entity.Message;
import com.wang.website.entity.Resume;
import com.wang.website.mapper.MessageMapper;
import com.wang.website.mapper.ResumeMapper;
import com.wang.website.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message")
@CrossOrigin
public class MessageController {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ResumeMapper resumeMapper;

    private static final String ADMIN_NICKNAME = "yu翔";
    private static final String GUEST_NICKNAME = "游客";
    private static final String GUEST_AVATAR = "";

    // ==================== 前台接口 ====================

    // 获取前台可见留言（仅已通过 + 未删除），置顶优先，按时间倒序
    @GetMapping("/list")
    public Result<List<Message>> list() {
        try {
            QueryWrapper<Message> wrapper = new QueryWrapper<Message>()
                    .eq("status", "approved")
                    .orderByDesc("is_pinned")
                    .orderByDesc("create_time");
            List<Message> allMessages = messageMapper.selectList(wrapper);

            for (Message msg : allMessages) {
                msg.setAdminPost(ADMIN_NICKNAME.equals(msg.getNickname()));
            }

            Map<Integer, List<Message>> replyMap = allMessages.stream()
                    .filter(msg -> msg.getParentId() != null)
                    .collect(Collectors.groupingBy(Message::getParentId));

            for (Message msg : allMessages) {
                if (replyMap.containsKey(msg.getId())) {
                    msg.setReplies(replyMap.get(msg.getId()));
                }
            }

            List<Message> topLevel = allMessages.stream()
                    .filter(msg -> msg.getParentId() == null)
                    .collect(Collectors.toList());

            return Result.success(topLevel);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取留言列表失败");
        }
    }

    // 提交新留言或回复
    @PostMapping("/add")
    public Result<String> add(@RequestBody Message message) {
        try {
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return Result.error("内容不能为空哦！");
            }

            if (Boolean.TRUE.equals(message.getIsAdmin())) {
                Resume resume = resumeMapper.selectById(1);
                if (resume != null) {
                    message.setNickname(resume.getName() != null ? resume.getName() : ADMIN_NICKNAME);
                    message.setAvatar(resume.getAvatar() != null ? resume.getAvatar() : "");
                } else {
                    message.setNickname(ADMIN_NICKNAME);
                    message.setAvatar("");
                }
                message.setStatus("approved"); // 管理员发言直接通过
            } else {
                message.setNickname(GUEST_NICKNAME);
                message.setAvatar(GUEST_AVATAR);
                message.setStatus("pending"); // 游客留言待审核
            }
            message.setEmail("");
            message.setLikes(0);
            message.setIsPinned(false);
            messageMapper.insert(message);

            // 管理员回复后，自动通过父留言
            if (Boolean.TRUE.equals(message.getIsAdmin()) && message.getParentId() != null) {
                Message parent = messageMapper.selectById(message.getParentId());
                if (parent != null && "pending".equals(parent.getStatus())) {
                    parent.setStatus("approved");
                    messageMapper.updateById(parent);
                }
            }

            return Result.success("留言成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("留言失败");
        }
    }

    // 点赞
    @PostMapping("/like/{id}")
    public Result<String> like(@PathVariable Integer id) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setLikes(msg.getLikes() == null ? 1 : msg.getLikes() + 1);
            messageMapper.updateById(msg);
            return Result.success("点赞成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("点赞失败");
        }
    }

    // 取消点赞
    @PostMapping("/unlike/{id}")
    public Result<String> unlike(@PathVariable Integer id) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setLikes(msg.getLikes() == null ? 0 : Math.max(0, msg.getLikes() - 1));
            messageMapper.updateById(msg);
            return Result.success("已取消点赞");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败");
        }
    }

    // 切换置顶
    @PostMapping("/pin/{id}")
    public Result<String> togglePin(@PathVariable Integer id) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setIsPinned(!Boolean.TRUE.equals(msg.getIsPinned()));
            messageMapper.updateById(msg);
            return Result.success(msg.getIsPinned() ? "已置顶" : "已取消置顶");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败");
        }
    }

    // 软删除留言
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setStatus("deleted");
            messageMapper.updateById(msg);
            return Result.success("删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }

    // ==================== 后台管理接口 ====================

    // 分页查询留言列表（后台管理，含筛选）
    @GetMapping("/admin/page")
    public Result<IPage<Message>> adminPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            QueryWrapper<Message> wrapper = new QueryWrapper<>();
            // 默认不显示已删除
            wrapper.ne("status", "deleted");
            // 仅显示顶级留言
            wrapper.isNull("parent_id");

            if (status != null && !status.trim().isEmpty() && !"all".equals(status)) {
                wrapper.eq("status", status);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.and(w -> w.like("nickname", keyword)
                        .or().like("content", keyword));
            }
            // 日期筛选
            boolean hasStart = startDate != null && !startDate.trim().isEmpty();
            boolean hasEnd = endDate != null && !endDate.trim().isEmpty();
            if (hasStart || hasEnd) {
                LocalDateTime start = hasStart
                        ? LocalDate.parse(startDate).atStartOfDay()
                        : LocalDate.of(2000, 1, 1).atStartOfDay();
                LocalDateTime end = hasEnd
                        ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                        : LocalDateTime.now().plusYears(100);
                wrapper.between("create_time", start, end);
            }
            wrapper.orderByDesc("create_time");

            IPage<Message> result = messageMapper.selectPage(new Page<>(page, pageSize), wrapper);

            // 标记管理员 + 计算回复数
            for (Message msg : result.getRecords()) {
                msg.setAdminPost(ADMIN_NICKNAME.equals(msg.getNickname()));
                QueryWrapper<Message> replyWrapper = new QueryWrapper<Message>()
                        .eq("parent_id", msg.getId())
                        .ne("status", "deleted");
                msg.setReplyCount(messageMapper.selectCount(replyWrapper).intValue());
            }

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取留言列表失败");
        }
    }

    // 获取单条留言详情（含回复）
    @GetMapping("/admin/{id}")
    public Result<Message> adminDetail(@PathVariable Integer id) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setAdminPost(ADMIN_NICKNAME.equals(msg.getNickname()));

            QueryWrapper<Message> replyWrapper = new QueryWrapper<Message>()
                    .eq("parent_id", id)
                    .ne("status", "deleted")
                    .orderByAsc("create_time");
            List<Message> replies = messageMapper.selectList(replyWrapper);
            for (Message r : replies) {
                r.setAdminPost(ADMIN_NICKNAME.equals(r.getNickname()));
            }
            msg.setReplies(replies);
            return Result.success(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取留言详情失败");
        }
    }

    // 修改审核状态
    @PutMapping("/admin/status/{id}")
    public Result<String> updateStatus(@PathVariable Integer id, @RequestParam String status) {
        try {
            Message msg = messageMapper.selectById(id);
            if (msg == null) return Result.error("留言不存在");
            msg.setStatus(status);
            messageMapper.updateById(msg);
            String label = "approved".equals(status) ? "已通过" : "rejected".equals(status) ? "已驳回" : "已更新";
            return Result.success(label);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败");
        }
    }

    // 批量操作
    @PostMapping("/admin/batch")
    public Result<String> batchOperation(@RequestBody BatchRequest req) {
        try {
            if (req.getIds() == null || req.getIds().isEmpty()) {
                return Result.error("请选择留言");
            }
            QueryWrapper<Message> wrapper = new QueryWrapper<>();
            wrapper.in("id", req.getIds());
            List<Message> list = messageMapper.selectList(wrapper);

            if ("delete".equals(req.getAction())) {
                for (Message msg : list) {
                    msg.setStatus("deleted");
                    messageMapper.updateById(msg);
                }
                return Result.success("批量删除成功");
            } else if ("approve".equals(req.getAction())) {
                for (Message msg : list) {
                    msg.setStatus("approved");
                    messageMapper.updateById(msg);
                }
                return Result.success("批量通过成功");
            } else if ("reject".equals(req.getAction())) {
                for (Message msg : list) {
                    msg.setStatus("rejected");
                    messageMapper.updateById(msg);
                }
                return Result.success("批量驳回成功");
            }
            return Result.error("未知操作");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量操作失败");
        }
    }

    // 管理员回复留言
    @PostMapping("/admin/reply")
    public Result<String> adminReply(@RequestBody Message message) {
        try {
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return Result.error("回复内容不能为空");
            }
            // 管理员回复
            Resume resume = resumeMapper.selectById(1);
            if (resume != null) {
                message.setNickname(resume.getName() != null ? resume.getName() : ADMIN_NICKNAME);
                message.setAvatar(resume.getAvatar() != null ? resume.getAvatar() : "");
            } else {
                message.setNickname(ADMIN_NICKNAME);
                message.setAvatar("");
            }
            message.setEmail("");
            message.setLikes(0);
            message.setIsPinned(false);
            message.setStatus("approved");
            messageMapper.insert(message);

            // 同时将父留言状态改为已通过
            if (message.getParentId() != null) {
                Message parent = messageMapper.selectById(message.getParentId());
                if (parent != null && "pending".equals(parent.getStatus())) {
                    parent.setStatus("approved");
                    messageMapper.updateById(parent);
                }
            }

            return Result.success("回复成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("回复失败");
        }
    }

    @lombok.Data
    public static class BatchRequest {
        private List<Integer> ids;
        private String action;
    }
}
