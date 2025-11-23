package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Java代码分析器
 * 使用JavaParser解析Java源代码
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
public class JavaCodeAnalyzer {

    private final JavaParser javaParser;

    public JavaCodeAnalyzer() {
        this.javaParser = new JavaParser();
    }

    /**
     * 解析Java文件
     *
     * @param javaFile Java源文件
     * @return ClassInfo对象，解析失败返回null
     */
    public ClassInfo analyzeFile(File javaFile) {
        try (FileInputStream in = new FileInputStream(javaFile)) {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(in, StandardCharsets.UTF_8);

            if (!parseResult.isSuccessful()) {
                log.warn("解析Java文件失败: {}", javaFile.getPath());
                return null;
            }

            Optional<CompilationUnit> cuOpt = parseResult.getResult();
            if (!cuOpt.isPresent()) {
                return null;
            }

            CompilationUnit cu = cuOpt.get();
            return extractClassInfo(cu, javaFile);

        } catch (Exception e) {
            log.error("分析Java文件异常: {}", javaFile.getPath(), e);
            return null;
        }
    }

    /**
     * 从CompilationUnit提取类信息
     */
    private ClassInfo extractClassInfo(CompilationUnit cu, File javaFile) {
        // 获取第一个类或接口声明
        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (!classOpt.isPresent()) {
            return null;
        }

        ClassOrInterfaceDeclaration classDecl = classOpt.get();
        ClassInfo classInfo = new ClassInfo();

        // 基本信息
        classInfo.setClassName(classDecl.getNameAsString());
        classInfo.setFilePath(javaFile.getAbsolutePath());
        classInfo.setInterface(classDecl.isInterface());
        classInfo.setAbstract(classDecl.isAbstract());

        // 包名
        cu.getPackageDeclaration().ifPresent(pkg ->
                classInfo.setPackageName(pkg.getNameAsString())
        );

        // 全限定类名
        String fullClassName = classInfo.getPackageName() != null
                ? classInfo.getPackageName() + "." + classInfo.getClassName()
                : classInfo.getClassName();
        classInfo.setFullClassName(fullClassName);

        // 父类
        classDecl.getExtendedTypes().stream().findFirst().ifPresent(type ->
                classInfo.setSuperClass(type.getNameAsString())
        );

        // 实现的接口
        classDecl.getImplementedTypes().forEach(type ->
                classInfo.getInterfaces().add(type.getNameAsString())
        );

        // 类注解
        classDecl.getAnnotations().forEach(ann ->
                classInfo.addAnnotation(extractAnnotation(ann))
        );

        // 字段
        classDecl.getFields().forEach(field ->
                classInfo.addField(extractFieldInfo(field))
        );

        // 方法
        classDecl.getMethods().forEach(method ->
                classInfo.addMethod(extractMethodInfo(method))
        );

        log.debug("解析类: {} (字段数: {}, 方法数: {})",
            fullClassName, classInfo.getFields().size(), classInfo.getMethods().size());
        return classInfo;
    }

    /**
     * 提取方法信息
     */
    private MethodInfo extractMethodInfo(MethodDeclaration method) {
        MethodInfo methodInfo = new MethodInfo();

        // 方法名
        methodInfo.setName(method.getNameAsString());

        // 返回类型
        methodInfo.setReturnType(method.getTypeAsString());

        // 参数
        method.getParameters().forEach(param ->
                methodInfo.getParameters().add(param.getTypeAsString() + " " + param.getNameAsString())
        );

        // 访问修饰符
        methodInfo.setPublic(method.hasModifier(Modifier.Keyword.PUBLIC));
        methodInfo.setStatic(method.hasModifier(Modifier.Keyword.STATIC));

        // 方法签名
        String signature = method.getNameAsString() + "(" +
                String.join(", ", methodInfo.getParameters()) + ")";
        methodInfo.setSignature(signature);

        // 方法注解
        method.getAnnotations().forEach(ann ->
                methodInfo.addAnnotation(extractAnnotation(ann))
        );

        // 提取方法调用
        method.findAll(MethodCallExpr.class).forEach(call -> {
            String calledMethod = call.getScope()
                    .map(scope -> scope.toString() + "." + call.getNameAsString())
                    .orElse(call.getNameAsString());
            methodInfo.addCalledMethod(calledMethod);
        });

        return methodInfo;
    }

    /**
     * 提取字段信息
     */
    private FieldInfo extractFieldInfo(FieldDeclaration field) {
        FieldInfo fieldInfo = new FieldInfo();

        // 字段可能声明多个变量，取第一个
        VariableDeclarator variable = field.getVariable(0);
        fieldInfo.setName(variable.getNameAsString());
        fieldInfo.setType(variable.getTypeAsString());

        // 字段注解
        field.getAnnotations().forEach(ann ->
                fieldInfo.addAnnotation(extractAnnotation(ann))
        );

        return fieldInfo;
    }

    /**
     * 提取注解信息
     */
    private AnnotationInfo extractAnnotation(AnnotationExpr annotation) {
        AnnotationInfo annotationInfo = new AnnotationInfo();
        annotationInfo.setName(annotation.getNameAsString());

        // 提取注解属性
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().forEach(pair ->
                    annotationInfo.addAttribute(
                            pair.getNameAsString(),
                            pair.getValue().toString()
                    )
            );
        } else if (annotation.isSingleMemberAnnotationExpr()) {
            // 单值注解 (如 @Value("xxx"))
            annotationInfo.addAttribute(
                    "value",
                    annotation.asSingleMemberAnnotationExpr().getMemberValue().toString()
            );
        }

        return annotationInfo;
    }
}
