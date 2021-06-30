package com.bilirkisi.proje.fragments.incoming_requests

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.IncomingRequestsFragmentBinding
import com.bilirkisi.proje.models.User
import com.bilirkisi.proje.util.LOGGED_USER

class IncomingRequestsFragment : Fragment() {


    private lateinit var adapter: IncomingRequestsAdapter
    private lateinit var binding: IncomingRequestsFragmentBinding
    var sendersList: MutableList<User>? = null


    companion object {
        fun newInstance() =
            IncomingRequestsFragment()
    }

    private lateinit var viewModel: IncomingRequestsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.title = "Bekleyen arkadaşlık istekleri"
        binding =
            DataBindingUtil.inflate(inflater, R.layout.incoming_requests_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(IncomingRequestsViewModel::class.java)


        //kullanıcıyı seç
        val mPrefs: SharedPreferences = activity!!.getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)

        //arkadaşlık istekleri alın
        val receivedRequest = loggedUser.receivedRequests
        if (!receivedRequest.isNullOrEmpty()) {
            viewModel.downloadRequests(receivedRequest).observe(viewLifecycleOwner, Observer { requestersList ->
                //hide loading
                binding.loadingRequestsImageView.visibility = View.GONE

                if (requestersList == null) {
                    //istekl alırken hata
                    binding.noIncomingRequestsLayout.visibility = View.VISIBLE
                    Toast.makeText(
                        context,
                        "Gelen arkadaşlık istekleri yüklenirken hata oluştu",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // istek başarıyla alınırsa
                    binding.noIncomingRequestsLayout.visibility = View.GONE
                    adapter.setDataSource(requestersList)
                    sendersList = requestersList
                    binding.receivedRequestsRecycler.adapter = adapter
                }
            })
        } else {
                //alınan istek yok
            binding.noIncomingRequestsLayout.visibility = View.VISIBLE
            binding.loadingRequestsImageView.visibility = View.GONE

        }


        //Arkadaşlık isteiğine geri dönüş
        adapter =
            IncomingRequestsAdapter(
                object :
                    IncomingRequestsAdapter.ButtonCallback {
                    override fun onConfirmClicked(requestSender: User, position: Int) {
                        viewModel.addToFriends(requestSender.uid!!, loggedUser.uid!!)

                        Toast.makeText(
                            context,
                            "${requestSender.username} arkadaş olarak eklendi",
                            Toast.LENGTH_LONG
                        ).show()
                        deleteFromRecycler(position)
                    }

                    override fun onDeleteClicked(requestSender: User, position: Int) {
                        viewModel.deleteRequest(requestSender.uid!!, loggedUser.uid!!)
                        Toast.makeText(context, "İstek silindi", Toast.LENGTH_LONG).show()
                        deleteFromRecycler(position)
                    }


                    // isteği Silin
                    private fun deleteFromRecycler(position: Int) {
                        sendersList?.removeAt(position)
                        adapter.setDataSource(sendersList)
                        adapter.notifyItemRemoved(position)
                        // hiçbir istek kalmadıysa  empty_box göster
                        if (sendersList?.size == 0) {
                            binding.noIncomingRequestsLayout.visibility = View.VISIBLE
                        }
                    }

                })


    }


}
