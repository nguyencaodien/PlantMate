# PlantMate release keep rules. Firestore documents are mapped by hand (no toObject), and
# Room, Firebase and AndroidX ship their own consumer rules, so little is needed here.

# Keep line numbers so crash reports stay readable; hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Custom views are created from XML by class name.
-keep class com.example.ungdungchiasecaycanh.ui.**.*View { <init>(android.content.Context, android.util.AttributeSet); }
