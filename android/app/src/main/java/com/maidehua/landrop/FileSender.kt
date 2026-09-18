package com.maidehua.landrop

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

object FileSender {
    private val client=HttpClient(CIO){engine{requestTimeout=0}}
    suspend fun send(context:Context,peer:Peer,uri:Uri){ val doc=DocumentFile.fromSingleUri(context,uri)?:error("无法读取文件"); val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:error("无法打开文件"); val response=client.post("http://${peer.address}:${peer.port}/api/files"){setBody(MultiPartFormDataContent(formData{append("file",bytes,Headers.build{append(HttpHeaders.ContentDisposition,"filename=\"${doc.name?:"file.bin"}\"");append(HttpHeaders.ContentType,doc.type?:"application/octet-stream")})}))}; if(!response.status.isSuccess())error("接收端返回 ${response.status.value}") }
}
