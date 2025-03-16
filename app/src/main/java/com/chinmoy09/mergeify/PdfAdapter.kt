package com.chinmoy09.mergeify

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.chinmoy09.mergeify.databinding.PdfItemBinding

class PdfAdapter(
    private var list: ArrayList<PdfModel>
) : RecyclerView.Adapter<PdfAdapter.PdfViewHolder>() {

    private var onItemClickListener: ((Int) -> Unit)? = null

    inner class PdfViewHolder(val binding: PdfItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val binding: PdfItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.pdf_item,
            parent,
            false
        )
        return PdfViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val pdf = list[position]
        holder.binding.pdfName.text = pdf.pdfName

        // Remove PDF on button click
        holder.binding.removeButton.setOnClickListener {
            onItemClickListener?.invoke(position)
        }
    }

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }
}
