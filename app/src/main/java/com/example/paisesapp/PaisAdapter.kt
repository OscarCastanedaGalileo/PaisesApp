package com.example.paisesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PaisAdapter(private var listaPaises: List<Pais>) :
    RecyclerView.Adapter<PaisAdapter.PaisViewHolder>() {

    class PaisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombrePais: TextView = itemView.findViewById(R.id.tvNombrePais)
        val ivBandera: ImageView = itemView.findViewById(R.id.ivBandera)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaisViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pais, parent, false)
        return PaisViewHolder(vista)
    }

    override fun onBindViewHolder(holder: PaisViewHolder, position: Int) {
        val pais = listaPaises[position]
        holder.tvNombrePais.text = pais.nombre

        if (pais.banderaUrl.isNotBlank()) {
            Glide.with(holder.itemView.context)
                .load(pais.banderaUrl)
                .into(holder.ivBandera)
        } else {
            Glide.with(holder.itemView.context).clear(holder.ivBandera)
            holder.ivBandera.setImageDrawable(null)
        }
    }

    override fun getItemCount(): Int = listaPaises.size

    fun actualizarLista(nuevaLista: List<Pais>) {
        listaPaises = nuevaLista
        notifyDataSetChanged()
    }
}