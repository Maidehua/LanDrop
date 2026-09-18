package com.maidehua.landrop

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket

class FileServer(private val context: Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO); private var server:ServerSocket?=null
    fun start(){if(server!=null)return;server=ServerSocket(Protocol.HTTP_PORT);scope.launch{while(isActive){val socket=try{server?.accept()?:break}catch(_:Exception){break};launch{runCatching{handle(socket)};runCatching{socket.close()}}}}}
    private fun handle(socket:Socket){socket.soTimeout=60_000;val input=BufferedInputStream(socket.getInputStream());val output=BufferedOutputStream(socket.getOutputStream());val request=readLine(input)?:return;val parts=request.split(' ');if(parts.size<2)return respond(output,400,"{\"ok\":false}");val headers=mutableMapOf<String,String>();while(true){val line=readLine(input)?:break;if(line.isEmpty())break;val i=line.indexOf(':');if(i>0)headers[line.substring(0,i).trim().lowercase()]=line.substring(i+1).trim()};when{parts[0]=="GET"&&parts[1]=="/api/info"->respond(output,200,"""{"app":"LanDrop","version":1,"name":"Android ${android.os.Build.MODEL}","platform":"android","port":${Protocol.HTTP_PORT}}""");parts[0]=="POST"&&parts[1]=="/api/files/raw"->receiveFile(input,output,headers);else->respond(output,404,"{\"ok\":false,\"error\":\"not found\"}")}}
    private fun receiveFile(input:BufferedInputStream,output:BufferedOutputStream,headers:Map<String,String>){val length=headers["content-length"]?.toLongOrNull()?:return respond(output,411,"{\"ok\":false,\"error\":\"length required\"}");val encoded=headers["x-file-name-b64"]?:return respond(output,400,"{\"ok\":false,\"error\":\"missing filename\"}");val decoded=runCatching{String(Base64.decode(encoded,Base64.NO_WRAP),Charsets.UTF_8)}.getOrDefault("file.bin");val dir=File(context.getExternalFilesDir(null),"Download/LanDrop").apply{mkdirs()};val file=unique(dir,sanitize(decoded));var remaining=length;var saved=0L;file.outputStream().buffered().use{target->val buffer=ByteArray(64*1024);while(remaining>0){val count=input.read(buffer,0,minOf(buffer.size.toLong(),remaining).toInt());if(count<0)break;target.write(buffer,0,count);remaining-=count;saved+=count}};if(remaining!=0L){file.delete();return respond(output,400,"{\"ok\":false,\"error\":\"incomplete upload\"}")};respond(output,201,buildJsonObject{put("ok",true);put("name",file.name);put("size",saved)}.toString())}
    private fun readLine(input:BufferedInputStream):String?{val bytes=ArrayList<Byte>();while(bytes.size<16384){val b=input.read();if(b<0)return if(bytes.isEmpty())null else bytes.toByteArray().toString(Charsets.UTF_8);if(b=='\n'.code)break;if(b!='\r'.code)bytes.add(b.toByte())};return bytes.toByteArray().toString(Charsets.UTF_8)}
    private fun respond(output:BufferedOutputStream,code:Int,json:String){val body=json.toByteArray();val reason=when(code){200->"OK";201->"Created";400->"Bad Request";404->"Not Found";411->"Length Required";else->"Error"};output.write("HTTP/1.1 $code $reason\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray());output.write(body);output.flush()}
    fun stop(){runCatching{server?.close()};server=null;scope.cancel()}
    private fun sanitize(name:String)=File(name).name.map{if(it in "<>:\"/\\|?*"||it.code<32)'_' else it}.joinToString("").ifBlank{"file.bin"}
    private fun unique(dir:File,name:String):File{var out=File(dir,name);var i=1;val base=out.nameWithoutExtension;val ext=out.extension.let{if(it.isBlank())"" else ".$it"};while(out.exists())out=File(dir,"$base (${i++})$ext");return out}
}
