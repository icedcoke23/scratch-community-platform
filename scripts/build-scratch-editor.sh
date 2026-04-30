#!/bin/bash
# build-scratch-editor.sh
# 构建 Scratch 编辑器静态文件
# 用法: bash scripts/build-scratch-editor.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SCRATCH_GUI_DIR="$PROJECT_ROOT/.scratch-gui"
OUTPUT_DIR="$PROJECT_ROOT/frontend-vue/public/scratch-editor"
TEMPLATES_DIR="$SCRIPT_DIR/scratch-editor-templates"

echo "🎨 构建 Scratch 编辑器..."
echo "  源码目录: $SCRATCH_GUI_DIR"
echo "  输出目录: $OUTPUT_DIR"
echo "  模板目录: $TEMPLATES_DIR"

# 1. 克隆 scratch-gui（如果不存在）
if [ ! -d "$SCRATCH_GUI_DIR" ]; then
  echo "📥 克隆 scratch-gui..."
  git clone --depth 1 https://github.com/scratchfoundation/scratch-gui.git "$SCRATCH_GUI_DIR"
fi

# 2. 安装依赖
echo "📦 安装依赖..."
cd "$SCRATCH_GUI_DIR"
npm install --ignore-scripts 2>/dev/null

# 3. 生成必要的 stub 文件
mkdir -p src/generated static/microbit
if [ ! -f src/generated/microbit-hex-url.cjs ]; then
  echo "// stub - microbit hex not available" > src/generated/microbit-hex-url.cjs
  echo "module.exports = '';" >> src/generated/microbit-hex-url.cjs
fi

# 4. 构建
echo "🔨 构建中..."
NODE_OPTIONS="--openssl-legacy-provider" npx webpack --mode production 2>&1 | tail -5

# 5. 复制到输出目录
echo "📁 复制文件..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cp -r build/* "$OUTPUT_DIR/"

# 6. 删除 source map 文件（减小体积）
rm -f "$OUTPUT_DIR"/*.map

# 7. 复制自定义模板页面（覆盖 scratch-gui 默认页面）
echo "📄 复制自定义模板..."
cp "$TEMPLATES_DIR/scratch-editor.html" "$OUTPUT_DIR/"
cp "$TEMPLATES_DIR/scratch-player.html" "$OUTPUT_DIR/"

# 8. 显示结果
TOTAL_SIZE=$(du -sh "$OUTPUT_DIR" | cut -f1)
echo ""
echo "✅ Scratch 编辑器构建完成!"
echo "  总大小: $TOTAL_SIZE"
echo "  输出: $OUTPUT_DIR"
echo ""
echo "文件列表:"
ls -lh "$OUTPUT_DIR"/*.html "$OUTPUT_DIR"/*.js 2>/dev/null | awk '{print "  " $NF " (" $5 ")"}'
