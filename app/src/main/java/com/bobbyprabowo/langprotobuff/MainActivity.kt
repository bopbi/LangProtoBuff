package com.bobbyprabowo.langprotobuff

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.protobuf.TranslationList

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val protoMessage = readProtobufBinaryFromAssets(this, "schema.bin")
        Toast.makeText(this, protoMessage?.getTranslations(1)?.jp, Toast.LENGTH_LONG).show()
    }

    private fun readProtobufBinaryFromAssets(context: Context, fileName: String): TranslationList? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                TranslationList.parseFrom(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
