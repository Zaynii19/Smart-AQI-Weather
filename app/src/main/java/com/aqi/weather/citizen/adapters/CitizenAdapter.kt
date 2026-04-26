package com.aqi.weather.citizen.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aqi.weather.R
import com.aqi.weather.data.model.User
import com.aqi.weather.databinding.CitizenItemBinding

class CitizenAdapter(val citizens: List<User>) : RecyclerView.Adapter<CitizenAdapter.CitizenViewHolder>() {
    class CitizenViewHolder(val binding: CitizenItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitizenViewHolder {
        return CitizenViewHolder(CitizenItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount() = citizens.size

    override fun onBindViewHolder(holder: CitizenViewHolder, position: Int) {
        val citizen = citizens[position]

        holder.binding.name.text = citizen.name
        when (citizen.gender) {
            "male" -> holder.binding.avatar.setImageResource(R.drawable.male)
            "female" -> holder.binding.avatar.setImageResource(R.drawable.female)
            else -> holder.binding.avatar.setImageResource(R.drawable.male)
        }
    }
}