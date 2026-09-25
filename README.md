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
- 支持按合同发起带截止时间的一轮多人签署邀请，并跟踪每位签署方的待签/已签/过期结果。
- 签署方凭邀请令牌和本人身份仅能签署一次；重复提交返回第一次签署结果。
- 所有签署方完成后合同自动变为已签署；邀请到期后合同自动变为已过期，迟到签名不会改写记录。
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
- `PATCH /api/contracts/{id}/status` 更新非终局合同状态（签署/过期由流程自动流转）
- `POST /api/contracts/{id}/signing-rounds` 发起一轮带截止时间的多人签署邀请
- `GET /api/contracts/{id}/signing-progress` 后台查看邀请进度和每位签署方处理结果
- `POST /api/contracts/signing-invitations/{token}/sign` 签署人校验本人身份并提交一次签名
- `GET /api/contracts` 用户合同库
- `POST /api/tickets` 提交法律工单
- `POST /api/tickets/{id}/replies` 添加工单回复
- `GET /api/knowledge` 搜索法律 FAQ

### 发起签署邀请

```json
{
  "expiresAt": "2026-10-01T18:00:00",
  "signers": [
    {"name": "张三", "email": "zhangsan@example.com", "identityNumber": "11010119900101001X"},
    {"name": "李四", "email": "lisi@example.com", "identityNumber": "110101199002020028"}
  ]
}
```

响应会返回每位签署方唯一的 `invitationToken`。签署接口由收到邀请的本人调用：

```json
{
  "signerName": "张三",
  "identityNumber": "11010119900101001X",
  "signatureValue": "张三"
}
```

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | Compose 项目名，默认 `contractapi` |
| `MYSQL_*` | MySQL 数据库配置 |
| `JWT_SECRET` | JWT 签名密钥 |

## License

MIT
