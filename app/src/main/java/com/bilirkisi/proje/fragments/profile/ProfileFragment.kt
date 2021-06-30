package com.bilirkisi.proje.fragments.profile

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.ProfileFragmentBinding
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.ui.mainActivity.SharedViewModel
import com.bilirkisi.proje.util.CLICKED_USER
import com.bilirkisi.proje.util.LOGGED_USER
import com.bilirkisi.proje.util.LoadState
import com.bilirkisi.proje.util.eventbus_events.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_profile_picture.view.*
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream

const val SELECT_PROFILE_IMAGE_REQUEST = 5
const val REQUEST_IMAGE_CAPTURE = 6

class ProfileFragment : Fragment() {


    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    lateinit var binding: ProfileFragmentBinding
    lateinit var adapter: FriendsAdapter

    companion object {
        fun newInstance() =
            ProfileFragment()
    }

    private lateinit var viewModel: ProfileViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.title = "Profil"
        binding = DataBindingUtil.inflate(inflater, R.layout.profile_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        getActivity()?.navView?.visibility = View.GONE

        viewModel = ViewModelProviders.of(this).get(ProfileViewModel::class.java)
        sharedViewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)

        // alt sayfayı ayarla
        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)


        // kullanıcıyı paylaşılan tercihlerden al
        val mPrefs: SharedPreferences = activity!!.getPreferences(MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)
        // kullanıcı adını ve e-postayı ve biyografiyi göster
        binding.bioTextView.text = loggedUser.bio ?: "Henüz bilgi yok"
        binding.email.text = loggedUser.email
        binding.name.text = loggedUser.username

        // Kullanıcı Uzman-Standart olduğunu profilde göster
        loggedUser.expertUser?.let {
            if(it)
                binding.expertUser.text="Uzman"
            else
                binding.expertUser.text="Standart"
        }

        // profil fotoğrafını indir
        setProfileImage(loggedUser.profile_picture_url)


        // adaptör oluşturun ve geri dönüşüm maddesini işleyin geri aramaya tıklayın
        adapter = FriendsAdapter(object :
            FriendsAdapter.ItemClickCallback {
            override fun onItemClicked(clickedUser: User) {

                val clickedUserString = gson.toJson(clickedUser)

                var bundle = bundleOf(
                    CLICKED_USER to clickedUserString
                )

                findNavController().navigate(
                    R.id.action_profileFragment_to_differentUserProfile,
                    bundle
                )
            }
        })


        //oturum açmış kullanıcının arkadaşlarını yükle and show in recycler
        sharedViewModel.loadFriends(loggedUser).observe(viewLifecycleOwner, Observer { friendsList ->
            //yüklemeyi gizle
            binding.loadingFriendsImageView.visibility = View.GONE
            if (friendsList != null) {
                binding.friendsLayout.visibility = View.VISIBLE
                binding.noFriendsLayout.visibility = View.GONE
                showFriendsInRecycler(friendsList)
            } else {
                binding.friendsLayout.visibility = View.GONE
                binding.noFriendsLayout.visibility = View.VISIBLE
                binding.addFriendsButton.setOnClickListener {
                    this@ProfileFragment.findNavController()
                        .navigate(R.id.action_profileFragment_to_findUserFragment)
                }
            }

        })



        binding.bottomSheet.cameraButton.setOnClickListener {
            openCamera()
        }
        binding.bottomSheet.galleryButton.setOnClickListener {
            selectFromGallery()
        }

        binding.bottomSheet.hide.setOnClickListener {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        // bu düğme tıklandığında seçimin alt sayfasını göster
        binding.profileImage.setOnClickListener { selectProfilePicture() }
        binding.cameraImageView.setOnClickListener { selectProfilePicture() }


        // hakkında tıklamasını düzenle
        binding.editTextview.setOnClickListener {
            if (binding.editTextview.text.equals(getString(R.string.edit))) {
                //show edit text to allow user to edit bio and change text view text to submit
                binding.editTextview.text = getString(R.string.submit)
                binding.editTextview.setTextColor(Color.GREEN)
                binding.bioTextView.visibility = View.GONE
                binding.newBioEditText.visibility = View.VISIBLE


            } else if (binding.editTextview.text.equals(getString(R.string.submit))) {
                //hide edit text and upload changes to user document
                binding.editTextview.text = getString(R.string.edit)
                binding.editTextview.setTextColor(Color.parseColor("#b39ddb"))
                binding.bioTextView.visibility = View.VISIBLE
                binding.bioTextView.text = binding.newBioEditText.text
                binding.newBioEditText.visibility = View.GONE
                EventBus.getDefault().post(KeyboardEvent())
                // kullanıcı belgesine bio yükle
                viewModel.updateBio(binding.newBioEditText.text.toString())

                // klavyeyi gizle
                EventBus.getDefault().post(KeyboardEvent())
            }
        }


    }

    private fun setProfileImage(profilePictureUrl: String?) {
        Glide.with(this).load(profilePictureUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loading_animation)
                    .error(R.drawable.anonymous_profile)
                    .circleCrop()
            )
            .into(binding.profileImage)
    }

    private fun showFriendsInRecycler(it: List<User>) {
        adapter.setDataSource(it)
        binding.friendsRecycler.adapter = adapter
        binding.friendsCountTextView.text = it.size.toString()
    }

    private fun setProfileImageLoadUi(it: LoadState?) {
        when (it) {

            LoadState.SUCCESS -> {
                binding.uploadProgressBar.visibility = View.GONE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.FAILURE -> {
                binding.uploadProgressBar.visibility = View.GONE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = 1f
            }
            LoadState.LOADING -> {
                binding.uploadProgressBar.visibility = View.VISIBLE
                binding.uploadText.visibility = View.GONE
                binding.profileImage.alpha = .5f

            }
        }
    }


    private fun selectProfilePicture() {
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED


        // galeriden resim seçmenin sonucu
        if (requestCode == SELECT_PROFILE_IMAGE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            // seçili resmi profil resmi görünümünde ayarlayın ve yükleyin

            //fotoğraf yükleniyor
            viewModel.uploadProfileImageByUri(data.data)


        }


        // kamera görüntüsü çekmenin sonucu
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap


            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteArray = baos.toByteArray()


            //fotoğraf yükleniyor
            viewModel.uploadImageAsBytearray(byteArray)


        }

        // yükleme sırasında yükleme düzenini göster
        viewModel.uploadImageLoadStateMutableLiveData.observe(this, Observer { imageUploadState ->
            setProfileImageLoadUi(imageUploadState)
        })


        // profil resmi görünümünde yeni resmi ayarla
        viewModel.newImageUriMutableLiveData.observe(this, Observer {
            setProfileImage(it.toString())
        })
    }


    private fun selectFromGallery() {
        var intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            SELECT_PROFILE_IMAGE_REQUEST
        )
    }


    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity!!.packageManager)?.also {
                startActivityForResult(takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
            }
        }
    }


}

