package com.maidehua.landrop

import android.content.Context
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

class FileServer(private val context:Context) {
    private var engine:EmbeddedServer<CIOApplicationEngine,CIOApplicationEngine.Configuration>?=null
    fun start(){ val dir=File(context.getExternalFilesDir(null),"Download/LanDrop").apply{mkdirs()}; engine=embeddedServer(CIO,port=Protocol.HTTP_PORT,host="0.0.0.0") { routing {
        get("/api/info"){call.respondText("""{"app":"LanDrop","version":1,"name":"Android ${android.os.Build.MODEL}","platform":"android","port":${Protocol.HTTP_PORT}}""",ContentType.Application.Json)}
        post("/api/files"){ var saved:File?=null; var size=0L; call.receiveMultipart().forEachPart{part-> if(part is PartData.FileItem){ val clean=sanitize(part.originalFileName?:"file.bin"); val out=unique(dir,clean); part.provider().use{input->out.outputStream().use{output->size=input.copyTo(output)}}; saved=out }; part.dispose() }; if(saved==null)call.respondText("{\"ok\":false,\"error\":\"missing file\"}",ContentType.Application.Json,HttpStatusCode.BadRequest) else call.respondText(buildJsonObject{put("ok",true);put("name",saved!!.name);put("size",size)}.toString(),ContentType.Application.Json,HttpStatusCode.Created) }
    }}.start(wait=false) }
    fun stop(){engine?.stop(500,1500)}
    private fun sanitize(name:String)=File(name).name.map{if(it in "<>:\"/\\|?*"||it.code<32)'_' else it}.joinToString("").ifBlank{"file.bin"}
    private fun unique(dir:File,name:String):File{var out=File(dir,name);var i=1;val base=out.nameWithoutExtension;val ext=out.extension.let{if(it.isBlank())"" else ".$it"};while(out.exists())out=File(dir,"$base (${i++})$ext");return out}
}
