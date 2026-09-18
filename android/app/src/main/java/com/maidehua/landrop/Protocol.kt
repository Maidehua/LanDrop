package com.maidehua.landrop

import kotlinx.serialization.Serializable

object Protocol { const val DISCOVERY_PORT = 40404; const val HTTP_PORT = 40405 }
@Serializable data class DiscoveryMessage(val app: String="LanDrop", val version: Int=1, val name: String, val platform: String="android", val port: Int=Protocol.HTTP_PORT)
data class Peer(val name: String, val platform: String, val address: String, val port: Int, val lastSeen: Long = System.currentTimeMillis()) { val display get()="$name · $platform\n$address:$port" }
