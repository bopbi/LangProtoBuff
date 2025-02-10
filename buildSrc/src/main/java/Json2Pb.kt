import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

abstract class Json2Pb : DefaultTask() {

    data class Translation(val name: String, val en: String, val jp: String)

    @TaskAction
    fun runTask() {
        val inputJsonPath = "lang/schema.json"
        val outputProtoPath = inputJsonPath.replace(".json", ".bin")

        val inputFile = File(inputJsonPath)
        if (!inputFile.exists()) {
            throw GradleException("JSON file not found: $inputJsonPath")
        }

        val json = inputFile.readText()
        val jsonObject = JSONObject(json)

        val translations = mutableListOf<Translation>()
        for (key in jsonObject.keys()) {
            val translation = Translation(
                name = key,
                en = jsonObject.getJSONObject(key).getString("en"),
                jp = jsonObject.getJSONObject(key).getString("jp")
            )
            translations.add(translation)
        }

        val jsonArray = JSONArray(translations.map {
            JSONObject().put("name", it.name).put("en", it.en).put("jp", it.jp)
        })

        // Define Protobuf Descriptor at runtime
        val descriptor = DescriptorProto.newBuilder()
            .setName("Translation")
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("name")
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
            )
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("en")
                .setNumber(2)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
            )
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("jp")
                .setNumber(3)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
            )
            .build()

        // Define a repeated list of "DynamicItem"
        val listDescriptor = DescriptorProto.newBuilder()
            .setName("TranslationList")
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("translations")
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setTypeName("Translation")
                .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) // Repeated list
            )
            .build()

        // Build file descriptor dynamically
        val fileDescriptorSet = DescriptorProtos.FileDescriptorSet.newBuilder()
            .addFile(DescriptorProtos.FileDescriptorProto.newBuilder()
                .addMessageType(descriptor)
                .addMessageType(listDescriptor)
            )
            .build()

        val fileDescriptor = Descriptors.FileDescriptor.buildFrom(
            fileDescriptorSet.fileList.first(), arrayOf()
        )

        val translationDescriptor = fileDescriptor.findMessageTypeByName("Translation")
        val translationListDescriptor = fileDescriptor.findMessageTypeByName("TranslationList")

        // Convert JSON to Protobuf DynamicMessage
        val jsonParser = JsonFormat.parser()
        val listBuilder = DynamicMessage.newBuilder(translationListDescriptor)

        for (itemJson in jsonArray) {
            val itemBuilder = DynamicMessage.newBuilder(translationDescriptor)
            jsonParser.merge(itemJson.toString(), itemBuilder)
            listBuilder.addRepeatedField(translationListDescriptor.findFieldByName("translations"), itemBuilder.build())
        }

        val protoData = listBuilder.build()

        // Serialize to binary
        val binaryData = protoData.toByteArray()
        File(outputProtoPath).writeBytes(binaryData)

        println("✅ Protobuf binary saved successfully!")

        // Deserialize Protobuf Binary back
        val deserializedProto = DynamicMessage.parseFrom(translationListDescriptor, binaryData)
        println("🔄 Deserialized Protobuf Message: $deserializedProto")


//        val structBuilder = Struct.newBuilder()
//        JsonFormat.parser().ignoringUnknownFields().merge(formattedJson, structBuilder)
//        val protoData = structBuilder.build().toByteArray()
//        Files.write(Paths.get(outputProtoPath), protoData)
//        println("Converted JSON to ProtoBuf: $outputProtoPath")
    }
}
