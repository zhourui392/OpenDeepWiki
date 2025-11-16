package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * CLI执行器 - 负责执行命令行工具
 *
 * <p>职责: 纯粹的CLI调用封装，不包含任何业务逻辑</p>
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
public class CLIExecutor {

    @Value("${ai.timeout:60000}")
    private long timeoutMs;

    /**
     * 执行CLI命令
     *
     * @param command 命令数组（如：["claude", "-p", "prompt"]）
     * @return CLI输出结果
     * @throws CLIExecutionException CLI执行异常
     */
    public String execute(String[] command) throws CLIExecutionException {
        log.debug("执行CLI命令: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);  // 合并标准错误到标准输出

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待完成（带超时）
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new CLIExecutionException("CLI执行超时（" + timeoutMs + "ms）");
            }

            int exitCode = process.exitValue();  // Java 8兼容
            String result = output.toString().trim();

            if (exitCode != 0) {
                throw new CLIExecutionException(
                    String.format("CLI执行失败，退出码: %d, 输出: %s", exitCode, result)
                );
            }

            log.debug("CLI执行成功，输出长度: {}", result.length());
            return result;

        } catch (IOException e) {
            throw new CLIExecutionException("CLI执行IO错误: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CLIExecutionException("CLI执行被中断", e);
        }
    }

    /**
     * 检查CLI工具是否可用
     *
     * @param cliCommand CLI命令名（如: "claude", "codex"）
     * @return true-可用, false-不可用
     */
    public boolean isAvailable(String cliCommand) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cliCommand, "--version");
            Process process = pb.start();

            // 等待最多2秒
            boolean finished = process.waitFor(2000, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;  // Java 8兼容

        } catch (Exception e) {
            log.debug("检查CLI工具失败: {} - {}", cliCommand, e.getMessage());
            return false;
        }
    }

    /**
     * CLI执行异常
     */
    public static class CLIExecutionException extends RuntimeException {
        public CLIExecutionException(String message) {
            super(message);
        }

        public CLIExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
