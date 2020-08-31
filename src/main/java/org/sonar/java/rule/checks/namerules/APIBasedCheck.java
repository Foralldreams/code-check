package org.sonar.java.rule.checks.namerules;

import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 基于注解的检查，基于OnlyAPICollector收集出来的被注解@Only注解的方法，扫描每一个代码文件，将匹配的对应类和方法信息存储到另一个map中进行返回。
 * 注：该如果检查到的方法被@Ignore注解，则该在该方法对其他api的所有调用都不会记录
 *
 * @author zx
 */


public class APIBasedCheck extends BaseTreeVisitor implements JavaFileScanner {
    private JavaFileScannerContext context;
    private String className;
    private HashMap<String, List<String>> onlyApi;
    private HashMap<String, List<String>> placeOfUsed;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    public APIBasedCheck() {
        super();
    }

    public APIBasedCheck(HashMap<String, List<String>> onlyApi, HashMap<String, List<String>> placeOfUsed) {
        super();
        this.onlyApi = onlyApi;
        this.placeOfUsed = placeOfUsed;
    }

    @Override
    public void visitClass(ClassTree tree) {
        className = tree.simpleName().name();
        super.visitClass(tree);
    }

    /**
     * 该方法对每个代码文件中被使用的到的其他类对象进行扫描
     *
     * @param tree
     */
    @Override
    public void visitMethod(MethodTree tree) {
        HashMap<String, String> paramAndType = new HashMap<>();
        if (tree.symbol().returnType() != null) {
            String methodName = tree.simpleName().name();
            ModifiersTree modifiers = tree.modifiers();
            for (int i = 0; i < modifiers.size(); i++) {
                ModifierTree modifierTree = modifiers.get(i);
                if (modifierTree instanceof AnnotationTreeImpl) {
                    AnnotationTreeImpl annotationTree = (AnnotationTreeImpl) modifierTree;
                    String annotationName = annotationTree.annotationType().toString();
                    if (annotationName.equals("Ignore")) {
                        super.visitMethod(tree);
                        return;
                    }
                }
            }

            if (tree.block() == null) {
                return;
            }

            List<StatementTree> body = tree.block().body();
            if (!body.isEmpty()) {
                for (int i = 0; i < body.size(); i++) {
                    StatementTree statementTree = body.get(i);
                    if (statementTree instanceof VariableTree) {
                        VariableTree variableTree = (VariableTree) statementTree;

                        //获取方法中的局部变量及其类型存入到map中
                        String varName = variableTree.simpleName().toString();
                        String varType = variableTree.type().toString();
                        paramAndType.put(varName, varType);

                        //检查参数是否使用了标记为Only的方法
                        ExpressionTree initializer = variableTree.initializer();
                        boolean flag1 = initializer != null;
                        boolean flag2 = (initializer instanceof MethodInvocationTreeImpl);

                        if (initializer != null && initializer instanceof MethodInvocationTree) {
                            MethodInvocationTree methodInvocationTree = (MethodInvocationTree) initializer;
                            ExpressionTree methodSelect = methodInvocationTree.methodSelect();

                            if (methodSelect != null) {
                                MemberSelectExpressionTreeImpl methodSelectimpl = (MemberSelectExpressionTreeImpl) methodSelect;
                                String paraName = methodSelectimpl.expression().toString();
                                String paraType = paramAndType.get(paraName);
                                String identifier = methodSelectimpl.identifier().toString();

                                List<String> list = onlyApi.get(paraType);
                                if (list != null) {
                                    if (list.contains(identifier)) {
                                        List<String> methodList = placeOfUsed.getOrDefault(paraType + "#" + identifier, new ArrayList<String>());
                                        methodList.add(className + "#" + methodName);
                                        placeOfUsed.put(paraType + "#" + identifier, methodList);
                                    }
                                }

                            }
                        }

                    }
                }
            }

        }
        super.visitMethod(tree);
    }
}
