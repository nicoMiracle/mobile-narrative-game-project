//Nicole Nechita, rone8293
//code for project 9.0
package com.example.narrativegame

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity

import com.example.narrativegame.databinding.GameOverLayoutBinding

class GameOver:AppCompatActivity() {
    private lateinit var viewBinding:GameOverLayoutBinding
    private var fileIsPicked:Boolean = false
    private var attachFilePath: Uri?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = GameOverLayoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.mainMenu.setOnClickListener {goMainMenu() }
        viewBinding.thankYouVideo.setOnClickListener { goToVideo("L3tsYC5OYhQ") }
        viewBinding.sendEmail.setOnClickListener {sendEmail()
             }
        viewBinding.filePicker.setOnClickListener { pickFilePressed() }
        viewBinding.cancelButton.setOnClickListener { cancelEmail() }
        viewBinding.sendEmailButton.setOnClickListener { setGameOverVisibility(View.GONE)
            setEmailVisibility(View.VISIBLE) }
    }
    //go back to the main menu
    private fun goMainMenu(){
        val intent =Intent(this,MainActivity::class.java)
        startActivity(intent)
    }
    //cancel email sending
    private fun cancelEmail(){
        fileIsPicked = true
        pickFilePressed()
        setEmailVisibility(View.GONE)
        setGameOverVisibility(View.VISIBLE)
    }
    //set visibility of the game over screen
    private fun setGameOverVisibility(visibility:Int){
        runOnUiThread {viewBinding.gameOverHeader.visibility = visibility
            viewBinding.thankYou.visibility = visibility
            viewBinding.thankYouVideo.visibility = visibility
            viewBinding.mainMenu.visibility = visibility
            viewBinding.sendEmailButton.visibility = visibility  }
    }
    //set the visibility of the send email components
    private fun setEmailVisibility(visibility: Int){
        runOnUiThread {viewBinding.textViewEmail.visibility = visibility
            viewBinding.filePicker.visibility = visibility
            viewBinding.fileText.visibility = visibility
            viewBinding.subject.visibility = visibility
            viewBinding.scrollView2.visibility = visibility
            viewBinding.sendEmail.visibility = visibility
            viewBinding.cancelButton.visibility = visibility}
    }

    //send the email to the creator
    private fun sendEmail(){
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type ="message/rfc822"

        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("nikkinekita61@gmail.com") )
        if (!TextUtils.isEmpty(viewBinding.subject.text)) {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, viewBinding.subject.text.toString())
        }
        if (!TextUtils.isEmpty(viewBinding.bodyText.text)) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, viewBinding.bodyText.text.toString())
        }
        attachFilePath?.let {
            emailIntent.putExtra(Intent.EXTRA_STREAM, it)
        }
        startActivity(Intent.createChooser(emailIntent,"Send Email!"))
    }
    //check if a file is attached or not, then either open an activity to pick one or
    //or remove it
    private fun pickFilePressed(){
        if(fileIsPicked){
            attachFilePath = null
            fileIsPicked = false
            runOnUiThread {
                viewBinding.fileText.text =""
                viewBinding.filePicker.setText(R.string.attach_file)}
        }
        else{
            pickFile()
        }
    }
    //create an intent that launches an activity that retrieves the
    //URI of any type of file on the users device
    private fun pickFile(){
        val intentFilePicker = Intent(Intent.ACTION_GET_CONTENT)
        intentFilePicker.type ="*/*"
        getContent.launch(intentFilePicker)
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    attachFilePath= uri

                    fileIsPicked = true
                    runOnUiThread {
                        viewBinding.filePicker.setText(R.string.de_attach)
                        viewBinding.fileText.text =getFileName(uri)}

                    Toast.makeText(baseContext,"File Picked!",Toast.LENGTH_SHORT).show()
                }
            }
        }
    //get the name of the file by querying it from memory
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null,
            null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = it.getString(displayNameIndex)
                }
            }
        }
        cursor?.close()
        if (result == null) {
            result = uri.lastPathSegment

        }
        return result.orEmpty()
    }
    //go to the video provided but only if connected to internet
    private fun goToVideo(videoCode: String){
        val connectionManager: ConnectivityManager =getSystemService(CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val currentNetwork: Network? = connectionManager.activeNetwork
        val capabilities: NetworkCapabilities? = connectionManager
            .getNetworkCapabilities(currentNetwork)

        if(capabilities!=null){
            if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                val youtubeAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoCode"))
                if(isYoutubeAppInstalled()){
                    startActivity(youtubeAppIntent)
                }
                else{
                    youtubeAppIntent.data = Uri.parse("https://www.youtube.com/watch?v=$videoCode")
                    startActivity(youtubeAppIntent)
                }
            }
        }
        else{
            Toast.makeText(baseContext,"You are not connected to Internet!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //check if youtube is installed
    private fun isYoutubeAppInstalled():Boolean{
        return try{
            packageManager.getApplicationInfo("com.google.android.youtube", PackageManager.GET_ACTIVITIES)
            true
        }catch(e: PackageManager.NameNotFoundException ){
            false
        }
    }
}