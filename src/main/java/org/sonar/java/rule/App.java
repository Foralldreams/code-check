package org.sonar.java.rule;

import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.rule.checks.namerules.*;
import org.sonar.plugins.java.api.JavaFileScanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 程序入口函数
 * @author zx
 */

public class App {

    static String PATH = "";
    static String RULES = "AbstractClassNameCheck;LowerCameCaseCheck;ConstantNameCheck;UpperCameCaseCheck;APIBasedCheck;";
    static HashMap<String, List<String>> api = new HashMap<>();
    static HashMap<String, List<String>> placeOfUsed = new HashMap<>();

    public static void main(String[] args) {
        FileInputStream fis = null;
        File projecHome = null;

        try {
            Properties props = new Properties();
            File propertyFile = new File("config.properties");

            if (propertyFile.exists()) {
                fis = new FileInputStream(new File("config.properties"));
                props.load(fis);
                RULES = props.getProperty("java.project.rules");
            }

            String[] Rules_Array = RULES.split(";");
            projecHome = new File(PATH);
            String absolutePath = projecHome.getAbsolutePath();
            absolutePath.replaceAll("\\\\", "/");
            projecHome = new File(absolutePath);

            scan(projecHome, Rules_Array);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param path  文件路径
     * @param rules 检查规则
     * @author zx
     */
    public static void scan(File path, String[] rules) {

        JavaFileScanner fileScanner;
        for (int i = 0; i < rules.length; i++) {
            String rule = rules[i];
            System.out.println();
            System.out.println("==========================" + rule + "==========================");

            switch (rule) {
                case "AbstractClassNameCheck":
                    fileScanner = new AbstractClassNameCheck();
                    break;
                case "LowerCameCaseCheck":
                    fileScanner = new LowerCameCaseCheck();
                    break;
                case "ConstantNameCheck":
                    fileScanner = new ConstantNameCheck();
                    break;
                case "UpperCameCaseCheck":
                    fileScanner = new UpperCameCaseCheck();
                    break;
                case "APIBasedCheck":
                    fileScanner = new APIBasedCheck(api, placeOfUsed);
                    break;
                default:
                    fileScanner = null;
            }

            if (fileScanner instanceof APIBasedCheck) {
                scanBasedAPI(path, fileScanner);
                continue;
            }

            if (fileScanner != null) {
                scan(path, fileScanner);
            }

        }
    }

    /**
     * 该方法主要针对基于api的代码检查
     * @param path  文件路径
     * @param check 具体检查规则
     * @author zx
     */
    public static void scanBasedAPI(File path, JavaFileScanner check) {
        OnlyAPICollector onlyAPICollector = new OnlyAPICollector(api);
        scan(path, onlyAPICollector);

        scan(path, check);
        Set<Map.Entry<String, List<String>>> entries = placeOfUsed.entrySet();

        for (Map.Entry<String, List<String>> entry : entries) {
            String key = entry.getKey();
            List<String> value = entry.getValue();

            if (value.size() >= 2) {
                System.out.println(key + "    is used in    " + value);
            }

        }
    }

    /**
     * 该方法主要功能是对单个代码文件进行扫描检查
     *
     * @param path  文件路径
     * @param check 具体检查规则
     * @author zx
     */
    public static void scan(File path, JavaFileScanner check) {
        if (!path.isDirectory()) {
            try {
                JavaCheckVerifier.verify(path.getAbsolutePath(), check);
            } catch (IllegalStateException e) {

            } catch (AssertionError e) {

            }
        } else {

            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    scan(file, check);
                } else {
                    if (file.getName().toLowerCase().endsWith(".java")) {
                        try {
                            JavaCheckVerifier.verify(file.getAbsolutePath(), check);
                        } catch (IllegalStateException e) {

                        } catch (AssertionError e) {

                        }

                    }
                }
            }
        }
    }

}

