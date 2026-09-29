# kotlinx.serialization: keep generated serializers for @Serializable classes (navigation routes, catalog DTOs).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
