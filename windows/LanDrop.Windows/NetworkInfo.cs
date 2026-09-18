using System.Net.NetworkInformation;
using System.Net.Sockets;
namespace LanDrop.Windows;
public static class NetworkInfo
{
    public static string? GetLocalIpv4() => NetworkInterface.GetAllNetworkInterfaces().Where(n => n.OperationalStatus == OperationalStatus.Up && n.NetworkInterfaceType != NetworkInterfaceType.Loopback).SelectMany(n => n.GetIPProperties().UnicastAddresses).FirstOrDefault(a => a.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(a.Address))?.Address.ToString();
}
