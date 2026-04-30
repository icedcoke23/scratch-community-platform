#!/bin/bash
# build-scratch-editor.sh
# 验证 Scratch 编辑器静态文件是否就绪
#
# 现在使用预编译的 scratch3 静态文件（来自 teaching-open 项目），
# 文件已直接提交到 frontend-vue/public/scratch-editor/ 目录，
# 无需在 CI 中编译 scratch-gui。

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="$PROJECT_ROOT/frontend-vue/public/scratch-editor"

echo "🎨 验证 Scratch 编辑器静态文件..."

# 检查必要文件是否存在
MISSING=0
for f in lib.min.js chunks/gui.js chunks/player.js scratch-editor.html scratch-player.html; do
  if [ ! -f "$OUTPUT_DIR/$f" ]; then
    echo "  ❌ 缺少: $f"
    MISSING=1
  fi
done

if [ $MISSING -eq 1 ]; then
  echo ""
  echo "❌ Scratch 编辑器静态文件不完整！"
  echo "   请确保 frontend-vue/public/scratch-editor/ 目录包含所有必要文件。"
  echo "   参考: https://github.com/open-scratch/teaching-open 的 web/public/scratch3/ 目录"
  exit 1
fi

# 显示文件信息
TOTAL_SIZE=$(du -sh "$OUTPUT_DIR" | cut -f1)
echo ""
echo "✅ Scratch 编辑器静态文件就绪!"
echo "  总大小: $TOTAL_SIZE"
echo "  目录: $OUTPUT_DIR"
echo ""
echo "核心文件:"
ls -lh "$OUTPUT_DIR/lib.min.js" "$OUTPUT_DIR/chunks/gui.js" "$OUTPUT_DIR/chunks/player.js" 2>/dev/null | awk '{print "  " $NF " (" $5 ")"}'
