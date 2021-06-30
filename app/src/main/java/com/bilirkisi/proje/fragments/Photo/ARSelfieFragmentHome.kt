package com.bilirkisi.proje.fragments.Photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.ArselfieHomeFragmentBinding
import kotlinx.android.synthetic.main.arselfie_home_fragment.*


class ARSelfieFragmentHome : Fragment() {
    private lateinit var binding: ArselfieHomeFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.arselfie_home_fragment, container, false)

        return binding.root
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

       cdNormalSelfie.setOnClickListener {
           findNavController().navigate(R.id.action_ARSelfieFragmentHome_to_selfieFragment)
       }
    }

}