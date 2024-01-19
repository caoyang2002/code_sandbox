package sspu.zzx.codesandbox.controller;

import sspu.zzx.codesandbox.JavaDockerCodeSandbox;
import sspu.zzx.codesandbox.JavaNativeCodeSandbox;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;

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
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response)
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
        executeCodeRequest.setCode("import java.io.IOException;\n" +
                "import java.net.Socket;\n" +
                "\n" +
                "/**\n" +
                " * 测试安全管理器\n" +
                " */\n" +
                "public class Main {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        String host = \"example.com\"; // 替换为你想连接的主机\n" +
                "        int port = 80; // 替换为你想连接的端口\n" +
                "\n" +
                "        try {\n" +
                "            // 尝试连接到指定主机和端口\n" +
                "            Socket socket = new Socket(host, port);\n" +
                "\n" +
                "            // 如果连接成功，打印连接成功的消息\n" +
                "            System.out.println(\"Connection to \" + host + \":\" + port + \" successful.\");\n" +
                "\n" +
                "            // 关闭连接\n" +
                "            socket.close();\n" +
                "        }catch (IOException e) {\n" +
                "            // 捕获IOException，即连接失败异常\n" +
                "            System.err.println(\"IOException: \" + e.getMessage());\n" +
                "        }\n" +
                "    }\n" +
                "}");
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @PostMapping("/executeCode3")
    ExecuteCodeResponse executeCode3()
    {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList(("1 2")));
        executeCodeRequest.setLanguage("java");
        executeCodeRequest.setCode("public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        int a = Integer.parseInt(args[0]);\n" +
                "        int b = Integer.parseInt(args[1]);\n" +
                "        System.out.println(\"结果:\" + (a + b));\n" +
                "    }\n" +
                "}");
        return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }
}
