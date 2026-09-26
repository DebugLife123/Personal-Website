-- 个人网站数据库初始化脚本
-- 说明：本脚本可重复执行（幂等）。
--   - 所有表均为 CREATE TABLE IF NOT EXISTS，不会删除已有数据；
--   - 种子数据使用 INSERT IGNORE / ON DUPLICATE KEY UPDATE，重复执行不会产生重复行；
--   - 如需重置某张表，请手动 DROP 后再执行本脚本。
-- 创建数据库
CREATE DATABASE IF NOT EXISTS personal_website DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE personal_website;

-- 0. 管理员账号表（默认账号 admin / admin123，首次登录后请立即修改密码）
CREATE TABLE IF NOT EXISTS admin_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员账号表';

-- 默认管理员（admin123 的 BCrypt 哈希；已存在同名账号时忽略）
INSERT IGNORE INTO admin_user (username, password)
VALUES ('admin', '$2a$10$.KR4H4sZKkRNRQAMOPe22eMe0pRXkHzWQUg3Cfx4K2be8wl5/l0VK');

-- 1. 文章表
CREATE TABLE IF NOT EXISTS article (
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
CREATE TABLE IF NOT EXISTS message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    content TEXT NOT NULL COMMENT '留言内容',
    avatar VARCHAR(255) COMMENT '头像',
    email VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    likes INT DEFAULT 0 COMMENT '点赞数',
    is_pinned TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态: pending/approved/rejected/deleted',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    parent_id INT COMMENT '父留言ID，用于实现回复功能',
    reply_to VARCHAR(50) COMMENT '回复对象的昵称',
    FOREIGN KEY (parent_id) REFERENCES message(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';

-- 3. 个人简历表
CREATE TABLE IF NOT EXISTS resume (
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

-- 5. 结构化简历经历表：支持教育、实习/工作、项目的增删改查
CREATE TABLE IF NOT EXISTS resume_entry (
    id INT AUTO_INCREMENT PRIMARY KEY,
    resume_id INT NOT NULL DEFAULT 1 COMMENT '所属简历ID',
    type VARCHAR(20) NOT NULL COMMENT '类型：education/work/project',
    title VARCHAR(255) NOT NULL COMMENT '学校、公司或项目名称',
    subtitle VARCHAR(255) DEFAULT '' COMMENT '专业、职位或项目角色',
    time_range VARCHAR(100) DEFAULT '' COMMENT '时间范围',
    description TEXT COMMENT '经历描述',
    sort INT DEFAULT 0 COMMENT '排序值，越小越靠前',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_entry_resume_type (resume_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化简历经历';

-- 6. 音乐表
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

-- 7. 项目展示表
CREATE TABLE IF NOT EXISTS project (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    tech_stack VARCHAR(255) DEFAULT '' COMMENT '技术栈，逗号分隔',
    github_url VARCHAR(500) DEFAULT '' COMMENT 'GitHub 地址',
    img_url VARCHAR(500) DEFAULT '' COMMENT '展示图',
    preview_url VARCHAR(500) DEFAULT '' COMMENT '在线预览地址',
    category VARCHAR(50) DEFAULT '' COMMENT '项目分类',
    status VARCHAR(20) DEFAULT '' COMMENT '项目状态',
    sort INT DEFAULT 0 COMMENT '排序值，越小越靠前',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目展示表';

-- 8. 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) DEFAULT '' COMMENT '操作人',
    action VARCHAR(100) DEFAULT '' COMMENT '操作类型',
    detail VARCHAR(500) DEFAULT '' COMMENT '操作详情',
    ip VARCHAR(50) DEFAULT '' COMMENT '来源IP',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_op_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 9. 访客记录表
CREATE TABLE IF NOT EXISTS visitor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ip VARCHAR(50) DEFAULT '' COMMENT '访客IP',
    user_agent VARCHAR(255) DEFAULT '' COMMENT '浏览器UA',
    path VARCHAR(200) DEFAULT '' COMMENT '访问路径',
    visit_date DATE COMMENT '访问日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_visitor_date (visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客记录表';

-- 10. 站点设置表
CREATE TABLE IF NOT EXISTS site_setting (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL COMMENT '设置项',
    setting_value TEXT COMMENT '设置值',
    description VARCHAR(200) DEFAULT '' COMMENT '说明',
    UNIQUE KEY uk_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置表';

-- ==================== 种子数据（幂等，可重复执行） ====================

-- 默认个人简历（固定 id=1）
INSERT INTO resume (id, name, avatar, email, phone, address, education, work_experience, project_experience, skill, award, intro)
VALUES (1, 'yu翔', '', '2739267961@qq.com', '', '湖南-衡阳', '南华大学 | 软件工程 | 2023.09 ~ 至今', '西安青砧智果科技有限公司 | 算法工程师助理 | 2026.01 ~ 2026.02', '七锡源集 | 任务看板系统 | 2025.12 ~ 2026.01', 'JavaScript, Vue, SpringBoot, MySQL', '', '初出茅庐 | 科班码农 | 拾枝者')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 示例文章（固定 id，重复执行会被忽略）
INSERT IGNORE INTO article (id, title, summary, content, cover, category, tags) VALUES
(1, 'Spring Boot入门教程', 'Spring Boot是一个快速开发框架，本文将介绍Spring Boot的基本概念和使用方法。', 'Spring Boot是由Pivotal团队提供的全新框架，其设计目的是用来简化新Spring应用的初始搭建以及开发过程。该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。', '', '后端', 'Spring Boot, Java'),
(2, 'Vue3组件开发最佳实践', 'Vue3已经发布了一段时间，本文将介绍Vue3组件开发的最佳实践。', 'Vue3引入了Composition API，这使得组件的逻辑组织更加灵活。本文将介绍如何使用Composition API来开发可复用的Vue3组件。', '', '前端', 'Vue3, JavaScript'),
(3, 'MySQL性能优化技巧', 'MySQL是最流行的关系型数据库之一，本文将介绍一些MySQL性能优化的技巧。', 'MySQL性能优化是一个复杂的话题，涉及到索引设计、查询优化、服务器配置等多个方面。本文将从多个角度介绍MySQL性能优化的技巧。', '', '数据库', 'MySQL, 性能优化'),
(4, '前端工程化实践', '前端工程化已经成为现代前端开发的标配，本文将介绍前端工程化的实践经验。', '前端工程化包括代码规范、构建工具、自动化测试等多个方面。本文将介绍如何搭建一个完整的前端工程化环境。', '', '前端', '前端工程化, Webpack'),
(5, 'Spring Cloud微服务架构', 'Spring Cloud是构建微服务架构的利器，本文将介绍Spring Cloud的核心组件和使用方法。', 'Spring Cloud提供了一整套微服务解决方案，包括服务注册与发现、配置中心、负载均衡、熔断器等组件。本文将介绍如何使用Spring Cloud构建微服务架构。', '', '后端', 'Spring Cloud, 微服务');

-- 示例留言（固定 id，重复执行会被忽略）
INSERT IGNORE INTO message (id, nickname, content, avatar, status) VALUES
(1, '访客1', '网站做得很棒！', '', 'approved'),
(2, '访客2', '内容很丰富，学习了很多知识。', '', 'approved');

-- 插入今天的统计数据
INSERT INTO statistic (date, page_views, unique_visitors, article_reads)
VALUES (CURDATE(), 0, 0, 0)
ON DUPLICATE KEY UPDATE page_views = VALUES(page_views), unique_visitors = VALUES(unique_visitors), article_reads = VALUES(article_reads);

-- 默认站点设置
INSERT INTO site_setting (setting_key, setting_value, description) VALUES
('site_title', 'yu翔的个人网站', '站点标题'),
('site_description', '记录技术与生活', '站点简介'),
('icp', '', '备案号'),
('footer_text', 'Built with Vue3 & SpringBoot', '页脚文案'),
('music_autoplay', 'false', '音乐自动播放')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
