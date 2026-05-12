-- 个人网站数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS personal_website DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE personal_website;

-- 1. 文章表
-- 先删除旧表（如果存在），确保表结构正确
DROP TABLE IF EXISTS article;
CREATE TABLE article (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '文章标题',
    summary VARCHAR(500) COMMENT '文章摘要',
    content TEXT COMMENT '文章内容',
    views INT DEFAULT 0 COMMENT '阅读量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    cover VARCHAR(255) COMMENT '封面图',
    category VARCHAR(50) COMMENT '分类',
    tags VARCHAR(255) COMMENT '标签，多个标签用逗号分隔',
    status VARCHAR(20) DEFAULT '已发布' COMMENT '状态：已发布/草稿'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 2. 留言表
DROP TABLE IF EXISTS message;
CREATE TABLE message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    content TEXT NOT NULL COMMENT '留言内容',
    avatar VARCHAR(255) COMMENT '头像',
    email VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    likes INT DEFAULT 0 COMMENT '点赞数',
    is_pinned TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    parent_id INT COMMENT '父留言ID，用于实现回复功能',
    reply_to VARCHAR(50) COMMENT '回复对象的昵称',
    FOREIGN KEY (parent_id) REFERENCES message(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';

-- 3. 个人简历表
DROP TABLE IF EXISTS resume;
CREATE TABLE resume (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    avatar VARCHAR(255) COMMENT '头像',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    address VARCHAR(100) COMMENT '地址',
    education VARCHAR(255) COMMENT '教育背景',
    work_experience VARCHAR(255) COMMENT '工作经历',
    project_experience VARCHAR(255) COMMENT '项目经验',
    skill VARCHAR(255) COMMENT '技能',
    award VARCHAR(255) COMMENT '奖项',
    intro VARCHAR(500) COMMENT '个人简介',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人简历表';

-- 4. 访问统计表
CREATE TABLE IF NOT EXISTS statistic (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL COMMENT '日期',
    page_views INT DEFAULT 0 COMMENT '页面访问量',
    unique_visitors INT DEFAULT 0 COMMENT '独立访客数',
    article_reads INT DEFAULT 0 COMMENT '文章阅读量',
    UNIQUE KEY uk_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问统计表';

-- 插入默认个人简历数据
INSERT INTO resume (name, avatar, email, phone, address, education, work_experience, project_experience, skill, award, intro) 
VALUES ('yu翔', 'https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500', '2739267961@qq.com', '', '湖南-衡阳', '南华大学 | 软件工程 | 2023.09 ~ 至今', '西安青砧智果科技有限公司 | 算法工程师助理 | 2026.01 ~ 2026.02', '七锡源集 | 任务看板系统 | 2025.12 ~ 2026.01', 'JavaScript, Vue, SpringBoot, MySQL', '', '初出茅庐 | 科班码农 | 拾枝者');

-- 插入示例文章数据
INSERT INTO article (title, summary, content, cover, category, tags) 
VALUES 
('Spring Boot入门教程', 'Spring Boot是一个快速开发框架，本文将介绍Spring Boot的基本概念和使用方法。', 'Spring Boot是由Pivotal团队提供的全新框架，其设计目的是用来简化新Spring应用的初始搭建以及开发过程。该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。', 'https://img0.baidu.com/it/u=1151343237,1329013895&fm=253&app=120&f=JPEG?w=640&h=400', '后端', 'Spring Boot, Java'),
('Vue3组件开发最佳实践', 'Vue3已经发布了一段时间，本文将介绍Vue3组件开发的最佳实践。', 'Vue3引入了Composition API，这使得组件的逻辑组织更加灵活。本文将介绍如何使用Composition API来开发可复用的Vue3组件。', 'https://img1.baidu.com/it/u=2169531724,3386818915&fm=253&app=120&f=JPEG?w=640&h=400', '前端', 'Vue3, JavaScript'),
('MySQL性能优化技巧', 'MySQL是最流行的关系型数据库之一，本文将介绍一些MySQL性能优化的技巧。', 'MySQL性能优化是一个复杂的话题，涉及到索引设计、查询优化、服务器配置等多个方面。本文将从多个角度介绍MySQL性能优化的技巧。', 'https://img2.baidu.com/it/u=3463004387,1689230059&fm=253&app=120&f=JPEG?w=640&h=400', '数据库', 'MySQL, 性能优化'),
('前端工程化实践', '前端工程化已经成为现代前端开发的标配，本文将介绍前端工程化的实践经验。', '前端工程化包括代码规范、构建工具、自动化测试等多个方面。本文将介绍如何搭建一个完整的前端工程化环境。', 'https://img3.baidu.com/it/u=3836708744,4265572743&fm=253&app=120&f=JPEG?w=640&h=400', '前端', '前端工程化, Webpack'),
('Spring Cloud微服务架构', 'Spring Cloud是构建微服务架构的利器，本文将介绍Spring Cloud的核心组件和使用方法。', 'Spring Cloud提供了一整套微服务解决方案，包括服务注册与发现、配置中心、负载均衡、熔断器等组件。本文将介绍如何使用Spring Cloud构建微服务架构。', 'https://img1.baidu.com/it/u=3334849785,3624443099&fm=253&app=120&f=JPEG?w=640&h=400', '后端', 'Spring Cloud, 微服务');

-- 插入示例留言数据
INSERT INTO message (nickname, content, avatar) 
VALUES 
('访客1', '网站做得很棒！', 'https://img0.baidu.com/it/u=3289832022,2938968940&fm=253&app=138&f=JPEG?w=500&h=500'),
('访客2', '内容很丰富，学习了很多知识。', 'https://img1.baidu.com/it/u=2169531724,3386818915&fm=253&app=120&f=JPEG?w=500&h=500');

-- 5. 音乐表
CREATE TABLE IF NOT EXISTS music (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '歌曲名',
    artist VARCHAR(255) DEFAULT '' COMMENT '歌手',
    album VARCHAR(255) DEFAULT '' COMMENT '专辑',
    cover VARCHAR(500) DEFAULT '' COMMENT '封面图URL',
    url VARCHAR(500) NOT NULL COMMENT '音频文件URL',
    duration INT DEFAULT 0 COMMENT '时长（秒）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音乐表';

-- 插入今天的统计数据
INSERT INTO statistic (date, page_views, unique_visitors, article_reads) 
VALUES (CURDATE(), 0, 0, 0) 
ON DUPLICATE KEY UPDATE page_views = VALUES(page_views), unique_visitors = VALUES(unique_visitors), article_reads = VALUES(article_reads);
