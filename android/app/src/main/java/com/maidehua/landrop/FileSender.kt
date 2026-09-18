package com.maidehua.landrop

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

object FileSender {
    private val client=HttpClient(CIO){engine{requestTimeout=0}}
    suspend fun send(context:Context,peer:Peer,uri:Uri){ val doc=DocumentFile.fromSingleUri(context,uri)?:error("无法读取文件"); val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:error("无法打开文件"); val name=Base64.encodeToString((doc.name?:"file.bin").toByteArray(Charsets.UTF_8),Base64.NO_WRAP); val response=client.post("http://${peer.address}:${peer.port}/api/files/raw"){header("X-File-Name-B64",name);contentType(ContentType.Application.OctetStream);setBody(bytes)}; if(!response.status.isSuccess())error("接收端返回 ${response.status.value}") }
}
