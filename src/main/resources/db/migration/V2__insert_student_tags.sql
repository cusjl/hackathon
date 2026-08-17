DROP TABLE IF EXISTS student_tag;

CREATE TABLE student_tag (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO student_tag (name) VALUES
('UI/UX 设计'),
('Prompt 工程'),
('产品策划'),
('AI Agent 架构'),
('领域业务洞察'),
('AI 工具集成');