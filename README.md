# PSkin2 - 跨版本兼容皮肤插件

离线服务器统一皮肤管理插件，支持 Mojang、LittleSkin、URL 抓取和网站上传。

## 特性

- **跨版本兼容**：所有 Paper 专属 API 通过反射调用，自动回退到 Bukkit API，无需重新编译即可适配新版本
- **多皮肤源**：Mojang（正版）、LittleSkin、URL 直接抓取、网站上传
- **存储模式**：本地文件 / 共享目录（多服共享）/ MySQL
- **来源锁定**：可为指定玩家锁定皮肤来源
- **基岩版支持**：可配置基岩版玩家的皮肤处理策略
- **Mineskin 重签**：自动为非正版皮肤重新签名
- **Web 管理界面**：内置皮肤上传网站和管理后台

## 版本要求

- Paper / Purpur 1.20+（api-version: 1.20）
- Java 17+

## 编译

```bash
# 需要 paper-api.jar, adventure-api.jar, adventure-serializer-legacy.jar, bungeecord-chat.jar, examination-api.jar
./build.sh
```

输出：`PSkin2-26.8.30.3.jar`

## 安装

1. 将 `PSkin2-26.8.30.3.jar` 放入服务器 `plugins/` 目录
2. 启动服务器，自动生成配置文件
3. 编辑 `plugins/PSkin2/config.yml` 按需配置

## 命令

| 命令 | 说明 |
|------|------|
| `/pskin set <玩家名>` | 设置皮肤为指定正版玩家的皮肤 |
| `/pskin url <图片URL>` | 从 URL 抓取皮肤 |
| `/pskin source [玩家] [来源]` | 查看/设置皮肤来源锁定 |
| `/pskin delete [玩家]` | 删除网站上传的皮肤 |
| `/pskin web` | 显示皮肤上传网站地址 |
| `/pskin clear [玩家]` | 清除手动设置的皮肤 |
| `/pskin update` | 重新拉取当前皮肤 |
| `/pskin info [玩家]` | 查看皮肤信息 |
| `/pskin apply <玩家>` | 强制为在线玩家应用皮肤 |
| `/pskin status` | 查看插件状态（管理员） |
| `/pskin reload` | 重载配置（管理员） |

## 皮肤来源优先级

来源锁定 > 网站上传（独占）> 手动设置 > skin-priority 配置 > 默认皮肤

## 配置说明

主要配置项见 `config.yml`，关键配置：

- `providers.order`：皮肤源查询顺序
- `skin-priority`：自动应用优先级
- `storage.type`：存储模式（file / shared-dir / mysql）
- `web.enabled`：是否启用皮肤上传网站
- `mineskin.api-key`：Mineskin API Key（推荐配置）

## License

本插件基于 PSkin2 改造，仅供学习交流使用。
