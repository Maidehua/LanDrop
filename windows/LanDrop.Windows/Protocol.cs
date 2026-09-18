namespace LanDrop.Windows;
public static class Protocol { public const int DiscoveryPort = 40404; public const int HttpPort = 40405; }
public sealed class Peer(string name, string platform, string address, int port, DateTime lastSeen)
{
    public string Name { get; set; } = name; public string Platform { get; } = platform; public string Address { get; } = address; public int Port { get; } = port; public DateTime LastSeen { get; set; } = lastSeen;
    public string Display => $"{Name} · {Platform}\n{Address}:{Port}";
}
