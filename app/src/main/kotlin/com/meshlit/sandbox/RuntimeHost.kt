package com.meshlit.sandbox

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.meshlit.core.sandbox.*
import com.meshlit.core.net.NetworkDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Optional tooling runtime; independent from RunAnywhere inference. */
class RuntimeHost(private val context: Context) {
    private val prefs = context.getSharedPreferences("meshlit_optional_runtime", Context.MODE_PRIVATE)
    val workspace = File(context.filesDir, "runtime/workspace").apply { mkdirs() }
    val artifacts = ArtifactStore(File(context.filesDir, "runtime/artifacts"))
    val diagnostics = NetworkDiagnostics()
    val vm = QemuVm(workspace)
    private val lock = Mutex()
    @Volatile var lastError: String? = null
        private set
    @Volatile private var sessionStartedAt = 0L

    fun config(): RuntimeConfig = RuntimeConfig(
        mode = runCatching { RuntimeMode.valueOf(prefs.getString("mode", "APP")!!) }.getOrDefault(RuntimeMode.APP),
        executable = prefs.getString("executable", "").orEmpty(), rootfs = prefs.getString("rootfs", "").orEmpty(),
        sshPort = prefs.getInt("sshPort", 2222), sshUser = prefs.getString("sshUser", "meshlit").orEmpty(),
        sshIdentity = prefs.getString("identity", "").orEmpty(), sshKnownHosts = prefs.getString("knownHosts", "").orEmpty(),
    )

    fun configure(config: RuntimeConfig) {
        prefs.edit().putString("mode", config.mode.name).putString("executable", config.executable)
            .putString("rootfs", config.rootfs).putInt("sshPort", config.sshPort)
            .putString("sshUser", config.sshUser).putString("identity", config.sshIdentity)
            .putString("knownHosts", config.sshKnownHosts).apply()
    }

    fun allowAgentVm(): Boolean = prefs.getBoolean("allowAgentVm", false)
    fun setAllowAgentVm(allowed: Boolean) { prefs.edit().putBoolean("allowAgentVm", allowed).apply() }

    fun vmConfig(): VmConfig = VmConfig(
        executable = prefs.getString("vmBinary", "").orEmpty(), disk = prefs.getString("vmDisk", "").orEmpty(),
        architecture = runCatching { GuestArchitecture.valueOf(prefs.getString("vmArch", "AARCH64")!!) }
            .getOrDefault(GuestArchitecture.AARCH64),
        kernel = prefs.getString("vmKernel", "").orEmpty(), initrd = prefs.getString("vmInitrd", "").orEmpty(),
        memoryMb = prefs.getInt("vmMemory", 512), cpus = prefs.getInt("vmCpus", 1),
        sshPort = prefs.getInt("vmSshPort", 2222), vncDisplay = prefs.getInt("vmVncDisplay", 1),
        enableDesktop = prefs.getBoolean("vmDesktop", false),
    )

    fun configureVm(config: VmConfig) {
        require(vm.state == VmState.STOPPED || vm.state == VmState.FAILED) { "Stop the VM before reconfiguring" }
        config.argv() // Reject unavailable artifacts and invalid limits before saving.
        prefs.edit().putString("vmBinary", config.executable).putString("vmDisk", config.disk)
            .putString("vmArch", config.architecture.name).putString("vmKernel", config.kernel)
            .putString("vmInitrd", config.initrd).putInt("vmMemory", config.memoryMb)
            .putInt("vmCpus", config.cpus).putInt("vmSshPort", config.sshPort)
            .putInt("vmVncDisplay", config.vncDisplay).putBoolean("vmDesktop", config.enableDesktop).apply()
    }

    suspend fun execute(argv: List<String>, rootConsent: Boolean): CommandResult {
        val config = config()
        if (config.mode == RuntimeMode.VM_SSH) require(vm.state == VmState.SSH_READY) { "Start the VM and wait for SSH first" }
        val plan = RuntimePlanner(workspace).plan(config, argv, rootConsent)
        return ProcessRunner().execute(plan)
    }

    suspend fun executeGuest(argv: List<String>): CommandResult {
        val config = config()
        require(config.mode == RuntimeMode.VM_SSH && vm.state == VmState.SSH_READY) {
            "Guest SSH is not configured and ready"
        }
        return ProcessRunner().execute(RuntimePlanner(workspace).plan(config, argv))
    }

    suspend fun startVm(agentRequested: Boolean = false): VmState = lock.withLock {
        require(!agentRequested || allowAgentVm()) { "User has not enabled agent VM activation" }
        if (vm.state == VmState.RUNNING || vm.state == VmState.SSH_READY) return@withLock vm.state
        val config = vmConfig()
        config.argv()
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        require(!memory.lowMemory && memory.availMem > (config.memoryMb + 256L) * 1024 * 1024) {
            "Insufficient available RAM for VM plus 256 MiB host headroom"
        }
        try {
            // Android may reject a background FGS start; propagate that failure.
            ContextCompat.startForegroundService(context, Intent(context, RuntimeForegroundService::class.java))
            withContext(Dispatchers.IO) { vm.stop(); vm.start(config) }
            sessionStartedAt = System.currentTimeMillis()
            lastError = null
            vm.state
        } catch (error: Exception) {
            vm.stop()
            context.stopService(Intent(context, RuntimeForegroundService::class.java))
            lastError = "VM startup failed: ${error.javaClass.simpleName}"
            throw error
        }
    }

    suspend fun stopVm() = lock.withLock {
        withContext(Dispatchers.IO) { vm.stop() }
        context.stopService(Intent(context, RuntimeForegroundService::class.java))
    }

    suspend fun maintainVm(): Boolean = lock.withLock { withContext(Dispatchers.IO) {
        vm.consoleTail()
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val thermalCritical = if (android.os.Build.VERSION.SDK_INT >= 29) {
            (context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).currentThermalStatus >=
                android.os.PowerManager.THERMAL_STATUS_SEVERE
        } else false
        if (vm.state == VmState.FAILED || memory.lowMemory || thermalCritical ||
            System.currentTimeMillis() - sessionStartedAt > 30L * 60 * 1000) {
            lastError = "VM stopped: failure, resource pressure or 30-minute session limit"
            vm.stop()
            false
        } else vm.state != VmState.STOPPED
    } }

    fun onServiceDestroyed() { vm.stop() }
}
