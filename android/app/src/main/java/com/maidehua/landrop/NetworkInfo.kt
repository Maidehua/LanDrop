package com.maidehua.landrop
import java.net.NetworkInterface
object NetworkInfo { fun localIpv4():String?=NetworkInterface.getNetworkInterfaces().toList().filter{it.isUp&&!it.isLoopback}.flatMap{it.inetAddresses.toList()}.firstOrNull{it.hostAddress?.contains(':')==false&&!it.isLoopbackAddress}?.hostAddress }
