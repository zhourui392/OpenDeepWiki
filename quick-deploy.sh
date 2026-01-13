#!/bin/bash

###############################################################################
# KoalaWiki 一键部署脚本
#
# 功能：自动完成编译 + 启动服务
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
    echo "  KoalaWiki 一键部署"
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
    step "1/3 检查运行环境..."

    check_command java "sudo apt install openjdk-8-jdk"
    check_command mvn "sudo apt install maven"

    JAVA_VERSION_STRING=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

    if [[ $JAVA_VERSION_STRING =~ ^1\. ]]; then
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $2}')
    else
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $1}')
    fi

    if [ "$JAVA_VERSION" -lt 8 ]; then
        error "Java 版本过低，需要 JDK 1.8+，当前: $JAVA_VERSION_STRING"
        exit 1
    fi

    info "✓ Java 版本: $(java -version 2>&1 | head -n 1)"
    info "✓ Maven 版本: $(mvn -version | head -n 1)"
    echo ""
}

# 编译打包后端
build_backend() {
    step "2/3 编译打包..."

    info "正在执行 Maven 打包..."
    mvn clean package -DskipTests -q

    JAR_FILE="target/koalawiki-0.1.0-SNAPSHOT.jar"

    if [ -f "$JAR_FILE" ]; then
        info "✓ 编译成功"
        info "✓ JAR 文件: $JAR_FILE"
        info "✓ 文件大小: $(du -h $JAR_FILE | cut -f1)"
    else
        error "编译失败"
        exit 1
    fi

    echo ""
}

# 启动应用
start_application() {
    step "3/3 启动应用..."

    if [ -f "./deploy.sh" ]; then
        ./deploy.sh stop 2>/dev/null || true
        sleep 2
        ./deploy.sh start
    else
        warn "未找到 deploy.sh，使用直接启动方式"

        PID=$(ps aux | grep "koalawiki" | grep -v grep | awk '{print $2}')
        if [ -n "$PID" ]; then
            info "发现已运行的进程 (PID: $PID)，正在停止..."
            kill -15 $PID
            sleep 2
        fi

        mkdir -p logs

        info "后台启动应用..."
        nohup java -jar \
            -Xms512m \
            -Xmx1024m \
            target/koalawiki-0.1.0-SNAPSHOT.jar \
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
    if [ ! -f "pom.xml" ]; then
        error "请在项目根目录下执行此脚本"
        exit 1
    fi

    print_banner
    check_environment
    build_backend
    start_application
    verify_deployment
    print_deployment_info
}

# 执行主函数
main
