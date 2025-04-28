# Sentinel 控制台

## 0. 概述

Sentinel 控制台是流量控制、熔断降级规则统一配置和管理的入口，它为用户提供了机器自发现、簇点链路自发现、监控、规则配置等功能。在 Sentinel 控制台上，我们可以配置规则并实时查看流量控制效果。

## 1. 编译和启动

### 1.1 如何编译

使用如下命令将代码打包成一个 fat jar:

```bash
mvn clean package
```

### 1.2 如何启动

使用如下命令启动编译后的控制台：

```bash
java -Dserver.port=8080 \
-Dcsp.sentinel.dashboard.server=localhost:8080 \
-Dproject.name=sentinel-dashboard \
-jar target/sentinel-dashboard.jar
```

上述命令中我们指定几个 JVM 参数，其中 `-Dserver.port=8080` 是 Spring Boot 的参数，
用于指定 Spring Boot 服务端启动端口为 `8080`。其余几个是 Sentinel 客户端的参数。

为便于演示，我们对控制台本身加入了流量控制功能，具体做法是引入 Sentinel 提供的 `CommonFilter` 这个 Servlet Filter。
上述 JVM 参数的含义是：

| 参数 | 作用 |
|--------|--------|
|`-Dcsp.sentinel.dashboard.server=localhost:8080`|向 Sentinel 接入端指定控制台的地址|
|`-Dproject.name=sentinel-dashboard`|向 Sentinel 指定应用名称，比如上面对应的应用名称就为 `sentinel-dashboard`|

全部的配置项可以参考 [启动配置项文档](https://github.com/alibaba/Sentinel/wiki/%E5%90%AF%E5%8A%A8%E9%85%8D%E7%BD%AE%E9%A1%B9)。

经过上述配置，控制台启动后会自动向自己发送心跳。程序启动后浏览器访问 `localhost:8080` 即可访问 Sentinel 控制台。

从 Sentinel 1.6.0 开始，Sentinel 控制台支持简单的**登录**功能，默认用户名和密码都是 `sentinel`。用户可以通过如下参数进行配置：

- `-Dsentinel.dashboard.auth.username=sentinel` 用于指定控制台的登录用户名为 `sentinel`；
- `-Dsentinel.dashboard.auth.password=123456` 用于指定控制台的登录密码为 `123456`；如果省略这两个参数，默认用户和密码均为 `sentinel`；
- `-Dserver.servlet.session.timeout=7200` 用于指定 Spring Boot 服务端 session 的过期时间，如 `7200` 表示 7200 秒；`60m` 表示 60 分钟，默认为 30 分钟；

## 2. 客户端接入

选择合适的方式接入 Sentinel，然后在应用启动时加入 JVM 参数 `-Dcsp.sentinel.dashboard.server=consoleIp:port` 指定控制台地址和端口。
确保客户端有访问量，**Sentinel 会在客户端首次调用的时候进行初始化，开始向控制台发送心跳包**，将客户端纳入到控制台的管辖之下。

客户端接入的详细步骤请参考 [Wiki 文档](https://github.com/alibaba/Sentinel/wiki/%E6%8E%A7%E5%88%B6%E5%8F%B0#3-%E5%AE%A2%E6%88%B7%E7%AB%AF%E6%8E%A5%E5%85%A5%E6%8E%A7%E5%88%B6%E5%8F%B0)。

## 3. 验证是否接入成功

客户端正确配置并启动后，会**在初次调用后**主动向控制台发送心跳包，汇报自己的存在；
控制台收到客户端心跳包之后，会在左侧导航栏中显示该客户端信息。如果控制台能够看到客户端的机器信息，则表明客户端接入成功了。

## 6. 构建Docker镜像

```bash
docker build --build-arg SENTINEL_VERSION=1.8.8 -t ${REGISTRY}/sentinel-dashboard:v1.8.8 .
```

*注意：Sentinel 控制台目前仅支持单机部署。Sentinel 控制台项目提供 Sentinel 功能全集示例，不作为开箱即用的生产环境控制台，不提供安全可靠保障。若希望在生产环境使用请根据[文档](https://github.com/alibaba/Sentinel/wiki/%E5%9C%A8%E7%94%9F%E4%BA%A7%E7%8E%AF%E5%A2%83%E4%B8%AD%E4%BD%BF%E7%94%A8-Sentinel)自行进行定制和改造。*

更多：[控制台功能介绍](./Sentinel_Dashboard_Feature.md)。


# sentinels-with-nacos
sentiment 保存规则到nacos 

增加了 保存到nacos的 功能 目前只支持 限流 和 熔断  其余功能照抄即可 主要是要注意 JSON 结构 需要在客户端 进行解析处理


默认格式是
appName 在dashboard注册的appName 加上后缀 -flow-rules 其他同理
scm-distributor-local-flow-rules
scm-distributor-local-degrade-rules

格式是这个样子

```json
[
    {
    "app": "scm-distributor-local",
    "clusterConfig":
    {
    "acquireRefuseStrategy": 0,
    "clientOfflineTime": 2000,
    "fallbackToLocalWhenFail": true,
    "resourceTimeout": 2000,
    "resourceTimeoutStrategy": 0,
    "sampleCount": 10,
    "strategy": 0,
    "thresholdType": 0,
    "windowIntervalMs": 1000
    },
    "clusterMode": false,
    "controlBehavior": 0,
    "count": 20.0,
    "gmtCreate": 1744781856148,
    "gmtModified": 1744783960406,
    "grade": 1,
    "id": 1,
    "ip": "192.168.192.202",
    "limitApp": "default",
    "port": 8719,
    "resource": "RULE_PLATFORM_ERP_QYB",
    "strategy": 0
    },
    {
    "app": "scm-distributor-local-1",
    "clusterConfig":
    {
    "acquireRefuseStrategy": 0,
    "clientOfflineTime": 2000,
    "fallbackToLocalWhenFail": true,
    "resourceTimeout": 2000,
    "resourceTimeoutStrategy": 0,
    "sampleCount": 10,
    "strategy": 0,
    "thresholdType": 0,
    "windowIntervalMs": 1000
    },
    "clusterMode": false,
    "controlBehavior": 0,
    "count": 20.0,
    "gmtCreate": 1744781856148,
    "gmtModified": 1744783960406,
    "grade": 1,
    "id": 1,
    "ip": "192.168.192.202",
    "limitApp": "default",
    "port": 8719,
    "resource": "RULE_PLATFORM_ERP_QYB",
    "strategy": 0
    }
]
```
客户端引入nacos包
<dependency>
<groupId>com.alibaba.csp</groupId>
<artifactId>sentinel-datasource-nacos</artifactId>
<version>1.8.6</version>
</dependency>

客户端手动配置 和 nacos 配置 只能生效一个  如果监听nacos 那么手动配置不会生效  因为移除了一个数据的监听


```java
Properties props = new Properties();
props.put("namespace", "c6928d05-e9bc-4252-94fe-8e723984817e");
props.put("serverAddr", "localhost:8848");
String appName = AppNameUtil.getAppName();
ReadableDataSource<String,List<FlowRule>> flowRuleDataSource =  new NacosDataSource<>(props, "DEFAULT_GROUP", appName+"-flow-rules",
source -> {

       List<FlowRuleEntity> ruleEntities = JSON.parseObject(source, new TypeReference<List<FlowRuleEntity>>() {});
        String ip = HostNameUtil.getIp();
        // String port = TransportConfig.getPort();
        String port = TransportConfig.getRuntimePort()+"";
        List<FlowRule> collect = ruleEntities.stream().filter(r -> {
            if (StringUtils.isNotBlank(port)) {
                return r.getIp().equals(ip) && r.getPort().equals(Integer.valueOf(port)) && appName.equals(r.getApp());
            } else {
                return r.getIp().equals(ip)  && appName.equals(r.getApp());
            }
        }).map(r -> JSON.parseObject(JSON.toJSONString(r),new TypeReference<FlowRule>() {})).collect(Collectors.toList());
        return collect;
    });
FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
```
###
其他注意事项

客户端监听是这个样子 通过appName和ip 以及端口进行过滤 

其他注意事项
熔断时长 必须是统计时长的2倍以上  至于为什么举个简单例子
统计如果是 10s  熔断 10s 10秒包含了 熔断的十秒 不会被统计 当11秒 再去判断 其实就统计了 一秒的数据 本质上不希望这样
统计是10s 熔断是5s 比如说从第2秒熔断 第一秒 失败了10个  到回复 是7秒  10秒统计的话 会统计到 第一秒的失败数据 会有误触发的风险

统计时长10s 熔断30s


统计时长越长 采样越多 稳定性越高 统计时长越短 灵敏度越高 也有可能误判 针对不同场景设置不同的统计时长

最少请求 主要是看流量低峰 避免误判


日志部分
client端 时先把metric保存在内存 然后每秒刷盘一次 当文件过大 会循环写
dashboard读取时 从内存读取 根据文件记录 做聚合展示 

dashboard 的数据来源 一部分是 心跳上报  一部分是 dashboard调用client  
clinet 启动一个serversocket 端口是配置的 如果冲突 可以下探 源码部分在 SimpleHttpCommandCenter
基本上都是通过SPIload加载实现的
提供的api都下载META-INF\service 下面文件的配置中了  com.alibaba.csp.sentinel.transport.CommandCenter 这个文件中定义了所有的API




