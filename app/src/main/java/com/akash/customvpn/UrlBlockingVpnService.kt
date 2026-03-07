package com.akash.customvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class UrlBlockingVpnService : VpnService(), Runnable {
    private var mThread: Thread? = null
    private var mInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mThread != null) {
            mThread?.interrupt()
        }
        mThread = Thread(this, "UrlBlockingVpnThread")
        mThread?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        if (mThread != null) {
            mThread?.interrupt()
        }
        super.onDestroy()
    }

    override fun run() {
        try {
            val builder = Builder()
            builder.setSession("UrlBlockingVpn")
            builder.addAddress("10.0.0.1", 24)
            builder.addDnsServer("8.8.8.8")
            builder.addRoute("0.0.0.0", 0)

            mInterface = builder.establish()
            
            val inputStream = FileInputStream(mInterface?.fileDescriptor)
            val outputStream = FileOutputStream(mInterface?.fileDescriptor)

            val packet = ByteBuffer.allocate(32767)

            while (!Thread.interrupted()) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    
                    // Simple logic to detect DNS queries (UDP port 53)
                    // This is a VERY crude way to extract domain names for demonstration.
                    try {
                        val domain = extractDomainFromDnsPacket(packet)
                        if (domain != null) {
                            DomainRepository.addDiscoveredDomain(domain)
                            
                            if (DomainRepository.isBlocked(domain)) {
                                Log.d("UrlBlockingVpn", "Blocked: $domain")
                                // To block, we simply don't write the packet back (drop it)
                                packet.clear()
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for non-DNS packets
                    }

                    // In a real implementation, you MUST forward this packet to the internet
                    // using a DatagramSocket or Socket, and then write the response back 
                    // to the outputStream. This is a complex NAT implementation.
                    
                    // For now, we just "process" it by clearing. 
                    // WARNING: This will break internet access unless you implement forwarding.
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("UrlBlockingVpn", "Error in VPN loop", e)
        } finally {
            mInterface?.close()
            mInterface = null
        }
    }

    private fun extractDomainFromDnsPacket(packet: ByteBuffer): String? {
        // Offset 0: IP Header (usually 20 bytes)
        // Offset 20: UDP Header (8 bytes)
        // Offset 28: DNS Header (12 bytes)
        // Offset 40: DNS Question Section (Domain name starts here)
        
        // This is a simplified extraction logic
        if (packet.get(9).toInt() == 17) { // Protocol 17 = UDP
            val destPort = ((packet.get(22).toInt() and 0xFF) shl 8) or (packet.get(23).toInt() and 0xFF)
            if (destPort == 53) {
                val domain = StringBuilder()
                var pos = 40
                while (pos < packet.limit()) {
                    val labelLen = packet.get(pos).toInt()
                    if (labelLen == 0) break
                    pos++
                    for (i in 0 until labelLen) {
                        domain.append(packet.get(pos).toChar())
                        pos++
                    }
                    domain.append(".")
                }
                if (domain.isNotEmpty()) {
                    return domain.toString().removeSuffix(".")
                }
            }
        }
        return null
    }
}
