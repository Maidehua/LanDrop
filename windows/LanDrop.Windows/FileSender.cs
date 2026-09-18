using System.Net.Http;
using System.Text;
namespace LanDrop.Windows;
public static class FileSender
{
    private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromHours(1) };
    public static async Task SendAsync(Peer peer, string path) { await using var stream=File.OpenRead(path); using var part=new StreamContent(stream); part.Headers.ContentType=new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream"); part.Headers.Add("X-File-Name-B64",Convert.ToBase64String(Encoding.UTF8.GetBytes(Path.GetFileName(path)))); using var response=await Client.PostAsync($"http://{peer.Address}:{peer.Port}/api/files/raw",part); response.EnsureSuccessStatusCode(); }
}
