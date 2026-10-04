#!/bin/bash
# PSkin2 构建脚本
# 依赖: paper-api.jar, adventure-api.jar, adventure-serializer-legacy.jar, bungeecord-chat.jar, examination-api.jar
# 将这些 jar 放入 libs/ 目录，或将原始插件 jar 放入项目根目录

set -e

VERSION="26.10.4.2"
SRC="src/main/java"
OUT="classes"
JAR="PSkin2-${VERSION}.jar"
LIBS_DIR="libs"

# 检查 Java
JAVAC=$(which javac 2>/dev/null || echo "/usr/lib/jvm/java-21-openjdk-amd64/bin/javac")
if [ ! -x "$JAVAC" ]; then
    echo "错误: 未找到 javac，请安装 JDK 17+"
    exit 1
fi

# 构建 classpath
CP=""
for jar in "$LIBS_DIR"/*.jar; do
    [ -f "$jar" ] && CP="$CP:$jar"
done
# 如果有原始插件 jar，也加入 classpath（提供 gson 等依赖）
for jar in *.jar; do
    [ -f "$jar" ] && CP="$CP:$jar"
done
CP="${CP#:}"

if [ -z "$CP" ]; then
    echo "错误: 未找到依赖 jar，请将 paper-api.jar 等放入 libs/ 目录"
    exit 1
fi

echo "==> 清理..."
rm -rf "$OUT"
mkdir -p "$OUT"

echo "==> 编译 (Java 17)..."
"$JAVAC" --release 17 -cp "$CP" -d "$OUT" $(find "$SRC" -name "*.java")

echo "==> 打包..."
# 从原始 jar 复制资源（gson 等依赖），再替换插件类
ORIG_JAR=$(ls PSkin2-*.jar 2>/dev/null | head -1)
if [ -n "$ORIG_JAR" ]; then
    python3 - "$ORIG_JAR" "$JAR" "$OUT" << 'PY'
import zipfile, os, sys
orig, out, classes = sys.argv[1], sys.argv[2], sys.argv[3]
with zipfile.ZipFile(orig, 'r') as zin:
    entries = {i.filename: zin.read(i.filename) for i in zin.infolist()}
for root, _, files in os.walk(classes):
    for fn in files:
        if fn.endswith('.class'):
            full = os.path.join(root, fn)
            arcname = os.path.relpath(full, classes)
            with open(full, 'rb') as f:
                entries[arcname] = f.read()
with open('src/main/resources/plugin.yml', 'rb') as f:
    entries['plugin.yml'] = f.read()
if os.path.exists(out):
    os.remove(out)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in entries.items():
        zout.writestr(name, data)
print(f"输出: {out} ({os.path.getsize(out)} bytes)")
PY
else
    jar cf "$JAR" -C "$OUT" . -C src/main/resources .
    echo "输出: $JAR (仅包含插件类，无依赖)"
fi

echo "==> 完成: $JAR"
