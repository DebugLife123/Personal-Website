package com.wang.website.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.website.entity.Message;
import com.wang.website.mapper.MessageMapper;
import com.wang.website.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/message")
@CrossOrigin
public class MessageController {
    @Autowired
    private MessageMapper messageMapper;

    // 1. 获取所有留言 (按时间倒序)
    @GetMapping("/list")
    public Result<List<Message>> list() {
        QueryWrapper<Message> wrapper = new QueryWrapper<Message>().orderByDesc("create_time");
        return Result.success(messageMapper.selectList(wrapper));
    }

    // 2. 提交新留言
    @PostMapping("/add")
    public Result<String> add(@RequestBody Message message) {
        if (message.getNickname() == null || message.getContent() == null) {
            return Result.error("内容不能为空哦！");
        }
        messageMapper.insert(message);
        return Result.success("留言成功！");
    }
}