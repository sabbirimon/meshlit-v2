package com.meshlit.hyperl

import com.meshlit.core.common.control.ManagedFeature
import com.meshlit.core.common.control.OperationGate
import com.meshlit.core.hyperl.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Semaphore

enum class HyperLCpuChoice { REFERENCE, NATIVE }
data class HyperLWorkbenchResult(val values:FloatArray,val plan:HyperLArrayPlan,val wallMs:Double,val backend:String="CPU_REFERENCE")
/** Human local preprocessing only; shares the saved function/global stop gate. */
class HyperLWorkbenchController(private val gate:OperationGate) {
    private val capacity=Semaphore(1)
    private data class Prepared(val program:HyperLProgram,val inputs:Map<String,FloatArray>,val plan:HyperLArrayPlan)
    private fun prepare(program:String,inputs:String,budgetBytes:Long):Prepared {
        require(program.length<=65536 && inputs.length<=65536){"Workbench text limit: 65,536 characters per editor"}
        val parsed=HyperLCodec.program(program);val values=HyperLCodec.inputs(inputs)
        return Prepared(parsed,values,HyperLAdmission.plan(parsed,values,budgetBytes))
    }
    fun plan(program:String,inputs:String,budgetBytes:Long):HyperLArrayPlan {
        return prepare(program,inputs,budgetBytes).plan
    }
    private suspend fun <T> bounded(work:suspend()->T):T {
        check(capacity.tryAcquire()){"HyperL busy; no request queued"}
        try{return gate.run(ManagedFeature.HYPERL){withTimeout(10000){withContext(Dispatchers.Default){work()}}}}
        finally{capacity.release()}
    }
    suspend fun execute(program:String,inputs:String,budgetBytes:Long,choice:HyperLCpuChoice=HyperLCpuChoice.REFERENCE):HyperLWorkbenchResult=bounded {
        val start=System.nanoTime();val prepared=prepare(program,inputs,budgetBytes);HyperLAdmission.requireAdmission(prepared.plan)
        val result=if(choice==HyperLCpuChoice.NATIVE) NativeCpu.execute(prepared.program,prepared.inputs,budgetBytes)
            else HyperLRuntime(listOf(HyperLCpuBackend(budgetBytes)),maxConcurrentExecutions=1)
                .execute(HyperLTarget.CPU_REFERENCE,prepared.program,prepared.inputs)
        HyperLWorkbenchResult(result,prepared.plan,(System.nanoTime()-start)/1_000_000.0,
            if(choice==HyperLCpuChoice.NATIVE) "NATIVE_C99 · ${HyperLContract.RUNTIME_REVISION}" else "CPU_REFERENCE")
    }
    suspend fun precise(inputs:String,budgetBytes:Long,choice:HyperLCpuChoice):Float=bounded {
        require(inputs.length<=65536){"Workbench input limit"}
        val vectors=HyperLCodec.inputs(inputs)
        require(vectors.size==1){"Precise sum requires exactly one input vector; it does not change graph semantics"}
        val values=vectors.values.single()
        if(choice==HyperLCpuChoice.NATIVE) NativeCpu.precise(values,budgetBytes)
        else PreciseReduction.sum(values,budgetBytes)
    }
    suspend fun emit(program:String,target:HyperLTarget):String=bounded {
        require(program.length<=65536){"Workbench program limit"}
        PortableEmitter.emit(HyperLCodec.program(program),target).source
    }
}
