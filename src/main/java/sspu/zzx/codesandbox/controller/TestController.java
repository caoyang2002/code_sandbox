package sspu.zzx.codesandbox.controller;

import sspu.zzx.codesandbox.service.java.JavaNativeCodeSandbox;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import sspu.zzx.codesandbox.model.ExecuteMessage;
import sspu.zzx.codesandbox.old.JavaDockerCodeSandbox;
import sspu.zzx.codesandbox.service.python3.Python3Native3CodeSandbox;
import sspu.zzx.codesandbox.utils.ProcessUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * 测试着玩的
 */
@Deprecated
@RestController("/")
public class TestController
{

    /**
     * 定义鉴权请求头
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    /**
     * 定义鉴权请求头中的密钥
     */
    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;
    @Resource
    private Python3Native3CodeSandbox python3NativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck()
    {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response)
    {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader))
        {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null)
        {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @PostMapping("/executeCode2")
    ExecuteCodeResponse executeCode2()
    {
        String cmd = "python3 --version";
        // String cmd = "docker run -v /Users/liuxinyu/Desktop/code:/code -it python:3.
        Process compileProcess = null;
        try
        {
            compileProcess = Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        System.out.println(executeMessage);
        System.out.println(executeMessage.getMessage());
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList(("1 2")));
        executeCodeRequest.setLanguage("java");
//        executeCodeRequest.setCode("public class Main {\n" +
//                "    public static void main(String[] args) {\n" +
//                "        int a = Integer.parseInt(args[0]);\n" +
//                "        int b = Integer.parseInt(args[1]);\n" +
//                "        System.out.println(\"结果:\" + (a + b));\n" +
//                "    }\n" +
//                "}");
        executeCodeRequest.setCode("import java.io.IOException;\n" + "import java.net.Socket;\n" + "\n" + "/**\n" + " * 测试安全管理器\n" + " */\n" + "public class Main {\n" + "\n" + "    public static void main(String[] args) {\n" + "        String host = \"example.com\"; // 替换为你想连接的主机\n" + "        int port = 80; // 替换为你想连接的端口\n" + "\n" + "        try {\n" + "            // 尝试连接到指定主机和端口\n" + "            Socket socket = new Socket(host, port);\n" + "\n" + "            // 如果连接成功，打印连接成功的消息\n" + "            System.out.println(\"Connection to \" + host + \":\" + port + \" successful.\");\n" + "\n" + "            // 关闭连接\n" + "            socket.close();\n" + "        }catch (IOException e) {\n" + "            // 捕获IOException，即连接失败异常\n" + "            System.err.println(\"IOException: \" + e.getMessage());\n" + "        }\n" + "    }\n" + "}");
        // 测试java原生实现
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
        // 测试Docker
        // return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }

    @PostMapping("/executeCode3")
    ExecuteCodeResponse executeCode3()
    {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList(("1 2")));
        executeCodeRequest.setLanguage("python");
        // executeCodeRequest.setCode("for i in [1,2,3]:\tprint(i)");
        executeCodeRequest.setCode("# calculate.py\n" + "\n" + "import sys\n" + "\n" + "def main():\n" + "    if len(sys.argv) != 3:\n" + "        print(\"Usage: python calculate.py <a> <b>\")\n" + "        return\n" + "\n" + "    a = float(sys.argv[1])\n" + "    b = float(sys.argv[2])\n" + "\n" + "    result = calculate_sum(a, b)\n" + "\n" + "    print(f\"The sum of {a} and {b} is: {result}\")\n" + "\n" + "def calculate_sum(a, b):\n" + "    return a + b\n" + "\n" + "if __name__ == \"__main__\":\n" + "    main()\n");
        System.out.println(executeCodeRequest.getCode());
        return python3NativeCodeSandbox.executeCode(executeCodeRequest);
//        String userDir = System.getProperty("user.dir");
//        System.out.println(userDir);
//        String globalCodePathName = userDir + File.separator + "code.py";
//        System.out.println(globalCodePathName);
//        FileUtil.writeString("for i in [1,2,3]:\tprint(i)", globalCodePathName, "utf-8");
//        String cmd = "python3 " + globalCodePathName;
//        // String cmd = "docker run -v /Users/liuxinyu/Desktop/code:/code -it python:3.
//        Process compileProcess = null;
//        try
//        {
//            compileProcess = Runtime.getRuntime().exec(cmd);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "运行Python");
//        Integer exitValue = executeMessage.getExitValue();
//        String message = executeMessage.getMessage();
//        String errorMessage = executeMessage.getErrorMessage();
//        Long time = executeMessage.getTime();
//        Long memory = executeMessage.getMemory();
//        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
//        executeCodeResponse.setMessage(message);
//        System.out.println(executeMessage);
    }
}
