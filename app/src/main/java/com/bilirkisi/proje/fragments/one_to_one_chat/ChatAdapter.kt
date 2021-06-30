package com.bilirkisi.proje.fragments.one_to_one_chat



import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.*
import com.bilirkisi.proje.models.*
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.eventbus_events.UpdateRecycleItemEvent
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import kotlin.properties.Delegates

var positionDelegate: Int by Delegates.observable(-1) { prop, old, new ->
    println("<positionDelegate>.:${old},,,,$new")
    if (old != new && old != -1)    //if old =-1 or old=new öğeyi güncelleme
        EventBus.getDefault().post(UpdateRecycleItemEvent(old))

}

class ChatAdapter(private val context: Context?, private val clickListener: MessageClickListener) :
    ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallbackMessages()) {




    companion object {
        private const val TYPE_SENT_MESSAGE = 0
        private const val TYPE_RECEIVED_MESSAGE = 1
        private const val TYPE_SENT_IMAGE_MESSAGE = 2
        private const val TYPE_RECEIVED_IMAGE_MESSAGE = 3
        private const val TYPE_SENT_FILE_MESSAGE = 4
        private const val TYPE_RECEIVED_FILE_MESSAGE = 5
        private const val TYPE_SENT_RECORD = 6
        private const val TYPE_RECEIVED_RECORD = 7
        private const val TYPE_SENT_RECORD_PLACEHOLDER = 8

        lateinit var messageList: MutableList<Message>

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {


        return when (viewType) {
            TYPE_SENT_MESSAGE -> {
                SentMessageViewHolder.from(parent)
            }
            TYPE_RECEIVED_MESSAGE -> {
                ReceivedMessageViewHolder.from(parent)
            }
            TYPE_SENT_IMAGE_MESSAGE -> {
                SentImageMessageViewHolder.from(parent)
            }
            TYPE_RECEIVED_IMAGE_MESSAGE -> {
                ReceivedImageMessageViewHolder.from(parent)
            }
            TYPE_SENT_FILE_MESSAGE -> {
                SentFileMessageViewHolder.from(parent)
            }
            TYPE_RECEIVED_FILE_MESSAGE -> {
                ReceivedFileMessageViewHolder.from(parent)
            }
            TYPE_SENT_RECORD -> {
                SentRecordViewHolder.from(parent)
            }
            TYPE_RECEIVED_RECORD -> {
                ReceivedRecordViewHolder.from(parent)
            }
            TYPE_SENT_RECORD_PLACEHOLDER -> {
                SentRecordPlaceHolderViewHolder.from(parent)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as TextMessage)
            }
            is ReceivedMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as TextMessage)
            }
            is SentImageMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as ImageMessage)
            }
            is ReceivedImageMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as ImageMessage)
            }
            is ReceivedFileMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as FileMessage)
            }
            is SentFileMessageViewHolder -> {
                holder.bind(clickListener, getItem(position) as FileMessage)
            }
            is SentRecordViewHolder -> {
                holder.bind(clickListener, getItem(position) as RecordMessage)
            }
            is ReceivedRecordViewHolder -> {
                holder.bind(clickListener, getItem(position) as RecordMessage)
            }
            is SentRecordPlaceHolderViewHolder -> {
                holder.bind(clickListener, getItem(position) as RecordMessage)
            }
            else -> throw IllegalArgumentException("Geçersiz ViewHolder")
        }
    }

    override fun getItemViewType(position: Int): Int {

        val currentMessage = getItem(position)

        if (currentMessage.from == AuthUtil.getAuthId() && currentMessage.type == 0.0) {
            return TYPE_SENT_MESSAGE
        } else if (currentMessage.from != AuthUtil.getAuthId() && currentMessage.type == 0.0) {
            return TYPE_RECEIVED_MESSAGE
        } else if (currentMessage.from == AuthUtil.getAuthId() && currentMessage.type == 1.0) {
            return TYPE_SENT_IMAGE_MESSAGE
        } else if (currentMessage.from != AuthUtil.getAuthId() && currentMessage.type == 1.0) {
            return TYPE_RECEIVED_IMAGE_MESSAGE
        } else if (currentMessage.from == AuthUtil.getAuthId() && currentMessage.type == 2.0) {
            return TYPE_SENT_FILE_MESSAGE
        } else if (currentMessage.from != AuthUtil.getAuthId() && currentMessage.type == 2.0) {
            return TYPE_RECEIVED_FILE_MESSAGE
        } else if (currentMessage.from == AuthUtil.getAuthId() && currentMessage.type == 3.0) {
            return TYPE_SENT_RECORD
        } else if (currentMessage.from != AuthUtil.getAuthId() && currentMessage.type == 3.0) {
            return TYPE_RECEIVED_RECORD
        } else if (currentMessage.type == 8.0) {
            return TYPE_SENT_RECORD_PLACEHOLDER
        } else {

            throw IllegalArgumentException("Geçersiz ItemViewType")
        }

    }


    //----------------Gönderilen Mesaj Görünümü------------
    class SentMessageViewHolder private constructor(val binding: SentMessageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: TextMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): SentMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SentMessageItemBinding.inflate(layoutInflater, parent, false)

                return SentMessageViewHolder(binding)
            }
        }


    }

    //----------------Alınan Mesaj Görünümü------------
    class ReceivedMessageViewHolder private constructor(val binding: IncomingMessageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: TextMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ReceivedMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = IncomingMessageItemBinding.inflate(layoutInflater, parent, false)

                return ReceivedMessageViewHolder(binding)
            }
        }
    }

    //----------------Gönderilen Resim Mesajı Görünümü------------
    class SentImageMessageViewHolder private constructor(val binding: SentChatImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: ImageMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): SentImageMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SentChatImageItemBinding.inflate(layoutInflater, parent, false)

                return SentImageMessageViewHolder(binding)
            }
        }
    }


    //----------------Alınan Resim Mesajı Görüntüleme ------------
    class ReceivedImageMessageViewHolder private constructor(val binding: IncomingChatImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: ImageMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()//
        }

        companion object {
            fun from(parent: ViewGroup): ReceivedImageMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = IncomingChatImageItemBinding.inflate(layoutInflater, parent, false)

                return ReceivedImageMessageViewHolder(binding)
            }
        }
    }


    //----------------Gönderilen Dosya Mesajı Görüntüleme ------------
    class SentFileMessageViewHolder private constructor(val binding: SentChatFileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: FileMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): SentFileMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SentChatFileItemBinding.inflate(layoutInflater, parent, false)

                return SentFileMessageViewHolder(binding)
            }
        }
    }


    //----------------Alınan Dosya Mesajı Görüntüleme ------------
    class ReceivedFileMessageViewHolder private constructor(val binding: IncomingChatFileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: FileMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()//
        }

        companion object {
            fun from(parent: ViewGroup): ReceivedFileMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = IncomingChatFileItemBinding.inflate(layoutInflater, parent, false)

                return ReceivedFileMessageViewHolder(binding)
            }
        }
    }


    //----------------SentRecordViewHolder------------
    class SentRecordViewHolder private constructor(val binding: SentAudioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(clickListener: MessageClickListener, item: RecordMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()


            //görünümleri sıfırla (oynatılanın dışındaki diğer kayıtları sıfırlamak için)
            val recordMessage = messageList[adapterPosition] as RecordMessage
            recordMessage.isPlaying = false

            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_black_24dp)
            binding.progressbar.max = 0
            binding.durationTextView.text = ""


            binding.playPauseImage.setOnClickListener {

                startPlaying(
                    item.uri!!,
                    adapterPosition,
                    recordMessage,
                    binding.playPauseImage,
                    binding.progressbar
                )


            }
        }

        companion object {
            fun from(parent: ViewGroup): SentRecordViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SentAudioItemBinding.inflate(layoutInflater, parent, false)

                return SentRecordViewHolder(binding)
            }
        }


    }


    //----------------SentRecordPlaceHolderViewHolder------------
    class SentRecordPlaceHolderViewHolder private constructor(val binding: SentAudioPlaceholderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(clickListener: MessageClickListener, item: Message) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()

        }

        companion object {
            fun from(parent: ViewGroup): SentRecordPlaceHolderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SentAudioPlaceholderItemBinding.inflate(layoutInflater, parent, false)

                return SentRecordPlaceHolderViewHolder(binding)
            }
        }


    }


    //----------------Kayıt Görünümü------------
    class ReceivedRecordViewHolder private constructor(val binding: ReceivedAudioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: MessageClickListener, item: RecordMessage) {
            binding.message = item
            binding.clickListener = clickListener
            binding.position = adapterPosition
            binding.executePendingBindings()


            //görünümleri sıfırla (oynatılanın dışındaki diğer kayıtları sıfırlamak için)
            val recordMessage = messageList[adapterPosition] as RecordMessage
            recordMessage.isPlaying = false

            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_black_24dp)
            binding.progressbar.max = 0
            binding.durationTextView.text = ""



            binding.playPauseImage.setOnClickListener {
                startPlaying(
                    item.uri!!,
                    adapterPosition,
                    recordMessage,
                    binding.playPauseImage,
                    binding.progressbar
                )


            }
        }

        companion object {
            fun from(parent: ViewGroup): ReceivedRecordViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ReceivedAudioItemBinding.inflate(layoutInflater, parent, false)

                return ReceivedRecordViewHolder(binding)
            }
        }
    }


}


private var player = MediaPlayer()
private lateinit var countDownTimer: CountDownTimer


private fun startPlaying(
    audioUri: String,
    adapterPosition: Int,
    recordMessage: RecordMessage,
    playPauseImage: ImageView,
    progressbar: ProgressBar
) {
//sıfırlanacak son tıklanan öğeyi güncelle
    positionDelegate = adapterPosition

    //ses indirilirken geçici yüklemeyi göster
    playPauseImage.setImageResource(R.drawable.loading_animation)

    if (recordMessage.isPlaying == null || recordMessage.isPlaying == false) {

        stopPlaying()
        recordMessage.isPlaying = false

        player.apply {
            try {
                setDataSource(audioUri)
                prepareAsync()
            } catch (e: IOException) {
                println("ChatFragment.startPlaying:prepare failed")
            }

            setOnPreparedListener {
                //medya indirildi ve oynatılacak

                recordMessage.isPlaying = true
                //kaydı çal
                start()

                //durdurmak ve kaydın ilerlemesini göstermek için görüntüyü değiştirin
                progressbar.max = player.duration
                playPauseImage.setImageResource(R.drawable.ic_stop_black_24dp)

                //kayıt oynatılırken kayıt ilerleme düğmesini göstermek için geri sayım sayacı
                countDownTimer = object : CountDownTimer(player.duration.toLong(), 50) {
                    override fun onFinish() {

                        progressbar.progress = (player.duration)
                        playPauseImage.setImageResource(R.drawable.ic_play_arrow_black_24dp)

                    }

                    override fun onTick(millisUntilFinished: Long) {

                        progressbar.progress = (player.duration.minus(millisUntilFinished)).toInt()
                    }

                }.start()
            }
        }

    } else {
        //Kayıtı durdur
        playPauseImage.setImageResource(R.drawable.ic_play_arrow_black_24dp)
        stopPlaying()
        recordMessage.isPlaying = false
        progressbar.progress = 0

    }


}

private fun stopPlaying() {
    if (::countDownTimer.isInitialized)
        countDownTimer.cancel()
    player.reset()
}


interface MessageClickListener {
    fun onMessageClick(position: Int, message: Message)
}


class DiffCallbackMessages : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.created_at == newItem.created_at
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.equals(newItem)
    }
}







