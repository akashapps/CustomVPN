package com.akash.customvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class UrlBlockingVpnService : VpnService(), Runnable {
    private var mThread: Thread? = null
    private var mInterface: ParcelFileDescriptor? = null
    private val executorService = Executors.newFixedThreadPool(10)

    override fun onCreate() {
        super.onCreate()
        DomainRepository.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mThread != null) mThread?.interrupt()
        mThread = Thread(this, "UrlBlockingVpnThread").apply { start() }
        return START_STICKY
    }

    override fun onDestroy() {
        mThread?.interrupt()
        executorService.shutdownNow()
        super.onDestroy()
    }

    override fun run() {
        try {
            mInterface = Builder()
                .setSession("UrlBlockingVpn")
                .addAddress("10.0.0.1", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("8.8.8.8", 32)
                .establish()

            val inputStream = FileInputStream(mInterface?.fileDescriptor)
            val outputStream = FileOutputStream(mInterface?.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            while (!Thread.interrupted()) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    val packetData = packet.array().copyOf(length)
                    val buffer = ByteBuffer.wrap(packetData).order(ByteOrder.BIG_ENDIAN)

                    val domain = extractDomainFromDnsPacket(buffer)
                    if (domain != null) {
                        DomainRepository.addDiscoveredDomain(domain)
                        if (DomainRepository.isBlocked(domain)) {
                            Log.i("UrlBlockingVpn", "BLOCKING: $domain")
                            continue
                        }
                    }

                    executorService.execute {
                        forwardDnsQuery(packetData, length, outputStream)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UrlBlockingVpn", "Error in VPN loop", e)
        } finally {
            try { mInterface?.close() } catch (e: Exception) {}
            mInterface = null
        }
    }

    private fun forwardDnsQuery(
        packetData: ByteArray,
        length: Int,
        outputStream: FileOutputStream
    ) {
        var dnsSocket: DatagramSocket? = null
        try {
            if (length <= 28) return
            
            dnsSocket = DatagramSocket()
            protect(dnsSocket) 
            dnsSocket.soTimeout = 1500

            val dnsPayload = packetData.copyOfRange(28, length)
            val dnsRequest = DatagramPacket(
                dnsPayload, dnsPayload.size,
                InetAddress.getByName("8.8.8.8"), 53
            )
            dnsSocket.send(dnsRequest)

            val responseBuffer = ByteArray(32767)
            val dnsResponse = DatagramPacket(responseBuffer, responseBuffer.size)
            dnsSocket.receive(dnsResponse)

            val responsePacket = ByteBuffer.allocate(28 + dnsResponse.length)
            responsePacket.order(ByteOrder.BIG_ENDIAN)
            
            responsePacket.put(0x45.toByte()) 
            responsePacket.put(0x00.toByte()) 
            responsePacket.putShort((28 + dnsResponse.length).toShort())
            responsePacket.putShort(0.toShort()) 
            responsePacket.putShort(0x4000.toShort()) 
            responsePacket.put(64.toByte()) 
            responsePacket.put(17.toByte()) 
            val checksumPos = responsePacket.position()
            responsePacket.putShort(0.toShort()) 
            responsePacket.put(8.toByte()); responsePacket.put(8.toByte())
            responsePacket.put(8.toByte()); responsePacket.put(8.toByte())
            responsePacket.put(10.toByte()); responsePacket.put(0.toByte())
            responsePacket.put(0.toByte()); responsePacket.put(1.toByte())

            val ipChecksum = calculateChecksum(responsePacket, 0, 20)
            responsePacket.putShort(checksumPos, ipChecksum)

            responsePacket.putShort(53.toShort()) 
            val originalSrcPort = ((packetData[20].toInt() and 0xFF) shl 8) or (packetData[21].toInt() and 0xFF)
            responsePacket.putShort(originalSrcPort.toShort())
            responsePacket.putShort((8 + dnsResponse.length).toShort())
            responsePacket.putShort(0.toShort()) 

            responsePacket.put(dnsResponse.data, 0, dnsResponse.length)

            synchronized(outputStream) {
                outputStream.write(responsePacket.array(), 0, responsePacket.position())
            }
        } catch (e: Exception) {
        } finally {
            dnsSocket?.close()
        }
    }

    private fun calculateChecksum(buf: ByteBuffer, offset: Int, length: Int): Short {
        var sum = 0
        val temp = buf.duplicate()
        temp.position(offset)
        for (i in 0 until length / 2) {
            sum += temp.short.toInt() and 0xFFFF
        }
        if (length % 2 != 0) {
            sum += (temp.get().toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv() and 0xFFFF).toShort()
    }

    private fun extractDomainFromDnsPacket(packet: ByteBuffer): String? {
        try {
            if (packet.limit() < 40) return null
            val protocol = packet.get(9).toInt()
            if (protocol == 17) { 
                val destPort = ((packet.get(22).toInt() and 0xFF) shl 8) or (packet.get(23).toInt() and 0xFF)
                if (destPort == 53) {
                    val domain = StringBuilder()
                    var pos = 40
                    while (pos < packet.limit()) {
                        val len = packet.get(pos).toInt() and 0xFF
                        if (len == 0) break
                        if (len > 63) return null
                        pos++
                        if (pos + len > packet.limit()) break
                        for (i in 0 until len) {
                            domain.append(packet.get(pos).toInt().toChar())
                            pos++
                        }
                        domain.append(".")
                    }
                    if (domain.isNotEmpty()) return domain.toString().removeSuffix(".")
                }
            }
        } catch (e: Exception) { }
        return null
    }
}
