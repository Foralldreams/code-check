package org.sonar.java.rule.checks.namerules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

/**
 * 方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase 风格，必须遵从 驼峰形式
 * @author zx
 */
@Rule(key = "LowerCameCaseCheck")
public class LowerCameCaseCheck extends BaseTreeVisitor implements JavaFileScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowerCameCaseCheck.class);

    private JavaFileScannerContext context;
    private String className;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        className = tree.simpleName().name();
//        System.out.println(className);
        super.visitClass(tree);
    }

    /**
     * 方法内检查驼峰命名
     * 构造函数除外
     *
     * @param tree
     */
    @Override
    public void visitMethod(MethodTree tree) {

        //判断是否为构造函数，构造函数返回类型为null
        if (tree.symbol().returnType() != null) {

            String methodName = tree.simpleName().name();
            LOGGER.info("构造函数:" + methodName + ">>>>" + tree.symbol().returnType());
            //判断方法名称首字符是否小写
            char methodChar = methodName.charAt(0);

            if (Character.isUpperCase(methodChar)) {
                LOGGER.info(">>判断方法名称首字符是否小写>>" + methodName);
                System.out.println("Warnning:  Class<" + className + ">#Method<" + methodName + ">" + "Parameter Name Not Standard: The first character of local Parameters should " +
                        "not be " +
                        "UpperCase");
                context.reportIssue(this, tree, "The first Character Of Method Name should not UpperCase");
            }

            if ("CLASS".equals(tree.parent().kind().name())) {
                //判断方法内的局部变量是否符合驼峰规则
                for (StatementTree statementTree : tree.block().body()) {

                    //判断值类型是否为VariableTree
                    if (statementTree instanceof VariableTree) {
                        VariableTree sTree = (VariableTree) statementTree;
                        if (sTree.simpleName() != null) {
                            char sName = sTree.simpleName().name().charAt(0);
                            if (Character.isUpperCase(sName)) {
                                LOGGER.info(">>判断方法内的局部变量是否符合驼峰规则>>" + sTree.simpleName().name());
                                System.out.println("Warnning:  Class<" + className + ">#Method<" + methodName + ">" + "Parameter Name Not Standard: The first character of local Parameters should " +
                                        "not be " +
                                        "UpperCase");

                                context.reportIssue(this, tree, "The first character of local Parameters should not be UpperCase");
                            }
                        }
                    }
                }
            }
        }

        super.visitMethod(tree);
    }

    /**
     * 判断变量命名是否符合规范(方法参数变量和成员变量)
     *
     * @param tree
     */
    @Override
    public void visitVariable(VariableTree tree) {

        String variableName = tree.simpleName().name();
        char cVariableName = variableName.charAt(0);

        if (!tree.symbol().isFinal()) {
            if (Character.isUpperCase(cVariableName)) {

                System.out.println("Warnning:  Class<" + className + ">#Parameter<" + cVariableName + ">" + "Parameter Name Not Standard: The first character of local Parameters should " +
                        "not be " +
                        "UpperCase");
                LOGGER.info(">>判断变量命名是否符合规范>>" + tree.simpleName().name());
                context.reportIssue(this, tree, "The first character of Member Variable should not be UpperCase");
            }
        }
        super.visitVariable(tree);
    }
}
