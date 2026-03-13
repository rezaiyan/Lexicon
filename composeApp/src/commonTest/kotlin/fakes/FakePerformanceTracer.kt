package fakes

import performance.IPerformanceTracer
import performance.TraceHandle

class FakePerformanceTracer : IPerformanceTracer {
    val startedTraces = mutableListOf<String>()
    val stoppedTraces = mutableListOf<String>()
    val metrics = mutableMapOf<String, MutableMap<String, Long>>()
    val attributes = mutableMapOf<String, MutableMap<String, String>>()

    override fun startTrace(name: String): TraceHandle {
        startedTraces += name
        return TraceHandle(name)
    }

    override fun putMetric(trace: TraceHandle, name: String, value: Long) {
        metrics.getOrPut(trace.name) { mutableMapOf() }[name] = value
    }

    override fun putAttribute(trace: TraceHandle, name: String, value: String) {
        attributes.getOrPut(trace.name) { mutableMapOf() }[name] = value
    }

    override fun stopTrace(trace: TraceHandle) {
        stoppedTraces += trace.name
    }
}
