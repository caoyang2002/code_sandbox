package sspu.zzx.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;
import sspu.zzx.codesandbox.model.ExecuteMessage;
import sspu.zzx.codesandbox.model.JudgeInfo;
import sspu.zzx.codesandbox.model.enums.JudgeInfoMessageEnum;
import sspu.zzx.codesandbox.model.enums.QuestionSubmitStatusEnum;
import sspu.zzx.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java 代码沙箱模板方法的实现
 *
 * @author zzx
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox
{

    /**
     * 待运行代码的文件夹路径名称
     */
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    /**
     * 待运行代码的存放文件名
     */
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    /**
     * 代码最大允许运行的时间
     */
    private static final long TIME_OUT = 15000L;

    private static final String SECURITY_MANAGER_PATH;

    private static final String SECURITY_MANAGER_CLASS_NAME = "NowSecurityManager";

    /**
     * 代码黑名单
     * 黑名单检测通常用于辅助安全策略，而不是作为唯一的安全手段
     */
    private static final List<String> blackList = Arrays.asList(
            // 文件操作相关
            "Files", "File", "FileInputStream", "FileOutputStream", "RandomAccessFile",
            "FileReader", "FileWriter", "FileChannel", "FileLock",
            "Path", "Paths", "File.createTempFile", "File.createTempDirectory",
            "ZipInputStream", "ZipOutputStream",

            // 网络相关
            "Socket", "ServerSocket", "DatagramSocket",
            "InetAddress", "URL", "URLConnection",
            "HttpURLConnection", "SocketChannel", "ServerSocketChannel", "DatagramChannel",
            "SocketPermission", "ServerSocketPermission",

            // 系统命令执行相关
            "exec", "Runtime.getRuntime().exec", "ProcessBuilder",
            "SecurityManager", "System.exit", "Runtime.getRuntime().halt",
            "SecurityManager.checkExec",

            // 反射相关
            "Class.forName", "Method.invoke", "sun.reflect.", "java.lang.reflect.",
            "Unsafe", "sun.misc.Unsafe", "sun.reflect.Unsafe",
            "Proxy",

            // 数据库相关
            "Statement", "PreparedStatement", "CallableStatement",
            "DataSource", "Connection", "ResultSet",
            "Hibernate", "JPA", // 防止使用 ORM 框架执行不安全的数据库操作
            "createStatement", "prepareStatement", "prepareCall",

            // 不安全的操作
            "Unsafe", "sun.misc.Unsafe", "sun.reflect.Unsafe",

            // 加密解密相关
            "Cipher", "MessageDigest", "KeyGenerator", "KeyPairGenerator",
            "SecretKeyFactory", "KeyStore", "SecureRandom",
            "java.security.",

            // 序列化相关
            "ObjectInputStream", "ObjectOutputStream",
            "Serializable", "Externalizable", "readObject", "writeObject",

            // 线程相关
            "Thread", "Runnable", "Executor", "ExecutorService", "ThreadPoolExecutor",
            "ThreadGroup", "ThreadLocal",
            "Thread.sleep", "Thread.yield", "Thread.stop", "Thread.suspend", "Thread.resume",
            "java.util.concurrent.",

            // 安全管理器相关
            "SecurityManager",

            // 其他可能导致安全问题的操作
            "System.load", "System.loadLibrary", // 防止加载本地库
            "JNI", "Java Native Interface", // 防止使用 JNI 调用本地代码
            "Unsafe.allocateMemory", "Unsafe.freeMemory", // 直接内存操作
            "System.getProperties", "System.setProperty", // 系统属性操作
            "System.getenv", // 获取环境变量
            "System.console", // 控制台访问
            "Runtime.addShutdownHook", // 添加关闭钩子
            "Runtime.load", "Runtime.loadLibrary" // 加载本地库
    );

    /**
     * 代码黑名单字典树
     */
    private static final WordTree WORD_TREE;

    static
    {
        // 初始化黑名单字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
        // 初始安全配置文件路径
        SECURITY_MANAGER_PATH = System.getProperty("user.dir");
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        // todo 后期可以根据编程语言选择不同的代码沙箱
        String language = executeCodeRequest.getLanguage();
        System.out.println("当前代码使用语言：" + language);

        // 0. 安全控制：限制敏感代码：黑名单检测
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null)
        {
            System.out.println("包含禁止词：" + foundWord.getFoundWord());
            // 返回错误信息
            return new ExecuteCodeResponse(null,
                    "包含禁止词：" + foundWord.getFoundWord(),
                    QuestionSubmitStatusEnum.FAILED.getValue(),
                    new JudgeInfo(JudgeInfoMessageEnum.DANGEROUS_OPERATION.getValue(), null, null));
        }

        // 1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        // 2. 编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println("编译结果：" + compileFileExecuteMessage);
        if (compileFileExecuteMessage.getErrorMessage() != null)
        {
            // 返回编译错误信息
            return new ExecuteCodeResponse(null,
                    compileFileExecuteMessage.getMessage(),
                    QuestionSubmitStatusEnum.FAILED.getValue(),
                    new JudgeInfo(compileFileExecuteMessage.getErrorMessage(), null, null));
        }

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

        // 4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b)
        {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }

        return outputResponse;
    }


    /**
     * 1. 把用户的代码保存为文件
     *
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code)
    {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName))
        {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        return userCodeFile;
    }

    /**
     * 2、编译代码
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile)
    {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try
        {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            // 编译失败
            if (executeMessage.getExitValue() != 0)
            {
                executeMessage.setExitValue(1);
                executeMessage.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getText());
                executeMessage.setErrorMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
            }
            return executeMessage;
        } catch (Exception e)
        {
            // 未知错误
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExitValue(1);
            executeMessage.setMessage(e.getMessage());
            executeMessage.setErrorMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
            return executeMessage;
        }
    }

    /**
     * 3、执行文件，获得执行结果列表
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList)
    {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList)
        {
            // 安全控制：限制资源分配：最大队资源大小：256MB
            // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try
            {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 安全控制：限制最大运行时间，超时控制
                new Thread(() ->
                {
                    try
                    {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超过程序最大运行时间，终止进程");
                        runProcess.destroy();
                    } catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println("本次运行结果：" + executeMessage);
                if (executeMessage.getExitValue() != 0)
                {
                    executeMessage.setExitValue(1);
                    executeMessage.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getText());
                    executeMessage.setErrorMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getValue());
                }
                executeMessageList.add(executeMessage);
            } catch (Exception e)
            {
                // 未知错误
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setExitValue(1);
                executeMessage.setMessage(e.getMessage());
                executeMessage.setErrorMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
                executeMessageList.add(executeMessage);
            }
        }
        return executeMessageList;
    }

    /**
     * 4、获取输出结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList)
    {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList)
        {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage))
            {
                executeCodeResponse.setMessage(executeMessage.getMessage());
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                executeCodeResponse.setJudgeInfo(new JudgeInfo(errorMessage, null, null));
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null)
            {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null)
            {
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size())
        {
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        // 运行正常完成则不设置message，交由判题机判题
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile)
    {
        if (userCodeFile.getParentFile() != null)
        {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e)
    {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
