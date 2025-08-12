package com.example.networkofone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.networkofone.R
import com.example.networkofone.mvvm.models.LocationModel

class LocationAdapter(
    private val locationList: MutableList<LocationModel>,
    private val onItemClick: (LocationModel) -> Unit,
) :
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder?>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location: LocationModel = locationList[position]
        holder.locationName.text = location.name
        holder.locationAddress.text = location.address
        holder.baseLayout.setOnClickListener {
            onItemClick(location)
        }
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var locationName: TextView = itemView.findViewById(R.id.location_name)
        var locationAddress: TextView = itemView.findViewById(R.id.location_address)
        var baseLayout: LinearLayout = itemView.findViewById(R.id.base)
    }
}

