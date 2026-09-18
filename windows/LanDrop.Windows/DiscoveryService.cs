using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
namespace LanDrop.Windows;
public sealed class DiscoveryService(string deviceName, string platform) : IDisposable
{
    private readonly CancellationTokenSource _cts = new(); private UdpClient? _listener;
    public event Action<Peer>? PeerSeen;
    public void Start() { _ = ListenAsync(); _ = BroadcastAsync(); }
    private async Task ListenAsync()
    {
        try { _listener = new UdpClient(); _listener.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true); _listener.Client.Bind(new IPEndPoint(IPAddress.Any, Protocol.DiscoveryPort));
            while (!_cts.IsCancellationRequested) { var r = await _listener.ReceiveAsync(_cts.Token); var msg = JsonSerializer.Deserialize<DiscoveryMessage>(r.Buffer); if (msg?.App == "LanDrop") PeerSeen?.Invoke(new(msg.Name, msg.Platform, r.RemoteEndPoint.Address.ToString(), msg.Port, DateTime.UtcNow)); }
        } catch (OperationCanceledException) { } catch (SocketException) when (_cts.IsCancellationRequested) { }
    }
    private async Task BroadcastAsync()
    {
        using var udp = new UdpClient { EnableBroadcast = true }; var msg = new DiscoveryMessage("LanDrop", 1, deviceName, platform, Protocol.HttpPort); var data = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(msg));
        while (!_cts.IsCancellationRequested) { try { await udp.SendAsync(data, new IPEndPoint(IPAddress.Broadcast, Protocol.DiscoveryPort), _cts.Token); await Task.Delay(3000, _cts.Token); } catch (OperationCanceledException) { break; } }
    }
    public void Dispose() { _cts.Cancel(); _listener?.Dispose(); _cts.Dispose(); }
    private sealed record DiscoveryMessage(string App, int Version, string Name, string Platform, int Port);
}
