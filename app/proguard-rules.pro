# kotlinx.serialization: conservar los serializadores generados de los DTO.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class bo.ahgora.comunidad.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class bo.ahgora.comunidad.data.remote.**$$serializer { *; }
-dontwarn org.osmdroid.**
