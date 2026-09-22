package components

/**
 * Liquid glass effects rely on RenderEffect/RuntimeShader backends that aren't available on
 * every OS version. Gate glass UI behind this check and fall back to a solid surface otherwise.
 */
expect fun isLiquidGlassSupported(): Boolean
