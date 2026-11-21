# هِمّة | Himma

<div align="center">

![Version](https://img.shields.io/badge/Version-v1.2.0-16A34A?logo=github&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-BOM%202024.12.01-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%20Design-3-16A34A?logo=materialdesign&logoColor=white)
![Android SDK](https://img.shields.io/badge/Android-8.0+_(SDK_26--35)-brightgreen?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-PolyForm%20Noncommercial-blue)
![Offline](https://img.shields.io/badge/100%25-Offline%20&%20Private-black)

<br/>

**تطبيق أندرويد مفتوح المصدر لتسجيل تمارين الجيم وتتبع الأحمال التدريبية بدون إنترنت، بدون إعلانات، وبخصوصية تامة 100%.**  
*A 100% offline, privacy-first native Android gym workout tracker and progressive overload companion.*

</div>

---

## ⚡ أبرز المميزات

- **🏋️‍♂️ مكتبة تمارين مدمجة (302 تمرين):** صور توضيحية ثنائية الإطارات، تعليمات باللغتين العربية والإنجليزية، وبحث لحظي ذكي يتجاهل التشكيل، مع إمكانية إضافة تمارين مخصصة.
- **⏱️ تتبع الأوزان والأزمنة والبلاطات:** دعم تمارين الوزن والعدات، تمارين المدة والوقت (كالكارديو والبلانك)، ونظام البلاطات (`Stack Plates`) لأجهزة الكيبل.
- **📋 منشئ روتينات مرن:** إنشاء وتكرار عدد غير محدود من الجداول التدريبية، مع تحديد التكرارات والمجموعات ومؤقتات الراحة لكل تمرين ومعاينة سريعة للحركات.
- **⚡ جلسة تمرين مريحة:** شريط تقدم مباشر، أزرار زيادة سريعة، إكمال تلقائي من الجلسات السابقة، ومؤقت راحة في الخلفية (`ForegroundService`) يظهر على شاشة القفل والإشعارات.
- **📈 أرقام قياسية وتحليلات:** حساب تلقائي للحد الأقصى (Brzycki 1RM)، رصد الأرقام القياسية (PRs)، ورسوم بيانية لتوزيع الحجم الأسبوعي وتطور الأوزان ووزن الجسم.
- **💾 نسخ احتياطي مفتوح:** تصدير واستعادة كاملة للبيانات محلياً عبر JSON وCSV (متوافق مع Excel)، مع نسخ احتياطي تلقائي دوري.
- **🎨 مظهر عصري وأداء فائق:** تصميم AMOLED داكن ومظهر Frost (ثلجي)، وشريط تنقل عائم، مع إمكانية إيقاف الحركات لتسريع الاستجابة على الأجهزة الاقتصادية.
- **🔄 تحديثات مباشرة:** فحص وتحميل التحديثات وتثبيتها من داخل التطبيق مباشرة عبر GitHub Releases.

---

## 🛠️ البنية التقنية

| المجال | التقنية |
|:---|:---|
| **اللغة والواجهة** | Kotlin `2.0.21` + Jetpack Compose (BOM `2024.12.01`) + Material 3 |
| **قاعدة البيانات والتفضيلات** | Room SQLite `2.6.1` (Offline) + DataStore Preferences |
| **المعمارية وحقن الاعتماديات** | Clean Architecture + MVVM + Dagger Hilt `2.52` |
| **المهام والمؤقت** | AndroidX WorkManager `2.10.0` + Foreground Service |
| **الصور والوسائط** | Coil Compose `2.7.0` (Local WebP Assets) |

---

## 📦 التحميل والبناء

### تحميل ملف الـ APK
حمّل أحدث إصدار جاهز للتثبيت مباشرة من صفحة **[GitHub Releases](https://github.com/abdo13579/himma-android-native/releases)**.

### البناء من المصدر (Build from Source)
```bash
git clone https://github.com/abdo13579/himma-android-native.git
cd himma-android-native

# تشغيل الاختبارات
./gradlew testDebugUnitTest

# بناء حزمة الإنتاج الرسمية (Production Release APK)
./gradlew assembleRelease
# الناتج: app/build/outputs/apk/release/app-release.apk

# أو تثبيت حزمة التطوير مباشرة عبر ADB
./gradlew installDebug
```

---

## 📄 الترخيص

مرخص بموجب **[PolyForm Noncommercial License 1.0.0](LICENSE)** — متاح ومجاني بالكامل للاستخدام الشخصي، والدراسة، والتطوير غير التجاري.
