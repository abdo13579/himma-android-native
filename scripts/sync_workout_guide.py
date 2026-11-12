#!/usr/bin/env python3
"""Sync & build Himma's offline exercise catalog + 3-frame WebP illustrations.

Open-Source Artwork & Metadata Sources (CC BY-SA 4.0 / MIT):
  - Visual illustrations & manifest: bryllim/workout-guide (derived from Everkinetic)
    https://github.com/bryllim/workout-guide (CC BY-SA 4.0 assets)
    https://github.com/everkinetic/data (CC BY-SA 4.0 original poses & steps)

Outputs:
  app/src/main/assets/exercises_seed.json
  app/src/main/assets/exercises/<slug>/<0|1|2>.webp
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import shutil
import sys
import urllib.request
from pathlib import Path
from typing import Any

WG_MANIFEST_URL = (
    "https://raw.githubusercontent.com/bryllim/workout-guide/main/packages/workout-guide/manifest.json"
)
WG_ASSET_BASE = (
    "https://raw.githubusercontent.com/bryllim/workout-guide/main/packages/workout-guide/assets"
)
EK_EXERCISES_URL = (
    "https://raw.githubusercontent.com/everkinetic/data/main/dist/exercises.json"
)

# Normalized into Himma's 6 core MuscleGroup values:
MUSCLE_MAP: dict[str, str] = {
    "chest": "CHEST",
    "back": "BACK",
    "lats": "BACK",
    "upper back": "BACK",
    "lower back": "BACK",
    "posterior chain": "BACK",
    "quads": "LEGS",
    "hamstrings": "LEGS",
    "glutes": "LEGS",
    "calves": "LEGS",
    "legs": "LEGS",
    "adductors": "LEGS",
    "hips": "LEGS",
    "shoulders": "SHOULDERS",
    "rear delts": "SHOULDERS",
    "biceps": "ARMS",
    "triceps": "ARMS",
    "forearms": "ARMS",
    "core": "CORE",
}

# Normalized into Himma's 5 core EquipmentType values:
EQUIPMENT_MAP: dict[str, str] = {
    "barbell": "BARBELL",
    "dumbbell": "DUMBBELL",
    "kettlebell": "DUMBBELL",
    "cable": "CABLE",
    "resistance band": "CABLE",
    "machine": "MACHINE",
    "bodyweight": "BODYWEIGHT",
    "pull-up bar": "BODYWEIGHT",
    "bench": "BODYWEIGHT",
    "chair": "BODYWEIGHT",
    "box": "BODYWEIGHT",
    "wall": "BODYWEIGHT",
    "plate": "BARBELL",
}

# Curated core gym & calisthenics catalog: slug -> (nameAr, instructionsAr, defaultRestSeconds)
CURATED_AR: dict[str, tuple[str, str, int]] = {
    # ---------------- CHEST (16) ----------------
    "bench-press": (
        "بنش برس بالبار (مستوي)",
        "استلقِ على المقعد المستوي وثبّت قدميك على الأرض.\nامسك البار بقبضة أوسع قليلاً من الكتفين واخفضه بتحكم إلى منتصف الصدر.\nادفع البار للأعلى مع الزفير دون قفل المرفقين.",
        120,
    ),
    "incline-bench-press": (
        "بنش برس مائل للأعلى بالبار",
        "اضبط المقعد بزاوية 30 إلى 45 درجة للأعلى.\nاخفض البار بتحكم نحو أعلى الصدر مع بقاء المرفقين بزاوية 45 درجة.\nادفع للأعلى بسلاسة واعصر أعلى الصدر.",
        120,
    ),
    "decline-bench-press": (
        "بنش برس مائل للأسفل بالبار",
        "ثبّت ساقيك في مسند المقعد المائل للأسفل.\nاخفض البار بتحكم إلى أسفل الصدر.\nادفع للأعلى بقوة حتى استقامة الذراعين.",
        120,
    ),
    "dumbbell-bench-press": (
        "بنش برس بالدمبل (مستوي)",
        "استلقِ على المقعد المستوي وارفع الدمبلين فوق الصدر.\nاخفضهما للجانبين بعمق مريح مع شد الكتفين للخلف.\nادفع للالتقاء فوق منتصف الصدر.",
        90,
    ),
    "incline-dumbbell-press": (
        "بنش برس مائل للأعلى بالدمبل",
        "اجلس على مقعد مائل للأعلى وابدأ بالدمبلين بمستوى الكتفين.\nادفع الدمبلين للأعلى مع تقاربهما في القمة.\nاخفض بتحكم للشعور بتمدد أعلى الصدر.",
        90,
    ),
    "decline-dumbbell-press": (
        "بنش برس مائل للأسفل بالدمبل",
        "استلقِ على المقعد المائل للأسفل وثبّت القدمين.\nاخفض الدمبلين بجانب أسفل الصدر بتحكم.\nادفع للأعلى مع التركيز على الجزء السفلي للصدر.",
        90,
    ),
    "machine-chest-press": (
        "دفع الصدر على الجهاز",
        "اضبط ارتفاع المقعد بحيث تكون المقابض بمستوى منتصف الصدر.\nادفع للأمام مع الزفير دون قفل المرفقين.\nارجع ببطء قبل تلامس الأوزان.",
        90,
    ),
    "smith-machine-bench-press": (
        "بنش برس على جهاز السميث",
        "استلقِ بحيث يكون البار فوق منتصف الصدر.\nفك القفل واخفض البار بتحكم نحو الصدر.\nادفع للأعلى بثبات حتى قمة الحركة.",
        120,
    ),
    "pec-deck": (
        "تفتيح الصدر على جهاز الفراشة",
        "اضبط المقعد وضع ساعديك أو يديك على المقابض.\nضم الذراعين للأمام واعصر عضلات الصدر لثانية.\nافتح الذراعين ببطء للشعور بتمدد الصدر.",
        60,
    ),
    "cable-fly": (
        "تفتيح الصدر بالكابل",
        "قف في منتصف جهاز الكابل وتقدم خطوة للأمام.\nاثنِ المرفقين قليلاً واجمع المقبضين أمام الصدر.\nارجع بتحكم إلى مستوى الكتفين.",
        60,
    ),
    "incline-cable-fly": (
        "تفتيح الصدر العالي بالكابل",
        "ضع البكرات في الوضع السفلي وامسك المقبضين.\nاسحب للداخل والأعلى حتى مستوى أعلى الصدر.\nاخفض ببطء مع الحفاظ على ثني خفيف بالمرفق.",
        60,
    ),
    "dumbbell-fly": (
        "تفتيح الصدر بالدمبل (مستوي)",
        "استلقِ على مقعد مستوٍ وارفع الدمبلين فوق الصدر.\nافتح الذراعين للجانبين بقوس واسع وثني بسيط في المرفق.\nاضغط بالصدر لإعادة الدمبلين للأعلى.",
        60,
    ),
    "push-up": (
        "تمرين الضغط (Push-Up)",
        "ضع اليدين أعرض قليلاً من الكتفين وحافظ على استقامة الجسم.\nاخفض الصدر نحو الأرض مع بقاء المرفقين للخلف قليلاً.\nادفع الأرض بقوة للعودة لوضع البداية.",
        60,
    ),
    "incline-push-up": (
        "تمرين الضغط المائل للأعلى",
        "ضع يديك على مقعد أو سطح مرتفع مع شد الجذع.\nاخفض الصدر نحو الحافة بتحكم.\nادفع للأعلى بثبات.",
        60,
    ),
    "decline-push-up": (
        "تمرين الضغط المائل للأسفل",
        "ارفع قدميك على مقعد وضع يديك على الأرض.\nاخفض الصدر ببطء نحو الأرض مع شد البطن.\nادفع للأعلى بقوة للتركيز على أعلى الصدر.",
        60,
    ),
    "chest-dip": (
        "المتوازي للصدر (Chest Dips)",
        "امسك قضبان المتوازي ومِل بجذعك للأمام قليلاً.\nانزل حتى تصل المرفقين لزاوية 90 درجة مع فتح خفيف.\nادفع للأعلى دون إقفال المرفقين بالكامل.",
        90,
    ),

    # ---------------- SHOULDERS (15) ----------------
    "overhead-press": (
        "دفع الكتف بالبار (Overhead Press)",
        "قف بثبات واحمل البار أمام أعلى الصدر.\nشد البطن والأرداف وادفع البار عمودياً فوق الرأس.\nاخفضه بتحكم إلى وضع البداية.",
        120,
    ),
    "seated-dumbbell-press": (
        "دفع الكتف بالدمبل جالساً",
        "اجلس بظهر مستقيم وارفع الدمبلين بمستوى الأذنين.\nادفعهما للأعلى حتى يقتربا فوق الرأس.\nاخفض ببطء مع تجنب تقوس أسفل الظهر.",
        90,
    ),
    "standing-dumbbell-press": (
        "دفع الكتف بالدمبل واقفاً",
        "قف بعرض الكتفين مع شد الجذع.\nادفع الدمبلين للأعلى عمودياً فوق الكتفين.\nاخفضهما بتحكم إلى مستوى الكتف.",
        90,
    ),
    "arnold-press": (
        "أرنولد برس بالدمبل",
        "ابدأ والدمبلان أمام الكتفين مع توجيه راحتي اليدين نحوك.\nأثناء الدفع للأعلى لف المعصمين للخارج.\nاعكس الحركة أثناء النزول بتحكم.",
        90,
    ),
    "machine-shoulder-press": (
        "دفع الكتف على الجهاز",
        "اضبط المقعد بحيث تكون المقابض بمستوى الكتفين.\nادفع للأعلى بسلاسة مع الزفير.\nاخفض ببطء دون إراحة الأوزان.",
        90,
    ),
    "push-press": (
        "دفع الكتف الانفجاري (Push Press)",
        "اثنِ ركبتيك قليلاً ثم اندفع للأعلى لدفع البار فوق الرأس.\nثبّت البار في القمة واخفضه بتحكم للكتفين.",
        120,
    ),
    "landmine-press": (
        "دفع الكتف بذراع اللاندماين",
        "امسك طرف البار أمام الكتف بيد واحدة.\nادفع للأعلى والأمام بزاوية مائلة مع شد الجذع.\nاخفض بتحكم وبدّل الذراع.",
        90,
    ),
    "lateral-raise": (
        "رفرفة جانبية بالدمبل",
        "امسك دمبلين بجانبيك مع ثني خفيف في المرفقين.\nارفع الذراعين للجانبين حتى مستوى الكتفين.\nاخفض ببطء لمدة ثانيتين.",
        60,
    ),
    "cable-lateral-raise": (
        "رفرفة جانبية بالكابل",
        "امسك المقبض باليد البعيدة عن البكرة السفلية.\nارفع الذراع جانبياً حتى مستوى الكتف مع ثبات الجذع.\nعد ببطء مع الحفاظ على التوتر.",
        60,
    ),
    "machine-lateral-raise": (
        "رفرفة جانبية على الجهاز",
        "اجلس وثبّت ساعديك على الوسادات الجانبية.\nارفع المرفقين للجانبين حتى مستوى الكتفين.\nاخفض ببطء دون إسقاط الوزن.",
        60,
    ),
    "front-raise": (
        "رفرفة أمامية بالدمبل",
        "قف مستقيماً وارفع الدمبلين للأمام بالتبادل أو معاً حتى مستوى الكتف.\nاخفضهما بتحكم دون أرجحة الجسم.",
        60,
    ),
    "cable-front-raise": (
        "رفرفة أمامية بالكابل",
        "قف وظهرك للبكرة السفلية وامسك المقبض.\nارفع الذراع للأمام حتى مستوى العينين.\nاخفض ببطء مع ثبات الكتفين.",
        60,
    ),
    "upright-row": (
        "سحب عمودي للكتف بالبار",
        "امسك البار بقبضة بعرض الكتفين تقريباً.\nاسحب المرفقين للأعلى حتى مستوى الصدر العلوي.\nاخفض البار ببطء وبدون هز الظهر.",
        90,
    ),
    "rear-delt-fly": (
        "رفرفة للكتف الخلفي بالدمبل",
        "انحنِ للأمام بظهر مستقيم وامسك الدمبلين أسفل الصدر.\nافتح الذراعين للجانبين مع عصر الكتف الخلفي.\nاخفض بتحكم.",
        60,
    ),
    "reverse-pec-deck": (
        "فراشة عكسية للكتف الخلفي",
        "اجلس مواجهاً للجهاز وامسك المقابض الأفقية.\nاسحب الذراعين للخلف بعصر عضلات الكتف الخلفي.\nارجع ببطء لوضع البداية.",
        60,
    ),

    # ---------------- BACK (19) ----------------
    "deadlift": (
        "رفعة ميتة بالبار (Deadlift)",
        "قف والبار فوق منتصف القدمين وأمسكه خارج الركبتين.\nافرد الصدر وشد الظهر ثم ادفع الأرض بقدميك لرفع البار.\nاخفضه بدفع الورك للخلف مع بقاء البار ملاصقاً للجسم.",
        150,
    ),
    "rack-pull": (
        "سحب من الحامل (Rack Pull)",
        "اضبط مساند الحامل عند مستوى الركبة وامسك البار بقوة.\nارفع الأوزان بفرد الوركين والكتفين للخلف.\nاخفض البار بتحكم للحامل.",
        150,
    ),
    "barbell-row": (
        "سحب بالبار من الانحناء (Barbell Row)",
        "انحنِ للأمام بزاوية 45 درجة مع استقامة الظهر.\nاسحب البار نحو أسفل البطن بعصر لوحي الكتف.\nاخفضه بتحكم حتى استقامة الذراعين.",
        120,
    ),
    "pendlay-row": (
        "سحب بندلاي من الأرض بالبار",
        "حافظ على موازاة الجذع للأرض تقريباً.\nاسحب البار بقوة من الأرض نحو أسفل الصدر.\nأعده للأرض بتحكم في كل عدة.",
        120,
    ),
    "t-bar-row": (
        "سحب T-Bar",
        "اثنِ ركبتيك قليلاً وحافظ على استقامة الظهر.\nاسحب المقبض نحو الصدر بضم لوحي الكتف.\nاخفض الوزن ببطء لتمديد الظهر.",
        120,
    ),
    "dumbbell-bent-over-row": (
        "سحب بالدمبل مع الانحناء",
        "انحنِ للأمام بظهر مستقيم وامسك دمبلين.\nاسحب المرفقين للأعلى بمحاذاة الجذع.\nاخفض الدمبلين بتحكم.",
        90,
    ),
    "one-arm-dumbbell-row": (
        "سحب منشار بالدمبل بيد واحدة",
        "ارتكز بيد وركبة على المقعد وحافظ على استقامة الظهر.\nاسحب الدمبل نحو الورك مع رفع المرفق للخلف.\nاخفضه ببطء لتمديد المجنص.",
        90,
    ),
    "chest-supported-row": (
        "سحب الظهر على مقعد مائل (دعم الصدر)",
        "اسند صدرك على المقعد أو الجهاز لحماية أسفل الظهر.\nاسحب المقابض للخلف مع عصر منتصف الظهر.\nارجع ببطء لوضع البداية.",
        90,
    ),
    "seated-row": (
        "سحب أرضي بالكابل (Seated Row)",
        "اجلس مستقيم الظهر وثبّت قدميك على المسند.\nاسحب المقبض نحو البطن مع ضم لوحي الكتف.\nأرجع الذراعين ببطء للأمام دون تقوس الظهر.",
        90,
    ),
    "machine-row": (
        "سحب الظهر على الجهاز",
        "ثبّت صدرك على الوسادة وامسك المقابض.\nاسحب للخلف مع الحفاظ على الكتفين منخفضين.\nعد ببطء للأمام.",
        90,
    ),
    "single-arm-cable-row": (
        "سحب بالكابل بيد واحدة",
        "امسك المقبض بيد واحدة واجلس أو قف بثبات.\nاسحب المرفق للخلف بمحاذاة الجذع.\nارجع ببطء لمد عضلات الظهر.",
        90,
    ),
    "lat-pulldown": (
        "سحب عالي للظهر بالكابل (Lat Pulldown)",
        "امسك البار بقبضة واسعة وثبّت الفخذين تحت المسند.\nاسحب البار نحو أعلى الصدر بخفض الكتفين للأسفل.\nارتفع بتحكم حتى التمدد الكامل.",
        90,
    ),
    "close-grip-lat-pulldown": (
        "سحب عالي قبضة ضيقة",
        "ركّب المقبض الضيق أو اسحب راحتي اليدين متقابلتين.\nاسحب نحو أعلى الصدر مع عصر عضلات المجنص.\nعد للأعلى بتحكم.",
        90,
    ),
    "straight-arm-pulldown": (
        "سحب أمامي بذراعين مستقيمتين",
        "قف أمام الكابل العالي بظهر مائل قليلاً.\nاسحب البار للأسفل نحو الفخذين مع إبقاء الذراعين مفرودتين.\nعد للأعلى بتحكم.",
        60,
    ),
    "pull-up": (
        "العقلة (Pull-Up)",
        "تعلق بالبار بقبضة أوسع من الكتفين مع شد البطن.\nاسحب جسمك للأعلى حتى يتجاوز الذقن مستوى البار.\nانزل بتحكم إلى وضع التمدد.",
        120,
    ),
    "assisted-pull-up": (
        "العقلة المساعدة على الجهاز",
        "ضع ركبتيك على منصة المساعدة.\nاسحب جسمك للأعلى بالتركيز على عضلات الظهر.\nاهبط ببطء وتحكم.",
        90,
    ),
    "inverted-row": (
        "سحب الجسم الأفقي تحت البار",
        "تمدد أسفل بار منخفض وامسك به بقبضة واسعة.\nاسحب صدرك نحو البار مع بقاء الجسم مستقيماً.\nانزل بتحكم.",
        90,
    ),
    "face-pull": (
        "سحب الحبل للوجه (Face Pull)",
        "اضبط البكرة بمستوى الوجه واسحب الحبل نحو الجبهة.\nباعد بين طرفي الحبل مع تدوير الكتفين للخارج.\nارجع للأمام ببطء.",
        60,
    ),
    "shrug": (
        "هز الكتفين للترابيس بالبار",
        "امسك البار أمام الفخذين بذراعين مستقيمتين.\nارفع كتفيك عمودياً للأعلى نحو الأذنين واثبت لثانية.\nاخفضهما ببطء.",
        60,
    ),
    "back-extension": (
        "تمديد أسفل الظهر (Hyperextension)",
        "ثبّت فخذيك على الجهاز مع ثني الجذع للأسفل.\nارفع جذعك حتى يستقيم الظهر على خط واحد مع الساقين.\nاخفض بتحكم دون زيادة تقوس.",
        60,
    ),

    # ---------------- LEGS (25) ----------------
    "squat": (
        "سكوات خلفي بالبار (Barbell Squat)",
        "ضع البار على أعلى الظهر واثبت بقدمين بعرض الكتفين.\nادفع الورك للخلف وانزل حتى توازي الفخذان الأرض.\nادفع الأرض بقدميك للعودة للأعلى.",
        150,
    ),
    "front-squat": (
        "سكوات أمامي بالبار",
        "اسند البار على الكتفين الأماميين مع رفع المرفقين.\nانزل بظهر منتصب بعمق كامل مريح.\nاصعد بقوة للأعلى.",
        150,
    ),
    "goblet-squat": (
        "جوبلت سكوات بالدمبل",
        "احمل دمبلاً عمودياً أمام الصدر بكلتا اليدين.\nانزل بين الركبتين مع إبقاء الصدر مرفوعاً.\nاصعد بالضغط من كعبي القدمين.",
        90,
    ),
    "smith-machine-squat": (
        "سكوات على جهاز السميث",
        "ضع قدميك للأمام قليلاً عن خط البار.\nانزل بتحكم حتى زاوية 90 درجة أو أعمق.\nادفع للأعلى بسلاسة.",
        120,
    ),
    "hack-squat": (
        "هاك سكوات على الجهاز",
        "اسند ظهرك بالكامل على المسند وثبّت القدمين على المنصة.\nانزل بأقصى مدى مريح دون رفع أسفل الظهر.\nادفع للأعلى دون إقفال الركبتين.",
        120,
    ),
    "leg-press": (
        "دفع الأرجل على الجهاز (Leg Press)",
        "ضع قدميك بعرض الكتفين في منتصف المنصة.\nاخفض المنصة بتحكم نحو الصدر مع ثبات الورك.\nادفع للأعلى دون إقفال الركبتين بالكامل.",
        120,
    ),
    "bulgarian-split-squat": (
        "السكوات البلغاري بساق واحدة",
        "ضع مشط قدمك الخلفية على المقعد خلفك.\nاخفض الركبة الخلفية نحو الأرض مع ميل خفيف للأمام.\nادفع بالقدم الأمامية للصعود.",
        90,
    ),
    "walking-lunge": (
        "الطعن بالمشي بالدمبل",
        "اخطُ للأمام واخفض كلتا الركبتين لزاوية 90 درجة.\nادفع بالقدم الأمامية للانتقال للخطوة التالية.\nحافظ على استقامة الجذع.",
        90,
    ),
    "reverse-lunge": (
        "الطعن للخلف بالدمبل",
        "اخطُ بساق واحدة للخلف واخفض الركبة نحو الأرض.\nادفع بالقدم الأمامية للعودة لوضع الوقوف.\nبدّل الساقين.",
        90,
    ),
    "step-up": (
        "الصعود على الصندوق بالدمبل",
        "ضع قدماً بالكامل على صندوق ثابت.\nادفع بهذه الساق لرفع الجسم للأعلى دون قفز بالساق الخلفية.\nانزل بتحكم.",
        90,
    ),
    "bodyweight-squat": (
        "سكوات بوزن الجسم",
        "قف بقدمين بعرض الكتفين ومد ذراعيك للتوازن.\nانزل للأسفل بدفع الورك للخلف وإبقاء الصدر مرفوعاً.\nاصعد للأعلى بثبات.",
        60,
    ),
    "leg-extension": (
        "تمديد الأرجل الأمامية (Leg Extension)",
        "اضبط المسند بحيث يستقر عند الكاحلين وتكون الركبتان بمحاذاة المحور.\nافرد ساقيك للأعلى واعصر الفخذ الأمامي لثانية.\nانزل ببطء.",
        60,
    ),
    "romanian-deadlift": (
        "ديدليفت روماني بالبار (RDL)",
        "قف والبار أمام الفخذين مع ثني طفيف ثابت في الركبتين.\nادفع الورك للخلف وانزل البار بمحاذاة الساقين حتى تمدد الخلفيات.\nعُد للأعلى بعصر الأرداف.",
        120,
    ),
    "dumbbell-romanian-deadlift": (
        "ديدليفت روماني بالدمبل",
        "امسك الدمبلين أمام الفخذين وادفع الورك للخلف بظهر مستقيم.\nتوقف عندما تشعر بتمدد واضح في الفخذ الخلفي.\nعد للأعلى بعصر الأرداف.",
        90,
    ),
    "leg-curl": (
        "ثني الأرجل الخلفية (Leg Curl)",
        "ثبّت الركبتين أسفل محور الدوران.\nاسحب المسند نحو الأرداف بتحكم.\nارجع ببطء دون ارتداد الوزن.",
        60,
    ),
    "lying-leg-curl": (
        "ثني الأرجل الخلفية مستلقياً",
        "استلقِ على البطن وثبّت الكاحلين تحت الوسادة.\nاثنِ الركبتين لسحب الوسادة نحو الأرداف.\nانزل ببطء حتى التمدد.",
        60,
    ),
    "seated-leg-curl": (
        "ثني الأرجل الخلفية جالساً",
        "اضبط مسند الفخذين بإحكام فوق الركبتين.\nاسحب ساقيك للأسفل والخلف بعصر الفخذ الخلفي.\nارجع بتحكم للأمام.",
        60,
    ),
    "good-morning": (
        "تمرين جود مورنينج بالبار",
        "ضع البار على أعلى الظهر واثنِ الركبتين قليلاً.\nانحنِ من مفصل الورك للأمام مع إبقاء الظهر مستقيماً.\nارجع للوقوف بعصر الأرداف والخلفيات.",
        120,
    ),
    "hip-thrust": (
        "دفع الحوض بالبار (Hip Thrust)",
        "اسند أعلى ظهرك على المقعد وضع البار فوق الورك مع وسادة.\nادفع الحوض للأعلى حتى يتوازى الجذع مع الأرض واثبت لثانية.\nانزل بتحكم.",
        120,
    ),
    "glute-bridge": (
        "جسر الأرداف (Glute Bridge)",
        "استلقِ على ظهرك مع ثني الركبتين وتثبيت القدمين.\nارفع الحوض للأعلى بعصر الأرداف.\nاخفضه ببطء قبل لمس الأرض.",
        60,
    ),
    "cable-kickback": (
        "ركلة الكابل الخلفية للأرداف",
        "اربط الكاحل بالكابل السفلي ومِل قليلاً للأمام.\nادفع الساق للخلف والأعلى مع شد الأرداف.\nارجع بتحكم.",
        60,
    ),
    "hip-abduction-machine": (
        "فتح الفخذين على الجهاز (Abduction)",
        "اجلس مع إسناد الجزء الخارجي للركبتين على الوسادتين.\nادفع الساقين للخارج واثبت لثانية.\nأغلق ببطء.",
        60,
    ),
    "hip-adduction-machine": (
        "ضم الفخذين على الجهاز (Adduction)",
        "اجلس وضع الوسادتين على الجانب الداخلي للركبتين.\nاضغط الساقين للداخل مع التحكم.\nافتح ببطء.",
        60,
    ),
    "standing-calf-raise": (
        "تمرين السمانة واقفاً على الجهاز",
        "قف بأمشاط القدمين على الحافة واخفض الكعبين لتمديد كامل.\nارتفع لأعلى نقطة على أطراف الأصابع واثبت لثانية.\nانزل ببطء.",
        60,
    ),
    "seated-calf-raise": (
        "تمرين السمانة جالساً",
        "ثبّت وسادة الجهاز فوق الفخذين.\nاخفض الكعبين ببطء للأسفل ثم ارفع لأعلى مدى.\nكرر بإيقاع متحكم.",
        60,
    ),

    # ---------------- ARMS (20) ----------------
    "bicep-curl": (
        "بايسبس كيرل بالدمبل",
        "قف ومرفقاك ثابتان بجانب الجذع.\nارفع الدمبلين للأعلى بتدوير الراحتين للأعلى.\nاخفض ببطء حتى التمدد الكامل.",
        60,
    ),
    "ez-bar-curl": (
        "بايسبس كيرل بالبار الزجزاج (EZ-Bar)",
        "امسك البار المتعرج بقبضة مريحة للمعصمين.\nارفع البار للأعلى دون تحريك المرفقين للأمام.\nاخفضه بتحكم لثانيتين.",
        60,
    ),
    "hammer-curl": (
        "هامر كيرل بالدمبل (قبضة محايدة)",
        "امسك الدمبلين وراحتا اليدين متجهتان نحو الجسم.\nارفع الدمبلين للأعلى مع ثبات المرفقين.\nاخفضهما ببطء.",
        60,
    ),
    "incline-dumbbell-curl": (
        "بايسبس كيرل على مقعد مائل",
        "استلقِ على مقعد مائل للخلف واترك الذراعين متدليتين عمودياً.\nارفع الدمبلين بعصر البايسبس دون سحب الكتف للأمام.\nاخفض حتى تمدد كامل.",
        60,
    ),
    "preacher-curl": (
        "بايسبس كيرل على مسند الواعظ (Preacher)",
        "ثبّت الذراعين فوق المسند لتثبيت الكتف.\nارفع الوزن للأعلى حتى القمة واخفضه ببطء دون إرخاء كامل للمرفق.",
        60,
    ),
    "concentration-curl": (
        "كيرل التركيز بالدمبل",
        "اجلس وأسند ظهر الذراع على الجزء الداخلي للفخذ.\nارفع الدمبل نحو الكتف واعصر البايسبس.\nانزل ببطء.",
        60,
    ),
    "cable-curl": (
        "بايسبس كيرل بالكابل",
        "امسك بار الكابل السفلي بقبضة سفلية.\nارفع نحو الكتفين مع ثبات المرفقين بجانبيك.\nاخفض ببطء مع الحفاظ على الشد.",
        60,
    ),
    "rope-hammer-curl": (
        "هامر كيرل بالحبل على الكابل",
        "اربط الحبل بالبكرة السفلية وامسك الطرفين.\nارفع للأعلى مع توجيه الإبهامين للأعلى.\nاخفض ببطء.",
        60,
    ),
    "chin-up": (
        "العقلة بقبضة معكوسة (Chin-Up)",
        "امسك البار براحتي اليدين نحوك وبعرض الكتفين.\nاسحب جسمك للأعلى مع التركيز على البايسبس والمجنص.\nاهبط بتحكم.",
        90,
    ),
    "reverse-curl": (
        "كيرل بقبضة علوية للساعد",
        "امسك البار براحتي اليدين للأسفل وبعرض الكتفين.\nارفع البار للأعلى مع ثبات المرفقين.\nاخفض ببطء لتقوية الساعدين.",
        60,
    ),
    "wrist-curl": (
        "ثني المعصم بالبار للساعد",
        "اسند الساعدين على مقعد مع تدلي اليدين للأمام.\nاثنِ المعصمين للأعلى بتحكم ثم اخفض ببطء.",
        45,
    ),
    "tricep-pushdown": (
        "دفع الترايسبس بالكابل (بار مستقيم)",
        "ثبّت المرفقين بجانب الجذع وامسك البار العلوي.\nادفع البار للأسفل حتى استقامة الذراعين تماماً.\nاصعد ببطء حتى زاوية 90 درجة.",
        60,
    ),
    "rope-tricep-pushdown": (
        "دفع الترايسبس بالحبل على الكابل",
        "اسحب الحبل للأسفل وافصل طرفيه عند القاع لعصر الترايسبس.\nارجع للأعلى بتحكم دون رفع المرفقين.",
        60,
    ),
    "overhead-tricep-extension": (
        "تمديد الترايسبس فوق الرأس بالكابل",
        "استدر عن الكابل وارفع الحبل فوق الرأس مرفقاً مثنياً.\nادفع للأمام والأعلى حتى فرد الذراعين.\nارجع بتحكم للشعور بتمدد الرأس الطويل للترايسبس.",
        60,
    ),
    "dumbbell-overhead-tricep-extension": (
        "تمديد الترايسبس فوق الرأس بالدمبل",
        "اجلس وارفع دمبلاً بكلتا اليدين خلف الرأس.\nمد الساعدين للأعلى مع ثبات المرفقين للأمام.\nاخفض ببطء.",
        60,
    ),
    "skull-crusher": (
        "تمديد الترايسبس مستلقياً بالبار (Skull Crusher)",
        "استلقِ على مقعد مستوٍ وارفع البار فوق الصدر.\nاثنِ المرفقين واخفض البار نحو الجبهة بتحكم.\nادفع للأعلى بعصر الترايسبس.",
        90,
    ),
    "close-grip-bench-press": (
        "بنش برس بقبضة ضيقة للترايسبس",
        "امسك البار بقبضة بعرض الكتفين أو أضيق قليلاً.\nاخفض البار مع إبقاء المرفقين قريبين من الجذع.\nادفع للأعلى بقوة.",
        90,
    ),
    "dip": (
        "تمرين المتوازي للترايسبس (Dips)",
        "حافظ على استقامة الجذع عمودياً مع بقاء المرفقين للخلف.\nانزل حتى زاوية 90 درجة.\nادفع للأعلى حتى التمدد.",
        90,
    ),
    "tricep-kickback": (
        "ركلة خلفية للترايسبس بالدمبل",
        "انحنِ للأمام مع رفع العضد موازياً للجذع.\nافرد الساعد للخلف بالكامل واعصر الترايسبس.\nارجع بتحكم.",
        60,
    ),
    "diamond-push-up": (
        "ضغط الماسة للترايسبس",
        "ضع اليدين متقاربتين على شكل مثلث أسفل الصدر.\nاخفض الجسم مع ضم المرفقين للداخل.\nادفع للأعلى بعصر الترايسبس.",
        60,
    ),

    # ---------------- CORE (15) ----------------
    "crunch": (
        "طحن البطن (Crunch)",
        "استلقِ على ظهرك مع ثني الركبتين ووضع اليدين خلف الأذنين.\nارفع أعلى الكتفين عن الأرض بعصر عضلات البطن مع الزفير.\nاخفض ببطء دون إرخاء التوتر.",
        45,
    ),
    "decline-sit-up": (
        "بطن على المقعد المائل للأسفل",
        "ثبّت القدمين على المقعد المائل.\nانزل للخلف بتحكم ثم اصعد بعصر البطن دون شد الرقبة.",
        60,
    ),
    "reverse-crunch": (
        "طحن البطن المعكوس (أسفل البطن)",
        "استلقِ على ظهرك واثنِ الركبتين للأعلى.\nارفع الحوض عن الأرض بتقريب الركبتين من الصدر.\nانزل ببطء وتحكم.",
        45,
    ),
    "lying-leg-raise": (
        "رفع الساقين مستلقياً",
        "استلقِ على ظهرك وضع يديك بجانبك أو أسفل الأرداف.\nارفع الساقين للأعلى حتى زاوية قائمة مع تثبيت أسفل الظهر.\nاخفضهما ببطء قبل لمس الأرض.",
        60,
    ),
    "hanging-leg-raise": (
        "رفع الساقين معلقاً على العقلة",
        "تعلق بالبار بثبات وبدون تأرجح.\nارفع الساقين للأمام والأعلى حتى مستوى الخصر أو أعلى.\nانزل بتحكم.",
        60,
    ),
    "hanging-knee-raise": (
        "رفع الركبتين معلقاً على العقلة",
        "تعلق بالبار مع شد الأكتاف للأسفل.\nاسحب الركبتين نحو الصدر بتقويس طفيف في الحوض.\nاخفض بتحكم.",
        60,
    ),
    "captains-chair-knee-raise": (
        "رفع الركبتين على جهاز المتوازي للبطن",
        "أسند ظهرك وساعديك على المساند.\nارفع الركبتين للأعلى بعصر أسفل البطن.\nانزل ببطء دون تأرجح.",
        60,
    ),
    "cable-crunch": (
        "طحن البطن بالحبل على الكابل",
        "اركع أمام الكابل العلوي وامسك الحبل بجانب الرأس.\nاقبض البطن للأسفل مع تقريب المرفقين من الفخذين.\nاصعد ببطء مع إبقاء التوتر.",
        60,
    ),
    "plank": (
        "تمرين البلانك (Plank)",
        "ارتكز على الساعدين وأطراف الأصابع في خط مستقيم.\nشد البطن والأرداف وتنفس بانتظام دون هبوط الحوض.\nحافظ على الثبات للمدة المحددة.",
        45,
    ),
    "side-plank": (
        "البلانك الجانبي",
        "ارتكز على ساعد واحد وجانب القدم مع رفع الورك.\nحافظ على خط مستقيم للجسم وشد عضلات الخصر الجانبية.\nبدّل الجانبين.",
        45,
    ),
    "ab-wheel": (
        "عجلة البطن (Ab Wheel Rollout)",
        "اركع وامسك مقابض العجلة تحت الكتفين.\nدحرج العجلة للأمام بتحكم مع شد البطن ومنع تقوس أسفل الظهر.\nاسحب للخلف للعودة لوضع البداية.",
        60,
    ),
    "russian-twist": (
        "التفاف الجذع الروسي",
        "اجلس مع إمالة الجذع للخلف 45 درجة وثني الركبتين.\nلف الجذع يميناً ويساراً بتحكم دون تقوس الظهر.",
        45,
    ),
    "bicycle-crunch": (
        "بطن الدراجة (Bicycle Crunch)",
        "استلقِ وارفع الساقين بالتناوب مع تدوير الجذع.\nوجّه المرفق نحو الركبة المقابلة مع مد الساق الأخرى بتحكم.",
        45,
    ),
    "cable-woodchop": (
        "تدوير الخصر بالكابل (Woodchop)",
        "قف جانبياً بالنسبة للكابل وامسك المقبض بكلتا اليدين.\nاسحب ولف الجذع قطرياً مع ثبات الوركين والقدمين.\nارجع بتحكم.",
        60,
    ),
    "pallof-press": (
        "دفع بالوف ضد الدوران بالكابل",
        "قف جانبياً وامسك المقبض أمام الصدر.\nادفع الذراعين للأمام مع مقاومة التفاف الجذع نحو الكابل.\nأرجع ببطء.",
        60,
    ),
}


def normalize_muscle(raw: str | None) -> str:
    return MUSCLE_MAP.get((raw or "").strip().lower(), "CORE")


def normalize_equipment(raw: str | None) -> str:
    return EQUIPMENT_MAP.get((raw or "").strip().lower(), "BODYWEIGHT")


def clean_step(step: str) -> str:
    return step.strip().rstrip(".") + "."


def build_seed(
    wg_manifest: list[dict[str, Any]],
    ek_by_name: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    wg_by_slug = {item["slug"]: item for item in wg_manifest}
    entries: list[dict[str, Any]] = []

    for slug, (name_ar, instructions_ar, rest_seconds) in CURATED_AR.items():
        item = wg_by_slug.get(slug)
        if not item:
            raise ValueError(f"Slug '{slug}' not found in workout-guide manifest")

        primary = normalize_muscle(item.get("primaryMuscle"))
        secondary = list(
            dict.fromkeys(
                m
                for m in (normalize_muscle(x) for x in (item.get("secondaryMuscles") or []))
                if m != primary
            )
        )
        equipment = normalize_equipment(item.get("equipment"))

        ek_entry = ek_by_name.get(slug) or ek_by_name.get(item.get("name", "").lower())
        steps_en = (ek_entry or {}).get("steps") or []
        instructions_en = "\n".join(clean_step(s) for s in steps_en if s and s.strip())
        if not instructions_en:
            instructions_en = (
                f"Perform {item['name']} through a full, controlled range of motion. "
                "Keep your core braced and avoid using momentum."
            )

        entries.append(
            {
                "id": slug,
                "nameEn": item["name"],
                "nameAr": name_ar,
                "primaryMuscle": primary,
                "secondaryMuscles": secondary,
                "equipment": equipment,
                "instructionsEn": instructions_en,
                "instructionsAr": instructions_ar,
                "localMediaKey": slug,
                "defaultRestSeconds": rest_seconds,
                "isCustom": False,
            }
        )

    return entries


def convert_exercise_frames(
    slug: str,
    local_assets_root: Path | None,
    out_media_dir: Path,
    size: int = 320,
    quality: int = 75,
) -> tuple[str, int, str | None]:
    from PIL import Image

    out_ex_dir = out_media_dir / slug
    out_ex_dir.mkdir(parents=True, exist_ok=True)
    written = 0

    for idx, frame_num in enumerate((1, 3)):
        dst = out_ex_dir / f"{idx}.webp"
        if local_assets_root is not None:
            src = local_assets_root / slug / f"frame-{frame_num}.png"
            if not src.exists():
                return slug, written, f"Missing source frame {src}"
            img = Image.open(src).convert("RGBA")
        else:
            import io

            url = f"{WG_ASSET_BASE}/{slug}/frame-{frame_num}.png"
            req = urllib.request.Request(url, headers={"User-Agent": "HimmaSeedSync/1.0"})
            with urllib.request.urlopen(req, timeout=30) as resp:
                img = Image.open(io.BytesIO(resp.read())).convert("RGBA")

        with img:
            img.thumbnail((size, size), Image.Resampling.LANCZOS)
            # Composite crisp white anatomical figure onto AMOLED dark surface (#0A110E)
            # so light/dark UI cards always render high-contrast strokes and encode quickly.
            backdrop = Image.new("RGB", (size, size), (10, 17, 14))
            offset_x = (size - img.width) // 2
            offset_y = (size - img.height) // 2
            backdrop.paste(img, (offset_x, offset_y), img)
            backdrop.save(dst, format="WEBP", quality=quality, method=2)
        written += 1

    return slug, written, None


def main() -> int:
    parser = argparse.ArgumentParser(description="Build Himma exercises_seed.json & 3-frame WebP assets")
    repo_root = Path(__file__).resolve().parents[1]
    parser.add_argument(
        "--seed-path",
        type=Path,
        default=repo_root / "app/src/main/assets/exercises_seed.json",
    )
    parser.add_argument(
        "--media-dir",
        type=Path,
        default=repo_root / "app/src/main/assets/exercises",
    )
    parser.add_argument(
        "--wg-manifest",
        type=Path,
        default=None,
        help="Optional local workout-guide manifest.json path",
    )
    parser.add_argument(
        "--wg-assets",
        type=Path,
        default=None,
        help="Optional local workout-guide assets folder (contains <slug>/frame-1.png etc.)",
    )
    parser.add_argument(
        "--ek-json",
        type=Path,
        default=None,
        help="Optional local everkinetic exercises.json path",
    )
    args = parser.parse_args()

    if args.wg_manifest and args.wg_manifest.exists():
        wg_manifest = json.loads(args.wg_manifest.read_text(encoding="utf-8"))
    else:
        with urllib.request.urlopen(WG_MANIFEST_URL, timeout=30) as resp:
            wg_manifest = json.loads(resp.read().decode("utf-8"))

    if args.ek_json and args.ek_json.exists():
        ek_items = json.loads(args.ek_json.read_text(encoding="utf-8"))
    else:
        try:
            with urllib.request.urlopen(EK_EXERCISES_URL, timeout=30) as resp:
                ek_items = json.loads(resp.read().decode("utf-8"))
        except Exception:  # noqa: BLE001
            ek_items = []

    ek_by_name: dict[str, dict[str, Any]] = {}
    for item in ek_items:
        if item.get("name"):
            ek_by_name[item["name"].lower()] = item
        if item.get("title"):
            ek_by_name[item["title"].lower()] = item

    seed = build_seed(wg_manifest, ek_by_name)
    args.seed_path.write_text(
        json.dumps(seed, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {len(seed)} exercises to {args.seed_path}")

    # Clean previous asset directory to remove stale folders cleanly.
    if args.media_dir.exists():
        shutil.rmtree(args.media_dir)
    args.media_dir.mkdir(parents=True, exist_ok=True)

    slugs = [item["id"] for item in seed]
    total_written = 0
    errors: list[tuple[str, str]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        for slug, written, err in pool.map(
            lambda s: convert_exercise_frames(s, args.wg_assets, args.media_dir),
            slugs,
        ):
            total_written += written
            if err:
                errors.append((slug, err))

    print(f"Generated {total_written} compressed 3-frame WebP files under {args.media_dir}")
    if errors:
        for slug, err in errors:
            print(f"ERROR {slug}: {err}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
