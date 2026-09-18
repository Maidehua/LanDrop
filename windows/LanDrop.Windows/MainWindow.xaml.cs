using Microsoft.Win32;
using System.Collections.ObjectModel;
using System.Net;

namespace LanDrop.Windows;

public partial class MainWindow : System.Windows.Window
{
    private readonly ObservableCollection<Peer> _peers = [];
    private readonly DiscoveryService _discovery;
    private readonly FileServer _server;
    private Peer? _target;

    public MainWindow()
    {
        InitializeComponent();
        PeerList.ItemsSource = _peers;
        var receiveDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "Downloads", "LanDrop");
        _server = new FileServer(receiveDir);
        _discovery = new DiscoveryService(Environment.MachineName, "windows");
        _discovery.PeerSeen += peer => Dispatcher.Invoke(() => Upsert(peer));
        Loaded += async (_, _) => {
            LocalInfo.Text = $"本机：{Environment.MachineName}  |  IP：{NetworkInfo.GetLocalIpv4() ?? "未知"}  |  接收目录：{receiveDir}";
            await _server.StartAsync();
            _discovery.Start();
            Status.Text = "接收服务已启动";
        };
        Closed += (_, _) => { _discovery.Dispose(); _server.Dispose(); };
    }

    private void Upsert(Peer p)
    {
        if (p.Address == NetworkInfo.GetLocalIpv4()) return;
        var old = _peers.FirstOrDefault(x => x.Address == p.Address && x.Port == p.Port);
        if (old is null) _peers.Add(p); else { old.Name = p.Name; old.LastSeen = DateTime.UtcNow; PeerList.Items.Refresh(); }
        foreach (var stale in _peers.Where(x => DateTime.UtcNow - x.LastSeen > TimeSpan.FromSeconds(10)).ToList()) _peers.Remove(stale);
    }

    private void PeerList_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
    { _target = PeerList.SelectedItem as Peer; TargetText.Text = _target is null ? "请先选择设备" : $"目标：{_target.Display}"; }

    private void UseIp_Click(object sender, System.Windows.RoutedEventArgs e)
    {
        if (!IPAddress.TryParse(ManualIp.Text.Trim(), out var ip)) { Status.Text = "IP 地址格式不正确"; return; }
        _target = new Peer("手动设备", "unknown", ip.ToString(), Protocol.HttpPort, DateTime.UtcNow);
        TargetText.Text = $"目标：{_target.Display}";
    }

    private async void Send_Click(object sender, System.Windows.RoutedEventArgs e)
    {
        if (_target is null) { Status.Text = "请先选择目标设备或输入 IP"; return; }
        var dialog = new OpenFileDialog { Multiselect = true, Title = "选择要发送的文件" };
        if (dialog.ShowDialog() != true) return;
        try {
            Progress.IsIndeterminate = true; Status.Text = "正在发送…";
            foreach (var file in dialog.FileNames) { Status.Text = $"正在发送 {Path.GetFileName(file)}"; await FileSender.SendAsync(_target, file); }
            Status.Text = $"发送完成，共 {dialog.FileNames.Length} 个文件";
        } catch (Exception ex) { Status.Text = $"发送失败：{ex.Message}"; }
        finally { Progress.IsIndeterminate = false; }
    }
}
