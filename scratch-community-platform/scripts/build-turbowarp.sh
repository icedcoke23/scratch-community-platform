#!/bin/bash

set -e

echo "=========================================="
echo "  TurboWarp 构建脚本"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TURBOWARP_DIR="$SCRIPT_DIR/turbowarp"
BUILD_DIR="$SCRIPT_DIR/../frontend-vue/public/turbowarp"

echo ""
echo "步骤 1: 克隆 TurboWarp scratch-gui..."
if [ -d "$TURBOWARP_DIR" ]; then
    echo "  - 已存在，跳过克隆（使用现有版本）"
else
    git clone https://github.com/TurboWarp/scratch-gui.git "$TURBOWARP_DIR"
fi

cd "$TURBOWARP_DIR"

echo ""
echo "步骤 2: 安装依赖..."
npm ci

echo ""
echo "步骤 3: 构建生产版本..."
NODE_ENV=production npm run build

echo ""
echo "步骤 4: 复制构建产物到前端目录..."
mkdir -p "$BUILD_DIR"
rm -rf "$BUILD_DIR"/*
cp -r "$TURBOWARP_DIR/build/." "$BUILD_DIR/"

echo ""
echo "步骤 5: 创建自定义 HTML 页面（用于 iframe 加载）..."

cat > "$BUILD_DIR/editor-iframe.html" << 'HTMLEOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scratch 编辑器</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; height: 100%; overflow: hidden; }
    </style>
</head>
<body>
    <div id="app"></div>
    <script src="./static/js/editor.js"></script>
    <script src="./static/js/bundle.js"></script>
    <script src="./static/js/gui.js"></script>
    <script src="./static/js/l10n.js"></script>
    <script src="./static/js/vendors~gui.js"></script>
    <script>
        // postMessage 双向通信协议
        // 允许父页面控制 TurboWarp 编辑器
        
        const MESSAGES_TO_PARENT = {
            VM_READY: 'vm-initialized',
            PROJECT_CHANGED: 'project-changed',
            PROJECT_SAVE: 'project-save',
            FULLSCREEN: 'fullscreen',
            ERROR: 'error',
            EDITOR_MODE: 'editor-mode',
            PLAYER_MODE: 'player-mode'
        };

        let vmInstance = null;

        // 等待 vm 实例初始化
        const waitForVM = (callback, maxAttempts = 50) => {
            let attempts = 0;
            const check = () => {
                if (window.vm && window.vm.initialized) {
                    vmInstance = window.vm;
                    callback(window.vm);
                } else if (attempts < maxAttempts) {
                    attempts++;
                    setTimeout(check, 100);
                } else {
                    console.error('[TW-API] VM 初始化超时');
                }
            };
            check();
        };

        // VM 准备就绪后通知父页面
        waitForVM((vm) => {
            console.log('[TW-API] VM 初始化完成');
            
            // 通知父页面
            window.parent.postMessage({
                type: MESSAGES_TO_PARENT.VM_READY,
                vmId: 'vm-instance-' + Date.now()
            }, '*');

            // 监听项目变化
            vm.on('PROJECT_CHANGED', () => {
                window.parent.postMessage({
                    type: MESSAGES_TO_PARENT.PROJECT_CHANGED
                }, '*');
            });

            // 监听全屏变化
            document.addEventListener('fullscreenchange', () => {
                window.parent.postMessage({
                    type: MESSAGES_TO_PARENT.FULLSCREEN,
                    fullscreen: !!document.fullscreenElement
                }, '*');
            });
        });

        // 处理来自父页面的消息
        window.addEventListener('message', (event) => {
            const { type, data } = event.data || {};
            if (!type) return;

            switch (type) {
                case 'export-project':
                    // 导出项目为 base64 编码的 sb3
                    waitForVM((vm) => {
                        vm.saveProjectSB3().then((sb3) => {
                            const reader = new FileReader();
                            reader.onload = () => {
                                const base64 = reader.result.split(',')[1];
                                window.parent.postMessage({
                                    type: MESSAGES_TO_PARENT.PROJECT_SAVE,
                                    data: base64
                                }, '*');
                            };
                            reader.readAsDataURL(sb3);
                        }).catch((err) => {
                            window.parent.postMessage({
                                type: MESSAGES_TO_PARENT.ERROR,
                                error: err.message || '导出失败'
                            }, '*');
                        });
                    });
                    break;

                case 'enter-editor':
                    // 切换到编辑模式
                    if (window.ReduxStore) {
                        window.ReduxStore.dispatch({ type: 'scratch-gui/mode/SET_PLAYER' });
                        window.parent.postMessage({ type: MESSAGES_TO_PARENT.EDITOR_MODE }, '*');
                    }
                    break;

                case 'enter-player':
                    // 切换到播放器模式
                    if (window.ReduxStore) {
                        window.ReduxStore.dispatch({ type: 'scratch-gui/mode/SET_PLAYER_ONLY' });
                        window.parent.postMessage({ type: MESSAGES_TO_PARENT.PLAYER_MODE }, '*');
                    }
                    break;

                case 'load-project':
                    // 加载项目
                    if (data && data.url && vmInstance) {
                        fetch(data.url)
                            .then(r => r.arrayBuffer())
                            .then(buffer => {
                                const sb3 = new File([buffer], 'project.sb3', { type: 'application/zip' });
                                return vmInstance.loadProject(sb3);
                            })
                            .then(() => {
                                window.parent.postMessage({
                                    type: 'project-loaded',
                                    success: true
                                }, '*');
                            })
                            .catch((err) => {
                                window.parent.postMessage({
                                    type: MESSAGES_TO_PARENT.ERROR,
                                    error: '加载项目失败: ' + err.message
                                }, '*');
                            });
                    }
                    break;

                case 'set-fullscreen':
                    // 设置全屏
                    if (data && data.fullscreen) {
                        document.documentElement.requestFullscreen?.();
                    } else {
                        document.exitFullscreen?.();
                    }
                    break;

                case 'get-vm-state':
                    // 获取 VM 状态
                    window.parent.postMessage({
                        type: 'vm-state',
                        running: vmInstance?.running || false
                    }, '*');
                    break;

                default:
                    console.log('[TW-API] 未知消息类型:', type);
            }
        });

        console.log('[TW-API] TurboWarp API 初始化完成');
    </script>
</body>
</html>
HTMLEOF

cat > "$BUILD_DIR/player-iframe.html" << 'HTMLEOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Scratch 播放器</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { width: 100%; height: 100%; overflow: hidden; }
    </style>
</head>
<body>
    <div id="app"></div>
    <script src="./static/js/player.js"></script>
    <script src="./static/js/bundle.js"></script>
    <script src="./static/js/l10n.js"></script>
    <script src="./static/js/vendors~player.js"></script>
    <script>
        // 播放器模式下的 postMessage 通信
        
        const MESSAGES_TO_PARENT = {
            VM_READY: 'vm-initialized',
            PROJECT_LOADED: 'project-loaded',
            ERROR: 'error'
        };

        let vmInstance = null;

        const waitForVM = (callback, maxAttempts = 50) => {
            let attempts = 0;
            const check = () => {
                if (window.vm && window.vm.initialized) {
                    vmInstance = window.vm;
                    callback(window.vm);
                } else if (attempts < maxAttempts) {
                    attempts++;
                    setTimeout(check, 100);
                } else {
                    console.error('[TW-Player] VM 初始化超时');
                }
            };
            check();
        };

        waitForVM((vm) => {
            console.log('[TW-Player] VM 初始化完成');
            window.parent.postMessage({
                type: MESSAGES_TO_PARENT.VM_READY,
                vmId: 'player-vm-' + Date.now()
            }, '*');

            vm.on('workspaceUpdate', () => {
                window.parent.postMessage({
                    type: 'project-changed'
                }, '*');
            });
        });

        window.addEventListener('message', (event) => {
            const { type, data } = event.data || {};
            if (!type) return;

            if (type === 'load-project' && data && data.url && vmInstance) {
                fetch(data.url)
                    .then(r => r.arrayBuffer())
                    .then(buffer => {
                        const sb3 = new File([buffer], 'project.sb3', { type: 'application/zip' });
                        return vmInstance.loadProject(sb3);
                    })
                    .then(() => {
                        window.parent.postMessage({
                            type: MESSAGES_TO_PARENT.PROJECT_LOADED,
                            success: true
                        }, '*');
                    })
                    .catch((err) => {
                        window.parent.postMessage({
                            type: MESSAGES_TO_PARENT.ERROR,
                            error: '加载项目失败: ' + err.message
                        }, '*');
                    });
            }
        });

        console.log('[TW-Player] TurboWarp Player API 初始化完成');
    </script>
</body>
</html>
HTMLEOF

echo ""
echo "=========================================="
echo "  构建完成！"
echo "=========================================="
echo ""
echo "产物位置: $BUILD_DIR"
echo ""
echo "包含的文件:"
ls -la "$BUILD_DIR"
echo ""
echo "下一步:"
echo "  1. 确保 nginx.conf 中已添加 turbowarp 静态文件路由"
echo "  2. 运行前端项目测试集成"
