package com.bilirkisi.proje.fragments.one_to_one_chat

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.ChatFragmentBinding
import com.bilirkisi.proje.models.*
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.CLICKED_USER
import com.bilirkisi.proje.util.LOGGED_USER
import com.bilirkisi.proje.util.eventbus_events.PermissionEvent
import com.bilirkisi.proje.util.eventbus_events.UpdateRecycleItemEvent
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.attachment_layout.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.*


const val SELECT_CHAT_IMAGE_REQUEST = 3
const val CHOOSE_FILE_REQUEST = 4


class ChatFragment : Fragment() {

    private var recordStart = 0L
    private var recordDuration = 0L

    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var recorder: MediaRecorder? = null
    var isRecording = false //şimdi kayıt olsun ya da olmasın
    var isRecord = true //metin mesajı mı yoksa kayıt mı
    private lateinit var loggedUser: User
    private lateinit var clickedUser: User


    private var messageList = mutableListOf<Message>()
    lateinit var binding: ChatFragmentBinding
    private val adapter: ChatAdapter by lazy {
        ChatAdapter(context, object : MessageClickListener {
            override fun onMessageClick(position: Int, message: Message) {
          //tıklanan öğe tam ekranda görüntü açıksa, yakınlaştırmak için kıstırma
                if (message.type == 1.0) {

                    binding.fullSizeImageView.visibility = View.VISIBLE

                    StfalconImageViewer.Builder<MyImage>(
                        activity!!,
                        listOf(MyImage((message as ImageMessage).uri!!)),
                        ImageLoader<MyImage> { imageView, myImage ->
                            Glide.with(activity!!)
                                .load(myImage.url)
                                .apply(RequestOptions().error(R.drawable.ic_broken_image_black_24dp))
                                .into(imageView)
                        })
                        .withDismissListener { binding.fullSizeImageView.visibility = View.GONE }
                        .show()


                }
                // kullanıcının dosyayı indirmek istediğini onaylayan iletişim kutusunu göster ve ardından indirmeye veya iptal etmeye devam et
                else if (message.type == 2.0) {
                    // indirmemiz gereken dosya mesajı
                    val dialogBuilder = context?.let { it1 -> AlertDialog.Builder(it1) }
                    dialogBuilder?.setMessage("Tıklanan dosyayı indirmek istiyor musunuz?")
                        ?.setPositiveButton(
                            "yes"
                        ) { _, _ ->
                            downloadFile(message)
                        }?.setNegativeButton("cancel", null)?.show()

                } else if (message.type == 3.0) {
                    adapter.notifyDataSetChanged()
                }
            }

        })
    }

    private fun downloadFile(message: Message) {
        //depolama iznini kontrol edin ve verilmiş olanı indirin
        Dexter.withActivity(activity!!)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    //download file
                    val downloadManager =
                        activity!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = Uri.parse((message as FileMessage).uri)
                    val request = DownloadManager.Request(uri)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        uri.lastPathSegment
                    )
                    downloadManager.enqueue(request)
                }
                // Dosyalar için İzin Gerekçesi Gösterilmelidir.
                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    //notify parent activity that permission denied to show toast for manual permission giving
                    showSnackBar()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    //notify parent activity that permission denied to show toast for manual permission giving
                    EventBus.getDefault().post(PermissionEvent())
                }
            }).check()
    }


    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var viewModel: ChatViewModel
    private lateinit var viewModeldFactory: ChatViewModelFactory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        binding = DataBindingUtil.inflate(inflater, R.layout.chat_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        getActivity()?.navView?.visibility = View.GONE
        //alt sayfayı ayarla
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        // kayıt görünümünü ayarla
        handleRecord()


        // kayıtlı kullanıcıyı paylaşılan tercihlerden alın
        val mPrefs: SharedPreferences = activity!!.getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        loggedUser = gson.fromJson(json, User::class.java)

        //Kişi bölümünden alıcı verilerini al(NOTE:kullanıcı bilgileri FCM-NOTIFICATION den çekiliyorsa yanlızca:  id,username)
        clickedUser = gson.fromJson(arguments?.getString(CLICKED_USER), User::class.java)


        activity?.title = " ${clickedUser.username} ile sohbet"


        if (clickedUser.uid != null) {
            viewModeldFactory = ChatViewModelFactory(loggedUser.uid, clickedUser.uid.toString())
            viewModel =
                ViewModelProviders.of(this, viewModeldFactory).get(ChatViewModel::class.java)
        }


        // Yazılım klavyesi gösterildiğinde düzenleri yukarı taşı
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        // klavyede mesaj gönder tamam tıklayın
        binding.messageEditText.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }


        binding.recycler.adapter = adapter

        // geri dönüştürücünün göstermesi için mesaj listesi ilet
        viewModel.loadMessages().observe(viewLifecycleOwner, Observer { mMessagesList ->
            messageList = mMessagesList as MutableList<Message>
            ChatAdapter.messageList = messageList
            adapter.submitList(mMessagesList)
            // geri dönüşüm cihazındaki son öğelere kaydır (son mesajlar)
            binding.recycler.scrollToPosition(mMessagesList.size - 1)

        })


        // alt sayfadaki öğelerin tıklamasını işle
        binding.bottomSheet.sendPictureButton.setOnClickListener {
            selectFromGallery()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.bottomSheet.sendFileButton.setOnClickListener {
            openFileChooser()
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        // alt sayfayı göster
        binding.attachmentImageView.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        // yeni kayıt yüklendiğinde gözlemle
        viewModel.chatRecordDownloadUriMutableLiveData.observe(
            viewLifecycleOwner,
            Observer { recordUri ->
                println("observer called")
                viewModel.sendMessage(
                    RecordMessage(
                        AuthUtil.getAuthId(),
                        Timestamp(Date()),
                        3.0,
                        clickedUser.uid,
                        loggedUser.username,
                        recordDuration.toString(),
                        recordUri.toString(),
                        null,
                        null
                    )
                )
            })
    }


    private fun handleRecord() {


        // metin mesajına bağlı olarak fab simgesini değiştirin empty_box veya değil
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    // empty_box metin mesajı
                    binding.recordFab.setImageResource(R.drawable.mic)
                    isRecord = true
                } else {
                    binding.recordFab.setImageResource(R.drawable.sendicon)
                    isRecord = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })


        binding.recordFab.setOnClickListener {
            if (isRecord) {
                // mesajı kaydet
                if (isRecording) {
                    // boyutu ve rengi veya düğmeyi değiştirin, böylece kullanıcı kayıt işleminin bittiğini bilir
                    val regainer = AnimatorInflater.loadAnimator(
                        context,
                        R.animator.regain_size
                    ) as AnimatorSet
                    regainer.setTarget(binding.recordFab)
                    regainer.start()
                    binding.recordFab.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#b39ddb"))
                    // kaydı durdur ve kaydı yükle
                    stopRecording()
                    showPlaceholderRecord()
                    viewModel.uploadRecord("${activity!!.externalCacheDir?.absolutePath}/audiorecord.3gp")
                    Toast.makeText(context, "Finished recording", Toast.LENGTH_SHORT).show()
                    isRecording = !isRecording

                } else {

                    Dexter.withActivity(activity)
                        .withPermission(Manifest.permission.RECORD_AUDIO)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                                // düğmenin boyutunu ve rengini değiştirin , böylece kullanıcı kaydını bilecek
                                val increaser = AnimatorInflater.loadAnimator(
                                    context,
                                    R.animator.increase_size
                                ) as AnimatorSet
                                increaser.setTarget(binding.recordFab)
                                increaser.start()
                                binding.recordFab.backgroundTintList =
                                    ColorStateList.valueOf(Color.parseColor("#EE4B4B"))
                                // kayda başla
                                startRecording()
                                Toast.makeText(context, "Recording", Toast.LENGTH_SHORT).show()
                                isRecording = !isRecording
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: com.karumi.dexter.listener.PermissionRequest?,
                                token: PermissionToken?
                            ) {
                                token?.continuePermissionRequest()
                                //Bu aktiviteye izin verilmediğini göster , manuel izin vermek için
                                showSnackBar()
                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                                //Bu aktiviteye izin verilmediğini göster , manuel izin vermek için
                                showSnackBar()
                            }
                        }).check()

                }

            } else {
                //metin mesajı
                sendMessage()
            }
        }


    }


    private fun sendMessage() {
        if (binding.messageEditText.text.isEmpty()) {
            Toast.makeText(context, getString(R.string.empty_message), Toast.LENGTH_LONG).show()
            return
        }
        viewModel.sendMessage(
            TextMessage(
                loggedUser.uid,
                Timestamp(Date()),
                0.0,
                clickedUser.uid,
                loggedUser.username,
                binding.messageEditText.text.toString()
            )
        )

        binding.messageEditText.setText("")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // dosya sonucunu seçin
        if (requestCode == CHOOSE_FILE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            val filePath = data.data

            showPlaceholderFile(filePath)

            // sohbet dosyası yüklendi.  uri'yi mesajla birlikte saklayın
            viewModel.uploadChatFileByUri(filePath).observe(this, Observer { chatFileMap ->
                viewModel.sendMessage(
                    FileMessage(
                        loggedUser.uid,
                        Timestamp(Date()),
                        2.0,
                        clickedUser.uid,
                        loggedUser.username,
                        chatFileMap["fileName"].toString(),
                        chatFileMap["downloadUri"].toString()
                    )
                )

            })

        }

        // resim sonucunu seç
        if (requestCode == SELECT_CHAT_IMAGE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            // resim yüklenene kadar geri dönüştürücudaki resimli sahte öğeyi göster
            showPlaceholderPhoto(data.data)

            // resmi firebase deposuna yükle
            viewModel.uploadChatImageByUri(data.data)
                .observe(this, Observer { uploadedChatImageUri ->
                    // sohbet resmi yüklendi şimdi uri'yi mesajla birlikte saklayın
                    viewModel.sendMessage(
                        ImageMessage(
                            loggedUser.uid,
                            Timestamp(Date()),
                            1.0,
                            clickedUser.uid,
                            loggedUser.username,
                            uploadedChatImageUri.toString()
                        )
                    )
                })

        }


    }


    private fun openFileChooser() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        try {
            startActivityForResult(i, CHOOSE_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Bu cihazda uygun bir dosya yöneticisi bulunamadı",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showPlaceholderPhoto(data: Uri?) {
        messageList.add(
            ImageMessage(
                AuthUtil.getAuthId(),
                null,
                1.0,
                clickedUser.uid,
                loggedUser.username,
                data.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }


    private fun showPlaceholderRecord() {
        //yüklemeleri kaydederken ilerleme çubuğuyla göster
        messageList.add(
            RecordMessage(
                AuthUtil.getAuthId(),
                null,
                8.0,
                null,
                null,
                null,
                null,
                null,
                null
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }


    private fun showPlaceholderFile(data: Uri?) {
        messageList.add(
            FileMessage(
                AuthUtil.getAuthId(),
                null,
                2.0,
                clickedUser.uid,
                loggedUser.username,
                data.toString(),
                data?.path.toString()
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    private fun selectFromGallery() {
        var intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Resim seç"),
            SELECT_CHAT_IMAGE_REQUEST
        )
    }

    private fun showSnackBar() {
        Snackbar.make(
            binding.coordinator,
            "Bu özelliğin çalışması için izin gerekiyor",
            Snackbar.LENGTH_LONG
        ).setAction(
            "Grant", View.OnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity!!.packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        ).show()

    }


    private fun startRecording() {

        // kaydın saklanacağı dosyanın adı
        val fileName = "${activity!!.externalCacheDir?.absolutePath}/audiorecord.3gp"

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()

            } catch (e: IOException) {
                println("ChatFragment.startRecording${e.message}")
            }

            start()
            recordStart = Date().time
        }
    }

    private fun stopRecording() {

        recorder?.apply {
            stop()
            release()
            recorder = null
        }

        recordDuration = Date().time - recordStart

    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRecycleItemEvent(event: UpdateRecycleItemEvent) {
        adapter.notifyItemChanged(event.adapterPosition)
    }
}

