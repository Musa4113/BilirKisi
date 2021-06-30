package com.bilirkisi.proje.fragments.different_user_profile

import android.os.Bundle
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bilirkisi.proje.fragments.profile.FriendsAdapter
import com.google.gson.Gson
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.DifferentUserProfileFragmentBinding
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.ui.mainActivity.SharedViewModel
import com.bilirkisi.proje.util.CLICKED_USER

class DifferentUserProfileFragment : Fragment() {
    private lateinit var binding: DifferentUserProfileFragmentBinding
    private val adapter by lazy {
        FriendsAdapter(object :
            FriendsAdapter.ItemClickCallback {
            override fun onItemClicked(user: User) {
                d("gg", "ok!!You clicked")
            }


        })
    }

    companion object {
        fun newInstance() =
            DifferentUserProfileFragment()
    }

    private lateinit var viewModel: DifferentUserProfileFragmentViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.different_user_profile_fragment,
            container,
            false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel =
            ViewModelProviders.of(this).get(DifferentUserProfileFragmentViewModel::class.java)
        sharedViewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)

        //// kullanıcı bulma  tıklandığında kullanıcının verilerini al
        val gson = Gson()
        val user = gson.fromJson(arguments?.getString(CLICKED_USER), User::class.java)


        activity?.title = user.username?.split("\\s".toRegex())?.get(0) + " profil"

        //Arkadaş ekli olup olmadığını kontrol edilir
        viewModel.checkIfFriends(user.uid).observe(viewLifecycleOwner, Observer { friendRequestState ->
            when (friendRequestState) {//bir isteğin gönderilip gönderilmediğini göstermek için düğme rengini ve simgesini değiştirin
                DifferentUserProfileFragmentViewModel.FriendRequestState.SENT -> {
                    showButtonAsSentRequest()
                }
                DifferentUserProfileFragmentViewModel.FriendRequestState.NOT_SENT -> {
                    showButtonAsRequestNotSent()
                }
                DifferentUserProfileFragmentViewModel.FriendRequestState.ALREADY_FRIENDS -> {
                    showButtonAsAlreadyFriends()
                }
            }
        })

        // Kullanıcı Uzman-Standart olduğunu profilde göster
        user.expertUser?.let {
            if(it)
                binding.expertUser.text="Uzman"
            else
                binding.expertUser.text="Standart"
        }

        //verileri görüntülemeye ayarla ve resmi indir
        binding.bioTextView.text = user.bio ?: "Bilgi yok"
        binding.name.text = user.username
        viewModel.downloadProfilePicture(user.profile_picture_url)


        //indirilen resmi profil görüntüsünde göster
        viewModel.loadedImage.observe(viewLifecycleOwner, Observer {
            it.into(binding.profileImage)
        })


        binding.sendFriendRequestButton.setOnClickListener {
            //kullanımda olan sendRequests (arkadaşlık istekleri) belgesine kimlik ekle
            if (binding.sendFriendRequestButton.text == getString(R.string.friend_request_not_sent)) {
                viewModel.updateSentRequestsForSender(user.uid)
                showButtonAsSentRequest()
            } else if (binding.sendFriendRequestButton.text == getString(R.string.cancel_request)) {
                viewModel.cancelFriendRequest(user.uid)
                showButtonAsRequestNotSent()
            } else if (binding.sendFriendRequestButton.text == getString(R.string.delete_from_friends)) {
                viewModel.removeFromFriends(user.uid)
                showButtonAsRequestNotSent()
            }
        }




        //o kullanıcının arkadaşlarını yükle
        sharedViewModel.loadFriends(user).observe(viewLifecycleOwner, Observer { friendsList ->
            if (friendsList.isNullOrEmpty()) {
                binding.friendsTextView.text = getString(R.string.no_friends)
            } else {
                binding.friendsTextView.text = getString(R.string.friends)
                binding.friendsCountTextView.text = friendsList.size.toString()
                showFriendsInRecycler(friendsList)
            }
        })

    }

    private fun showFriendsInRecycler(friendsList: List<User>?) {
        adapter.setDataSource(friendsList)
        binding.friendsRecycler.adapter = adapter

    }

    //kullanıcıların arkadaş olduğunu göstermek için değiştir düğmesi
    private fun showButtonAsAlreadyFriends() {
        binding.sendFriendRequestButton.text =
            getString(R.string.delete_from_friends)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_remove_circle_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { it1 -> ContextCompat.getColorStateList(it1, R.color.red) }
    }


    //hiçbir isteğin gönderilmediğini göstermek için gönderilen düğmesini değiştir
    private fun showButtonAsRequestNotSent() {
        binding.sendFriendRequestButton.text =
            getString(R.string.friend_request_not_sent)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_person_add_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { it1 -> ContextCompat.getColorStateList(it1, R.color.grey) }
    }


    //isteğin gönderildiğini göstermek için gönderilen düğmesini değiştir
    private fun showButtonAsSentRequest() {
        binding.sendFriendRequestButton.text = getString(R.string.cancel_request)
        binding.sendFriendRequestButton.setIconResource(R.drawable.ic_done_black_24dp)
        binding.sendFriendRequestButton.backgroundTintList =
            context?.let { it1 -> ContextCompat.getColorStateList(it1, R.color.green) }
    }


}
