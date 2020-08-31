package org.sonar.java.rule.checks.namerules;

import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 收集所检查代码中被@only注解的方法，将该方法添加到map中供后续检查使用
 * @author zx
 */

public class OnlyAPICollector extends BaseTreeVisitor implements JavaFileScanner {
    private JavaFileScannerContext context;
    private String className;

    private HashMap<String, List<String>> API;

    public OnlyAPICollector() {
        super();
    }

    public OnlyAPICollector(HashMap<String, List<String>> api) {
        super();
        this.API = api;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        this.className = tree.simpleName().name();
        super.visitClass(tree);
    }

    /***
     * 该方法主要用来将标注为@only的注解进行扫描并收集
     * @param tree
     */
    @Override
    public void visitMethod(MethodTree tree) {

        if (tree.symbol().returnType() != null) {
            String methodName = tree.simpleName().name();
            ModifiersTree modifiers = tree.modifiers();
            for (int i = 0; i < modifiers.size(); i++) {
                ModifierTree modifierTree = modifiers.get(i);
                if(modifierTree instanceof AnnotationTreeImpl){
                    AnnotationTreeImpl annotationTree = (AnnotationTreeImpl)modifierTree;
                    String annotationName = annotationTree.annotationType().toString();
                    if(annotationName.equals("Only")){
                        List<String> methodOnly = API.getOrDefault(className, new ArrayList<String>());
                        methodOnly.add(methodName);
                        API.put(className,methodOnly);
                    }
                }
            }
        }
        super.visitMethod(tree);
    }

}
