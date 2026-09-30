package dev.zohaib.networkfirewall.vpn

import java.net.InetAddress

data class PacketInfo(
    val protocol: Int,
    val srcIp: InetAddress,
    val srcPort: Int,
    val destIp: InetAddress,
    val destPort: Int
)

/**
 * Minimal IPv4 / IPv6 header parser for packets read from the TUN interface.
 * Ports are 0 when the packet is not TCP/UDP, is a non-first IPv4 fragment,
 * or is too short to contain the transport header.
 */
object PacketParser {
    const val PROTOCOL_TCP = 6
    const val PROTOCOL_UDP = 17

    fun parse(packet: ByteArray, length: Int): PacketInfo? {
        if (length < 1 || length > packet.size) return null
        return when ((packet[0].toInt() shr 4) and 0x0F) {
            4 -> parseIpv4(packet, length)
            6 -> parseIpv6(packet, length)
            else -> null
        }
    }

    private fun parseIpv4(packet: ByteArray, length: Int): PacketInfo? {
        if (length < 20) return null
        val headerLength = (packet[0].toInt() and 0x0F) * 4
        if (headerLength < 20 || length < headerLength) return null

        val protocol = packet[9].toInt() and 0xFF
        val srcIp = InetAddress.getByAddress(packet.copyOfRange(12, 16))
        val destIp = InetAddress.getByAddress(packet.copyOfRange(16, 20))

        val fragmentOffset = ((packet[6].toInt() and 0x1F) shl 8) or (packet[7].toInt() and 0xFF)
        val (srcPort, destPort) = if (fragmentOffset == 0) {
            readPorts(packet, length, headerLength, protocol)
        } else {
            0 to 0
        }
        return PacketInfo(protocol, srcIp, srcPort, destIp, destPort)
    }

    private fun parseIpv6(packet: ByteArray, length: Int): PacketInfo? {
        if (length < 40) return null
        // Extension headers are not walked; their packets are reported without ports.
        val nextHeader = packet[6].toInt() and 0xFF
        val srcIp = InetAddress.getByAddress(packet.copyOfRange(8, 24))
        val destIp = InetAddress.getByAddress(packet.copyOfRange(24, 40))
        val (srcPort, destPort) = readPorts(packet, length, 40, nextHeader)
        return PacketInfo(nextHeader, srcIp, srcPort, destIp, destPort)
    }

    private fun readPorts(packet: ByteArray, length: Int, offset: Int, protocol: Int): Pair<Int, Int> {
        if ((protocol != PROTOCOL_TCP && protocol != PROTOCOL_UDP) || length < offset + 4) return 0 to 0
        return readUnsignedShort(packet, offset) to readUnsignedShort(packet, offset + 2)
    }

    private fun readUnsignedShort(packet: ByteArray, offset: Int): Int =
        ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)
}
