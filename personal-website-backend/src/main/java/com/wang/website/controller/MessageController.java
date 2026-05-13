package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.entity.Message;
import com.wang.website.entity.Resume;
import com.wang.website.mapper.MessageMapper;
import com.wang.website.mapper.ResumeMapper;
import com.wang.website.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // 获取所有留言（含嵌套回复），置顶优先，按时间倒序
    @GetMapping("/list")
    public Result<List<Message>> list() {
        try {
            QueryWrapper<Message> wrapper = new QueryWrapper<Message>()
                    .orderByDesc("is_pinned")
                    .orderByDesc("create_time");
            List<Message> allMessages = messageMapper.selectList(wrapper);

            // 标记管理员留言
            for (Message msg : allMessages) {
                msg.setAdminPost(ADMIN_NICKNAME.equals(msg.getNickname()));
            }

            // 按 parentId 分组构建回复树
            Map<Integer, List<Message>> replyMap = allMessages.stream()
                    .filter(msg -> msg.getParentId() != null)
                    .collect(Collectors.groupingBy(Message::getParentId));

            // 将回复挂到对应的父留言上
            for (Message msg : allMessages) {
                if (replyMap.containsKey(msg.getId())) {
                    msg.setReplies(replyMap.get(msg.getId()));
                }
            }

            // 只返回顶级留言
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
                // 管理员留言：从简历表取姓名和头像
                Resume resume = resumeMapper.selectById(1);
                if (resume != null) {
                    message.setNickname(resume.getName() != null ? resume.getName() : ADMIN_NICKNAME);
                    message.setAvatar(resume.getAvatar() != null ? resume.getAvatar() : "");
                } else {
                    message.setNickname(ADMIN_NICKNAME);
                    message.setAvatar("");
                }
            } else {
                // 游客留言
                message.setNickname(GUEST_NICKNAME);
                message.setAvatar(GUEST_AVATAR);
            }
            message.setEmail("");
            message.setLikes(0);
            message.setIsPinned(false);
            messageMapper.insert(message);
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

    // 删除留言
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        try {
            // 同时删除子留言（外键 ON DELETE CASCADE 已设置）
            messageMapper.deleteById(id);
            return Result.success("删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }
}
