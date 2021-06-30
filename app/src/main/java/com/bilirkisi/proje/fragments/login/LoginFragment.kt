package com.bilirkisi.proje.fragments.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.bilirkisi.proje.R
import com.bilirkisi.proje.databinding.LoginFragmentBinding
import com.bilirkisi.proje.util.AuthUtil
import com.bilirkisi.proje.util.ErrorMessage
import com.bilirkisi.proje.util.LoadState
import com.bilirkisi.proje.util.eventbus_events.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.issue_layout.view.*
import org.greenrobot.eventbus.EventBus


class LoginFragment : Fragment() {

    private lateinit var binding: LoginFragmentBinding



    companion object {
        fun newInstance() = LoginFragment()
    }

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.login_fragment, container, false)

        // FirebaseAut dan daha önce giriş yapıldığını kontrol et
        if (AuthUtil.firebaseAuthInstance.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)

        getActivity()?.navView?.visibility = View.GONE

        // Kayıt Butonuna tıklandığında Kayıt bölümüne git
        binding.gotoSignUpFragmentTextView.setOnClickListener {
            it.findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        //viewModel EmailFormat
        binding.emailEditText.afterTextChanged { email ->
            viewModel.EmailFormat(email).observe(viewLifecycleOwner, Observer { EmailFormat ->
                if (!EmailFormat) {//email format is not correct
                    binding.email.error = getString(R.string.wrong_email_format)
                } else {
                    binding.email.isErrorEnabled = false
                }

            })
        }


        // şifre uzunluğu en az 6 karakter olmalıdır
        binding.passwordEditText.afterTextChanged {
            if (it.length < 6) {
                binding.password.error = getString(R.string.password_size)
            } else {
                binding.password.isErrorEnabled = false
            }
        }


        // login buttonuna tıklandığında
        binding.loginButton.setOnClickListener {
            login()
        }


        // X simgesine tıklandığında gizle
        binding.issueLayout.cancelImage.setOnClickListener {
            binding.issueLayout.visibility = View.GONE
        }


/**    // ?? */
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
           login()
           true
      }

    }

    private fun login() {
        EventBus.getDefault().post(KeyboardEvent())
        if (binding.email.error != null || binding.password.error != null || binding.email.editText!!.text.isEmpty() || binding.password.editText!!.text.isEmpty()) {
            // İsim veya şifre eşleşmiyorsa
            Toast.makeText(context, "EMail ve şifreyi kontrol edip tekrar deneyin", Toast.LENGTH_LONG)
                .show()
        } else {
            // Hiç bir eror yoksa , Giriş yapabiliriz
            viewModel.login(
                AuthUtil.firebaseAuthInstance,
                binding.email.editText!!.text.toString(),
                binding.password.editText!!.text.toString()
            ).observe(viewLifecycleOwner, Observer { loadState ->

                when (loadState) {
                    LoadState.SUCCESS -> {   //Şifre ve Mail ile giriş başaralı olduğunda tetiklenir
                        this@LoginFragment.findNavController()
                            .navigate(R.id.action_loginFragment_to_homeFragment)
                        Toast.makeText(context, "Giriş Başarılı", Toast.LENGTH_LONG).show()
                        viewModel.doneNavigating()
                    }
                    LoadState.LOADING -> {
                        binding.loadingLayout.visibility = View.VISIBLE
                        binding.issueLayout.visibility = View.GONE
                    }
                    LoadState.FAILURE -> {
                        binding.loadingLayout.visibility = View.GONE
                        binding.issueLayout.visibility = View.VISIBLE
                    //    binding.issueLayout.textViewIssue.text = ErrorMessage.errorMessage    // Firebase verdiği otomatik hatalar
                        binding.issueLayout.textViewIssue.text = "Bir şeyler ters gitti. HATA !!!"         // SABİT HATA

                    }
                }
            })

        }
    }


    /**
     * Extension function to simplify setting an afterTextChanged action to EditText components.
     */
    fun TextInputEditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

    }
}
