#!/bin/bash

###############################################################################
# OpenDeepWiki 前后端同机一键部署脚本
#
# 功能：自动完成前端构建 + 后端编译 + 启动服务
# 使用：./quick-deploy.sh
#
# @author zhourui(V33215020)
# @since 2025/01/21
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带颜色的信息
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 打印标题
print_banner() {
    echo ""
    echo "=========================================="
    echo "  OpenDeepWiki 前后端同机一键部署"
    echo "=========================================="
    echo ""
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        error "$1 未安装"
        echo "请先安装: $2"
        exit 1
    fi
}

# 检查环境
check_environment() {
    step "1/6 检查运行环境..."

    check_command java "sudo apt install openjdk-8-jdk"
    check_command mvn "sudo apt install maven"
    check_command node "sudo apt install nodejs"
    check_command npm "sudo apt install npm"

    # 检查版本
    # Java 版本提取：支持 1.8.x 和 11+ 两种格式
    JAVA_VERSION_STRING=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

    # 判断是 1.x 还是新版本号格式
    if [[ $JAVA_VERSION_STRING =~ ^1\. ]]; then
        # 1.8.x 格式，提取 1.8 中的 8
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $2}')
    else
        # 11+ 格式，提取主版本号
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $1}')
    fi

    NODE_VERSION=$(node -v | sed 's/v//' | awk -F '.' '{print $1}')

    if [ "$JAVA_VERSION" -lt 8 ]; then
        error "Java 版本过低，需要 JDK 1.8+，当前: $JAVA_VERSION_STRING"
        exit 1
    fi

    # Vite 5.x 要求 Node.js 18+
    if [ "$NODE_VERSION" -lt 18 ]; then
        error "Node.js 版本过低，Vite 要求 Node.js 18+，当前: $(node -v)"
        echo ""
        echo "升级方法："
        echo "  # 使用 nvm (推荐)"
        echo "  curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash"
        echo "  source ~/.bashrc"
        echo "  nvm install 18"
        echo "  nvm use 18"
        echo ""
        echo "  # 或使用 NodeSource 仓库"
        echo "  curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -"
        echo "  sudo apt-get install -y nodejs"
        exit 1
    fi

    info "✓ Java 版本: $(java -version 2>&1 | head -n 1)"
    info "✓ Maven 版本: $(mvn -version | head -n 1)"
    info "✓ Node.js 版本: $(node -v)"
    info "✓ NPM 版本: $(npm -v)"
    echo ""
}

# 配置前端
configure_frontend() {
    step "2/6 配置前端 API 地址..."

    cd koalawiki-web-vue

    # 创建生产环境配置
    if [ ! -f ".env.production.local" ]; then
        cat > .env.production.local << 'EOF'
# 前后端同机部署 - 使用相对路径
VITE_API_BASE_URL=/api
EOF
        info "✓ 创建前端配置文件: .env.production.local"
    else
        info "✓ 前端配置文件已存在"
    fi

    # 显示配置
    echo ""
    info "前端配置内容："
    cat .env.production.local | sed 's/^/  /'
    echo ""

    cd ..
}

# 安装前端依赖
install_frontend_deps() {
    step "3/6 安装前端依赖..."

    cd koalawiki-web-vue

    if [ ! -d "node_modules" ]; then
        info "首次安装，可能需要几分钟..."
        npm install
        info "✓ 前端依赖安装完成"
    else
        info "✓ 前端依赖已安装（如需重新安装，请删除 node_modules 目录）"
    fi

    cd ..
    echo ""
}

# 构建前端
build_frontend() {
    step "4/6 构建前端..."

    cd koalawiki-web-vue

    info "正在构建 Vue 前端..."
    npm run build

    # 验证构建结果
    if [ -f "../koalawiki-web/src/main/resources/static/index.html" ]; then
        info "✓ 前端构建成功"
        info "✓ 静态文件已复制到: koalawiki-web/src/main/resources/static/"
    else
        error "前端构建失败，未找到 index.html"
        exit 1
    fi

    cd ..
    echo ""
}

# 编译打包后端
build_backend() {
    step "5/6 编译打包后端..."

    info "正在执行 Maven 打包..."
    mvn clean package -DskipTests -q

    JAR_FILE="koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar"

    if [ -f "$JAR_FILE" ]; then
        info "✓ 后端编译成功"
        info "✓ JAR 文件: $JAR_FILE"
        info "✓ 文件大小: $(du -h $JAR_FILE | cut -f1)"
    else
        error "后端编译失败"
        exit 1
    fi

    echo ""
}

# 启动应用
start_application() {
    step "6/6 启动应用..."

    # 检查是否已经在运行
    if [ -f "./deploy.sh" ]; then
        # 使用部署脚本启动
        ./deploy.sh stop 2>/dev/null || true
        sleep 2
        ./deploy.sh start
    else
        warn "未找到 deploy.sh，使用直接启动方式"

        # 检查是否有进程在运行
        PID=$(ps aux | grep "koalawiki-web" | grep -v grep | awk '{print $2}')
        if [ -n "$PID" ]; then
            info "发现已运行的进程 (PID: $PID)，正在停止..."
            kill -15 $PID
            sleep 2
        fi

        # 创建日志目录
        mkdir -p logs

        # 启动应用
        info "后台启动应用..."
        nohup java -jar \
            -Xms512m \
            -Xmx1024m \
            koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar \
            > logs/koalawiki.log 2>&1 &

        NEW_PID=$!
        echo $NEW_PID > koalawiki.pid

        sleep 3

        if ps -p $NEW_PID > /dev/null 2>&1; then
            info "✓ 应用启动成功，PID: $NEW_PID"
        else
            error "应用启动失败，请查看日志: logs/koalawiki.log"
            exit 1
        fi
    fi

    echo ""
}

# 验证部署
verify_deployment() {
    info "验证部署..."
    echo ""

    # 等待应用完全启动
    info "等待应用完全启动（最多 30 秒）..."

    MAX_WAIT=30
    WAIT_TIME=0

    while [ $WAIT_TIME -lt $MAX_WAIT ]; do
        if curl -s http://localhost:18081/actuator/health > /dev/null 2>&1; then
            info "✓ 应用健康检查通过"
            break
        fi
        echo -n "."
        sleep 1
        WAIT_TIME=$((WAIT_TIME + 1))
    done
    echo ""

    if [ $WAIT_TIME -ge $MAX_WAIT ]; then
        warn "健康检查超时，但应用可能仍在启动中"
        warn "请稍后访问或查看日志: logs/koalawiki.log"
    fi

    echo ""
}

# 打印部署信息
print_deployment_info() {
    echo "=========================================="
    echo "  部署完成！"
    echo "=========================================="
    echo ""
    echo "访问地址:"
    echo "  本地:  http://localhost:18081"
    echo "  局域网: http://$(hostname -I | awk '{print $1}'):18081"
    echo ""
    echo "管理命令:"
    echo "  查看状态: ./deploy.sh status"
    echo "  查看日志: ./deploy.sh tail"
    echo "  停止应用: ./deploy.sh stop"
    echo "  重启应用: ./deploy.sh restart"
    echo ""
    echo "健康检查:"
    echo "  curl http://localhost:18081/actuator/health"
    echo ""
    echo "日志文件:"
    echo "  logs/koalawiki.log"
    echo ""
    echo "=========================================="
}

# 主函数
main() {
    # 检查是否在项目根目录
    if [ ! -f "pom.xml" ]; then
        error "请在项目根目录下执行此脚本"
        exit 1
    fi

    print_banner

    # 执行部署步骤
    check_environment
    configure_frontend
    install_frontend_deps
    build_frontend
    build_backend
    start_application
    verify_deployment
    print_deployment_info
}

# 执行主函数
main
