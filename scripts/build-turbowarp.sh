#!/bin/bash
# ============================================================
# build-turbowarp.sh - 构建本地自托管 TurboWarp 编辑器
# ============================================================
# 用法: ./scripts/build-turbowarp.sh
#
# 前置要求:
#   - Node.js 22+ (推荐通过 nvm 安装)
#   - git
#   - 网络连接（首次需要克隆仓库）
#
# 构建产物: frontend-vue/public/turbowarp/
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TW_DIR="/tmp/turbowarp-scratch-gui"
TW_BUILD_DIR="$PROJECT_ROOT/frontend-vue/public/turbowarp"

echo "🎨 TurboWarp 自托管构建脚本"
echo "================================"

# 1. 克隆或更新 TurboWarp 仓库
if [ -d "$TW_DIR/.git" ]; then
  echo "📦 更新 TurboWarp 仓库..."
  cd "$TW_DIR"
  git pull origin master --ff-only 2>/dev/null || echo "  已是最新"
else
  echo "📦 克隆 TurboWarp 仓库..."
  git clone --depth 1 https://github.com/TurboWarp/scratch-gui.git "$TW_DIR"
fi

cd "$TW_DIR"

# 2. 安装依赖
echo "📥 安装依赖..."
npm install --legacy-peer-deps --ignore-scripts

# 3. 生成必要文件
echo "🔧 生成构建资源..."
node scripts/prepublish.mjs

# 4. 构建
echo "🏗️  构建 TurboWarp..."
NODE_OPTIONS="--openssl-legacy-provider --max-old-space-size=4096" npx webpack --bail

# 5. 复制构建产物
echo "📋 复制构建产物到 frontend-vue/public/turbowarp/..."
rm -rf "$TW_BUILD_DIR"
mkdir -p "$TW_BUILD_DIR"
cp -r build/* "$TW_BUILD_DIR/"

# 6. 清理大文件（可选：移除 sourcemaps 等）
echo "🧹 清理不必要文件..."
find "$TW_BUILD_DIR" -name "*.map" -delete 2>/dev/null || true

SIZE=$(du -sh "$TW_BUILD_DIR" | cut -f1)
echo ""
echo "✅ 构建完成！"
echo "   产物目录: $TW_BUILD_DIR"
echo "   总大小: $SIZE"
echo ""
echo "📝 下一步:"
echo "   1. 运行 'cd frontend-vue && npm run dev' 启动开发服务器"
echo "   2. 访问 /editor 进入 Scratch 编辑器"
echo "   3. 访问 /turbowarp/editor.html 直接访问 TurboWarp"
