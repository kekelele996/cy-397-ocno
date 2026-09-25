# 合同模板生成与法律工单 API

```bash
cp .env.example .env
docker compose up -d --build
```

合同模板生成与法律工单 API 提供合同模板管理、变量填充生成、签署状态跟踪、法律咨询工单和法律 FAQ 检索能力。

## 项目主要功能

- 管理租赁、劳动、借款、合作、保密协议模板与占位变量。
- 根据变量生成纯文本/HTML 合同，并预留 wkhtmltopdf 导出 PDF。
- 合同状态支持草稿、待签署、已签署、已过期。
- 多方签署邀请：按合同发起一轮带截止时间的邀请，每位签署方在期限内签署一次，重复提交返回首次结果；全员签完合同置为已签署，过期后合同置为已过期且不再受理签名。
- 法律工单提交、分配、回复和关闭。
- 法律 FAQ 分类维护与关键词搜索。
- 用户合同库与模板收藏。

## 本地开发

```bash
cd backend
mvn spring-boot:run
```

## 技术栈

| 类型 | 技术 |
| --- | --- |
| 后端 | Spring Boot + Java 17 |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 认证 | JWT |
| PDF | wkhtmltopdf |

## 目录结构

```text
.
├── backend
│   ├── src/main/java/com/contractapi
│   └── src/main/resources
├── database
│   └── init.sql
└── docker-compose.yml
```

## 主要 API

- `GET /api/templates` 模板列表
- `POST /api/templates` 新增模板
- `POST /api/contracts/generate` 合同生成
- `PATCH /api/contracts/{id}/status` 更新签署状态
- `POST /api/contracts/{id}/invitations` 发起一轮签署邀请（签署方名单 + 截止时间）
- `POST /api/contracts/{id}/sign` 签署方签名（幂等，重复提交返回首次结果）
- `GET /api/contracts/{id}/invitations` 查看邀请进度与每位签署方处理结果
- `GET /api/contracts` 用户合同库
- `POST /api/tickets` 提交法律工单
- `POST /api/tickets/{id}/replies` 添加工单回复
- `GET /api/knowledge` 搜索法律 FAQ

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | Compose 项目名，默认 `contractapi` |
| `MYSQL_*` | MySQL 数据库配置 |
| `JWT_SECRET` | JWT 签名密钥 |

## License

MIT
