package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    @Value("${ai.timeout:600000}")
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
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 并发读取输出，避免缓冲区满导致死锁
            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("读取CLI输出异常: {}", e.getMessage());
                }
            });
            readerThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                readerThread.interrupt();
                throw new CLIExecutionException("CLI执行超时（" + timeoutMs + "ms）");
            }

            // 等待输出读取完成
            readerThread.join(5000);

            int exitCode = process.exitValue();
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
     * 执行CLI命令并通过标准输入传递数据
     *
     * @param command 命令数组
     * @param input 标准输入内容
     * @return CLI输出结果
     * @throws CLIExecutionException CLI执行异常
     */
    public String executeWithInput(String[] command, String input) throws CLIExecutionException {
        log.debug("执行CLI命令(带输入): {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 写入标准输入
            try (OutputStream writer = process.getOutputStream()) {
                writer.write(input.getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }

            // 并发读取输出
            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("读取CLI输出异常: {}", e.getMessage());
                }
            });
            readerThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                readerThread.interrupt();
                throw new CLIExecutionException("CLI执行超时（" + timeoutMs + "ms）");
            }

            // 等待输出读取完成
            readerThread.join(5000);

            int exitCode = process.exitValue();
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
