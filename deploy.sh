#!/bin/bash

###############################################################################
# OpenDeepWiki Linux 部署脚本
#
# 功能：编译、打包、停止旧进程、启动新进程
# 使用：./deploy.sh [start|stop|restart|build]
#
# @author zhourui(V33215020)
# @since 2025/01/21
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量
APP_NAME="koalawiki"
JAR_NAME="koalawiki-web-0.1.0-SNAPSHOT.jar"
JAR_PATH="koalawiki-web/target/${JAR_NAME}"
PID_FILE="/var/run/${APP_NAME}.pid"
LOG_DIR="logs"
LOG_FILE="${LOG_DIR}/koalawiki.log"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

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

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        error "$1 未安装，请先安装: sudo apt install $1"
        exit 1
    fi
}

# 检查环境
check_environment() {
    info "检查运行环境..."
    check_command java
    check_command mvn

    # 检查 Java 版本
    # 支持 1.8.x 和 11+ 两种版本号格式
    JAVA_VERSION_STRING=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')

    # 判断是 1.x 还是新版本号格式
    if [[ $JAVA_VERSION_STRING =~ ^1\. ]]; then
        # 1.8.x 格式，提取 1.8 中的 8
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $2}')
    else
        # 11+ 格式，提取主版本号
        JAVA_VERSION=$(echo $JAVA_VERSION_STRING | awk -F '.' '{print $1}')
    fi

    if [ "$JAVA_VERSION" -lt 8 ]; then
        error "Java 版本过低，需要 JDK 1.8+，当前版本: $JAVA_VERSION_STRING"
        exit 1
    fi

    info "Java 版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 创建必要的目录
create_directories() {
    info "创建必要的目录..."
    mkdir -p ${LOG_DIR}
    mkdir -p ./data/git
    info "目录创建完成"
}

# 编译打包
build() {
    info "开始编译打包..."

    # 清理旧的编译产物
    info "清理旧的编译产物..."
    mvn clean -q

    # 编译打包（跳过测试）
    info "执行 Maven 打包（跳过测试）..."
    mvn package -DskipTests -q

    if [ ! -f "${JAR_PATH}" ]; then
        error "打包失败，未找到 JAR 文件: ${JAR_PATH}"
        exit 1
    fi

    info "编译打包完成: ${JAR_PATH}"
    info "JAR 文件大小: $(du -h ${JAR_PATH} | cut -f1)"
}

# 获取应用进程ID
get_pid() {
    # 方式1：从 PID 文件读取
    if [ -f "${PID_FILE}" ]; then
        PID=$(cat ${PID_FILE})
        # 验证进程是否存在
        if ps -p ${PID} > /dev/null 2>&1; then
            echo ${PID}
            return
        fi
    fi

    # 方式2：通过 JAR 名称查找
    PID=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}')
    echo ${PID}
}

# 停止应用
stop() {
    info "停止应用..."

    PID=$(get_pid)

    if [ -z "${PID}" ]; then
        warn "应用未运行"
        return
    fi

    info "找到进程 PID: ${PID}"

    # 优雅停止（发送 TERM 信号）
    info "发送停止信号..."
    kill -15 ${PID}

    # 等待进程退出（最多30秒）
    WAIT_TIME=0
    MAX_WAIT=30
    while ps -p ${PID} > /dev/null 2>&1; do
        if [ ${WAIT_TIME} -ge ${MAX_WAIT} ]; then
            warn "优雅停止超时，强制终止进程"
            kill -9 ${PID}
            break
        fi
        echo -n "."
        sleep 1
        WAIT_TIME=$((WAIT_TIME + 1))
    done
    echo ""

    # 清理 PID 文件
    if [ -f "${PID_FILE}" ]; then
        rm -f ${PID_FILE}
    fi

    info "应用已停止"
}

# 启动应用
start() {
    info "启动应用..."

    # 检查是否已经运行
    PID=$(get_pid)
    if [ -n "${PID}" ]; then
        error "应用已在运行，PID: ${PID}"
        exit 1
    fi

    # 检查 JAR 文件是否存在
    if [ ! -f "${JAR_PATH}" ]; then
        error "JAR 文件不存在: ${JAR_PATH}"
        error "请先执行编译: ./deploy.sh build"
        exit 1
    fi

    # 启动应用（后台运行）
    info "启动 Spring Boot 应用..."
    info "配置文件: ${SPRING_PROFILES_ACTIVE}"
    info "日志文件: ${LOG_FILE}"

    nohup java -jar \
        -Xms512m \
        -Xmx1024m \
        -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} \
        -Dfile.encoding=UTF-8 \
        ${JAR_PATH} \
        > ${LOG_FILE} 2>&1 &

    NEW_PID=$!
    echo ${NEW_PID} > ${PID_FILE}

    # 等待启动
    info "等待应用启动..."
    sleep 3

    # 验证进程是否存在
    if ps -p ${NEW_PID} > /dev/null 2>&1; then
        info "应用启动成功，PID: ${NEW_PID}"
        info "查看日志: tail -f ${LOG_FILE}"
        info "健康检查: curl http://localhost:18081/actuator/health"
    else
        error "应用启动失败，请查看日志: ${LOG_FILE}"
        exit 1
    fi
}

# 重启应用
restart() {
    info "重启应用..."
    stop
    sleep 2
    start
}

# 查看状态
status() {
    PID=$(get_pid)

    if [ -z "${PID}" ]; then
        warn "应用未运行"
        exit 1
    fi

    info "应用正在运行"
    info "PID: ${PID}"
    info "内存使用: $(ps -p ${PID} -o rss= | awk '{printf "%.2f MB\n", $1/1024}')"
    info "运行时间: $(ps -p ${PID} -o etime= | xargs)"
    info "日志文件: ${LOG_FILE}"
}

# 查看日志
logs() {
    if [ ! -f "${LOG_FILE}" ]; then
        warn "日志文件不存在: ${LOG_FILE}"
        exit 1
    fi

    info "查看最近100行日志..."
    tail -100 ${LOG_FILE}
}

# 实时查看日志
tail_logs() {
    if [ ! -f "${LOG_FILE}" ]; then
        warn "日志文件不存在: ${LOG_FILE}"
        exit 1
    fi

    info "实时查看日志（Ctrl+C 退出）..."
    tail -f ${LOG_FILE}
}

# 显示帮助信息
usage() {
    cat << EOF
OpenDeepWiki Linux 部署脚本

使用方法:
    $0 <command>

命令:
    build       编译打包项目
    start       启动应用
    stop        停止应用
    restart     重启应用
    status      查看应用状态
    logs        查看最近日志
    tail        实时查看日志
    deploy      完整部署（编译+重启）
    help        显示帮助信息

示例:
    $0 build           # 仅编译打包
    $0 start           # 启动应用
    $0 restart         # 重启应用
    $0 deploy          # 完整部署流程
    $0 tail            # 实时查看日志

环境变量:
    SPRING_PROFILES_ACTIVE    Spring 配置文件（默认: prod）

    示例: SPRING_PROFILES_ACTIVE=dev $0 start

日志文件: ${LOG_FILE}
PID文件: ${PID_FILE}

EOF
}

# 完整部署流程
deploy() {
    info "=========================================="
    info "开始完整部署流程"
    info "=========================================="

    check_environment
    create_directories
    build

    # 检查是否有旧进程
    PID=$(get_pid)
    if [ -n "${PID}" ]; then
        stop
        sleep 2
    fi

    start

    info "=========================================="
    info "部署完成！"
    info "=========================================="
    status
}

# 主函数
main() {
    # 检查是否有参数
    if [ $# -eq 0 ]; then
        usage
        exit 1
    fi

    # 解析命令
    case "$1" in
        build)
            check_environment
            build
            ;;
        start)
            check_environment
            create_directories
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        status)
            status
            ;;
        logs)
            logs
            ;;
        tail)
            tail_logs
            ;;
        deploy)
            deploy
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            error "未知命令: $1"
            usage
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
