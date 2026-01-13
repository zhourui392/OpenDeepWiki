package ai.opendw.koalawiki.core.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
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

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

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
        return execute(command, null);
    }

    /**
     * 在指定工作目录下执行CLI命令
     *
     * @param command 命令数组
     * @param workingDirectory 工作目录，null表示使用默认目录
     * @return CLI输出结果
     * @throws CLIExecutionException CLI执行异常
     */
    public String execute(String[] command, String workingDirectory) throws CLIExecutionException {
        String[] actualCommand = wrapCommandForPlatform(command);
        log.debug("执行CLI命令: {}, 工作目录: {}", String.join(" ", actualCommand), workingDirectory);

        try {
            ProcessBuilder pb = new ProcessBuilder(actualCommand);
            pb.redirectErrorStream(true);

            if (workingDirectory != null) {
                File workDir = new File(workingDirectory);
                if (!workDir.exists() || !workDir.isDirectory()) {
                    throw new CLIExecutionException("工作目录不存在或不是目录: " + workingDirectory);
                }
                pb.directory(workDir);
                log.debug("设置CLI工作目录: {}", workingDirectory);
            }

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
                    log.debug("读取CLI输出中断: {}", e.getMessage());
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                closeProcessStreams(process);
                readerThread.interrupt();

                readerThread.join(2000);
                if (readerThread.isAlive()) {
                    log.warn("读取线程未能在2秒内结束，已设置为守护线程自动清理");
                }
                throw new CLIExecutionException("CLI执行超时（" + timeoutMs + "ms）");
            }

            // 等待输出读取完成
            readerThread.join(5000);
            if (readerThread.isAlive()) {
                log.warn("输出读取线程未在5秒内完成，强制中断");
                closeProcessStreams(process);
                readerThread.interrupt();
            }

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
        return executeWithInput(command, input, null);
    }

    /**
     * 在指定工作目录下执行CLI命令并通过标准输入传递数据
     *
     * @param command 命令数组
     * @param input 标准输入内容
     * @param workingDirectory 工作目录，null表示使用默认目录
     * @return CLI输出结果
     * @throws CLIExecutionException CLI执行异常
     */
    public String executeWithInput(String[] command, String input, String workingDirectory) throws CLIExecutionException {
        String[] actualCommand = wrapCommandForPlatform(command);
        log.debug("执行CLI命令(带输入): {}, 工作目录: {}", String.join(" ", actualCommand), workingDirectory);

        try {
            ProcessBuilder pb = new ProcessBuilder(actualCommand);
            pb.redirectErrorStream(true);

            if (workingDirectory != null) {
                File workDir = new File(workingDirectory);
                if (!workDir.exists() || !workDir.isDirectory()) {
                    throw new CLIExecutionException("工作目录不存在或不是目录: " + workingDirectory);
                }
                pb.directory(workDir);
                log.debug("设置CLI工作目录: {}", workingDirectory);
            }

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
                    log.debug("读取CLI输出中断: {}", e.getMessage());
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                closeProcessStreams(process);
                readerThread.interrupt();

                readerThread.join(2000);
                if (readerThread.isAlive()) {
                    log.warn("读取线程未能在2秒内结束，已设置为守护线程自动清理");
                }
                throw new CLIExecutionException("CLI执行超时（" + timeoutMs + "ms）");
            }

            // 等待输出读取完成
            readerThread.join(5000);
            if (readerThread.isAlive()) {
                log.warn("输出读取线程未在5秒内完成，强制中断");
                closeProcessStreams(process);
                readerThread.interrupt();
            }

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
     * @param cliCommand CLI命令名（如: "claude"）
     * @return true-可用, false-不可用
     */
    public boolean isAvailable(String cliCommand) {
        try {
            String[] command = wrapCommandForPlatform(new String[]{cliCommand, "--version"});
            log.info("检查CLI可用性: {}", String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5000, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("CLI检查超时: {}", cliCommand);
                return false;
            }

            int exitCode = process.exitValue();
            log.info("CLI检查结果: {}, exitCode={}, output={}", cliCommand, exitCode, output.toString().trim());
            return exitCode == 0;

        } catch (Exception e) {
            log.warn("检查CLI工具失败: {} - {}", cliCommand, e.getMessage());
            return false;
        }
    }

    /**
     * 关闭进程的所有流，确保读取线程能正常退出
     */
    private void closeProcessStreams(Process process) {
        try {
            process.getInputStream().close();
        } catch (Exception e) {
            log.debug("关闭进程输入流失败: {}", e.getMessage());
        }
        try {
            process.getOutputStream().close();
        } catch (Exception e) {
            log.debug("关闭进程输出流失败: {}", e.getMessage());
        }
        try {
            process.getErrorStream().close();
        } catch (Exception e) {
            log.debug("关闭进程错误流失败: {}", e.getMessage());
        }
    }

    /**
     * 根据操作系统包装命令
     * Windows需要通过cmd.exe执行命令
     */
    private String[] wrapCommandForPlatform(String[] command) {
        if (!IS_WINDOWS || command.length == 0) {
            return command;
        }
        String[] wrapped = new String[command.length + 2];
        wrapped[0] = "cmd.exe";
        wrapped[1] = "/c";
        System.arraycopy(command, 0, wrapped, 2, command.length);
        return wrapped;
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
