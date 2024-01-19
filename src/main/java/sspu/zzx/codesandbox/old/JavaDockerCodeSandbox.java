package sspu.zzx.codesandbox.old;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import sspu.zzx.codesandbox.JavaCodeSandboxTemplate;
import sspu.zzx.codesandbox.model.ExecuteCodeRequest;
import sspu.zzx.codesandbox.model.ExecuteCodeResponse;
import sspu.zzx.codesandbox.model.ExecuteMessage;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate
{

    /**
     * 代码允许允许的最大时间
     * 15秒
     */
    private static final long TIME_OUT = 15 * 1000L;

    /**
     * 镜像全名
     */
    private static final String IMAGE_FULL_NAME = "openjdk:8-alpine";

    /**
     * 容器名称
     */
    private static final String CONTAINER_NAME = "java-code-sandbox-container";


    public static void main(String[] args)
    {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    /**
     * 3、创建容器，把文件复制到容器内
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList)
    {
        try ( // 获取默认的 Docker Client
              DockerClient dockerClient = DockerClientBuilder.getInstance().build();
              )
        {

            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

            // 拉取镜像，并确保镜像一定存在
            makeSureDockerImage(IMAGE_FULL_NAME, dockerClient);

            // 获取容器id，并确保容器一定存在
            String containerId = getContainerId(CONTAINER_NAME, dockerClient, userCodeParentPath);

            // docker exec keen_blackwell java -cp /app Main 1 3
            // 执行命令并获取结果
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (String inputArgs : inputList)
            {
                StopWatch stopWatch = new StopWatch();
                String[] inputArgsArray = inputArgs.split(" ");
                String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId).withCmd(cmdArray).withAttachStderr(true).withAttachStdin(true).withAttachStdout(true).exec();
                System.out.println("创建执行命令：" + execCreateCmdResponse);

                ExecuteMessage executeMessage = new ExecuteMessage();

                final long[] maxMemory = {-2L};

                // 获取占用的内存
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>()
                {

                    @Override
                    public void onNext(Statistics statistics)
                    {
                        System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                        maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                    }

                    @Override
                    public void close()
                    {
                    }

                    @Override
                    public void onStart(Closeable closeable)
                    {
                    }

                    @Override
                    public void onError(Throwable throwable)
                    {
                    }

                    @Override
                    public void onComplete()
                    {
                    }
                });
                statsCmd.exec(statisticsResultCallback);
                statsCmd.start();

                final String[] message = {null};
                final String[] errorMessage = {null};
                long time = 0L;
                // 判断是否超时
                // todo 利用超时标志
                final boolean[] timeout = {true};
                String execId = execCreateCmdResponse.getId();
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback()
                {
                    @Override
                    public void onComplete()
                    {
                        // 如果执行完成，则表示没超时
                        timeout[0] = false;
                        super.onComplete();
                    }

                    @Override
                    public void onNext(Frame frame)
                    {
                        StreamType streamType = frame.getStreamType();
                        System.out.println(new String(frame.getPayload()));
                        System.out.println("内存占用-zzx：" + maxMemory[0]);
                        if (StreamType.STDERR.equals(streamType))
                        {
                            if (errorMessage[0] == null)
                            {
                                errorMessage[0] = new String(frame.getPayload());
                            }
                            else
                            {
                                errorMessage[0] += new String(frame.getPayload());
                            }
                            System.out.println("输出错误结果：" + errorMessage[0]);
                        }
                        else
                        {
                            if (message[0] == null)
                            {
                                message[0] = new String(frame.getPayload());
                            }
                            else
                            {
                                message[0] += new String(frame.getPayload());
                            }
                            System.out.println("输出结果：" + message[0]);
                        }
                        super.onNext(frame);
                    }
                };
                try
                {
                    stopWatch.start();
                    dockerClient.execStartCmd(execId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                    stopWatch.stop();
                    time = stopWatch.getLastTaskTimeMillis();
                    statsCmd.close();
                }
                catch (InterruptedException e)
                {
                    System.out.println("程序执行异常");
                    throw new RuntimeException(e);
                }
                while (maxMemory[0] == -2L || message[0] == null)
                {
                    // 等待结果
                }
                // 获取代码运行结果
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMessage[0]);
                executeMessage.setTime(time);
                executeMessage.setMemory(maxMemory[0]);
                executeMessageList.add(executeMessage);
            }
            System.out.println("最终结果集：" + executeMessageList);
            return executeMessageList;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 确保代码沙箱镜像一定存在
     *
     * @param imageFullName
     * @param dockerClient
     */
    public void makeSureDockerImage(String imageFullName, DockerClient dockerClient)
    {
        if (checkImage(imageFullName, dockerClient))
        {
            return;
        }
        else
        {
            downloadImage(imageFullName, dockerClient);
        }
    }

    /**
     * 检查镜像是否存在
     *
     * @param imageFullName
     * @param dockerClient
     * @return
     */
    public Boolean checkImage(String imageFullName, DockerClient dockerClient)
    {
        // 获取本地所有镜像
        List<Image> images = dockerClient.listImagesCmd().exec();

        // 检查指定镜像是否存在
        boolean imageExists = images.stream().anyMatch(image -> image.getRepoTags() != null && Arrays.asList(image.getRepoTags()).contains(imageFullName));

        if (imageExists)
        {
            System.out.println(imageFullName + "镜像存在！");
            return true;
        }
        else
        {
            System.out.println(imageFullName + "镜像不存在！");
            return false;
        }
    }

    /**
     * 下载镜像文件
     *
     * @param imageFullName
     * @param dockerClient
     */
    public void downloadImage(String imageFullName, DockerClient dockerClient)
    {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageFullName);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback()
        {
            @Override
            public void onNext(PullResponseItem item)
            {
                System.out.println("下载" + imageFullName + "镜像：" + item.getStatus());
                super.onNext(item);
            }
        };
        try
        {
            pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            System.out.println(imageFullName + "镜像下载完成");
        }
        catch (InterruptedException e)
        {
            System.out.println("拉取" + imageFullName + "镜像异常");
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取容器id，并确保其一定存在
     *
     * @param containerName
     * @param dockerClient
     * @param userCodeParentPath
     * @return
     */
    public String getContainerId(String containerName, DockerClient dockerClient, String userCodeParentPath)
    {
        // 列出所有容器
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withShowAll(true);
        List<Container> containers = listContainersCmd.exec();

        // 查找指定名称的容器
        Container targetContainer = containers.stream().filter(container -> container.getNames() != null && Arrays.asList(container.getNames()).contains("/" + containerName)).findFirst().orElse(null);

        if (targetContainer != null)
        {
            String containerId = targetContainer.getId();
            // 获取容器的详细信息
            com.github.dockerjava.api.command.InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();

            // 这里的 inspectContainerResponse 包含了容器的详细信息
            System.out.println("查找到" + containerName + "容器，containerId = " + inspectContainerResponse.getId());
            Boolean running = inspectContainerResponse.getState().getRunning();
            System.out.println("容器状态：" + (running ? "运行中" : "已停止"));
            if (!running)
            {
                // 启动容器
                dockerClient.startContainerCmd(containerId).exec();
                System.out.println(containerName + "容器启动成功");
            }
            return inspectContainerResponse.getId();
        }
        else
        {
            System.out.println("未找到" + containerName + "容器！");
            // 创建容器
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_FULL_NAME).withName(containerName);
            System.out.println(containerName + "容器创建成功");
            HostConfig hostConfig = new HostConfig();
            hostConfig.withMemory(100 * 1000 * 1000L);
            hostConfig.withMemorySwap(0L);
            hostConfig.withCpuCount(1L);
            hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
            CreateContainerResponse createContainerResponse = containerCmd.withHostConfig(hostConfig).withNetworkDisabled(true).withReadonlyRootfs(true).withAttachStdin(true).withAttachStderr(true).withAttachStdout(true).withTty(true).exec();
            System.out.println("创建后的" + containerName + "容器信息：" + createContainerResponse);
            String containerId = createContainerResponse.getId();
            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println(containerName + "容器启动成功");
            return containerId;
        }
    }


}



