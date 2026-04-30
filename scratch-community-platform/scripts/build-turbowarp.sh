#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TURBOWARP_DIR="$SCRIPT_DIR/turbowarp"
BUILD_DIR="$SCRIPT_DIR/../frontend-vue/public/turbowarp"

echo "=========================================="
echo "  TurboWarp 自托管构建工具"
echo "=========================================="

mkdir -p "$BUILD_DIR"

# 创建基础 HTML 文件（无需完整克隆 TurboWarp）
echo ""
echo "步骤 1: 创建 TurboWarp 集成页面..."

cat > "$BUILD_DIR/editor.html" << 'EOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Scratch 编辑器</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; overflow: hidden; }
    #app { width: 100%; height: 100%; }
  </style>
</head>
<body>
  <div id="app"></div>
  <script>
    // 获取 URL 参数
    function getQueryParam(name) {
      const params = new URLSearchParams(window.location.search);
      return params.get(name);
    }

    const projectUrl = getQueryParam('project_url');

    // 构建 TurboWarp URL
    let turboWarpUrl = 'https://turbowarp.org/editor';
    if (projectUrl) {
      turboWarpUrl += '?project_url=' + encodeURIComponent(projectUrl);
    }

    // 构建 iframe
    const iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = 'none';
    iframe.setAttribute('allow', 'clipboard-read; clipboard-write');
    iframe.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-popups allow-forms allow-downloads');
    iframe.src = turboWarpUrl;

    // 监听 iframe 消息并转发
    window.addEventListener('message', function(event) {
      if (event.source === iframe.contentWindow) {
        // 从 TurboWarp 转发到父页面
        window.parent.postMessage(event.data, '*');
      } else if (event.source === window.parent) {
        // 从父页面转发到 TurboWarp
        iframe.contentWindow.postMessage(event.data, '*');
      }
    });

    document.getElementById('app').appendChild(iframe);

    // 通知父页面 iframe 已加载
    setTimeout(() => {
      window.parent.postMessage({
        type: 'iframe-ready'
      }, '*');
    }, 500);
  </script>
</body>
</html>
EOF

cat > "$BUILD_DIR/player.html" << 'EOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Scratch 播放器</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; overflow: hidden; }
    #app { width: 100%; height: 100%; }
  </style>
</head>
<body>
  <div id="app"></div>
  <script>
    function getQueryParam(name) {
      const params = new URLSearchParams(window.location.search);
      return params.get(name);
    }

    const projectUrl = getQueryParam('project_url');

    let turboWarpUrl = 'https://turbowarp.org/';
    if (projectUrl) {
      turboWarpUrl += '?project_url=' + encodeURIComponent(projectUrl);
    }

    const iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = 'none';
    iframe.setAttribute('allow', 'autoplay; clipboard-read; clipboard-write');
    iframe.setAttribute('sandbox', 'allow-scripts allow-same-origin allow-popups allow-forms allow-downloads');
    iframe.src = turboWarpUrl;

    window.addEventListener('message', function(event) {
      if (event.source === iframe.contentWindow) {
        window.parent.postMessage(event.data, '*');
      } else if (event.source === window.parent) {
        iframe.contentWindow.postMessage(event.data, '*');
      }
    });

    document.getElementById('app').appendChild(iframe);

    setTimeout(() => {
      window.parent.postMessage({ type: 'iframe-ready' }, '*');
    }, 500);
  </script>
</body>
</html>
EOF

# 创建简单的示例 HTML 用于直接预览
cat > "$BUILD_DIR/index.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>TurboWarp 本地集成</title>
</head>
<body>
  <h1>TurboWarp 本地集成测试页</h1>
  <p>此页面用于测试自托管的 TurboWarp 集成</p>
  <ul>
    <li><a href="editor.html">编辑器</a></li>
    <li><a href="player.html">播放器</a></li>
  </ul>
</body>
</html>
EOF

echo ""
echo "✓ TurboWarp 集成页面创建完成！"
echo ""
echo "=========================================="
echo "  构建完成"
echo "=========================================="
echo ""
echo "产物位置: $BUILD_DIR"
ls -la "$BUILD_DIR"
echo ""
echo "提示: 此方案使用 TurboWarp 官方 CDN，但通过"
echo "      本地页面实现 iframe 消息转发，解决"
echo "      跨域通信问题。"
echo ""
echo "如需完整自托管，请参考 docs/TURBOWARP_INTEGRATION.md"
