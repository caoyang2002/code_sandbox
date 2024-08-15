# code_sandbox
【瞧！多语言代码沙箱的设计与实现~-哔哩哔哩】 https://b23.tv/QmNRuYV

# depoly
./
|- `dockerfile` (in resource)
L `xx.jar` (in target)
 
# Terminal
```bash
# create image
docker build -t sspuoj:codesandbox .

# list iamges
docker image list

# create container
docker run -p 8090:8090 -d --name sspuoj-codesandbox-01 sspuoj-codesandbox

# list container
docker container list

# show logs
docker logs -f sspuoj-codesandbox-01
```
# document

swagger:
ip:8090
