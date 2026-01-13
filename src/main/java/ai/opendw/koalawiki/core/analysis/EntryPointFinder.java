package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 入口点搜索器
 * 根据关键词智能搜索业务入口点
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-23
 */
@Slf4j
@Component
public class EntryPointFinder {

    /**
     * 相关度阈值（低于此分数的结果将被过滤）
     */
    private static final int RELEVANCE_THRESHOLD = 15;

    /**
     * 根据关键词搜索入口点
     *
     * @param keywords   关键词列表
     * @param structures 项目结构列表
     * @return 匹配的入口点列表
     */
    public List<EntryPointMatch> findByKeywords(
            List<String> keywords,
            List<ProjectStructure> structures) {

        log.info("开始搜索入口点，关键词：{}，项目数量：{}", keywords, structures.size());

        List<EntryPointMatch> allMatches = new ArrayList<>();

        // 遍历所有项目结构
        for (ProjectStructure structure : structures) {
            // 遍历所有入口点
            for (EntryPoint entryPoint : structure.getEntryPoints()) {
                // 查找对应的类信息
                ClassInfo classInfo = structure.getClasses().get(entryPoint.getClassName());
                if (classInfo == null) {
                    continue;
                }

                // 查找对应的方法信息
                MethodInfo methodInfo = findMethodInfo(classInfo, entryPoint.getMethodName());

                // 尝试匹配
                EntryPointMatch match = tryMatch(entryPoint, classInfo, methodInfo, keywords, structure.getProjectName());
                if (match != null) {
                    allMatches.add(match);
                }
            }
        }

        // 过滤低分结果并排序
        List<EntryPointMatch> filteredMatches = allMatches.stream()
                .filter(match -> match.getRelevanceScore() >= RELEVANCE_THRESHOLD)
                .sorted(Comparator.comparingInt(EntryPointMatch::getRelevanceScore).reversed())
                .collect(Collectors.toList());

        log.info("搜索完成，找到 {} 个匹配的入口点", filteredMatches.size());

        return filteredMatches;
    }

    /**
     * 查找方法信息
     */
    private MethodInfo findMethodInfo(ClassInfo classInfo, String methodName) {
        if (classInfo == null || methodName == null) {
            return null;
        }

        return classInfo.getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 尝试匹配方法
     */
    private EntryPointMatch tryMatch(
            EntryPoint entryPoint,
            ClassInfo classInfo,
            MethodInfo methodInfo,
            List<String> keywords,
            String projectName) {

        // 计算相关度分数
        ScoreResult scoreResult = calculateRelevanceScore(entryPoint, classInfo, methodInfo, keywords);

        if (scoreResult.getTotalScore() < RELEVANCE_THRESHOLD) {
            return null;
        }

        // 构建匹配结果
        EntryPointMatch match = new EntryPointMatch();
        match.setEntryPoint(entryPoint);
        match.setRelevanceScore(scoreResult.getTotalScore());
        match.setMatchReasons(scoreResult.getReasons());
        match.setProjectName(projectName);

        return match;
    }

    /**
     * 计算相关度分数
     */
    private ScoreResult calculateRelevanceScore(
            EntryPoint entryPoint,
            ClassInfo classInfo,
            MethodInfo methodInfo,
            List<String> keywords) {

        int totalScore = 0;
        List<String> reasons = new ArrayList<>();

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();

            // 1. 类名匹配 +10分
            if (classInfo != null) {
                String className = classInfo.getClassName();
                String fullClassName = classInfo.getFullClassName();
                if (className != null && className.toLowerCase().contains(lowerKeyword)) {
                    totalScore += 10;
                    reasons.add(String.format("类名包含'%s'(+10)", keyword));
                } else if (fullClassName != null && fullClassName.toLowerCase().contains(lowerKeyword)) {
                    totalScore += 10;
                    reasons.add(String.format("类名包含'%s'(+10)", keyword));
                }
            }

            // 2. 方法名匹配 +20分
            if (entryPoint.getMethodName() != null &&
                    entryPoint.getMethodName().toLowerCase().contains(lowerKeyword)) {
                totalScore += 20;
                reasons.add(String.format("方法名包含'%s'(+20)", keyword));
            }

            // 3. API路径匹配 +15分
            if (entryPoint.getPath() != null &&
                    entryPoint.getPath().toLowerCase().contains(lowerKeyword)) {
                totalScore += 15;
                reasons.add(String.format("API路径包含'%s'(+15)", keyword));
            }

            // 4. 描述匹配 +5分
            if (entryPoint.getDescription() != null &&
                    entryPoint.getDescription().contains(keyword)) {
                totalScore += 5;
                reasons.add(String.format("描述包含'%s'(+5)", keyword));
            }

            // 5. 注解值匹配 +8分
            if (hasAnnotationValueContaining(entryPoint, lowerKeyword)) {
                totalScore += 8;
                reasons.add(String.format("注解值包含'%s'(+8)", keyword));
            }

            // 6. 方法注解匹配（如果有方法信息）+8分
            if (methodInfo != null && hasMethodAnnotationContaining(methodInfo, lowerKeyword)) {
                totalScore += 8;
                reasons.add(String.format("方法注解包含'%s'(+8)", keyword));
            }
        }

        return new ScoreResult(totalScore, reasons);
    }

    /**
     * 检查入口点注解值是否包含关键词
     */
    private boolean hasAnnotationValueContaining(EntryPoint entryPoint, String keyword) {
        Map<String, Object> annotations = entryPoint.getAnnotations();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        for (Object value : annotations.values()) {
            if (value != null && value.toString().toLowerCase().contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查方法注解是否包含关键词
     */
    private boolean hasMethodAnnotationContaining(MethodInfo methodInfo, String keyword) {
        List<AnnotationInfo> annotations = methodInfo.getAnnotations();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        for (AnnotationInfo annotation : annotations) {
            if (annotation.getName() != null &&
                    annotation.getName().toLowerCase().contains(keyword)) {
                return true;
            }
            if (annotation.getAttributes() != null) {
                for (Object value : annotation.getAttributes().values()) {
                    if (value != null && value.toString().toLowerCase().contains(keyword)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 评分结果内部类
     */
    @Data
    @AllArgsConstructor
    private static class ScoreResult {
        private final int totalScore;
        private final List<String> reasons;
    }
}
