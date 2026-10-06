package com.meshlit.core.inference.pipeline

import kotlinx.serialization.Serializable

@Serializable data class NodeOffer(
    val nodeId:String,val abi:String,val freeMemoryBytes:Long,val freeDiskBytes:Long,
    val runtimeRevision:String,val workerAllowed:Boolean,val coordinatorAllowed:Boolean=false,
    val coordinatorModelSha256:String?=null,val thermalStatus:Int=0,val observedAtMs:Long=System.currentTimeMillis(),
    val deviceClass:String="phone",val backend:String="CPU",val cpuThreads:Int=2,
)
data class NegotiatedCluster(val coordinatorId:String,val workers:List<NodeOffer>,val weights:List<Int>,val memoryBudgetBytes:Long,val estimatedWorkerBytes:List<Long> = emptyList(),val estimatedKvBytes:Long=0,val estimatedCoordinatorBytes:Long=0)

/** Deterministic capability election. Device labels never imply computation.
 * Capability snapshots are admission estimates, not memory reservations. */
object ClusterNegotiation {
    const val REVISION="4df29be4f4c3673f428170fda944a5b19f743bb8"
    fun budget(offer:NodeOffer):Long=(offer.freeMemoryBytes*0.65).toLong().minus(256L*1024*1024).coerceAtLeast(0)
    fun plan(offers:List<NodeOffer>,modelSha256:String,modelBytes:Long,nowMs:Long=System.currentTimeMillis(),kvBytes:Long=0,blocks:Int?=null):NegotiatedCluster {
        require(offers.size<=64){"Too many cluster offers"}
        require(offers.all{it.freeMemoryBytes in 0..(1L shl 50) && it.freeDiskBytes>=0 && it.nodeId.matches(Regex("[A-Za-z0-9_-]{1,80}"))}){"Invalid node capability bounds"}
        require(modelSha256.matches(Regex("[a-f0-9]{64}")) && modelBytes>0 && modelBytes<=Long.MAX_VALUE/4 && kvBytes>=0 && kvBytes<=Long.MAX_VALUE/4)
        require(blocks==null || blocks in 1..1024)
        require(offers.map{it.nodeId}.distinct().size==offers.size){"Duplicate node identities"}
        val compatible=offers.filter { it.runtimeRevision==REVISION && it.backend=="CPU" && it.observedAtMs>=0 && it.observedAtMs<=nowMs+5000 &&
            nowMs-it.observedAtMs<=30000 && it.thermalStatus<3 && it.freeMemoryBytes>0 }
        val master=compatible.filter{it.coordinatorAllowed && it.coordinatorModelSha256==modelSha256}
            .sortedWith(compareByDescending<NodeOffer>{budget(it)}.thenBy{it.nodeId}).firstOrNull()
            ?: error("No approved coordinator has the selected model")
        val workers=compatible.filter{it.workerAllowed && it.nodeId!=master.nodeId && budget(it)>0}
            .sortedBy{it.nodeId}
        require(workers.size>=2){"Need at least two fresh, compatible, approved workers"}
        val shared=(modelBytes*1.3+kvBytes*1.2).toLong()
        // Leave graph overhead and one layer of placement granularity on each worker.
        val padding=256L*1024*1024 + if(blocks!=null) shared/blocks else shared/8
        val effective=workers.map{(budget(it)-padding).coerceAtLeast(0)}
        require(effective.all{it>0}){"A worker lacks room for graph and layer-placement overhead"}
        val capacity=workers.sumOf{budget(it)}
        val usable=effective.sum()
        require(usable>=shared){"Cluster memory estimate is too small for weights, context/KV and graph overhead"}
        val coordinatorEstimate=(modelBytes*0.1+kvBytes*0.05).toLong()+256L*1024*1024
        require(budget(master)>=coordinatorEstimate){"Coordinator lacks estimated embedding/scheduling memory"}
        val weights=effective.map{((it.toDouble()/usable)*1000).toInt().coerceAtLeast(1)}
        val sumWeights=weights.sum().toDouble()
        val estimates=weights.map{ kotlin.math.ceil(shared*(it/sumWeights)).toLong()+padding }
        require(estimates.zip(workers).all{(estimate,node)->estimate<=budget(node)}){"Rounded layer placement exceeds a worker budget"}
        return NegotiatedCluster(master.nodeId,workers,weights,capacity,estimates,kvBytes,coordinatorEstimate)
    }
}
