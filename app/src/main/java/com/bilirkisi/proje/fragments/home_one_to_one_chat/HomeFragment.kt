package com.bilirkisi.proje.fragments.home_one_to_one_chat

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.HomeFragmentBinding
import com.bilirkisi.proje.models.ChatParticipant
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.services.MyFirebaseMessagingService
import com.bilirkisi.proje.ui.mainActivity.SharedViewModel
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.CLICKED_USER
import com.bilirkisi.proje.util.FirestoreUtil
import kotlinx.android.synthetic.main.activity_main.*

class HomeFragment : Fragment() {
    private var receivedRequestsCount: Int? = null
    lateinit var binding: HomeFragmentBinding
    val gson = Gson()
    private lateinit var countBadgeTextView: TextView
    private val adapter: ChatPreviewAdapter by lazy {
        ChatPreviewAdapter(ClickListener { chatParticipant ->
            //seçili kullanıcıyla sohbete git
            activity?.navView?.visibility = View.GONE
            val clickedUser = gson.toJson(chatParticipant.particpant)
            findNavController().navigate(
                R.id.action_homeFragment_to_chatFragment, bundleOf(
                    CLICKED_USER to clickedUser
                )
            )
        })
    }


    companion object {
        fun newInstance() = HomeFragment()
    }

    private lateinit var viewModel: HomeViewModel
    lateinit var sharedViewModel: SharedViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.title = "Sohbet"

        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.home_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        sharedViewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
        activity?.navView?.visibility = View.VISIBLE
        //kayıtlı kullanıcı jetonunu alın ve kullanıcı belgesine ekleyin (FCM için)
        MyFirebaseMessagingService.getInstanceId()


        //bu amaç ekstraları FCM bildirim tıklamasından geliyor, bu nedenle boş değilse belirli bir sohbete geçmemiz gerekiyor
        val senderId = activity!!.intent.getStringExtra("senderId")
        val senderName = activity!!.intent.getStringExtra("senderName")
        if (senderId != null && senderName != null) {
            val receiverUser =
                User(uid = senderId, username = senderName)
            findNavController().navigate(
                R.id.action_homeFragment_to_chatFragment, bundleOf(
                    CLICKED_USER to gson.toJson(receiverUser)
                )
            )
            val nullSting: CharSequence? = null
            activity!!.intent.putExtra("senderId", nullSting)
            activity!!.intent.putExtra("senderName", nullSting)
        }


        //kullanıcı verilerini al
        viewModel.loggedUserMutableLiveData.observe(viewLifecycleOwner, Observer { loggedUser ->
            //kaydedilen kullanıcı verilerini diğer bölümlerde kullanmak için paylaşılan tercihe kaydedin
            val mPrefs: SharedPreferences = activity!!.getPreferences(Context.MODE_PRIVATE)
            val prefsEditor: SharedPreferences.Editor = mPrefs.edit()
            val json = gson.toJson(loggedUser)
            prefsEditor.putString("loggedUser", json)
            prefsEditor.apply()


            //gelen istekler varsa bildirim rozetini göster
            receivedRequestsCount = loggedUser.receivedRequests?.size ?: 0
            setupBadge(receivedRequestsCount)


            //kullanıcı sohbet geçmişi
            viewModel.getChats(loggedUser!!)
                ?.observe(viewLifecycleOwner, Observer { chatParticipantsList ->

                    //Yüklenen resmi gizle
                    binding.loadingChatImageView.visibility = View.GONE
                    if (chatParticipantsList.isNullOrEmpty()) {
                        //sohbet düzeni gösterme
                        binding.noChatLayout.visibility = View.VISIBLE
                    } else {

                        //mesajları tarihe göre sırala, böylece yeniler en üstte gösterilir
                        val sortedChatParticipantsList: List<ChatParticipant> =
                            chatParticipantsList.sortedWith(compareBy { it.lastMessageDate?.get("seconds") })
                                .reversed()

                        binding.noChatLayout.visibility = View.GONE
                        binding.recycler.adapter = adapter
                        adapter.submitList(sortedChatParticipantsList)
                        adapter.chatList = sortedChatParticipantsList
                    }

                })


        })


        // startChatFab başlatmak için tıkla
        binding.startChatFab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactsFragment)
        }


    }


    private fun logout() {
        removeUserToken()
        FirebaseAuth.getInstance().signOut()
//        LoginManager.getInstance().logOut()
        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
    }

    private fun removeUserToken() {
        val loggedUserID = AuthUtil.firebaseAuthInstance.currentUser?.uid
        if (loggedUserID != null) {
            FirestoreUtil.firestoreInstance.collection("users").document(loggedUserID)
                .update("token", null)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)


        inflater.inflate(R.menu.main_menu, menu)
        val menuItem = menu.findItem(R.id.action_incoming_requests)
        val actionView = menuItem?.actionView
        countBadgeTextView = actionView?.findViewById<View>(R.id.count_badge) as TextView
        //setupbadge'ı tekrar çağırmamız gerekir
        setupBadge(receivedRequestsCount)



        actionView.setOnClickListener { onOptionsItemSelected(menuItem) }

        //arama yazdığımda veya aramayı tıkladığımda filtreleme yap
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(queryString: kotlin.String?): Boolean {
                adapter.filter.filter(queryString)
                return false
            }

            override fun onQueryTextChange(queryString: kotlin.String?): Boolean {
                adapter.filter.filter(queryString)
                if (queryString != null) {
                    adapter.onChange(queryString)
                }

                return false
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

//        R.id.action_add_friend -> {
//            findNavController().navigate(R.id.action_homeFragment_to_findUserFragment)
//            true
//        }
//        R.id.action_edit_profile -> {
//            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
//            true
//        }
        R.id.action_logout -> {
            logout()
            true
        }
        R.id.action_incoming_requests -> {
            findNavController().navigate(R.id.action_homeFragment_to_incomingRequestsFragment)


            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }

    }


    private fun setupBadge(count: Int?) {
        if (::countBadgeTextView.isInitialized) {
            if (null == count || count == 0) {
                countBadgeTextView.visibility = View.GONE
            } else {
                countBadgeTextView.visibility = View.VISIBLE
                countBadgeTextView.text =
                    count.let { Math.min(it, 99) }.toString()
            }
        }
    }


}