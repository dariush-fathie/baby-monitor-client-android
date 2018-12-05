package co.netguru.baby.monitor.client.feature.communication.websocket

import co.netguru.baby.monitor.client.feature.common.RunsInBackground
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ClientsHandler(
        private val listener: ConnectionListener
) {

    var webSocketClients = mutableMapOf<String, CustomWebSocketClient>()

    @RunsInBackground
    fun addClient(address: String) = Single.fromCallable {
        connect(address)
        address
    }

    private fun onAvailabilityChange(client: CustomWebSocketClient, status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.UNKNOWN -> Unit
            ConnectionStatus.CONNECTED -> listener.onClientConnected(client)
            ConnectionStatus.DISCONNECTED -> retryConnection(client)
        }
    }

    private fun onMessageReceived(client: CustomWebSocketClient, message: String?) {
        val jsonObject = JSONObject(message)
        //TODO handle received action 4.12.2018
    }

    private fun retryConnection(client: CustomWebSocketClient) =
            Completable.timer(RETRY_SECONDS_DELAY, TimeUnit.SECONDS)
                    .andThen {
                        connect(client.address)
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(
                            onComplete = { Timber.i("connected to ${client.address}") }
                    )


    fun onDestroy() {
        for (client in webSocketClients) {
            client.value.onDestroy()
        }
    }

    private fun connect(address: String) {
        Timber.i("connecting $address...")
        webSocketClients[address] = CustomWebSocketClient(
                address,
                onAvailabilityChange = this::onAvailabilityChange,
                onMessageReceived = this::onMessageReceived
        )
    }

    fun getClient(address: String?) = webSocketClients[address]

    interface ConnectionListener {
        fun onClientConnected(client: CustomWebSocketClient)
    }

    companion object {
        private const val RETRY_SECONDS_DELAY = 3L
    }
}