//Nicole Nechita, rone8293
//code for project 9.0
package com.example.narrativegame


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CallLog
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.narrativegame.databinding.GameLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraX"
private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

class GameActivity:AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var viewBinding: GameLayoutBinding
    private var textBool:Boolean =false
    private var speechBool:Boolean = false
    private lateinit var textToSpeech: TextToSpeech
    private var imageCapture: ImageCapture? = null
    private var continuous: Job? = null
    private var vibrationActive:Boolean = true
    private var textList:MutableList<String> = ArrayList()
    private lateinit var smsManager:SmsManager
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var cameraExecutor: ExecutorService

    private var latestPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = GameLayoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        textBool=intent.getBooleanExtra("textBool",false)
        speechBool=intent.getBooleanExtra("speechBool",false)

        //depending on settings, change text size
        if(textBool){
            runOnUiThread {viewBinding.chapter.textSize =25f
                viewBinding.textContent.textSize =25f  }
        }
        else{
            runOnUiThread {viewBinding.chapter.textSize =18f
                viewBinding.textContent.textSize =18f  }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewBinding.backButton.setOnClickListener { showBackDialog() }
        setNextButtonVisibility(View.VISIBLE)
        setNextListener (::startChapterOne)
    }
    //start off the first chapter, the morning, set the choices
    private fun startChapterOne(){
        vibrateDevice(2000)
        setChapterContent(getString(R.string.morningContent),getString(R.string.chapter_One))
        setChoices(getString(R.string.morningChoiceOne),getString(R.string.morningChoiceTwo),::sleepInOne,::goToWorkChapOne)
        setVisibilityChoices(View.VISIBLE)
        setNextButtonVisibility(View.GONE)

        if(speechBool){
            startTextToSpeech()
        }
    }

    //choose to go to work
    private fun goToWorkChapOne(){
        continuous?.cancel()
        setVisibilityChoices(View.VISIBLE)
        setChapterContent(getString(R.string.goToWork),getString(R.string.ChapterTwoStart))
        playTextToSpeech(1f,getString(R.string.goToWork))
        setChoices(getString(R.string.workChoiceOne),getString(R.string.workChoiceTwo),::workChoiceOne,::workChoiceTwo)
    }
    //the first choice at work is chosen
    private fun workChoiceOne(){
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        setChapterContent(getString(R.string.workChoiceOneText))
        playTextToSpeech(1f,getString(R.string.workChoiceOneText))
        setNextListener { workFinale() }
    }
    //the second choice at work is chosen
    private fun workChoiceTwo(){
        if(speechBool)textToSpeech.stop()
        setCoreItemsVisibility(View.GONE)
        setVisibilityChoices(View.GONE)
        setVoiceTextVisibility(View.VISIBLE)
        viewBinding.buttonSpeak.setOnClickListener { detectSpeech() }
        viewBinding.buttonTakeThat.setOnClickListener { workFinale() }
    }
    //the last chapter after work is shown
    private fun workFinale(){
        setCoreItemsVisibility(View.VISIBLE)
        setVisibilityChoices(View.VISIBLE)
        setVoiceTextVisibility(View.GONE)
        setNextButtonVisibility(View.GONE)
        setChapterContent(getString(R.string.workFinale),getString(R.string.finalChapter))
        playTextToSpeech(1f,getString(R.string.workFinale))
        setChoices(getString(R.string.workFinaleChoiceOne),getString(R.string.workFinaleChoiceTwo),::goToAlaska,::playMusic)
    }
    //the first choice is chosen in the finale of work arc final chapter
    private fun goToAlaska(){
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        setChapterContent(getString(R.string.goToAlaskaText))
        playTextToSpeech(1f,getString(R.string.goToAlaskaText))
        setNextListener { gameOver() }
    }
    //the second choice is chosen in the finale of work arc final chapter
    private fun playMusic(){
        mediaPlayer = MediaPlayer.create(this,R.raw.tuba).apply {  isLooping = true
            setVolume(1.0f, 1.0f)
            start() }
        mediaPlayer?.start()
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        setNextListener {
            mediaPlayer?.release()
            gameOver() }
    }
    //set the visibility of the voice to text page as needed
    private fun setVoiceTextVisibility(visibility: Int){
        runOnUiThread {viewBinding.buttonSpeak.visibility = visibility
            viewBinding.textViewSpeech.visibility = visibility
            viewBinding.headerSpeech.visibility = visibility
            viewBinding.scrollViewSpeech.visibility = visibility
            viewBinding.buttonTakeThat.visibility = visibility  }
    }
    //activate, detect and put speech into text
    private fun detectSpeech(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speech to Text")
        speechActivityResultLauncher.launch(intent)
    }
    //try to record then use that data to insert into the text view
    private val speechActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data!=null) {
                val data: Intent = result.data!!
                runOnUiThread {viewBinding.textViewSpeech.text= data.getStringArrayListExtra(
                    RecognizerIntent
                    .EXTRA_RESULTS)!![0]}

            }
        }
    //choice "sleep in" selected in first chapter
    private fun sleepInOne(){
        continuous?.cancel()
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        setChapterContent(getString(R.string.sleepInMorning))
        playTextToSpeech(1f,getString(R.string.sleepInMorning))
        setNextListener(::sleepInOneContinue)
    }
    //show the phone call registry, with a fake first missed call from the boss
    private fun sleepInOneContinue(){
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = createCalLogAdapter()
        runOnUiThread {viewBinding.recyclerView.visibility = View.VISIBLE }
        setCoreItemsVisibility(View.GONE)
        setNextButtonVisibility(View.GONE)
        runOnUiThread {viewBinding.recyclerButton.visibility = View.VISIBLE    }
        playTextToSpeech(0.1f,getString(R.string.fired_Text))
        viewBinding.recyclerButton.setOnClickListener { sleepInChapterTwoStart() }
    }
    //start chapter two after sleeping
    private fun sleepInChapterTwoStart(){
        runOnUiThread {viewBinding.recyclerView.visibility = View.INVISIBLE
            viewBinding.recyclerButton.visibility = View.INVISIBLE}
            setCoreItemsVisibility(View.VISIBLE)
            setChapterContent(getString(R.string.sleepInDay),getString(R.string.ChapterTwoStart))

            playTextToSpeech(1f,getString(R.string.sleepInDay))

            setChoices(getString(R.string.sleepMore),getString(R.string.goOutside),::sleepMoreChapTwo,::goOutside)
            setVisibilityChoices(View.VISIBLE)
    }
    //choose to sleep some more in chapter two
    private fun sleepMoreChapTwo(){
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        setChapterContent(getString(R.string.sleepInMoreContent))
        playTextToSpeech(1f,getString(R.string.sleepInMoreContent))
        setNextListener(::takeSelfie)
    }
    //choose to go outside instead of sleeping in chapter two
    private fun goOutside(){
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        vibrateDevice(300)
        setChapterContent(getString(R.string.goOutsideContent))
        playTextToSpeech(1f,getString(R.string.goOutsideContent))
        setNextListener { stayInFinale(getString( R.string.outsideFinale)) }
    }
    //stay indoors finale
    private fun stayInFinale(content:String){
        continuous?.cancel()
        setCoreItemsVisibility(View.VISIBLE)
        removePictureComponents()
        setNextButtonVisibility(View.GONE)
        setChapterContent(content,getString(R.string.finalChapter))

        setVisibilityChoices(View.VISIBLE)
        playTextToSpeech(1f,content)
        setChoices(getString(R.string.sleepInFinaleChoiceOne),getString(R.string.sleepInFinaleChoiceTwo),::sleptAllDay,::bullyCreator)
    }
    //choose to sleep yet again in the finale
    private fun sleptAllDay(){
        setChapterContent(getString(R.string.sleepInFinaleChoiceOneText))
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        playTextToSpeech(1f,getString(R.string.sleepInFinaleChoiceOneText))
        setNextListener(::gameOver)
    }
    //decide to bully me in the finale
    private fun bullyCreator(){
        setChapterContent(getString(R.string.bullyChoiceContent))
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.VISIBLE)
        playTextToSpeech(1f,getString(R.string.bullyChoiceContent))
        setNextListener(::startBullying)
    }
    //set up the page for messaging
    private fun startBullying(){
        setCoreItemsVisibility(View.GONE)
        setVisibilityChoices(View.GONE)
        setNextButtonVisibility(View.GONE)
        setSmsManager()
        setMessagingVisibility(View.VISIBLE)
        viewBinding.sendButton.setOnClickListener { sendText() }
    }
    //send to game over screen
    private fun gameOver(){
        if(speechBool)textToSpeech.stop()
        val intent =Intent(this,GameOver::class.java)
        startActivity(intent)
    }
    //set the chapter and content text shown on screen
    private fun setChapterContent(content:String,chapter:String?=null){
        runOnUiThread {
            if(chapter!=null)viewBinding.chapter.text = chapter
            viewBinding.textContent.text = content}
    }
    //set text and function triggers to choice buttons
    private fun setChoices (firstChoiceText:String,secondChoiceText:String,firstChoiceFun: ()->Unit, secondChoiceFun: ()-> Unit){
        runOnUiThread {viewBinding.firstChoice.text=firstChoiceText}

        viewBinding.firstChoice.setOnClickListener { firstChoiceFun() }
        runOnUiThread {viewBinding.secondChoice.text=secondChoiceText}
        viewBinding.secondChoice.setOnClickListener { secondChoiceFun() }
    }
    //set what function should the next button do next
    private fun setNextListener(func: ()->Unit){
        viewBinding.nextButton.setOnClickListener { func() }
    }
    //play a given string using text to speech
    private fun playTextToSpeech(pitch:Float, text:String){
        if(speechBool){
            textToSpeech.stop()
            textToSpeech.setPitch(pitch)
            textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,null,
                null)
        }
    }
    //set up the app look to take a selfie
    private fun takeSelfie(){
        if(speechBool){
            textToSpeech.stop()
        }
        setCoreItemsVisibility(View.GONE)
        setNextButtonVisibility(View.GONE)
        runOnUiThread {viewBinding.viewFinder.visibility = View.VISIBLE
            viewBinding.takePic.visibility = View.VISIBLE  }
        cameraStart()
        viewBinding.takePic.setOnClickListener { takePhoto() }
        runOnUiThread {Glide.with(this).load(File(latestPhotoPath.toString())).into(viewBinding.imageView)
            viewBinding.imageView.visibility = View.VISIBLE}
    }
    //start up the camera in order to take a selfie
    private fun cameraStart(){
        val cameraProvider = ProcessCameraProvider.getInstance(this)

        cameraProvider.addListener({
            val cameraProviderVar: ProcessCameraProvider = cameraProvider.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProviderVar.unbindAll()

                cameraProviderVar.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    //once the selfie section is over, disable the camera to avoid resource waste
    private fun cameraStop() {
        val cameraProvider = ProcessCameraProvider.getInstance(this)

        cameraProvider.addListener({
            val cameraProviderVar: ProcessCameraProvider = cameraProvider.get()

            try {
                cameraProviderVar.unbindAll()
            } catch(exc: Exception) {
                Log.e(TAG, "ERROR while releasing use case binding", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    //take the picture
    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
        var filePath:String

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Take photo failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    val contentResolver = applicationContext.contentResolver

                    val cursor = contentResolver.query(savedUri, null, null,
                        null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {

                            val filePathColumnIndex =
                                it.getColumnIndex(MediaStore.Images.Media.DATA)
                            filePath = it.getString(filePathColumnIndex)
                            showPicture(filePath)
                        }//if statement
                    }//cursor
                }//onImageSaved function
            }
        )
    }
    //once the selfie section is over, remove all the components associated with it
    private fun removePictureComponents(){
        runOnUiThread {viewBinding.viewFinder.visibility = View.GONE
            viewBinding.takePic.visibility = View.GONE
            viewBinding.imageView.visibility = View.GONE
            viewBinding.photoText.visibility = View.GONE  }

    }
    //show the picture taken with a text over it, much like a snapchat type of selfie
    private fun showPicture(path:String){
        cameraStop()
        cameraExecutor.shutdown()
        runOnUiThread {viewBinding.imageView.visibility = View.VISIBLE  }
        Glide.with(this).load(File(path)).into(viewBinding.imageView)
        runOnUiThread {viewBinding.viewFinder.visibility = View.GONE
            viewBinding.photoText.visibility = View.VISIBLE
            viewBinding.takePic.text = getString(R.string.next)  }
        viewBinding.takePic.setOnClickListener { stayInFinale(getString(R.string.sleepInFinale)) }
    }
    //show this dialog if the user wants to back out in the middle of the story
    private fun showBackDialog(){
        val dialogBuild = AlertDialog.Builder(this)
        dialogBuild.setTitle("Are you sure?")
        dialogBuild.setMessage("Progress will not be saved!")
            .setPositiveButton("Understood!"){
                    _, _ -> run {
                    textToSpeech.stop()
                    val intent =Intent(this,MainActivity::class.java)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel"){
                _,_->
            }
            .setCancelable(false)
        dialogBuild.create()
        dialogBuild.show()
    }

    //set the visibility of the choice buttons as needed
    private fun setVisibilityChoices(visibility:Int){
        runOnUiThread {viewBinding.firstChoice.visibility = visibility
            viewBinding.secondChoice.visibility = visibility}

    }
    //set the visibility of the next button as needed
    private fun setNextButtonVisibility(visibility: Int){
        runOnUiThread {viewBinding.nextButton.visibility = visibility}
    }
    //initialise the text to speech language
    private fun startTextToSpeech(){
        textToSpeech = TextToSpeech(this,this)
        textToSpeech.language = Locale.US
    }
    //initialise the text to speech to talk
    override fun onInit(status: Int) {
        if(status ==TextToSpeech.SUCCESS){
            Log.d("success","Text to speech worked")
            textToSpeech.speak(viewBinding.textContent.text.toString(),0,null,null)
        }
        else if(status ==TextToSpeech.ERROR){
            Toast.makeText(baseContext,"Can't launch text to speech", Toast.LENGTH_SHORT).show()
            textToSpeech.shutdown()
        }
    }
    //create the call log page by querying data and putting it inside a Recycler list view
    private fun createCalLogAdapter() : RecyclerView.Adapter<CallItemViewHolder>{
        val callHistory = getCalls().reversed()

        return object : RecyclerView.Adapter<CallItemViewHolder>(){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallItemViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.call_log_item,
                    parent,false)
                return CallItemViewHolder(itemView)
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: CallItemViewHolder, position: Int) {
                val currentItem = callHistory[position]
                runOnUiThread {holder.nameTextView.text = getString(R.string.callFrom)+" "+currentItem.name
                    holder.numberHolderTextView.text = currentItem.number
                    holder.typeTextView.text = getTypeString(currentItem.typeOfCall)
                    holder.lastMessage.text =getString(R.string.lastMsg)+ currentItem.lastMessage  }
            }

            override fun getItemCount(): Int =callHistory.size
        }
    }
    //return a string based on the type of call the call log query returns
    private fun getTypeString(type:Int):String{
        return when(type){
            1->"Incoming"
            2->"Outgoing"
            3->"Missed"
            else->"Unknown"
        }
    }
    //a template call history item class to represent a call log in the recycler view list
    data class CallHistoryItem(
        val name:String?,
        val number:String,
        val typeOfCall:Int,
        val lastMessage:String
    )
    // a class where the layout IDs correlated with each call log text view are identified
    private class CallItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val nameTextView:TextView = itemView.findViewById(R.id.nameText)
        val numberHolderTextView : TextView = itemView.findViewById(R.id.numberTextView)
        val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        val lastMessage:TextView = itemView.findViewById(R.id.lastMessageView)
    }
    //the function that returns data queried such as name, phone number, and the type of call
    private fun getCalls():List<CallHistoryItem>{
        val callHistoryList = mutableListOf<CallHistoryItem>()
        val details = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.CACHED_NAME,CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE)

        val queryCursor: Cursor? = baseContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            details,null,null, CallLog.Calls.DATE)
        queryCursor?.use {
            val nameIndex =it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeOfCallIndex = it.getColumnIndex(CallLog.Calls.TYPE)

            while(it.moveToNext()){
                val name =it.getString(nameIndex)
                val number = it.getString(numberIndex)
                val type = it.getInt(typeOfCallIndex)

                val callHistoryItem = CallHistoryItem(name,number,type,"etc etc")
                callHistoryList.add(callHistoryItem)
            }
        }
        val callHistoryItem = CallHistoryItem("BossMan","070038284",3
            ,getString(R.string.fired_Text))
        callHistoryList.add(callHistoryItem)
        return callHistoryList
    }
    //set the visibility of the core game components like the chapter and content textview as needed
    private fun setCoreItemsVisibility(visibility:Int){
        runOnUiThread {viewBinding.backButton.visibility = visibility
            viewBinding.chapter.visibility = visibility
            viewBinding.dividerLine.visibility = visibility
            viewBinding.secondDividerLine.visibility = visibility
            viewBinding.textContent.visibility = visibility}
    }
    //vibrate the device for 0.3 seconds at a given interval
    private fun vibrateDevice(interval:Long){
        val vibration: VibrationEffect = VibrationEffect.createOneShot(
            300,
            VibrationEffect.DEFAULT_AMPLITUDE
        )
        if(Build.VERSION.SDK_INT >=31){
            val vibratorManager: VibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE)
                    as VibratorManager
            val combinedVibration: CombinedVibration = CombinedVibration.createParallel(vibration)
            vibratorManager.cancel()
            continuous = CoroutineScope(Dispatchers.Main).launch {
                while(vibrationActive){
                    vibratorManager.vibrate(combinedVibration)
                    delay(interval)
                }
            }
            vibratorManager.vibrate(combinedVibration)
        }
        else {
            @Suppress("DEPRECATION")
            val vibrator: Vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
            continuous = CoroutineScope(Dispatchers.Main).launch {
                while(vibrationActive){
                    vibrator.vibrate(vibration)
                    delay(interval)
                }
            }

        }
    }
    //set the visibility of the messaging components as needed
    private fun setMessagingVisibility(visibility: Int){
        runOnUiThread {viewBinding.scrollView2.visibility = visibility
            viewBinding.headerTextView.visibility = visibility
            viewBinding.sendButton.visibility = visibility  }
    }
    //set up the SMS manager needed to send SMS
    private fun setSmsManager(){
        smsManager = getSystemService(SmsManager::class.java)
    }
    //called when sending the SMS
    private fun sendText(){
        val text = viewBinding.bodyText.text.trim().toString()

        //some checks in place
        if(text.isEmpty()){
            Toast.makeText(baseContext,"Text must be a letter long min!",Toast.LENGTH_SHORT).show()
            return
        }
        //text must be divided into a text array, and sent
        textList = smsManager.divideMessage(text)
        if(textList.size==1){
            Log.d(TAG,"Sending one part msg.")
            smsManager.sendTextMessage("+46705554758",null,text,null,null)
        }
        else{
            Log.d(TAG,"Sending multiple parts msg.")
            smsManager.sendMultipartTextMessage("+46705554758",null,
                textList as ArrayList<String>?,null,null)
        }
        setMessagingVisibility(View.GONE)
        setCoreItemsVisibility(View.VISIBLE)
        gameOver()
        Toast.makeText(baseContext,"Message Sent!",Toast.LENGTH_SHORT).show()
    }
    //called when the view is destroyed
    override fun onDestroy() {
        continuous?.cancel()
        mediaPlayer = null
        super.onDestroy()

    }
}