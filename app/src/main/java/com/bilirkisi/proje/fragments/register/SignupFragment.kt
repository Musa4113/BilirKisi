package com.bilirkisi.proje.fragments.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.SignupFragmentBinding
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.ErrorMessage
import com.bilirkisi.proje.util.LoadState
import com.bilirkisi.proje.util.eventbus_events.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.issue_layout.view.*
import kotlinx.android.synthetic.main.no_incoming_requests.*
import kotlinx.android.synthetic.main.signup_fragment.*
import org.greenrobot.eventbus.EventBus
import java.util.regex.Matcher
import java.util.regex.Pattern


class SignupFragment : Fragment() {

    private lateinit var binding: SignupFragmentBinding
    private lateinit var pattern: Pattern
    private var expertUser : Boolean = false
    companion object {
        fun newInstance() = SignupFragment()
    }

    private lateinit var viewModel: SignupViewModel



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.signup_fragment, container, false)
        return binding.root
    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {

        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SignupViewModel::class.java)


/**       //RadioButton seçenek
        if(binding.radioButtonStandart.isChecked()){
            binding.statu="Standart";
        }
        if(binding.radioButtonUzman.isChecked()){
            binding.statu="Uzman";
        }*/

        binding.radioGroup.setOnCheckedChangeListener { group, i ->
            if(R.id.radio_button_Standart==i)
                expertUser = false
            if(R.id.radio_button_Uzman==i)
                expertUser = true
        }


        //e-posta biçimini kontrol etmek için kalıp
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$"
        pattern = Pattern.compile(emailRegex)

        getActivity()?.navView?.visibility = View.GONE

        //kayıt ol tıklayın
        binding.registerButton.setOnClickListener {

            signUp()

        }

        //x simgesine tıklandığında sorun düzenini gizle
        binding.issueLayout.cancelImage.setOnClickListener {
            binding.issueLayout.visibility = View.GONE
        }

        //uygun yükleme / hata kullanıcı arayüzünü göster (ui)
        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            when (it) {
                LoadState.LOADING -> {
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.issueLayout.visibility = View.GONE
                }
                LoadState.SUCCESS -> {
                    binding.loadingLayout.visibility = View.GONE
                    binding.issueLayout.visibility = View.GONE
                }
                LoadState.FAILURE -> {
                    binding.loadingLayout.visibility = View.GONE
                    binding.issueLayout.visibility = View.VISIBLE
                    binding.issueLayout.textViewIssue.text = ErrorMessage.errorMessage
                }

            }
        })


        // klavyede kaydolun tamamlandı odak açıkken tıklayın passwordEditText
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            signUp()
            true
        }

    }

    private fun signUp() {
        EventBus.getDefault().post(KeyboardEvent())

        binding.userName.isErrorEnabled = false
        binding.email.isErrorEnabled = false
        binding.password.isErrorEnabled = false


        if (binding.userName.editText!!.text.length < 4) {
            binding.userName.error = "Kullanıcı adı en az 4 karakter olmalıdır"
            return
        }


        //e-postanın empty mı yoksa yanlış format mı olduğunu kontrol edin
        if (!binding.email.editText!!.text.isEmpty()) {
            val matcher: Matcher = pattern.matcher(binding.email.editText!!.text)
            if (!matcher.matches()) {
                binding.email.error = "E-posta biçimi doğru değil"
                return
            }
        } else if (binding.email.editText!!.text.isEmpty()) {
            binding.email.error = "E-posta alanı boş  olamaz."
            return
        }


        if (binding.password.editText!!.text.length < 6) {
            binding.password.error = "Şifre en az 6 karakter olmalıdır"
            return
        }

        //e-posta ve şifre eşleşen gereksinimlerdir artık firebase kimlik doğrulamasına kaydolabiliriz

        viewModel.registerEmail(
            AuthUtil.firebaseAuthInstance,
            binding.email.editText!!.text.toString(),
            binding.password.editText!!.text.toString(),
            binding.userName.editText!!.text.toString(),
            expertUser
        )


        viewModel.navigateToHomeMutableLiveData.observe(viewLifecycleOwner, Observer { navigateToHome ->
            if (navigateToHome != null && navigateToHome) {
                this@SignupFragment.findNavController()
                    .navigate(R.id.action_signupFragment_to_homeFragment)
                Toast.makeText(context, "Kayıt başarılı", Toast.LENGTH_LONG).show()
                viewModel.doneNavigating()
            }
        })

    }

}



