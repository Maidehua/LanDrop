using System.Net.Http.Headers;
using System.Net.Http;
namespace LanDrop.Windows;
public static class FileSender
{
    private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromHours(1) };
    public static async Task SendAsync(Peer peer, string path) { await using var stream=File.OpenRead(path); using var form=new MultipartFormDataContent(); using var part=new StreamContent(stream); part.Headers.ContentType=new MediaTypeHeaderValue("application/octet-stream"); form.Add(part,"file",Path.GetFileName(path)); using var response=await Client.PostAsync($"http://{peer.Address}:{peer.Port}/api/files",form); response.EnsureSuccessStatusCode(); }
}
