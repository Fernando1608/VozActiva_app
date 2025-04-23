package com.example.vozactiva_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var btnVoice: Button
    private lateinit var tvResult: TextView
    private lateinit var tvStatus: TextView

    private val REQUEST_RECORD_AUDIO_PERMISSION = 101
    private val REQUEST_SEND_SMS_PERMISSION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnVoice = findViewById(R.id.btnVoice)
        tvResult = findViewById(R.id.tvResult)
        tvStatus = findViewById(R.id.tvStatus)

        // Verificar y solicitar permisos
        checkPermissions()

        // Inicializar reconocimiento de voz
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)

        btnVoice.setOnClickListener {
            startListening()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                REQUEST_SEND_SMS_PERMISSION
            )
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: Envía mensaje a Juan diciendo Hola, ¿cómo estás?")

        try {
            speechRecognizer.startListening(intent)
            tvStatus.text = "Escuchando..."
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Implementación de RecognitionListener
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        tvStatus.text = "Procesando comando..."
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de red agotado"
            SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el comando"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de habla agotado"
            else -> "Error desconocido"
        }
        tvStatus.text = errorMessage
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val bestMatch = matches[0]
            tvResult.text = "Comando: $bestMatch"
            processVoiceCommand(bestMatch)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processVoiceCommand(command: String) {
        val lowerCommand = command.toLowerCase()

        if (lowerCommand.contains("envía mensaje a") &&
            (lowerCommand.contains("diciendo") || lowerCommand.contains("decir"))) {

            try {
                // Extraer el contacto y el mensaje
                val parts = command.split("diciendo", "decir", ignoreCase = true)
                val contactPart = parts[0].replace("envía mensaje a", "", ignoreCase = true).trim()
                val message = parts[1].trim()

                // Aquí deberías implementar la búsqueda del contacto en la agenda
                // Por simplicidad, asumiremos que el usuario dijo el número directamente
                val phoneNumber = extractPhoneNumber(contactPart)

                if (phoneNumber.isNotEmpty()) {
                    sendSMS(phoneNumber, message)
                    tvStatus.text = "Mensaje enviado a $phoneNumber"
                } else {
                    tvStatus.text = "No se pudo obtener el número de teléfono"
                }
            } catch (e: Exception) {
                tvStatus.text = "Error procesando el comando: ${e.message}"
            }
        } else {
            tvStatus.text = "Comando no reconocido. Intenta decir: 'Envía mensaje a [contacto] diciendo [mensaje]'"
        }
    }

    private fun extractPhoneNumber(contactPart: String): String {
        // Implementación básica - asume que el usuario dijo el número directamente
        // En una app real, deberías buscar en los contactos del dispositivo

        // Extraer solo dígitos
        return contactPart.replace("[^0-9]".toRegex(), "")
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "Mensaje enviado a $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al enviar SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
