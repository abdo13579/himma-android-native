# هِمّة | Himma

<div align="center">

![Version](https://img.shields.io/badge/Version-v1.3.0-16A34A?logo=github&logoColor=white)
[![Google Play](https://img.shields.io/badge/Google%20Play-Available-01875F?logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=com.himma.fitness)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-BOM%202024.12.01-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%20Design-3-16A34A?logo=materialdesign&logoColor=white)
![Android SDK](https://img.shields.io/badge/Android-8.0+_(SDK_26--36)-brightgreen?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-PolyForm%20Noncommercial-blue)
![Offline-First](https://img.shields.io/badge/Offline--First-Private-black)

<br/>

**تطبيق أندرويد مفتوح المصدر لتسجيل تمارين الجيم وتتبع الأحمال التدريبية بمبدأ الأولوية للوضع المحلي (Offline-First)، بدون إعلانات، مع عمل جميع الميزات الأساسية بدون إنترنت.**  
*An offline-first, privacy-respecting native Android gym workout tracker and progressive overload companion — core features work completely offline with zero ads.*

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
- **🔄 قناتا توزيع رسميتان (Google Play & GitHub):** متاح رسمياً على **[متجر Google Play](https://play.google.com/store/apps/details?id=com.himma.fitness)** (تحديثات مُدارة عبر المتجر ومتوافقة تماماً مع سياساته)، وعبر **[GitHub Releases](https://github.com/abdo13579/himma-android-native/releases)** مع ميزة فحص وتثبيت التحديثات مباشرة من داخل التطبيق.

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

## 📦 التحميل وإصدارات البناء (Build Flavors)

يتوفر التطبيق رسمياً عبر قناتين رئيسيتين بنفس معرّف الحزمة (`com.himma.fitness`) وبكامل المميزات دون أي فروق في الخصوصية أو قاعدة البيانات المحلية، عبر نسختي بناء (`distribution` flavors):

| النسخة (`Flavor`) | القناة | آلية التحديثات | صلاحية `REQUEST_INSTALL_PACKAGES` |
|:---|:---|:---|:---|
| **`store`** | **[متجر Google Play](https://play.google.com/store/apps/details?id=com.himma.fitness)** | تُدار تلقائياً عبر متجر Google Play (`SUPPORTS_IN_APP_UPDATES = false`) | ❌ غير مضمّنة (امتثال كامل لسياسات المتجر) |
| **`github`** | **[GitHub Releases](https://github.com/abdo13579/himma-android-native/releases)** | فحص وتثبيت التحديثات من داخل التطبيق عبر GitHub (`SUPPORTS_IN_APP_UPDATES = true`) | ✅ مضمّنة لتثبيت التحديثات المباشرة |

### التحميل المباشر
- **متجر Google Play (نسخة المتجر `store`):** ثبّت التطبيق مباشرة من **[Google Play](https://play.google.com/store/apps/details?id=com.himma.fitness)**.
- **ملف APK مباشر (نسخة `github`):** حمّل أحدث إصدار جاهز للتثبيت من صفحة **[GitHub Releases](https://github.com/abdo13579/himma-android-native/releases)**.

### البناء من المصدر (Build from Source)
```bash
git clone https://github.com/abdo13579/himma-android-native.git
cd himma-android-native

# تشغيل اختبارات الوحدة (لكلتا النسختين)
./gradlew testGithubDebugUnitTest testStoreDebugUnitTest
# أو للاختصار (تشغّل testGithubDebugUnitTest):
./gradlew testDebugUnitTest

# بناء حزمة الإنتاج لـ GitHub (APK مع التحديث التلقائي)
./gradlew assembleGithubRelease
# الناتج: app/build/outputs/apk/github/release/app-github-release.apk

# بناء حزمة الإنتاج لمتجر Google Play (AAB متوافق 100% مع سياسات المتجر)
./gradlew bundleStoreRelease
# الناتج: app/build/outputs/bundle/storeRelease/app-store-release.aab

# أو تثبيت حزمة التطوير مباشرة عبر ADB
./gradlew installGithubDebug
# أو لنسخة المتجر:
./gradlew installStoreDebug
```

---

## 🔒 الخصوصية (Privacy Policy)

يعمل تطبيق هِمّة بمبدأ الأولوية للوضع المحلي (Offline-First)؛ حيث تُحفظ بيانات تمارينك على جهازك وتعمل جميع الميزات الأساسية دون الحاجة للاتصال بالإنترنت، ودون إعلانات أو تتبّع طرف ثالث، بينما تبقى أي ميزات أو تفاعلات تتطلب الإنترنت اختيارية بالكامل ولا تؤثر على عمل التطبيق بدون اتصال.  
يمكنك مراجعة **[سياسة الخصوصية الرسمية (Privacy Policy)](PRIVACY_POLICY.md)** للتفاصيل.

---

## 📄 الترخيص

مرخص بموجب **[PolyForm Noncommercial License 1.0.0](LICENSE)** — متاح ومجاني بالكامل للاستخدام الشخصي، والدراسة، والتطوير غير التجاري.
