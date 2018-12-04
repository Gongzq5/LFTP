# LFTP

一个文件传输系统，使用CS结构，分为服务器和客户端两个程序。

## 运行

### 服务器部分
服务器端运行`lftp`包中的`ServerUI.java`程序。
我们提供了导出的jar包，命名为LFTPServer.jar，可以使用
```shell
$ java -jar LFTPServer.jar
```
运行该服务器程序，程序默认绑定在5066端口上。

### 客户端部分

客户端运行`lftp`包中的`Client.java`程序。

我们同样提供了导出的jar包文件，命名为`LFTPClient.jar`，可以使用

```shell
$ java -jar LFTPClient.jar
```

运行客户端程序，程序提供命令行的UI。

#### 获取文件命令

```shell
LFTP > lget serverIP serverPort fileName
```

**Example:**

```shell
LFTP > lget 120.77.155.38 5066 test\\src10m.txt
```

即连接到服务器，指定其地址为120.77.155.38，端口为5066，从此获取test\\src10m.txt文件到本地

#### 上传文件命令

```shell
LFTP > lsend serverIP serverPort fileName
```

**Example**

```shell
LFTP > lsend 120.77.155.38 5066 test\\src10m.txt
```

即连接到服务器，指定其地址为120.77.155.38，端口为5066，从本地传输test\\src10m.txt文件到服务器上
