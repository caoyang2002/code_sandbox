package sspu.zzx.codesandbox;


import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 *
 * @author zzx
 */
public interface CodeSandbox
{

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
