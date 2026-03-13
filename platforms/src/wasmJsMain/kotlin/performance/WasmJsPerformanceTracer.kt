package performance

/**
 * KAN-15: WasmJs stub — logs traces to browser console.
 */
class WasmJsPerformanceTracer : IPerformanceTracer {

    override fun startTrace(name: String): TraceHandle {
        println("[Perf] Trace started: $name")
        return TraceHandle(name)
    }

    override fun putMetric(trace: TraceHandle, name: String, value: Long) {
        println("[Perf] ${trace.name} metric: $name=$value")
    }

    override fun putAttribute(trace: TraceHandle, name: String, value: String) {
        println("[Perf] ${trace.name} attr: $name=$value")
    }

    override fun stopTrace(trace: TraceHandle) {
        println("[Perf] Trace stopped: ${trace.name}")
    }
}

actual fun createPerformanceTracer(): IPerformanceTracer = WasmJsPerformanceTracer()
