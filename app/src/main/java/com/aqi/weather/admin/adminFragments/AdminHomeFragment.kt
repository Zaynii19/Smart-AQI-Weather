package com.aqi.weather.admin.adminFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aqi.weather.auth.SignInOptionActivity
import com.aqi.weather.databinding.FragmentAdminHomeBinding
import com.aqi.weather.util.isInternetAvailable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AdminHomeFragment : Fragment() {
    private val binding by lazy {
        FragmentAdminHomeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.admin.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                Firebase.auth.signOut()
                startActivity(Intent(requireActivity(), SignInOptionActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "No internet available, please check your connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}