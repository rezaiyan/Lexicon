package performance

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * KAN-15: Android implementation using Firebase Performance Monitoring.
 * Automatic traces (HTTP, screen rendering, app start) are enabled by the SDK.
 * This class provides custom trace support for key user flows.
 */
class AndroidPerformanceTracer : IPerformanceTracer {

    private val firebasePerformance = FirebasePerformance.getInstance()

    override fun startTrace(name: String): TraceHandle {
        val trace = firebasePerformance.newTrace(name)
        trace.start()
        return TraceHandle(name, trace)
    }

    override fun putMetric(trace: TraceHandle, name: String, value: Long) {
        (trace.platformTrace as? Trace)?.putMetric(name, value)
    }

    override fun putAttribute(trace: TraceHandle, name: String, value: String) {
        (trace.platformTrace as? Trace)?.putAttribute(name, value)
    }

    override fun stopTrace(trace: TraceHandle) {
        (trace.platformTrace as? Trace)?.stop()
    }
}

actual fun createPerformanceTracer(): IPerformanceTracer = AndroidPerformanceTracer()
