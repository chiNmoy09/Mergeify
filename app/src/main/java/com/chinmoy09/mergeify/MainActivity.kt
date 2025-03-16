package com.chinmoy09.mergeify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.chinmoy09.mergeify.databinding.ActivityMainBinding
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfList: ArrayList<PdfModel>
    private lateinit var pdfAdapter: PdfAdapter

    private val REQUEST_PICK_PDF_FILES: Int = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeStatusBarColor()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        pdfList = ArrayList()
        pdfAdapter = PdfAdapter(pdfList)
        binding.recyclerView.adapter = pdfAdapter

        manageUI()
        // Handle select PDFs button
        binding.selectPdfs.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, REQUEST_PICK_PDF_FILES)
        }

        // Handle merge PDFs button
        binding.mergePdf.setOnClickListener {
            if (pdfList.size < 2) {
                Toast.makeText(this, "Please select at least 2 files!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pdfList.size > 10) {
                Toast.makeText(this, "You can merge only 10 files!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Create the merged PDF file name
                val filename = "Merged_PDF_${System.currentTimeMillis()}.pdf"

                // Get the URI for the Downloads folder
                val contentResolver = applicationContext.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                // Insert the new file into the MediaStore
                val uri = contentResolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)

                // Open an OutputStream for the merged file
                val outputStream = contentResolver.openOutputStream(uri!!)
                val document = Document()
                val copy = PdfCopy(document, outputStream)
                document.open()

                // Add all selected PDFs to the merged file
                for (pdf in pdfList) {
                    val reader = PdfReader(contentResolver.openInputStream(pdf.pdfUri!!))
                    copy.addDocument(reader)
                    reader.close()
                }

                // Close the document and the OutputStream
                document.close()
                outputStream?.close()

                // Show success message
                Toast.makeText(this, "PDF files merged successfully.", Toast.LENGTH_SHORT).show()

                // Clear the list and update UI
                pdfList.clear()
                pdfAdapter.notifyDataSetChanged()
                manageUI()

                // Open the merged PDF file
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/pdf")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error merging PDFs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }


        // Handle file removal
        pdfAdapter.setOnItemClickListener { position ->
            pdfList.removeAt(position)
            pdfAdapter.notifyItemRemoved(position)
            pdfAdapter.notifyItemRangeChanged(position, pdfList.size)
            manageUI()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_PDF_FILES && resultCode == RESULT_OK) {
            if (data != null) {
                val selectedUris = ArrayList<Uri>()

                // Handle single or multiple files
                if (data.data != null) {
                    selectedUris.add(data.data!!)
                } else if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        selectedUris.add(data.clipData!!.getItemAt(i).uri)
                    }
                }

                // Add selected files to the list but limit to 10 PDFs
                for (uri in selectedUris) {
                    if (pdfList.size < 10) {
                        val fileName = getFileName(uri)
                        pdfList.add(PdfModel(fileName, uri))
                    } else {
                        Toast.makeText(this, "You can only select a maximum of 10 PDFs.", Toast.LENGTH_SHORT).show()
                        break // Stop adding files if limit reached
                    }
                }
                manageUI()
                pdfAdapter.notifyDataSetChanged()
            }
        }
    }


    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.getString(index).also { cursor.close() }
        } else {
            "Unknown"
        }
    }

    private fun manageUI(){
        if(pdfList.size != 0){

            binding.emptyList.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }else {
            binding.emptyList.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            //Toast.makeText(this@MainActivity, "Not Found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeStatusBarColor() {
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.appColor)
    }
}
