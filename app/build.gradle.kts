plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.softcraft.a1logistics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.softcraft.a1logistics"
        minSdk = 28
        targetSdk = 35
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Android Studio Meerkat Feature Drop | 2024.3.2 May 6, 2025
    // android studio version 2024.3.2.14 link "https://r4---sn-q4fl6ndl.gvt1.com/edgedl/android/studio/install/2024.3.2.14/android-studio-2024.3.2.14-windows.exe?met=1761495076,&mh=Rj&pl=24&rms=ltu,ltu&shardbypass=sd&cm2rm=sn-puxacq-q5jl7e,sn-npokl76&rrc=80,80&fexp=24353128&req_id=4b4562a9ec7fa3fc&redirect_counter=2&cms_redirect=yes&cmsv=e&mip=103.89.26.170&mm=34&mn=sn-q4fl6ndl&ms=ltu&mt=1761494918&mv=m&mvi=4&rmhost=r3---sn-q4fl6ndl.gvt1.com"
    // Gradle version 8.11.1
    // agp (libs.versions.toml) version 8.10.1
    //git add .
    //git commit -m "Prepare release 1.4"
    //git push origin main
    //git tag -a v1.4 -m "Release 1.4"
    //git push origin v1.4
    // gh release create v1.4 "app\release\app-release.apk" --title "Version 1.4" --notes "Initial release with Advanced functionality"

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("org.apache.poi:poi:5.2.2")
    implementation("org.apache.poi:poi-ooxml:5.2.2")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.media:media:1.6.0")
    implementation("com.google.android.gms:play-services-base:18.6.0") // Usually required
    implementation("androidx.appcompat:appcompat:1.6.1") // or the latest version
    implementation("com.airbnb.android:lottie:6.1.0") // Latest stable version
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    // https://mvnrepository.com/artifact/com.google.android.gms/play-services-auth-api-phone
    implementation("com.google.android.gms:play-services-auth-api-phone:18.2.0")
    // https://mvnrepository.com/artifact/androidx.gridlayout/gridlayout
    implementation("androidx.gridlayout:gridlayout:1.1.0")

    implementation("com.google.android.material:material:1.6.0")// or higher
    implementation("com.google.firebase:firebase-storage:20.3.0")  // For APK hosting
    implementation("com.google.firebase:firebase-appdistribution:16.0.0-beta10")  // For update checks
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.config)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}