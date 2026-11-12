# هِمّة | Himma — خطة التنفيذ (Open-Source Native Android Plan)

**المنصة المستهدفة:** Native Android فقط (Kotlin + Jetpack Compose)  
**النموذج والترخيص:** تطبيق مجاني ومفتوح المصدر 100% (Free & Open Source — بدون باقات مدفوعة، بدون اشتراكات، بدون إعلانات، وبدون تتبع)  
**نظام التصميم:** Material Design 3 (M3)  
**قاعدة البيانات والتخزين:** محلي بالكامل على الجهاز (Room / SQLite + DataStore + نسخ احتياطي واستعادة محلية) — بدون خوادم سحابية أو حسابات  
**الويب:** غير موجود (تطبيق أندرويد فقط)  
**عدد الـ Specs:** 7 specs متساوية الحجم بنظام Vertical Slices

---

## القرارات المعمارية المعتمدة (Architectural Decisions)

| القرار | الخيار المعتمد | السبب |
|:---|:---|:---|
| **التقنية الأساسية** | Native Android (Kotlin + Jetpack Compose) | أعلى أداء ممكن ($< 10\text{ ms}$)، استقرار تام في الخلفية، وتجربة أندرويد أصلية |
| **نموذج التوزيع** | مجاني ومفتوح المصدر بالكامل (100% FOSS) | جميع الميزات متاحة للجميع بلا قيود أو جدران دفع، مع شفافية كاملة وقابلية البناء عبر GitHub Releases / F-Droid |
| **نظام التصميم** | Material Design 3 (Material 3) | مكونات أصلية حديثة، دعم Dynamic Color، مع ثيم هِمّة المظلم (AMOLED) والنهاري |
| **قاعدة البيانات** | Room Database (SQLite محلي) + Jetpack DataStore | كل البيانات محفوظة محلياً 100% على جهاز المستخدم بدون إنترنت أو سيرفر |
| **النسخ الاحتياطي والاستعادة** | تخزين واستعادة محلية على الجهاز (SAF / JSON & CSV) | يضمن حفظ واسترجاع بيانات التمارين والروتينات محلياً عند تغيير الجهاز أو إعادة التثبيت |
| **النطاق المنصاتي** | تطبيق موبايل أندرويد فقط (No Web) | تركيز كامل على تجربة الجيم على الهاتف، بدون لوحة ويب أو خدمات سحابية |
| **المعمارية** | Clean Architecture + MVVM + Hilt DI | فصل واضح بين الواجهة ومنطق القوة وقاعدة البيانات لسهولة الصيانة والمساهمة المفتوحة والاختبار |
| **اللغات** | العربية (RTL أساسي) + الإنجليزية (LTR) | واجهة RTL أصلية عبر `LayoutDirection.Rtl` و `AppCompatDelegate` |
| **المصادقة والخصوصية** | لا يوجد تسجيل دخول أو تتبع (Local Profile & Zero Telemetry) | التطبيق يعمل فوراً بعد شاشة إعداد أولية محلية بدون حساب سحابي أو أدوات تحليلات خارجية |

---

## هيكل المشروع المقترح (Android Project Structure)

```
himma-android-native/
├── assets/                          # الشعارات والوسائط المنسوخة للمشروع
│   └── logo/
├── app/
│   └── src/main/
│       ├── assets/                  # قاعدة التمارين الأولية (Seed JSON)
│       ├── res/                     # Vector Drawables، الخطوط، الترجمات (values-ar / values)، الثيمات
│       └── java/com/himma/app/
│           ├── core/
│           │   ├── theme/           # Material 3 ColorScheme, Typography (IBM Plex Sans Arabic + Inter), Shapes
│           │   ├── database/        # Room Database, DAOs, Entities
│           │   ├── datastore/       # تفضيلات المستخدم المحلية (اللغة، الوحدات، الثيم)
│           │   ├── locale/          # إدارة اللغة والاتجاه (AppCompatDelegate)
│           │   ├── backup/          # محرك التصدير والاستعادة المحلي (SAF / JSON / CSV)
│           │   └── di/              # Hilt Modules
│           ├── domain/              # نماذج الدومين وحسابات القوة (1RM, Total Volume, PRs)
│           ├── features/
│           │   ├── onboarding/      # الإعداد المحلي الأولي بدون حساب سحابي
│           │   ├── shell/           # الهيكل الرئيسي M3 وشريط التنقل السفلي
│           │   ├── exercises/       # مكتبة التمارين المحلية والتمارين المخصصة
│           │   ├── routines/        # منشئ الروتينات وترتيب التمارين (بعدد غير محدود)
│           │   ├── workout/         # شاشة التمرين النشط + Foreground Service للمؤقت
│           │   ├── analytics/       # الرسوم البيانية وسجل الأرقام القياسية (PRs) مجاناً بالكامل
│           │   ├── sharing/         # توليد Story Card ومشاركتها عبر Intent
│           │   └── settings/        # الإعدادات، المظهر، اللغة، والنسخ الاحتياطي المحلي
│           └── service/             # RestTimerForegroundService
├── build.gradle.kts
├── settings.gradle.kts
└── PLAN.md
```

---

## الخطة التفصيلية — 7 Specs (مفتوحة المصدر بالكامل)

---

### Spec 1 — Foundation & Material Design 3 Architecture

**الهدف:** تأسيس مشروع Native Android حديث بـ Kotlin و Jetpack Compose و Material Design 3، مع قاعدة البيانات المحلية ونظام اللغة والاتجاه.

**النطاق والمخرجات:**

- **تهيئة مشروع Android (Gradle Kotlin DSL):**
  - إنشاء المشروع بمعمارية Clean Architecture + MVVM + Hilt Dependency Injection
  - إعداد Jetpack Compose + Navigation Compose + Kotlin Coroutines & Flow

- **نظام التصميم Material Design 3 (M3 Theme):**
  - تطبيق `MaterialTheme` بمواصفات **Material 3** (`ColorScheme`, `Typography`, `Shapes`)
  - دعم **Dark-First AMOLED Theme** بألوان هِمّة الأساسية (الأخضر الزمردي مع خلفيات داكنة مريحة لإضاءة الجيم) + دعم Light Theme + خيار **Dynamic Color** لأندرويد 12+
  - تهيئة خط `IBM Plex Sans Arabic` للعربية و `Inter` للإنجليزية والأرقام
  - بناء الهيكل الأساسي للشاشات باستخدام مكونات Material 3:
    - `Scaffold` + `NavigationBar` (شريط التنقل السفلي)
    - `CenterAlignedTopAppBar`
    - ضبط مساحات اللمس ($\ge 48\text{ dp}$) لتناسب الاستخدام داخل الجيم

- **طبقة التخزين المحلي (Room + DataStore):**
  - تهيئة **Room Database** (`HimmaDatabase`) مع إعداد Migration strategy
  - تهيئة **Jetpack DataStore (Preferences)** لحفظ الإعدادات: اللغة (`ar`/`en`)، وحدة الوزن (`kg`/`lbs`)، وقت الراحة الافتراضي، وحالة ثيم Material 3
  - كيان `UserProfileEntity` محلي (الاسم الاختياري، وزن الجسم، تفضيلات التدريب) — بدون تسجيل دخول أو خوادم

- **التدويل ودعم العربية (i18n & RTL):**
  - ملفات موارد Android القياسية: `res/values-ar/strings.xml` (أساسي) و `res/values/strings.xml`
  - دعم تبديل اللغة من داخل التطبيق عبر `AppCompatDelegate.setApplicationLocales()`
  - دعم RTL الفيزيائي الكامل في Jetpack Compose (`LocalLayoutDirection`)

- **الإعداد الأولي المحلي (Local Onboarding):**
  - شاشة ترحيب مبسطة في أول فتح للتطبيق (اختيار اللغة، `kg/lbs`، وقت الراحة الافتراضي، الاسم والوزن اختيارياً) وحفظها محلياً فوراً

**معيار القبول:** فتح التطبيق على هاتف أندرويد يعرض واجهة Material 3 أصلية بالثيم المظلم، شاشة إعداد أولي تحفظ التفضيلات محلياً في DataStore و Room، وشريط تنقل سفلي M3 يعمل بسلاسة باللغتين العربية (RTL) والإنجليزية (LTR).

**التبعيات:** لا يوجد (أول Spec).

---

### Spec 2 — Exercise Library (Local Bundled Catalog)

**الهدف:** بناء مكتبة تمارين محلية كاملة مدمجة داخل التطبيق، سريعة البحث والتصفية، مع إمكانية إضافة تمارين مخصصة.

**النطاق والمخرجات:**

- **قاعدة البيانات المحلية (Room):**
  - إنشاء `ExerciseEntity` (id, nameEn, nameAr, primaryMuscle, secondaryMuscles, equipment, instructionsAr, instructionsEn, localMediaKey, defaultRestSeconds, isCustom, createdAt, updatedAt)
  - إنشاء `ExerciseDao` باستعلامات Flow تفاعلية للبحث والتصفية الفورية
  - تضمين Seed JSON في `assets/` يحتوي على 80–120 تمريناً أساسياً بالعربية والإنجليزية يتم تعبئتها تلقائياً في Room عند أول تشغيل

- **الوسائط المحلية (Bundled Local Media):**
  - حفظ صور/رسوم التمارين التوضيحية محلياً داخل التطبيق (`res/drawable` أو `assets/exercises/`) بدون اعتماد على الإنترنت أو CDN
  - عرض الصور والرسوم باستخدام مكتبة مفتوحة المصدر مثل **Coil** (`coil-compose`)

- **واجهة مكتبة التمارين (Material Design 3):**
  - شاشة المكتبة باستخدام `LazyColumn` عالية الأداء
  - شريط بحث **Material 3 `SearchBar`** يبحث لحظياً في الأسماء العربية والإنجليزية
  - شرائح تصفية أفقية **Material 3 `FilterChip`** حسب العضلة الأساسية (صدر، ظهر، أرجل، أكتاف، ذراعين، بطن) وحسب المعدات (بار، دمبل، كابل، جهاز، وزن الجسم)
  - شاشة تفاصيل التمرين بـ `ElevatedCard` و `AssistChip` توضح العضلات المستهدفة، التعليمات، وقت الراحة الافتراضي، والصورة التوضيحية

- **التمارين المخصصة (Custom Exercises):**
  - نموذج إنشاء وتعديل تمرين مخصص باستخدام `OutlinedTextField` و `ExposedDropdownMenuBox` (Material 3)
  - حفظ التمرين المخصص في Room مباشرة وظهوره فوراً في البحث والفلاتر بدون أي قيود عددية

**معيار القبول:** المستخدم يفتح التطبيق بدون إنترنت تماماً، يرى 100+ تمريناً بالعربية والإنجليزية مع رسومها المحلية، يبحث ويصفي بسرعة فورية، ويضيف تمريناً مخصصاً يُحفظ في قاعدة البيانات المحلية.

**التبعيات:** Spec 1.

---

### Spec 3 — Routine Builder (Unlimited & Open)

**الهدف:** تمكين المتدرب من تصميم وتنظيم عدد غير محدود من روتيناته التدريبية اليومية والأسبوعية محلياً بواجهات Material Design 3.

**النطاق والمخرجات:**

- **قاعدة البيانات المحلية (Room):**
  - إنشاء `RoutineEntity` (id, title, notes, orderIndex, createdAt, updatedAt, isDeleted)
  - إنشاء `RoutineExerciseEntity` (id, routineId, exerciseId, orderIndex, targetSets, targetRepsMin, targetRepsMax, targetRestSeconds) مع علاقة Foreign Key
  - إنشاء `RoutineDao` لاسترجاع الروتينات مع تمارينها عبر `@Transaction`

- **واجهة إدارة الروتينات (Material Design 3):**
  - شاشة "روتيناتي" تعرض البطاقات عبر Material 3 `ElevatedCard` مع عدد التمارين وزر بدء سريع `FilledTonalButton`
  - زر عائم **Material 3 `ExtendedFloatingActionButton`** لإنشاء روتين جديد
  - إنشاء، تعديل، استنساخ (Duplicate)، وحذف الروتين (مع Material 3 `Snackbar` للتراجع Undo) — بعدد غير محدود للجميع

- **محرر تمارين الروتين (Routine Exercise Editor):**
  - اختيار التمارين عبر **Material 3 `ModalBottomSheet`** مدمج مع بحث وفلاتر المكتبة (Spec 2)
  - إعادة ترتيب التمارين بالسحب والإفلات (Drag & Drop في `LazyColumn`)
  - تحديد عدد المجموعات المستهدفة، نطاق العدات (`min - max`)، ووقت الراحة الخاص بكل تمرين

**معيار القبول:** المتدرب ينشئ روتينات غير محدودة مثل "Push Day"، يضيف إليها التمارين ويرتبها ويحدد العدات والراحة، وتُحفظ جميعها محلياً في Room وتظهر فوراً بعد إعادة تشغيل التطبيق.

**التبعيات:** Spec 1, Spec 2.

---

### Spec 4 — Active Workout Session & Background Rest Timer

**الهدف:** بناء شاشة التمرين النشط فائقة السرعة في الجيم، مع الإكمال التلقائي ومؤقت الراحة الأصلي الذي يعمل في الخلفية عبر Android Foreground Service.

**النطاق والمخرجات:**

- **قاعدة البيانات المحلية (Room):**
  - إنشاء `WorkoutEntity` (id, routineId, title, startTime, endTime, durationSeconds, totalVolumeKg, notes, isCompleted)
  - إنشاء `WorkoutSetEntity` (id, workoutId, exerciseId, setIndex, weightKg, reps, rpe, setType, isCompleted, isPr, completedAt)
  - إنشاء `WorkoutDao` لاستعلام آخر أداء سابق واستخراج التاريخ

- **بدء وإدارة الجلسة النشطة:**
  - بدء تمرين من روتين (نسخ تمارين الروتين للجلسة) أو بدء "تمرين حر" فارغ
  - حفظ الجلسة النشطة كمسودة (`isCompleted = false`) لحظياً في Room بعد كل تعديل؛ بحيث إذا أُغلق التطبيق أو نُفذت البطارية، يستأنف التمرين تلقائياً من آخر مجموعة مسجلة
  - عداد زمني مستمر للجلسة (Elapsed Timer) في الـ `TopAppBar`

- **شاشة التسجيل السريع (Material Design 3):**
  - عرض التمارين والمجموعات في بطاقات Material 3 منظمة وواضحة تحت إضاءة الجيم
  - خانات وزن وعدات كبيرة سهلة اللمس مع أزرار زيادة سريعة (`+1`, `+2.5`, `+5 kg`) عبر `AssistChip`
  - تصنيف المجموعة (Set Tag: `Normal`, `Warmup`, `Drop Set`, `Failure`) عبر قائمة منسدلة أو Badge ملون
  - زر إضافة مجموعة جديدة وزر إضافة تمرين أثناء الجلسة
  - استجابة لمسية (`LocalHapticFeedback`) عند إتمام المجموعات

- **الإكمال التلقائي الذكي (Previous-Session Auto-fill):**
  - استعلام فوري محلي من Room لآخر مجموعة مكتملة لنفس التمرين وبنفس رقم المجموعة (`setIndex`)
  - عند الضغط على زر الإتمام (Checkmark) لمجموعة فارغة، يُعبأ الوزن والعدات تلقائياً من الجلسة السابقة

- **مؤقت الراحة التلقائي الأصلي (Native Android Foreground Service):**
  - بدء العد التنازلي تلقائياً بمجرد تعليم المجموعة كمكتملة، وإيقافه عند إلغاء التحديد
  - تحديد المدة حسب الأولوية: راحة التمرين في الروتين ← راحة التمرين الافتراضية ← إعداد المستخدم العام
  - شريط مؤقت مدمج أسفل الشاشة مع أزرار سريعة (`-15s`, `+15s`, `Skip`)
  - تشغيل **`RestTimerForegroundService`** مع إشعار دائم (Persistent Notification) يعرض العد التنازلي على شاشة القفل، ويهتز ويصدر صوتاً عند انتهاء الراحة حتى في وضع Doze Mode (مع مراعاة أذونات `POST_NOTIFICATIONS` و `FOREGROUND_SERVICE`)

- **إنهاء التمرين (Finish Workout):**
  - حساب `durationSeconds` و `totalVolumeKg` ($\sum \text{weight} \times \text{reps}$ للمجموعات المكتملة)، وتعليم `isCompleted = true`

**معيار القبول:** المتدرب يبدأ تمريناً، يلمس Checkmark فتُعبأ البيانات السابقة ويبدأ المؤقت، يغلق شاشة الهاتف فيرى العد التنازلي في الإشعارات ويهتز الهاتف عند الصفر، يغلق التطبيق ويفتحه فيجد التمرين مستمراً، وعند الإنهاء تُحفظ الجلسة محلياً في أقل من $10\text{ ms}$.

**التبعيات:** Spec 1, Spec 2, Spec 3.

---

### Spec 5 — Local Backup, Restore & Data Portability

**الهدف:** توفير محرك موثوق ومفتوح الصيغة لحفظ واستعادة بيانات التطبيق بالكامل محلياً على وحدة تخزين الجهاز (Local Device Backup & Restore)، لضمان حرية البيانات وعدم ضياعها أبداً.

**النطاق والمخرجات:**

- **تصدير النسخة الاحتياطية المحلية (Local Export):**
  - استخدام إطار عمل أندرويد الرسمي **Storage Access Framework (SAF)** (`ActivityResultContracts.CreateDocument`) للسماح للمستخدم بحفظ ملف نسخة احتياطية كامل بصيغة JSON مفتوحة وموثقة (`.himma.json`) في أي مجلد يختاره على جهازه (مثل `Downloads` أو بطاقة SD أو مشاركته لحفظه شخصياً)
  - تشمل النسخة: التمارين المخصصة، الروتينات، سجل التمارين الكامل، المجموعات، الأرقام القياسية، وأوزان الجسم

- **استعادة البيانات المحلية (Local Restore & Import):**
  - استخدام `ActivityResultContracts.OpenDocument` لاختيار ملف النسخة الاحتياطية من الجهاز
  - التحقق الصارم من سلامة الملف وبنيته (Schema & Version Validation) قبل الاستعادة
  - خيار للمستخدم عبر Material 3 `AlertDialog`: **استبدال البيانات الحالية بالكامل (Replace)** أو **دمج البيانات الناقصة (Smart Merge)** دون تكرار

- **التصدير كجداول بيانات (CSV Export):**
  - خيار تصدير سجل التمارين بصيغة `.csv` لفتحه في LibreOffice Calc أو Excel أو Google Sheets

- **النسخ التلقائي الدوري المحلي (Auto-Local Snapshot):**
  - جدولة مهمة خفيفة عبر **WorkManager** لإنشاء نسخة احتياطية محلية دورية داخل التخزين المحلي أو عبر `backup_rules.xml`

**معيار القبول:** المتدرب يضغط "تصدير نسخة احتياطية" فيحفظ الملف في مجلد Downloads على هاتفه. يحذف بيانات التطبيق بالكامل أو يثبته من جديد، ثم يختار "استعادة نسخة احتياطية" من نفس الملف فتعود كل روتيناته وسجل تمارينه وأرقامه القياسية كما كانت بدقة 100%.

**التبعيات:** Spec 1, Spec 2, Spec 3, Spec 4.

---

### Spec 6 — Analytics, Progression Insights & Personal Records (PR Engine)

**الهدف:** بناء محرك الأرقام القياسية وحساب الـ 1RM، وتوصيات التقدم التدريجي المحلية، والرسوم البيانية التفاعلية بالكامل ومجاناً لجميع المستخدمين.

**النطاق والمخرجات:**

- **قاعدة البيانات المحلية (Room):**
  - إنشاء `PersonalRecordEntity` (id, exerciseId, maxWeightKg, maxVolumeSetKg, estimated1rmKg, achievedAt, workoutId)
  - إنشاء `BodyWeightEntity` (id, weightKg, recordedAt) لتتبع وزن الجسم محلياً
  - استعلامات تجميعية (SQL Aggregations) سريعة في Room لحساب الحجم الأسبوعي وتطور الأوزان

- **محرك الأرقام القياسية والتقدم المحلي (PR & Progression Engine — يعمل دائماً أوفلاين):**
  - حساب الحد الأقصى التقديري للتكرار الواحد بمعادلة Brzycki:
    $$1RM = \frac{\text{weight}}{1.0278 - 0.0278 \times \text{reps}}$$
  - فحص الأرقام القياسية تلقائياً فور إنهاء التمرين (ربط مع Spec 4) وتحديث `PersonalRecordEntity` وتعليم `isPr = true` على المجموعة المحطمة للرقم مع استجابة لمسية عند كسر الرقم
  - خوارزميات محلية بسيطة وشفافة لاقتراح الزيادة التدريجية (Progressive Overload Hint) وتنبيه نقص حجم التمرين لعضلة مقارنة ببقية العضلات — دون أي ذكاء اصطناعي خارجي أو API مدفوع

- **لوحة التحليلات المتكاملة (متاحة بالكامل ومجاناً بدون أي قفل):**
  - شاشة السجل التاريخي (Workout History) ببطاقات Material 3 تعرض التاريخ، المدة، إجمالي الحجم، وعدد الـ PRs
  - شاشة تفاصيل الجلسة السابقة كاملة
  - مخطط وزن الجسم (7 / 30 / 90 يوماً)
  - رسوم بيانية أصلية لـ Jetpack Compose (باستخدام Compose `Canvas` أو مكتبة مفتوحة المصدر مثل **Vico**):
    - منحنى الحمل التدريجي (Progressive Overload) لأقصى وزن وحجم لكل تمرين
    - منحنى تطور الـ `Estimated 1RM` عبر الزمن
    - توزيع الحجم التدريبي الأسبوعي على المجموعات العضلية
  - شاشة **سجل الأرقام القياسية الشامل (PR Ledger)** مع شارة 🏆 بجانب المجموعات القياسية

**معيار القبول:** عند إنهاء تمرين وتحقيق وزن جديد في تمرين السكوات، يلتقط المحرك الرقم القياسي فوراً ويضع شارة 🏆، وتنعكس البيانات في جميع مخططات التطور وتوزيع العضلات محلياً بدون إنترنت وبشكل مجاني بالكامل.

**التبعيات:** Spec 1, Spec 2, Spec 4.

---

### Spec 7 — Story Card, Native Sharing & Open-Source Release Polish

**الهدف:** تحويل كل تمرين مكتمل إلى بطاقة إنجاز بصرية بدقة 1080×1920 لمشاركتها عبر أندرويد، وإتمام تلميع التجربة والاختبارات وتجهيز حزمة مفتوحة المصدر نظيفة وقابلة للبناء للجميع.

**النطاق والمخرجات:**

- **شاشة الاحتفال بعد التمرين (Post-Workout Celebration Screen):**
  - تظهر فور إنهاء التمرين بتصميم Material 3 احتفالي
  - تعرض: مدة التمرين، إجمالي الحجم المرفوع، عدد المجموعات، وشارات الأرقام القياسية الجديدة المحققة
  - زر رئيسي بارز (`FilledButton`): **"مشاركة كقصة 📸"** وزر ثانوي (`TextButton`): "تم"

- **محرك توليد الصورة الأصلي (Native Compose to Bitmap Generator):**
  - تصميم بطاقة الإنجاز كـ Composable مستقل بمقاسات قياسية (`1080 × 1920 px`)
  - تحويل الـ Composable إلى `Bitmap` عالي الدقة مباشرة باستخدام **`GraphicsLayer.toImageBitmap()`** أو `Picture` Canvas بدون مكتبات هشة
  - محتويات البطاقة: شعار هِمّة الموجود في `assets/logo/`، اسم الروتين، المدة، إجمالي الحجم المنسق، رسم العضلات المدربة (`VectorDrawable` ملون ديناميكياً)، وشارات الـ PRs
  - إتاحة **كل ثيمات البطاقة (3–5 ثيمات احترافية) وخيار إظهار/إخفاء الشعار مجاناً للجميع**

- **المشاركة الأصلية عبر أندرويد (`FileProvider` + `Intent`):**
  - حفظ الصورة المؤقتة في `cacheDir` ومشاركتها عبر **`FileProvider`** و **`Intent.ACTION_SEND`** (أو `com.instagram.share.ADD_TO_STORY` / ShareSheet النظامية)
  - دعم خيار حفظ البطاقة في معرض الصور (`MediaStore`)

- **تلميع التجربة، الاختبارات، وتجهيز الإطلاق مفتوح المصدر (FOSS Polish & QA):**
  - شاشات الحالة الفارغة (Empty States) الجذابة عند عدم وجود روتينات أو تمارين مسجلة
  - تحريكات سلسة (`AnimatedContent`, `animateContentSize`) في التنقل والمؤقت
  - Unit Tests لحسابات 1RM، الحجم الكلي، اكتشاف الأرقام القياسية، ومحرك النسخ الاحتياطي والاستعادة
  - Room Migration & DAO Tests و Compose UI Tests لأهم مسارات التطبيق وفحص شامل لـ RTL بالعربية
  - خلو التطبيق التام من أي مكتبات مغلقة أو Google Play Billing أو Trackers، وإعداد R8/ProGuard وملف `LICENSE` ووثائق البناء من المصدر (`README.md`) لبناء APK/AAB نظيف للنشر عبر GitHub Releases أو F-Droid

**معيار القبول:** المستخدم ينهي التمرين → تظهر شاشة الاحتفال → يشارك البطاقة بأي ثيم يختاره في أقل من ثانية، ويمر التطبيق بجميع اختبارات الوحدة والواجهة، ويُبنى بنسخة Release نظيفة 100% مفتوحة المصدر بدون أي تبعيات تجارية أو مدفوعة.

**التبعيات:** Spec 1, Spec 4, Spec 6.

---

## مخطط التبعيات (Dependency Graph)

```
Spec 1 (Foundation & Material 3)
  ├── Spec 2 (Local Exercise Library)
  │     ├── Spec 3 (Routine Builder — Unlimited)
  │     │     └── Spec 4 (Active Workout & Foreground Service)
  │     │           ├── Spec 5 (Local Backup & Restore)
  │     │           └── Spec 6 (Analytics, Progression & PRs)
  │     │                 └── Spec 7 (Story Card, Sharing & FOSS Polish)
```

---

## ملخص الأحجام المتوقعة (Estimated Scope Balance)

| Spec | المكونات الرئيسية | التقنيات الأساسية | الحجم النسبي |
|:---|:---|:---|:---|
| 1 — Foundation & M3 | Gradle + M3 Theme + Room + DataStore + RTL | Kotlin, Compose M3, Hilt | ████████░░ |
| 2 — Exercise Library | Seed DB + Local Media + Search/Filter + Custom | Room, Coil, M3 SearchBar | ████████░░ |
| 3 — Routine Builder | CRUD + Drag&Drop + Unlimited Routines | Room, BottomSheet, M3 Cards | ███████░░░ |
| 4 — Active Workout | Logger + Auto-fill + Rest Timer Service | Compose, ForegroundService | █████████░ |
| 5 — Backup & Restore | Local Export/Import + Merge + CSV | SAF, WorkManager, JSON | ████████░░ |
| 6 — Analytics & PRs | Brzycki 1RM + PR Detection + Full Free Charts | Room Aggregations, Canvas/Vico | ████████░░ |
| 7 — Story Card & FOSS Polish | Compose-to-Bitmap + Share + All Themes + QA | GraphicsLayer, FileProvider, Tests | ████████░░ |
