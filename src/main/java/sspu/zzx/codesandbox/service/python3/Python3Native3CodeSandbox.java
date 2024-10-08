package sspu.zzx.codesandbox.service.python3;

import org.springframework.stereotype.Component;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;

/**
 * Python3 原生代码沙箱实现（直接复用模板方法）
 *
 * @author zzx
 */
@Component
public class Python3Native3CodeSandbox extends Python3CodeSandboxTemplate
{

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        return super.executeCode(executeCodeRequest);
    }
}
