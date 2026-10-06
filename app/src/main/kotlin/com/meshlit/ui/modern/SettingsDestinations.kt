package com.meshlit.ui.modern

/** One catalog drives navigation and search. No row is allowed a no-op action. */
data class SettingsDestination(val id:String,val title:String,val description:String,val advanced:Boolean=false,
    val keywords:String="")
object SettingsDestinations {
    val all=listOf(
        SettingsDestination("appearance","Appearance","Dynamic colors, accents, light and dark mode",keywords="theme display wallpaper font motion palette"),
        SettingsDestination("models","Models and downloads","Import files, manage downloads and load local models",keywords="hugging face token storage GGUF RAM"),
        SettingsDestination("media","Camera, vision and audio","Real image input, microphone and existing SDK media paths",keywords="CCTV webcam phone camera microphone WAV speech VLM STT TTS"),
        SettingsDestination("configuration","Configuration profiles","Export, review and apply portable device settings",keywords="import backup default custom declarative ansible terraform pulumi IaC"),
        SettingsDestination("behavior","Custom local model behavior","Custom weights and your own local instructions",keywords="uncensored jailbreak system prompt offline"),
        SettingsDestination("training","Fine-tuning","Real Soup LoRA/QLoRA jobs on a paired training host",keywords="train adapter dataset quantization stream layers compact GPU"),
        SettingsDestination("router","Model router","Scenario rules, model chains and answer comparisons",keywords="multi model tasks coding reasoning routing pipeline ensemble"),
        SettingsDestination("providers","Online providers","OpenAI, Claude, DeepSeek, Qwen, Gemini and compatible APIs",keywords="online cloud key token price cost Hugging Face"),
        SettingsDestination("power","Power and costs","Battery readings, download policies and electricity estimates",keywords="meter current energy charging expense thermal"),
        SettingsDestination("external","External devices and OTG","USB discovery, permissions and removable storage",keywords="gpu USB hardware peripherals drives NAS keyboard camera"),
        SettingsDestination("acceleration","Acceleration","Actual backend availability, CPU limits and platform roadmap",keywords="CUDA Vulkan OpenCL OpenGL Metal ROCm QNN CANN Intel NVIDIA AMD Huawei"),
        SettingsDestination("agents","Agent management","Delegated permissions, real jobs and registered tools",keywords="autonomous scopes tasks MCP"),
        SettingsDestination("ssh","SSH connections","Pinned host-key remote commands to owner-approved devices",keywords="server NAS terminal external inter device"),
        SettingsDestination("firewall","Network rules","Persisted Meshlit listener firewall and network status",keywords="ports security IP allow deny"),
        SettingsDestination("device","Device","Device profile, name and hardware capabilities",keywords="role chipset gpu peripherals"),
        SettingsDestination("tasks","Task manager","Plan tasks, track real jobs, bulk finish and stop operations",keywords="todo priorities tags due subtasks queue retry cancel agent"),
        SettingsDestination("ide","Code workspace","Offline source editor, files, syntax highlighting and search",keywords="IDE VS Code programming Kotlin Python JavaScript JSON develop"),
        SettingsDestination("permissions","App permissions","Optional setup, runtime grants and Android accessibility",keywords="first launch camera microphone location nearby Bluetooth security"),
        SettingsDestination("files","Files and storage","Browse app files and user-granted storage volumes",keywords="SAF copy move share export folders"),
        SettingsDestination("termux","Termux integration","Probe an installed shell, manage delegation and view its audit",true,"terminal commands Linux tools networking"),
        SettingsDestination("notifications","Notifications","System notification permission and channels",keywords="alert sound silent"),
        SettingsDestination("monitor","Monitoring and layer pipeline","Memory, thermal status and approved model workers",keywords="shard cluster battery CPU"),
        SettingsDestination("logs","Logs and export","Search logs; filter severity and source; export TXT or JSONL",keywords="debug diagnostics error trace download"),
        SettingsDestination("network","Network and pairing","Enroll devices, choose transports and approve layer workers",keywords="QR SSH Bluetooth web NAS hive pairing discovery"),
        SettingsDestination("peers","Forwarding peers","Manage peers for the existing inference router",true,"network IP routing"),
        SettingsDestination("runtime","Linux runtime and sandbox","VM configuration and agent activation consent",true,"root rootless terminal VNC desktop QEMU"),
        SettingsDestination("crawler","Web crawler","Configure the optional Crawl4AI companion",true,"search web endpoint token browser"),
        SettingsDestination("openclaw","OpenClaw and autonomy","Gateway agent, phone model sharing and Android delegation",keywords="agent autonomous OS accessibility local provider pairing"),
        SettingsDestination("automation","Android automation","Accessibility automation and app allowlists",true,"permissions security agents"),
        SettingsDestination("hooks","Agent hooks","Enable and edit user-authored lifecycle scripts",true,"scripts tools"),
        SettingsDestination("about","About and availability","Build information and feature implementation status",keywords="licenses version help")
    )
    fun search(query:String,advanced:Boolean)=all.filter{ (advanced || !it.advanced) &&
        query.trim().split(Regex("\\s+")).filter{it.isNotBlank()}.all { word ->
            "${it.title} ${it.description} ${it.keywords}".contains(word,true)
        }
    }
}
