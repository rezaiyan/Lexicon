package performance

/**
 * KAN-15: Cross-platform performance tracing interface.
 * Provides custom trace tracking for key user flows.
 */
interface IPerformanceTracer {

    /** Start a named trace. Returns a trace handle for stopping. */
    fun startTrace(name: String): TraceHandle

    /** Add a metric to a running trace. */
    fun putMetric(trace: TraceHandle, name: String, value: Long)

    /** Add an attribute to a running trace. */
    fun putAttribute(trace: TraceHandle, name: String, value: String)

    /** Stop a running trace. */
    fun stopTrace(trace: TraceHandle)
}

/** Opaque handle representing a running performance trace. */
class TraceHandle(val name: String, val platformTrace: Any? = null)

/**
 * Factory function to create platform-specific performance tracer.
 */
expect fun createPerformanceTracer(): IPerformanceTracer
