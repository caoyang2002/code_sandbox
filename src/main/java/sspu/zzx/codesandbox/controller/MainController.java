package sspu.zzx.codesandbox.controller;

import org.springframework.web.bind.annotation.*;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;
import sspu.zzx.codesandbox.model.enums.QuestionSubmitStatusEnum;
import sspu.zzx.codesandbox.model.enums.SupportLanguageEnum;
import sspu.zzx.codesandbox.service.java.JavaNativeCodeSandbox;
import sspu.zzx.codesandbox.service.python3.Python3Native3CodeSandbox;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @version 1.0
 * @Author ZZX
 * @Date 2024/1/19 8:17
 */
@RestController
@RequestMapping("/codesandbox")
public class MainController
{

    /**
     * 定义鉴权请求头
     */
    private static final String AUTH_REQUEST_HEADER = "sspuoj-codesandbox-auth-by-zzx";

    /**
     * 定义鉴权请求头中的密钥
     */
    private static final String AUTH_REQUEST_SECRET = "$W$~vrZwe7z&L!ht^U%fF2zZzHTjWSwY%@ZeEJ^*(qZ()D3npx";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private Python3Native3CodeSandbox python3NativeCodeSandbox;

    /**
     * 检测系统健康状态
     *
     * @return
     */
    @GetMapping("/healthCheck")
    public String healthCheck()
    {
        return "I'm fine";
    }

    /**
     * 系统自我介绍
     *
     * @return
     */
    @GetMapping("/intro")
    public String selfIntroduction()
    {
        return "您好，我是由【ZZX】开发的代码沙箱，可完成代码的运行和结果返回。\n" +
                "目前我支持的语言有"+SupportLanguageEnum.getValues()+"\n"+
                "如果对您有帮助，欢迎来我主页点个关注~\n" +
                "作者Gitee："+"https://gitee.com/crzzx"+
                "\n作者B站主页："+"https://space.bilibili.com/1198127286?spm_id_from=333.788.0.0"+"\n"+
                "PS：本项目原始代码源于【程序员鱼皮】，再次特别鸣谢~";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/run")
    public ExecuteCodeResponse runCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response)
    {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader))
        {
            System.out.println(AUTH_REQUEST_SECRET.length());
            response.setStatus(403);
            return new ExecuteCodeResponse(null, "身份校验失败！", QuestionSubmitStatusEnum.FAILED.getValue(), null);
        }
        if (executeCodeRequest == null)
        {
            throw new RuntimeException("请求参数为空");
        }
        String language = executeCodeRequest.getLanguage();
        if (SupportLanguageEnum.JAVA.getValue().equals(language))
        {
            return javaNativeCodeSandbox.executeCode(executeCodeRequest);
        }
        else if (SupportLanguageEnum.PYTHON3.getValue().equals(language))
        {
            return python3NativeCodeSandbox.executeCode(executeCodeRequest);
        }
        else
        {
            return new ExecuteCodeResponse(null, "不支持的编程语言：" + language + "；当前仅支持：" + SupportLanguageEnum.getValues(), QuestionSubmitStatusEnum.FAILED.getValue(), null);
        }
    }

}