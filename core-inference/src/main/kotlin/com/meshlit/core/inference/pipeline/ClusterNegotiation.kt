package com.meshlit.core.inference.pipeline

import kotlinx.serialization.Serializable

@Serializable data class NodeOffer(
    val nodeId:String,val abi:String,val freeMemoryBytes:Long,val freeDiskBytes:Long,
    val runtimeRevision:String,val workerAllowed:Boolean,val coordinatorAllowed:Boolean=false,
    val coordinatorModelSha256:String?=null,val thermalStatus:Int=0,val observedAtMs:Long=System.currentTimeMillis(),
    val deviceClass:String="phone",val backend:String="CPU",val cpuThreads:Int=2,
)
data class NegotiatedCluster(val coordinatorId:String,val workers:List<NodeOffer>,val weights:List<Int>,val memoryBudgetBytes:Long)

/** Deterministic capability election. Device labels never imply computation.
 * Capability snapshots are admission estimates, not memory reservations. */
object ClusterNegotiation {
    const val REVISION="4df29be4f4c3673f428170fda944a5b19f743bb8"
    fun budget(offer:NodeOffer):Long=(offer.freeMemoryBytes*0.65).toLong().minus(256L*1024*1024).coerceAtLeast(0)
    fun plan(offers:List<NodeOffer>,modelSha256:String,modelBytes:Long,nowMs:Long=System.currentTimeMillis()):NegotiatedCluster {
        require(modelSha256.matches(Regex("[a-f0-9]{64}")) && modelBytes>0)
        require(offers.map{it.nodeId}.distinct().size==offers.size){"Duplicate node identities"}
        val compatible=offers.filter { it.runtimeRevision==REVISION && it.observedAtMs<=nowMs+5000 &&
            nowMs-it.observedAtMs<=30000 && it.thermalStatus<3 && it.freeMemoryBytes>0 }
        val master=compatible.filter{it.coordinatorAllowed && it.coordinatorModelSha256==modelSha256}
            .sortedWith(compareByDescending<NodeOffer>{budget(it)}.thenBy{it.nodeId}).firstOrNull()
            ?: error("No approved coordinator has the selected model")
        val workers=compatible.filter{it.workerAllowed && it.nodeId!=master.nodeId && budget(it)>0}
            .sortedBy{it.nodeId}
        require(workers.size>=2){"Need at least two fresh, compatible, approved workers"}
        val capacity=workers.sumOf{budget(it)}
        // Dense weights + conservative KV/activation allowance. GGUF size is only an estimate;
        // native per-device placement still decides the final fit.
        require(capacity>=modelBytes*1.3){"Cluster memory estimate is too small for model weights and runtime overhead"}
        val weights=workers.map{((budget(it).toDouble()/capacity)*1000).toInt().coerceAtLeast(1)}
        return NegotiatedCluster(master.nodeId,workers,weights,capacity)
    }
}
