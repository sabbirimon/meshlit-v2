package com.meshlit.core.common.control
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class OperationGateTest {
    @Test fun stopCancelsAndSurvivesRestartWithoutResume() = runBlocking {
        var saved = OperationPolicy(); val gate = OperationGate(persist = { saved = it }); val started = CompletableDeferred<Unit>()
        val child = launch { gate.run(ManagedFeature.CLUSTER) { started.complete(Unit); awaitCancellation() } }
        started.await(); gate.emergencyStop(); child.join(); assertTrue(child.isCancelled)
        assertTrue(runCatching { OperationGate(saved).requireAllowed(ManagedFeature.INFERENCE) }.isFailure)
        gate.resumeHuman(); gate.requireAllowed(ManagedFeature.CLUSTER)
    }
    @Test fun separateAgentRevocationAndDisabledFeatureSurviveResume() = runBlocking {
        val gate = OperationGate(); val started = CompletableDeferred<Unit>()
        val child = launch { gate.run(ManagedFeature.SSH, true) { started.complete(Unit); awaitCancellation() } }
        started.await(); gate.setFeature(ManagedFeature.SSH, false, true); child.join(); assertTrue(child.isCancelled)
        gate.requireAllowed(ManagedFeature.SSH); assertTrue(runCatching { gate.requireAllowed(ManagedFeature.SSH, true) }.isFailure)
        gate.setFeature(ManagedFeature.CYBER, false); gate.emergencyStop(); gate.resumeHuman()
        assertTrue(runCatching { gate.requireAllowed(ManagedFeature.CYBER) }.isFailure)
    }
    @Test fun persistenceFailureNeverAllowsResumeOrRevocationBypass() {
        val gate = OperationGate(OperationPolicy(emergencyStopped = true), persist = { error("disk failure") })
        assertTrue(runCatching { gate.resumeHuman() }.isFailure); assertTrue(gate.policy.value.emergencyStopped)
        val revocation = OperationGate(persist = { error("disk failure") }); runCatching { revocation.setFeature(ManagedFeature.CLOUD, false) }
        assertTrue(runCatching { revocation.requireAllowed(ManagedFeature.CLOUD) }.isFailure)
    }
}
