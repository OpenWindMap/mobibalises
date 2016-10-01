# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\PMU\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}




-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keep public class com.google.android.vending.licensing.ILicensingService


-keep public class org.pedro.android.mobibalises_common.view.BaliseDrawable {
    protected BaliseDrawable(java.lang.String, org.pedro.balises.BaliseProvider, org.pedro.android.map.MapView, java.lang.String);
}
-keep public class com.pedro.android.mobibalises.view.FullBaliseDrawable {
    public FullBaliseDrawable(java.lang.String, org.pedro.balises.BaliseProvider, org.pedro.android.map.MapView, java.lang.String);
}

-keep public class org.pedro.android.map.tileprovider.MBTilesTileProvider {
    public MBTilesTileProvider(java.lang.String, java.lang.String);
}
-keep public class org.pedro.android.map.tileprovider.MBTilesDirectoryTileProvider {
    public MBTilesDirectoryTileProvider(java.lang.String, java.lang.String);
}


-keep public class org.pedro.android.widget.DragDropListView
-keep public class org.pedro.android.widget.SliderPreference
-keep public class org.pedro.android.widget.VerticalTextView
-keep public class org.pedro.android.widget.R


-keep public class org.pedro.android.map.MultiTouchHandler


-keep public class org.pedro.spots.Spot {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class * extends org.pedro.spots.Spot {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class org.pedro.spots.Orientation {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class org.pedro.spots.Pratique {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class org.pedro.spots.TypeSpot {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


-keep public class org.pedro.balises.Balise {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class * extends org.pedro.balises.Balise {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class org.pedro.balises.Releve {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class * extends org.pedro.balises.Releve {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
