package performance

import platform.Foundation.NSLog

/**
 * KAN-15: iOS stub — logs traces to console.
 * Firebase Performance for iOS is handled by the Swift layer.
 */
class IOSPerformanceTracer : IPerformanceTracer {

    override fun startTrace(name: String): TraceHandle {
        NSLog("[Perf] Trace started: $name")
        return TraceHandle(name)
    }

    override fun putMetric(trace: TraceHandle, name: String, value: Long) {
        NSLog("[Perf] ${trace.name} metric: $name=$value")
    }

    override fun putAttribute(trace: TraceHandle, name: String, value: String) {
        NSLog("[Perf] ${trace.name} attr: $name=$value")
    }

    override fun stopTrace(trace: TraceHandle) {
        NSLog("[Perf] Trace stopped: ${trace.name}")
    }
}

actual fun createPerformanceTracer(): IPerformanceTracer = IOSPerformanceTracer()
