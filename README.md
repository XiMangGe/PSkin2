# PSkin2 — 跨版本兼容的统一皮肤插件

> 一个功能完整的 Minecraft 皮肤管理插件，支持 Mojang 正版、LittleSkin、自定义皮肤站、URL 抓取和网站上传皮肤，适配 Paper / Purpur 1.20 及以上版本。

---

## 📋 目录

- [支持版本](#支持版本)
- [功能特性](#功能特性)
- [安装方法](#安装方法)
- [快速开始](#快速开始)
- [配置文件详解](#配置文件详解)
- [命令大全](#命令大全)
- [权限节点](#权限节点)
- [多语言支持](#多语言支持)
- [皮肤源配置](#皮肤源配置)
- [网站功能](#网站功能)
- [多服共享](#多服共享)
- [基岩版支持](#基岩版支持geyserfloodgate)
- [常见问题](#常见问题)

---

## 支持版本

| 项目 | 要求 |
|------|------|
| **服务端** | Paper / Purpur **1.20** 及以上（含 1.20.x、1.21.x、1.22.x） |
| **Java** | Java 17 及以上 |
| **软依赖** | AuthMe（可选，登录后重新应用皮肤） |
| **软依赖** | Geyser + Floodgate（可选，基岩版玩家支持） |

> 插件通过反射兼容 Paper 和 Bukkit API，不直接依赖 Paper 专属接口，理论上支持任何基于 Bukkit 的 1.20+ 服务端。

---

## 功能特性

### 🎨 多皮肤源
- **Mojang 正版** — 自动查询正版玩家的官方皮肤
- **LittleSkin** — 国内最流行的第三方皮肤站
- **自定义 Yggdrasil 皮肤站** — 支持任意 Blessing Skin 站点（自建皮肤站）
- **URL 抓取** — 直接用图片 URL 设置皮肤
- **网站上传** — 玩家通过网页上传自己的皮肤图片

### 🔄 智能皮肤解析
- 优先级：**来源锁定 > 网站上传 > 手动设置 > 远程皮肤源列表**
- 按配置的顺序依次查询皮肤源，找到即用
- 进服自动加载，支持 AuthMe 登录后重新应用

### ✍️ Mineskin 重签
- 第三方皮肤站的签名非 Mojang 官方签名，部分客户端不显示
- 自动通过 Mineskin 重新生成带 Mojang 官方签名的纹理数据
- 支持 API Key（专属限流池，避免高峰期 429）
- 支持镜像站和端点自动切换

### 🌐 内置皮肤上传网站
- 玩家通过浏览器上传皮肤图片，全服可见
- 皮肤画廊页面，展示全服玩家上传的皮肤
- 管理后台，可查看/删除任意玩家皮肤
- 完全可自定义的网页模板（HTML/CSS/JS）
- 端口自动分配，支持反代和域名

### 🌍 多语言
- 游戏内消息：简体中文、繁體中文、English、日本語
- 网站界面：简体中文、English
- 修改配置即时切换，无需重启

### 🤝 多服数据共享
- **本地文件**（默认）— 单服模式
- **共享目录** — 多台服务器读写同一文件夹，数据互通
- **MySQL** — 数据库模式，适合跨机器多服

### 📱 基岩版支持（Geyser/Floodgate）
- 自动识别基岩版玩家前缀
- 支持网站上传/手动设置的皮肤
- 支持查询远程皮肤源（用去掉前缀的名字查询）

### 🔒 其他
- 皮肤缓存，避免频繁请求皮肤服务器
- 命令冷却，防止被限流
- 来源锁定，强制玩家使用指定皮肤源
- 调试模式，方便排查问题

---

## 安装方法

1. 下载最新版 `PSkin2-*.jar`：[Releases](https://github.com/XiMangGe/PSkin2/releases)
2. 将 JAR 文件放入服务器的 `plugins/` 目录
3. 启动服务器，插件自动生成配置文件
4. （可选）修改 `plugins/PSkin2/config.yml` 进行个性化配置
5. 执行 `/pskin reload` 或重启服务器

---

## 快速开始

安装后默认配置即可使用：

- 玩家进服自动加载 Mojang / LittleSkin 皮肤
- 输入 `/pskin web` 获取皮肤上传网站地址
- 在网站上传皮肤图片，全服可见
- 管理员可通过 `/pskin status` 查看运行状态

---

## 配置文件详解

配置文件路径：`plugins/PSkin2/config.yml`

### 基础设置

```yaml
# 游戏内语言：zh_CN / zh_TW / en_US / ja_JP
language: zh_CN

# 进服自动加载皮肤
auto-apply-on-join: true

# 进服后延迟多少 tick 应用皮肤（20 tick = 1 秒）
join-delay-ticks: 20
```

### 进服提示横幅

进服自动加载皮肤时发送的多行消息，支持 `&` 颜色代码和变量：

| 变量 | 说明 |
|------|------|
| `{player}` | 玩家名 |
| `{source}` | 皮肤来源（mojang / littleskin / web / manual / offline） |
| `{server}` | 服务器名 |

```yaml
join-notify:
  enabled: true
  start:    # 开始加载时
  success:  # 加载成功时
  fail:     # 加载失败时
```

### AuthMe 集成

```yaml
authme:
  enabled: true   # 装了 AuthMe 就开 true，没装保持 false
```

### 皮肤解析优先级

```yaml
skin-priority:
  - mojang
  - littleskin
```

> 固定优先级（不受此列表影响）：来源锁定 > 网站上传 > 手动设置
>
> 此列表只决定远程皮肤源的查询顺序。

### 皮肤源 API 配置

详见 [皮肤源配置](#皮肤源配置) 章节。

### Mineskin 重签

```yaml
mineskin:
  resign-enabled: true          # 总开关
  timeout-seconds: 30           # API 超时
  api-key: ""                   # 强烈建议填写！https://account.mineskin.org/keys
  api-url: "https://api.mineskin.org"
  mirrors:
    enabled: false              # 镜像站开关
    urls: []                    # 镜像地址列表
```

> **为什么需要重签？** LittleSkin 等第三方皮肤站的纹理签名不是 Mojang 官方签名，使用 PCL2、HMCL 正版模式等客户端的玩家看不到皮肤。开启重签后，插件会通过 Mineskin 重新生成带 Mojang 官方签名的纹理数据。

### 离线玩家

```yaml
offline:
  default-skin-enabled: false   # 是否给没皮肤的玩家应用默认皮肤
  default-skin-name: "MHF_Steve" # 默认皮肤名字（会去查这个名字的皮肤）
```

### 基岩版

详见 [基岩版支持](#基岩版支持geyserfloodgate) 章节。

### 网站设置

详见 [网站功能](#网站功能) 章节。

### 命令冷却

```yaml
commands:
  cooldown-seconds: 30   # 命令冷却（秒），0 = 不冷却
```

### 缓存

```yaml
skin-cache-minutes: 720   # 皮肤缓存时长（分钟），720 = 12 小时，0 = 禁用
```

> 玩家手动设置的皮肤永久保留，不受缓存时长限制。

### 存储（多服共享）

详见 [多服共享](#多服共享) 章节。

### 高级设置

```yaml
user-agent: "PSkin2/26.10.4.1"    # HTTP 请求标识
refresh-visibility: false         # 应用皮肤后是否强制刷新可见性
debug: false                      # 调试模式
```

---

## 命令大全

主命令：`/pskin`（别名：`/skinp`、`/skin`）

| 命令 | 说明 | 权限 |
|------|------|------|
| `/pskin set <玩家名>` | 按玩家名设置皮肤（从皮肤源查询） | pskin.use |
| `/pskin url <图片URL>` | 用图片 URL 设置皮肤 | pskin.use |
| `/pskin source <来源>` | 锁定皮肤来源（mojang/littleskin/自定义源名） | pskin.use |
| `/pskin delete` | 删除自己的皮肤 | pskin.use |
| `/pskin web` | 获取皮肤上传网站地址 | pskin.use |
| `/pskin clear` | 清除皮肤（恢复默认） | pskin.use |
| `/pskin update` | 更新皮肤（重新查询） | pskin.use |
| `/pskin info` | 查看当前皮肤信息 | pskin.use |
| `/pskin apply [玩家]` | 重新应用皮肤 | pskin.admin |
| `/pskin status` | 查看插件运行状态 | pskin.admin |
| `/pskin reload` | 重载配置文件 | pskin.admin |
| `/pskin help` | 查看帮助 | pskin.use |

---

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `pskin.use` | 使用基础命令 | 所有玩家 |
| `pskin.admin` | 管理命令（apply/status/reload/操作他人） | OP |
| `pskin.bypasscooldown` | 绕过命令冷却 | OP |

---

## 多语言支持

### 游戏内语言

修改 `config.yml` 中的 `language` 字段：

| 值 | 语言 |
|----|------|
| `zh_CN` | 简体中文（默认） |
| `zh_TW` | 繁體中文 |
| `en_US` | English |
| `ja_JP` | 日本語 |

修改后执行 `/pskin reload` 立即生效。

### 网站语言

修改 `config.yml` 中 `web.language` 字段：

| 值 | 语言 |
|----|------|
| `zh_CN` | 简体中文（默认） |
| `en_US` | English |

---

## 皮肤源配置

在 `config.yml` 的 `providers` 节点配置皮肤源。

### 内置皮肤源

#### Mojang 正版

```yaml
providers:
  mojang:
    enabled: true
    profile-url: "https://api.mojang.com/users/profiles/minecraft/%s"
    session-url: "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false"
    timeout-seconds: 10
```

#### LittleSkin

```yaml
providers:
  littleskin:
    enabled: true
    api-root: "https://littleskin.cn/api/yggdrasil"
    timeout-seconds: 10
```

### 自定义皮肤站（v26.10.4.1+）

支持任意基于 **Blessing Skin Server** 的皮肤站（Yggdrasil 协议），可以添加任意多个：

```yaml
providers:
  order:
    - mojang
    - littleskin
    - myskin          # 把自定义皮肤站名字加到 order 里
    - friends-skin    # 可以加多个

  # 你的自建皮肤站
  myskin:
    type: yggdrasil                      # 必须填 yggdrasil
    enabled: true
    api-root: "https://你的域名/api/yggdrasil"
    timeout-seconds: 10

  # 朋友的皮肤站
  friends-skin:
    type: yggdrasil
    enabled: true
    api-root: "https://朋友的皮肤站/api/yggdrasil"
    timeout-seconds: 10
```

| 配置项 | 说明 |
|--------|------|
| `type` | 必须填 `yggdrasil` |
| `enabled` | `true` 启用 / `false` 禁用 |
| `api-root` | 皮肤站的 Yggdrasil API 地址（一般是 `https://域名/api/yggdrasil`） |
| `timeout-seconds` | 请求超时（秒） |

> 进服日志会显示每个皮肤源是否注册成功。

---

## 网站功能

### 基本配置

```yaml
web:
  enabled: true              # 网站总开关
  show-link: true            # 游戏内显示网站地址
  port: 7505                 # 网站端口
  auto-port: true            # 端口被占用时自动换端口
  bind: "0.0.0.0"            # 绑定地址
  public-address: ""         # 公网访问地址（有域名/反代时填写）
  language: zh_CN            # 网站语言
```

### 网站内容自定义

```yaml
web:
  title: "服务器皮肤上传网站"       # 网站标题
  subtitle: "上传你的专属皮肤"       # 副标题
  announcement: ""                  # 公告
  footer: "Powered by PSkin2"       # 页脚
  gallery-title: "全服皮肤画廊"      # 画廊标题
  theme-color: "#4f8cff"            # 主题色
```

### 上传限制

```yaml
web:
  max-upload-kb: 256              # 图片最大体积（KB）
  validate-image: true            # 校验 PNG 格式和皮肤尺寸
  upload-cooldown-seconds: 60     # 同一玩家上传间隔（秒）
```

### 管理后台

```yaml
web:
  admin-password: ""    # 设置密码后启用 /admin 管理页
```

设置密码后访问 `http://服务器地址:端口/admin`，可查看/删除任意玩家上传的皮肤。

### 自定义网页模板

开启 `files-enabled` 后，插件会在 `plugins/PSkin2/Web/` 下生成 HTML 模板文件：

| 文件 | 用途 |
|------|------|
| `upload.html` | 上传页（首页） |
| `gallery.html` | 皮肤画廊页 |
| `admin.html` | 管理后台页 |
| `404.html` | 404 页面 |

直接修改这些文件，执行 `/pskin reload` 生效。

---

## 多服共享

支持三种存储模式，在 `storage.type` 中配置：

### 1. 本地文件（默认）

```yaml
storage:
  type: file
```

单服模式，数据只存在本服。

### 2. 共享目录

```yaml
storage:
  type: shared-dir
  directory: "/srv/pskin-data"    # 两台服务器填同一个路径
```

多台服务器读写同一个文件夹，数据互通，无需数据库。

> 要求：所有服务器都能访问这个目录（同一台机器或网络共享）。

### 3. MySQL 数据库

```yaml
storage:
  type: mysql
  mysql:
    enabled: true
    host: "127.0.0.1"
    port: 3306
    database: "pskin"
    username: "pskin"
    password: "你的密码"
    table-prefix: "pskin_"
    use-ssl: false
```

适合跨机器的多服架构，所有服务器连接同一个数据库。

> **注意**：修改存储模式需要重启服务器，`/pskin reload` 无效。

---

## 基岩版支持（Geyser/Floodgate）

```yaml
bedrock:
  enabled: true               # 总开关
  prefix: "."                 # Floodgate 玩家名前缀
  apply-custom-skins: true    # 应用网站上传/手动设置的皮肤
  remote-skins: true          # 查询远程皮肤源
```

- 基岩版玩家上传皮肤时，需要用带前缀的完整名字（如 `.Steve`）
- 查询远程皮肤源时，自动去掉前缀用 `Steve` 查询

---

## 常见问题

### Q: 皮肤不显示怎么办？

1. 确认 `mineskin.resign-enabled` 为 `true`
2. 建议配置 `mineskin.api-key`（免费申请：https://account.mineskin.org/keys）
3. 执行 `/pskin status` 检查皮肤源是否正常
4. 开启 `debug: true` 查看详细日志

### Q: 网站打不开？

1. 检查 `web.enabled` 是否为 `true`
2. 检查端口 `web.port` 是否被防火墙拦截
3. 查看控制台日志中网站实际监听的端口（开启了 `auto-port` 可能换端口）

### Q: AuthMe 服务器皮肤进服就没了？

确保 `authme.enabled: true`，插件会在登录验证通过后重新应用皮肤。

### Q: 多服数据不互通？

1. 确认所有服务器的 `storage.type` 一致（shared-dir 或 mysql）
2. 共享目录模式：所有服务器的 `directory` 路径必须完全相同
3. MySQL 模式：所有服务器连接同一个数据库
4. 修改存储模式后必须重启服务器

### Q: 如何添加自建皮肤站？

在 `providers` 下添加 `type: yggdrasil` 的配置项，参考 [自定义皮肤站](#自定义皮肤站v261041) 章节。

---

## 更新日志

### v26.10.4.1
- ✨ 新增：支持通过配置文件添加多个自定义 Yggdrasil 皮肤站
- 🔧 修复：8 个逻辑 bug（XSS、NPE、TTL 不一致等）
- 🔧 跨版本兼容：Paper API 改反射，支持 1.20 ~ 最新版

### v26.8.30.3
- 🔧 跨版本兼容改造
- 🔧 修复反编译垃圾变量和类型安全问题

---

## License

本插件仅供学习和个人服务器使用。
