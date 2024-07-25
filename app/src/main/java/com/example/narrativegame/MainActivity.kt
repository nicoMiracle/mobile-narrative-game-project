//Nicole Nechita, rone8293
//code for project 9.0
package com.example.narrativegame

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.narrativegame.databinding.ActivityMainBinding

const val REQUEST_CODE =77
class MainActivity : AppCompatActivity() {
    private var permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.VIBRATE)
    } else {
        arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.VIBRATE)
    }

    private lateinit var viewBinding: ActivityMainBinding
    private var booleanArray = booleanArrayOf(false,false)
    private var biggerText:Boolean =false
    private var textToSpeech:Boolean =false
    private var showedDialog:Boolean = false
    private lateinit var preferences:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        preferences =getSharedPreferences("settingPref",Context.MODE_PRIVATE)
        showedDialog = preferences.getBoolean("showedBool",false)
        //show the first time dialog then save the fact it has been shown
        if(!showedDialog){
            createFirstDialog()
            val editor = preferences.edit()
            editor.putBoolean("showedBool",true)
            editor.apply()
        }
        //check for already saved settings
        textToSpeech = preferences.getBoolean("speechBool",false)
        biggerText = preferences.getBoolean("textBool",false)

        viewBinding.startButton.setOnClickListener { startGame() }
        viewBinding.settingButton.setOnClickListener { settingsDialog() }
    }
    //check the permissions and ask for them
    private fun checkPermissions(){
        for(permission in permissionArray){
            if(ContextCompat.checkSelfPermission(this,permission)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(permissionArray, REQUEST_CODE)
                break
            }
        }
    }
    //start the game, provided all permissions are accepted
    private fun startGame(){
        var startGame =true
        for(permission in permissionArray){
            if(ContextCompat.checkSelfPermission(this,permission)!=PackageManager.PERMISSION_GRANTED){
                startGame = false
                Toast.makeText(baseContext,"Permissions missing!",Toast.LENGTH_SHORT).show()
                break
            }
        }
        if(startGame){
            val intent =Intent(this,GameActivity::class.java)
            intent.putExtra("textBool",biggerText)
            intent.putExtra("speechBool",textToSpeech)
            startActivity(intent)
        }
    }
    //show this dialog the first time the app is run to warn the user
    private fun createFirstDialog(){
        val dialogBuild = AlertDialog.Builder(this)
        dialogBuild.setTitle("Before you begin.")
        dialogBuild.setMessage("Be sure to check all permissions to be able to experience the game!")
            .setPositiveButton("Understood!"){
                    _, _ ->checkPermissions()
            }
            .setCancelable(false)
        dialogBuild.create()
        dialogBuild.show()
    }
    //show the settings dialog
    private fun settingsDialog(){
        val dialogBuild = AlertDialog.Builder(this)
        booleanArray[0]=textToSpeech
        booleanArray[1]=biggerText
        dialogBuild
            .setTitle("Settings?")
            .setPositiveButton("Confirm!") { _, _ ->
                setSettings()
                Toast.makeText(baseContext,"Settings Saved!",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel!") { _, _ ->
            }
            .setMultiChoiceItems(arrayOf("Text to Speech", "Bigger Text!"),
                booleanArray){_, which,isChecked->
                booleanArray[which] =isChecked
            }
        dialogBuild.create()
        dialogBuild.show()
    }
    //save changes in the dialog to settings using Shared preferences
    private fun setSettings(){
        booleanArray.forEachIndexed { index, bool->
            when(index){
                0-> {textToSpeech=bool
                    val editor = preferences.edit()
                    editor.putBoolean("speechBool",bool)
                    editor.apply()}
                1->{biggerText =bool
                    val editor = preferences.edit()
                    editor.putBoolean("textBool",bool)
                    editor.apply()}
            }
        }
    }
}

