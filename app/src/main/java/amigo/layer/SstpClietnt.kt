package com.app.amigo.layer

import android.util.Log
import com.app.amigo.ControlClient
import com.app.amigo.enumStateVPN
import com.app.amigo.misc.*
import com.app.amigo.negotiator.*
import com.app.amigo.unit.MessageType
import com.app.amigo.unit.PPP_HEADER
import com.app.amigo.unit.PacketType


internal class SstpClient(parent: ControlClient) : Client(parent) {
    internal val negotiationTimer = Timer(60_000L)
    internal val negotiationCounter = Counter(3)
    internal val echoTimer = Timer(10_000L)
    internal val echoCounter = Counter(1)

    private fun proceedRequestSent() {
        if (negotiationTimer.isOver) {
            sendCallConnectRequest()
            return
        }

        if (!challengeWholePacket()) return

        if (PacketType.resolve(incomingBuffer.getShort()) != PacketType.CONTROL) {
            parent.informInvalidUnit(::proceedRequestSent)
            status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
            return
        }

        incomingBuffer.move(2)

        when (MessageType.resolve(incomingBuffer.getShort())) {
            MessageType.CALL_CONNECT_ACK -> receiveCallConnectAck()

            MessageType.CALL_CONNECT_NAK -> {
                parent.inform("Received Call Connect Ack", null)
                status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
                return
            }

            MessageType.CALL_DISCONNECT -> {
                parent.informReceivedCallDisconnect(::proceedRequestSent)
                status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_2
            }

            MessageType.CALL_ABORT -> {
                parent.informReceivedCallAbort(::proceedRequestSent)
                status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_2
            }

            else -> {
                status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
                return
            }
        }

        incomingBuffer.forget()
    }

    private fun proceedAckReceived() {
        if (negotiationTimer.isOver) {
            parent.informTimerOver(::proceedAckReceived)
            status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
            return
        }

        if (status.ppp == PppStatus.NEGOTIATE_IPCP || status.ppp == PppStatus.NETWORK) {
            sendCallConnected()
            parent.stateAndSettings.vpn_state = enumStateVPN.VPN_CONNECTED
            status.sstp = SstpStatus.CLIENT_CALL_CONNECTED
            echoTimer.reset()
            return
        }

        if (!challengeWholePacket()) return

        when (PacketType.resolve(incomingBuffer.getShort())) {
            PacketType.DATA -> {
                readAsData()
            }

            PacketType.CONTROL -> {
                incomingBuffer.move(2)

                when (MessageType.resolve(incomingBuffer.getShort())) {
                    MessageType.CALL_DISCONNECT -> {
                        parent.informReceivedCallDisconnect(::proceedAckReceived)
                        status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_2
                    }

                    MessageType.CALL_ABORT -> {
                        parent.informReceivedCallAbort(::proceedAckReceived)
                        status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_2
                    }

                    else -> {
                        parent.informInvalidUnit(::proceedAckReceived)
                        status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
                        return
                    }
                }

                incomingBuffer.forget()
            }

            null -> {
                parent.informInvalidUnit(::proceedAckReceived)
                status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
            }
        }
    }

    private fun proceedConnected() {
        if (echoTimer.isOver) {
            sendEchoRequest()
            return
        }

        if (!challengeWholePacket()) return
        echoTimer.reset()
        echoCounter.reset()

        when (PacketType.resolve(incomingBuffer.getShort())) {
            PacketType.DATA -> {
                readAsData()
            }

            PacketType.CONTROL -> {
                incomingBuffer.move(2)

                when (MessageType.resolve(incomingBuffer.getShort())) {
                    MessageType.CALL_DISCONNECT -> {
                        parent.informReceivedCallDisconnect(::proceedConnected)
                        status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_2
                    }

                    MessageType.CALL_ABORT -> {
                        parent.informReceivedCallAbort(::proceedConnected)
                        status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_2
                    }

                    MessageType.ECHO_REQUEST -> receiveEchoRequest()

                    MessageType.ECHO_RESPONSE -> receiveEchoResponse()

                    else -> {
                        parent.informInvalidUnit(::proceedConnected)
                        status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
                        return
                    }
                }

                incomingBuffer.forget()
            }

            null -> {
                parent.informInvalidUnit(::proceedConnected)
                status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
            }
        }
    }

    val TAG = "@!@SstpClient"
    override fun proceed() {
        when (status.sstp) {
            SstpStatus.CLIENT_CALL_DISCONNECTED -> {
                Log.e(TAG, "*****status.sstp0: " + status.sstp)
                Log.e(TAG, "*****status.sstp1: Подключаемся...")
//                parent.sslTerminal.release()//**del?
                status_sstp_old = status.sstp
                Log.e(TAG, "*****status.sstp2: Подключаемся...")
                parent.sslTerminal.initializeSocket()
                Log.e(TAG, "*****status.sstp3: Подключаемся...")
                sendCallConnectRequest()
                Log.e(TAG, "*****status.sstp4: Подключаемся...")
                status.sstp = SstpStatus.CLIENT_CONNECT_REQUEST_SENT
                Log.e(TAG, "*****status.sstp5: Подключаемся...")
                parent.vpnService.helper.updateNotification("Подключаемся...")
                Log.e(TAG, "*****status.sstp6: Подключаемся...")
            }

            SstpStatus.CLIENT_CONNECT_REQUEST_SENT -> {
                proceedRequestSent()
                Log.e(TAG, "*****status.sstp7: " + status.sstp)
            }

            SstpStatus.CLIENT_CONNECT_ACK_RECEIVED -> {
                proceedAckReceived()
                Log.e(TAG, "*****status.sstp8: " + status.sstp)
            }

            SstpStatus.CLIENT_CALL_CONNECTED -> {
                if (status_sstp_old != status.sstp) {
                    parent.vpnService.helper.updateNotification("Подключено")
                    status_sstp_old = status.sstp
                    Log.e(TAG, "*****status.sstp9: " + status.sstp)
                }
                proceedConnected()
            }

//        SstpStatus.CALL_ABORT_IN_PROGRESS_1,
//        SstpStatus.CALL_ABORT_IN_PROGRESS_2,
//        SstpStatus.CALL_DISCONNECT_IN_PROGRESS_1,
//        SstpStatus.CALL_DISCONNECT_IN_PROGRESS_2,
            else -> {
                status_sstp_old = status.sstp
                sendLastGreeting()
                parent.vpnService.helper.updateNotification("Обрыв соединения")
                parent.stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTED
                parent.run()
                Log.e(TAG, "*****status.sstp10: " + status.sstp)
            }
        }
    }

    private fun readAsData() {
        incomingBuffer.pppLimit = incomingBuffer.getShort().toInt() + incomingBuffer.position() - 4

        if (incomingBuffer.getShort() != PPP_HEADER) {
            parent.inform("Received a non-PPP payload", null)
            status.sstp = SstpStatus.CALL_ABORT_IN_PROGRESS_1
        }
    }
}
