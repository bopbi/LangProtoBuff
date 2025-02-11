package com.bobbyprabowo.langprotobuff

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.protobuf.TranslationList
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private val dataStore: DataStore<TranslationList> by dataStore(
        fileName = "translation.pb",
        serializer = TranslationListSerializer(),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            this@MainActivity.assets.open("schema.bin").use { inputStream ->
                dataStore.updateData { translationList ->
                    translationList.toBuilder().mergeFrom(inputStream).build()
                }
            }
            dataStore.data.first().let { translationList ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, translationList.getTranslations(0)?.en, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    class TranslationListSerializer : Serializer<TranslationList> {

        override val defaultValue: TranslationList = TranslationList.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): TranslationList {
            return try {
                TranslationList.parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

        override suspend fun writeTo(t: TranslationList, output: OutputStream) {
            t.writeTo(output)
        }
    }
}
