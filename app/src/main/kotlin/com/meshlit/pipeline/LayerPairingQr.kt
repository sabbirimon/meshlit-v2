package com.meshlit.pipeline
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
object LayerPairingQr {
    private const val PREFIX="meshlit-layer-v1:"
    fun encode(peer:PipelinePeer)=PREFIX+Json.encodeToString(peer)
    fun decode(raw:String):PipelinePeer {
        require(raw.length<=4096 && raw.startsWith(PREFIX)){"Use a Meshlit layer worker QR"}
        val peer=Json.decodeFromString<PipelinePeer>(raw.removePrefix(PREFIX))
        require(peer.host.isNotBlank() && peer.host.length<=253 && peer.host.none{it.isWhitespace() || it in "/?#@"})
        require(peer.port in 1024..65535 && peer.fingerprint.matches(Regex("[a-fA-F0-9]{64}")))
        require(peer.token.length in 32..128 && peer.token.all{it.code in 33..126})
        return peer.copy(weight=1)
    }
}
