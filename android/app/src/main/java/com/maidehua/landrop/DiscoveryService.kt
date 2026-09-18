package com.maidehua.landrop

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.net.*

class DiscoveryService(context: Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val _peers=MutableStateFlow<List<Peer>>(emptyList()); val peers=_peers.asStateFlow()
    private val lock=(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("LanDropDiscovery").apply { setReferenceCounted(true) }
    private var socket: DatagramSocket?=null
    fun start(){ lock.acquire(); scope.launch { listen() }; scope.launch { broadcast() }; scope.launch { while(isActive){ delay(2000); val now=System.currentTimeMillis(); _peers.value=_peers.value.filter{now-it.lastSeen<10000} } } }
    private suspend fun listen()=withContext(Dispatchers.IO){ try { socket=DatagramSocket(null).apply { reuseAddress=true; bind(InetSocketAddress(Protocol.DISCOVERY_PORT)); broadcast=true }; val buf=ByteArray(2048); while(currentCoroutineContext().isActive){ val p=DatagramPacket(buf,buf.size); socket!!.receive(p); val m=runCatching{Json.decodeFromString<DiscoveryMessage>(p.data.decodeToString(0,p.length))}.getOrNull() ?: continue; if(m.app!="LanDrop")continue; val peer=Peer(m.name,m.platform,p.address.hostAddress?:continue,m.port); val list=_peers.value.toMutableList(); val i=list.indexOfFirst{it.address==peer.address&&it.port==peer.port}; if(i<0)list.add(peer)else list[i]=peer; _peers.value=list } } catch(_:SocketException){} }
    private suspend fun broadcast()=withContext(Dispatchers.IO){ val data=Json.encodeToString(DiscoveryMessage(name="Android ${android.os.Build.MODEL}")).encodeToByteArray(); DatagramSocket().use{it.broadcast=true; while(currentCoroutineContext().isActive){ runCatching{it.send(DatagramPacket(data,data.size,InetAddress.getByName("255.255.255.255"),Protocol.DISCOVERY_PORT))}; delay(3000) }} }
    fun stop(){ socket?.close(); scope.cancel(); if(lock.isHeld)lock.release() }
}
