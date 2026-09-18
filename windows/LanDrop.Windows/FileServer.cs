using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;

namespace LanDrop.Windows;

public sealed class FileServer(string receiveDirectory) : IDisposable
{
    private WebApplication? _app;
    public async Task StartAsync()
    {
        Directory.CreateDirectory(receiveDirectory);
        var builder = WebApplication.CreateSlimBuilder();
        builder.WebHost.UseUrls($"http://0.0.0.0:{Protocol.HttpPort}");
        builder.Services.ConfigureHttpJsonOptions(_ => { });
        _app = builder.Build();
        _app.MapGet("/api/info", () => Results.Json(new { app="LanDrop", version=1, name=Environment.MachineName, platform="windows", port=Protocol.HttpPort }));
        _app.MapPost("/api/files", async (HttpRequest request) => {
            if (!request.HasFormContentType) return Results.BadRequest(new { ok=false, error="multipart/form-data required" });
            var form = await request.ReadFormAsync();
            var upload = form.Files.GetFile("file");
            if (upload is null) return Results.BadRequest(new { ok=false, error="missing file" });
            var path = UniquePath(receiveDirectory, Sanitize(Path.GetFileName(upload.FileName)));
            await using (var output = File.Create(path)) await upload.CopyToAsync(output);
            return Results.Json(new { ok=true, name=Path.GetFileName(path), size=upload.Length }, statusCode:201);
        });
        _app.MapPost("/api/files/raw", async (HttpRequest request) => {
            var encoded = request.Headers["X-File-Name-B64"].FirstOrDefault();
            if (string.IsNullOrWhiteSpace(encoded)) return Results.BadRequest(new { ok=false, error="missing filename" });
            string decoded;
            try { decoded = System.Text.Encoding.UTF8.GetString(Convert.FromBase64String(encoded)); }
            catch { return Results.BadRequest(new { ok=false, error="invalid filename" }); }
            var path = UniquePath(receiveDirectory, Sanitize(Path.GetFileName(decoded)));
            await using (var output = File.Create(path)) await request.Body.CopyToAsync(output);
            return Results.Json(new { ok=true, name=Path.GetFileName(path), size=new FileInfo(path).Length }, statusCode:201);
        });
        await _app.StartAsync();
    }
    private static string Sanitize(string name) { foreach (var c in Path.GetInvalidFileNameChars()) name=name.Replace(c,'_'); return string.IsNullOrWhiteSpace(name)?"file.bin":name; }
    private static string UniquePath(string dir,string name) { var path=Path.Combine(dir,name); for(var i=1;File.Exists(path);i++) path=Path.Combine(dir,$"{Path.GetFileNameWithoutExtension(name)} ({i}){Path.GetExtension(name)}"); return path; }
    public void Dispose() { if (_app is not null) _app.StopAsync().GetAwaiter().GetResult(); }
}
