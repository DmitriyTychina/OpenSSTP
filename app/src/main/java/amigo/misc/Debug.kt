package com.app.amigo.misc

import com.app.amigo.ControlClientVPN
import com.app.amigo.unit.DataUnit
import org.chromium.base.Log
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KFunction


internal class DataUnitParsingError : Error("Failed to parse data unit")

internal class SuicideException : Exception("Kill this client as intended")

internal class Ticker(private val key:String, private val logStream: BufferedOutputStream) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
    private var lastTick = System.nanoTime()

    internal fun tick(event: String, value: String="NULL") {
        val currentTick = System.nanoTime()
        val diff = currentTick - lastTick
        lastTick = currentTick

        "TICK, $key, ${dateFormat.format(Date())}, $diff, $event, $value\n".also {
            logStream.write(it.toByteArray())
        }
    }
}

internal fun ControlClientVPN.inform(message: String, cause: Throwable?) {
    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    var printing = "[$currentTime] $message"
    cause?.also {
        printing += ":\n"
        val trace =
            it::class.simpleName + "\n" + it.message + "\n" + it.stackTrace.joinToString("\n")
        printing += trace
    }
    printing += "\n"
    Log.e("@!@.inform :", printing)

    logStream?.write(printing.toByteArray())
}

internal fun ControlClientVPN.informDataUnitParsingError(unit: DataUnit<*>, cause: DataUnitParsingError) {
    inform("Failed to parse ${unit::class.simpleName}", cause)
}

internal fun ControlClientVPN.informTimerOver(where: KFunction<*>) {
    inform("The timer was over: ${where.name}", null)
}

internal fun ControlClientVPN.informCounterExhausted(where: KFunction<*>) {
    inform("The counter was exhausted: ${where.name}", null)
}

internal fun ControlClientVPN.informOptionRejected(option: DataUnit<*>) {
    inform("${option::class.simpleName} was rejected", null)
}

internal fun ControlClientVPN.informInvalidUnit(where: KFunction<*>) {
    inform("Received an invalid unit: ${where.name}", null)
}

internal fun ControlClientVPN.informAuthenticationFailed(where: KFunction<*>) {
    inform("Failed to be authenticated: ${where.name}", null)
}

internal fun ControlClientVPN.informReceivedCallDisconnect(where: KFunction<*>) {
    inform("Received a Call Disconnect: ${where.name}", null)
}

internal fun ControlClientVPN.informReceivedCallAbort(where: KFunction<*>) {
    inform("Received a Call Abort: ${where.name}", null)
}
