package co.netguru.baby.monitor.client.feature.client.home.livecamera

import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.netguru.baby.monitor.client.R
import co.netguru.baby.monitor.client.data.server.ConfigurationRepository
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_client_live_camera.*
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import javax.inject.Inject

class ClientLiveCameraFragment : DaggerFragment() {

    @Inject
    internal lateinit var configurationRepository: ConfigurationRepository

    private lateinit var libvlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private val liveCameraOptions by lazy { LiveCameraOptions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libvlc = LibVLC(requireContext(), liveCameraOptions.provideOptions())
        mediaPlayer = MediaPlayer(libvlc)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_client_live_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareRtspPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
    }

    private fun prepareRtspPlayer() {
        // Seting up video output
        with(mediaPlayer.vlcVout) {
            setVideoView(clientLiveCameraPreviewSv)
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            with(displayMetrics) {
                setWindowSize(widthPixels, heightPixels)
            }
            attachViews()
        }

        with(mediaPlayer) {
            this.media = Media(
                    libvlc, Uri.parse(configurationRepository.serverAddress)
            ).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=150")
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
                addOption(":fullscreen")
            }
            play()
        }
    }

    private fun releasePlayer() = with(mediaPlayer) {
        stop()
        vlcVout.detachViews()
        libvlc.release()
    }

    companion object {
        fun newInstance() = ClientLiveCameraFragment()
    }
}
