# LanDrop 局域网文件互传

Android 与 Windows 双端局域网文件互传工具。两台设备连接同一 Wi‑Fi/局域网后，可自动发现彼此，也可手动输入 IP；文件通过 HTTP 在局域网内点对点传输，不经过云端。

## 功能

- UDP 广播自动发现（端口 `40404`）
- HTTP 文件上传（端口 `40405`）
- Android：Kotlin + Jetpack Compose
- Windows：C# + WPF + .NET 8
- 文件名清理、防目录穿越、流式传输
- 手动输入 IP 作为自动发现的备用方式

## 项目结构

- `android/`：Android Studio 工程，最低 Android 8.0（API 26）
- `windows/`：Visual Studio / .NET 8 WPF 工程
- `PROTOCOL.md`：两端通信协议

## 快速开始

### Windows

1. 安装 [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)。
2. 进入 `windows/LanDrop.Windows`。
3. 执行 `dotnet run`。
4. 首次启动若 Windows 防火墙询问，请允许“专用网络”。

### Android

1. 使用 Android Studio 打开 `android`。
2. 等待 Gradle 同步，连接手机后运行 `app`。
3. 授予通知权限；接收的文件保存到应用外部文件目录 `Download/LanDrop`。

## 使用

1. 两端连接同一个局域网并保持应用打开。
2. 在设备列表点击目标设备；若未发现，输入对方显示的 IP。
3. 选择文件并发送。

> 当前版本面向可信局域网。请勿在公共 Wi‑Fi 下保持接收服务开启。后续可增加 6 位配对码及 TLS。

## 构建

Windows：

```powershell
dotnet publish windows/LanDrop.Windows/LanDrop.Windows.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

Android：

```bash
cd android
gradle assembleDebug
```

## 许可证

MIT
