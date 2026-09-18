package com.maidehua.landrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){
    private var discovery:DiscoveryService?=null; private var server:FileServer?=null
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);var startupError:String?=null;try{server=FileServer(this).also{it.start()}}catch(t:Throwable){startupError="接收服务启动失败：${t.message}"};try{discovery=DiscoveryService(this).also{it.start()}}catch(t:Throwable){startupError=listOfNotNull(startupError,"设备发现启动失败：${t.message}").joinToString("；")};setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF376CF6),background=Color(0xFFF5F7FB))){App(discovery,startupError)}}}
    override fun onDestroy(){discovery?.stop();server?.stop();super.onDestroy()}
}

@Composable private fun ComponentActivity.App(discovery:DiscoveryService?,startupError:String?){
    val peers by (discovery?.peers ?: remember{ kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }).collectAsStateWithLifecycle(); var target by remember{mutableStateOf<Peer?>(null)};var ip by remember{mutableStateOf("")};var status by remember{mutableStateOf(startupError?:"接收服务已启动")};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->val peer=target;if(peer!=null&&uris.isNotEmpty())scope.launch{busy=true;try{uris.forEachIndexed{i,u->status="正在发送 ${i+1}/${uris.size}";FileSender.send(this@App,peer,u)};status="发送完成，共 ${uris.size} 个文件"}catch(e:Exception){status="发送失败：${e.message}"}finally{busy=false}}}
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxSize().padding(22.dp)){
        Text("LanDrop",fontSize=30.sp);Text("本机 IP：${NetworkInfo.localIpv4()?:"未知"}",color=Color.Gray);Spacer(Modifier.height(18.dp))
        Row{OutlinedTextField(ip,{ip=it},Modifier.weight(1f),label={Text("对方 IP")},singleLine=true);Spacer(Modifier.width(8.dp));Button(onClick={if(ip.matches(Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$"))){target=Peer("手动设备","unknown",ip,Protocol.HTTP_PORT);status="已选择 $ip"}else status="IP 地址格式不正确"},modifier=Modifier.height(56.dp)){Text("使用")}}
        Spacer(Modifier.height(20.dp));Text("附近设备",fontSize=18.sp);Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f)){if(peers.isEmpty())Box(Modifier.padding(18.dp)){Text("正在搜索同一局域网内的设备…",color=Color.Gray)}else LazyColumn{items(peers,key={"${it.address}:${it.port}"}){p->ListItem(headlineContent={Text(p.name)},supportingContent={Text("${p.platform} · ${p.address}:${p.port}")},modifier=Modifier.clickable{target=p;status="已选择 ${p.name}"});HorizontalDivider()}}}
        Spacer(Modifier.height(16.dp));Text(target?.let{"目标：${it.name} (${it.address})"}?:"请先选择设备",color=Color.Gray);Spacer(Modifier.height(8.dp));Button(onClick={picker.launch(arrayOf("*/*"))},enabled=target!=null&&!busy,modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(busy)"发送中…" else "选择文件并发送")};if(busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp));Text(status,Modifier.padding(top=10.dp))
    }}
}
