"""Questions 31-40 of shahzad-2015, in the style the owner asked for on 2026-11-15 (see ../batch03/batch03.py,
whose prompt frame and lesson builder this file reuses).

Usage:  python3 batch04.py <mcqs-0001-0100.json> <exported chapter q0001-0100.json> <out folder>
(keep ../batch03/batch03.py beside it, as on the content branch).

The stories were written from the scenes BEFORE the pictures existed: check each against its picture when it
arrives, adjust, and only then set the spots.
"""
import json, os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "batch03"))
import batch03
from batch03 import prompt, lesson

batch03.TYPOS.update({"Tirfiban": "Tirofiban", "Abiciximab": "Abciximab"})

BATCH = [
 dict(n=31, title="Quinidine and digitalis toxicity", titleAr="الكينيدين وسُمّية الديجيتال",
  tags=["cardiac/arrhythmia", "d:pharmacology", "q:recall", "l:2"],
  palette="deep emerald room, royal purple flowers and blood-red bark, lit by one window",
  words='"K"',
  scene=("the parlour of an old widow's house. On the mantelpiece stands a large bright red clock shaped like a heart; it has "
         "gone mad, its hands flying and its pendulum swinging crookedly: this is the focal point. The old widow stands before it "
         "holding in one hand a small cup of purple tea, with a bunch of tall purple foxglove bells in a vase beside her, and in "
         "the other hand a strip of rough blood-red tree bark that she has just dipped into the cup. On a shelf stand five "
         "identical tea caddies in a row; four are closed and untouched, the fifth is open and full of the same red bark. In the "
         "open cellar door a maid points down in alarm at a large barrel marked with the single letter K, tipped on its side and "
         "completely empty."),
  story=[
   "في بيت الأرملة براسكوفيا ساعةٌ حمراء على هيئة قلب، لا يغمض للعجوز جفنٌ إلا على دقّاتها.",
   "وكانت كلَّ صباحٍ تسقيها قطرةً من منقوع الأجراس البنفسجية، فتشتدّ دقّتها وتنتظم.",
   "قطرةً لا تزيد؛ فبين دواء الأجراس وسُمّها شعرة.",
   "وعلى الرفّ خمسُ علبٍ جاء بها ابن أخيها من المدينة، وأقسم أنها كلّها تُسكّن الساعات الهائجة.",
   "جرّبت أربعًا فما ضرّت؛ ثم مدّت يدها إلى الخامسة، علبةِ اللحاء الأحمر.",
   "فما إن خالط اللحاءُ منقوعَ الأجراس حتى جُنّت الساعة: تدقّ وتتعثّر، وتدقّ وتتعثّر، كقلب محكومٍ عليه يُساق إلى الساحة.",
   "وصرخت الخادمة من باب القبو: «والبرميل يا سيدتي! برميل K فارغٌ منذ أسبوع!»",
   "شهقت العجوز: «فارغ؟ إذن تحالف عليها ثلاثة: الأجراس، واللحاء، والفراغ».",
   "وجلست أمام الساعة المرتجفة، ترى فيها جميلًا يقتل ونافعًا يخون، وقالت:",
  ],
  quote=("«الجمال شيءٌ رهيبٌ مخيف! ... هنا يتصارع الشيطان والله، وساحةُ المعركة قلوبُ البشر.»", "الإخوة كارامازوف"),
  explanation=[
   "Class I antiarrhythmics block the fast sodium channels responsible for rapid depolarisation.",
   "Among them quinidine, a class Ia drug, enhances digitalis toxicity.",
   "It raises the plasma concentration of digoxin, so the two together can produce toxicity at a dose of digoxin that was safe before.",
   "The danger is greatest if hypokalaemia is present, because a low potassium by itself makes the heart more sensitive to digitalis.",
   "Of the five drugs listed, quinidine is the one the book gives for this interaction.",
  ]),

 dict(n=32, title="Class I antiarrhythmics act on phase 0", titleAr="مضادات اضطراب النظم من الصنف الأول تعمل على الطور 0",
  tags=["cardiac/arrhythmia", "d:pharmacology", "d:physiology", "q:recall", "l:1", "x:high-yield"],
  palette="rose-pink dawn on a dazzling white salt plain, with cool blue shadows and one scarlet soldier",
  words='"0", "1", "2", "3", "4"',
  scene=("a strange mountain standing alone on a white salt plain at dawn, seen from the side so that its outline is perfectly "
         "clear against the pink sky: a flat plain, then a sheer vertical cliff, then a small notch at the top, then a long flat "
         "plateau, then a slope running back down to the plain. Large numbers are painted on the rock: 4 on the plain, 0 on the "
         "cliff, 1 at the notch, 2 on the plateau, 3 on the slope. At the foot of the cliff is a gate through which porters "
         "carrying sacks of white salt try to rush; a big soldier in a scarlet coat stands in the gate blocking half of it with "
         "his shoulders: this is the focal point. Up the cliff face runs a wooden lift cage on a rope; because only a few porters "
         "get through, the cage is climbing slowly and crookedly and has stopped short of the top, and a pale ghost outline shows "
         "the higher, steeper line it used to follow. Far away on a small separate hill stands a little chapel with a white chalk "
         "door wide open and its bell ringing, untroubled."),
  story=[
   "في السهل الأبيض جبلٌ عجيب يصعده أهل المدينة كلَّ فجر: سهلٌ منبسط، ثم جُرفٌ قائم كالجدار، ثم نتوءٌ صغير، ثم هضبةٌ طويلة، ثم منحدرٌ يعود إلى السهل.",
   "وقد رقّموا مراحله على الصخر: 4 للسهل، و0 للجرف، ثم 1 فـ2 فـ3.",
   "وسرُّ الجرف بوّابةٌ في أسفله يندفع منها حمّالو الملح أفواجًا؛ فكلّما تدافعوا طار المصعد إلى القمّة في طرفة عين.",
   "ثم جاء جنديٌّ من العاصمة بمعطفه القرمزيّ، فوقف في البوّابة وسدّ نصفها بمنكبيه.",
   "فتسلّل الحمّالون واحدًا واحدًا؛ وصعد المصعد متثاقلًا مائلًا، ووقف دون الذروة.",
   "أمّا النتوء والهضبة والمنحدر فلم يمسسها بسوء؛ ما كان له شأنٌ إلا بالجرف.",
   "وعلى التلّ البعيد كنيسةٌ صغيرة تقرع جرسها كعادتها: بابُها من طبشورٍ لا من ملح، والجنديّ لا يعرف إليها سبيلًا.",
   "وقف الفتى ساشا عند البوّابة المسدودة يبكي صعوده الذي كان.",
   "فوضع المحقّق الشيخ يده على كتفه، وقال له ما قاله يومًا لقاتلٍ شابّ:",
  ],
  quote=("«كُن شمسًا، يَرَك الجميع.»", "الجريمة والعقاب"),
  explanation=[
   "Class I antiarrhythmics are the sodium-channel blockers of the Vaughan-Williams classification.",
   "They bind to and block the fast sodium channels that are responsible for the rapid depolarisation, phase 0, of fast-response cardiac action potentials.",
   "This type of action potential is found in non-nodal tissue: atrial and ventricular myocytes and Purkinje tissue.",
   "The slope of phase 0 depends on the rapid entry of sodium into the cell, so blocking the channels decreases the slope of phase 0 and also the amplitude of the action potential.",
   "In the sinoatrial and atrioventricular nodes phase 0 is carried by calcium currents instead, so sodium-channel blockers have no direct effect on nodal tissue.",
  ]),

 dict(n=33, title="Disopyramide: the anticholinergic one", titleAr="ديزوبيراميد: صاحب الآثار المضادّة للكولين",
  tags=["cardiac/arrhythmia", "d:pharmacology", "d:complications", "q:recall", "l:2"],
  palette="blazing dusty yellow noon, bleached stone, with a grey horse and hard black shadows",
  words="",
  scene=("the yard of a posting inn at blazing noon. Three brothers, stable-hands, stand in a row: the first a giant, the second "
         "of middle size, the third small and thin, each holding a pair of shears that matches his size. The giant has just cut "
         "clean through the reins of a grey horse with his enormous shears, and the horse bolts at a wild gallop, the cut reins "
         "flying: this is the focal point. Around the yard, each in its own place: a stone fountain that has run completely dry, "
         "its basin cracked like parched earth; an old stationmaster wiping spectacles whose lenses are fogged white; a large "
         "watering can, full to the brim, whose spout is tied in a knot so that not a drop comes out; and in the gateway a mule "
         "cart loaded sky-high and stuck fast, unable to move forward or back. A sick traveller sits on his trunk counting on five "
         "fingers."),
  story=[
   "في خان البريد ثلاثة إخوةٍ سُيّاس: ديمتري الضخم، وأوسطهم، وأصغرهم النحيل؛ وفي يد كلٍّ منهم مقصٌّ على قدر جسمه.",
   "جيء بهم ليروّضوا الخيل الجامحة، وقد كانوا يروّضونها.",
   "لكنّ فيهم عادةً خبيثة: يقصّون الأعنّة التي تكبح.",
   "في ذلك الظهر أطبق ديمتري مقصّه العظيم على عنان الحصان الأشهب، فانطلق يعدو عدوَ المجنون.",
   "ثم جفّت نافورة الفناء حتى تشقّق حوضها كأرضٍ عطشى.",
   "وغام زجاج نظّارة ناظر المحطة، فصار يرى الدنيا من وراء ضباب.",
   "وانعقد عنق إبريق السقي على مائه، فما تنزل منه قطرة.",
   "ووقفت عربة البغال في البوّابة محمّلةً حتى السماء، لا تتقدّم ولا تتأخّر.",
   "قال الناظر وهو يمسح زجاجه: «الأوسط يقصّ أقلّ، والصغير أقلّ منه؛ أمّا هذا الضخم فلا يُبقي عنانًا».",
   "وجلس المسافر العليل على حقيبته يعدّ المصائب الخمس على أصابعه، ثم أملى على كاتبه:",
  ],
  quote=("«أنا رجلٌ مريض... أنا رجلٌ شرّير. رجلٌ لا يروق لأحد. أظنّ أنّ كبدي عليلة.»", "رسائل من تحت الأرض"),
  explanation=[
   "The direct effect of class IA antiarrhythmic drugs is significantly modified by their anticholinergic actions.",
   "By inhibiting vagal activity they can increase the sinoatrial rate and atrioventricular conduction, offsetting their direct effect on these tissues.",
   "So a IA drug may slow the atrial rate in flutter and yet increase the ventricular rate, which calls for a beta-blocker or calcium-channel blocker as well.",
   "These actions are most prominent at the sinoatrial and atrioventricular nodes, which are extensively innervated by the vagus.",
   "The drugs differ in strength: disopyramide more than quinidine, and quinidine more than procainamide.",
   "The anticholinergic effects produce tachycardia, dry mouth, urinary retention, blurred vision and constipation.",
  ]),

 dict(n=34, title="The three GP IIb/IIIa inhibitors", titleAr="مثبّطات البروتين السكري IIb/IIIa الثلاثة",
  tags=["cardiac/coronary", "d:pharmacology", "q:recall", "l:3"],
  palette="ice blue and white winter light, canary-yellow coats, and one red giant",
  words='"48", "8", "4"',
  scene=("a frozen lake on a clear winter day. Skaters in canary-yellow coats are trying to join hands into a chain, and three "
         "figures stop them, each in its own part of the picture. On the left, the focal point: a red-coated giant with the head "
         "of a mouse and the body of a man grips the hands of two skaters in his iron fists and with one elbow also pins the scarf "
         "of an innocent passer-by; beside him stands a huge hourglass marked 48. In the middle a small tin toy soldier with a "
         "wind-up key in his back touches exactly one outstretched palm with one fingertip and is already springing away; beside "
         "him a medium hourglass marked 8. On the right a girl throws a bracelet made of exactly seven beads joined in a ring over "
         "a pair of reaching hands; beside her a small hourglass marked 4. In the distance the governor's palace watches."),
  story=[
   "على بحيرة الجليد يتزلّج أهل المدينة بمعاطفهم الصفراء؛ فإذا تشابكت أيديهم صاروا سلسلةً لا يخترقها أحد.",
   "وكان الحاكم يخشى السلاسل، فأرسل ثلاثةً يحولون بين الأيدي وبين أن تتشابك.",
   "أوّلهم عملاقٌ له رأس فأرٍ وجسد رجل: يقبض على كل يدٍ تلقاه، وعلى مَن لا شأن له أيضًا، قبضةً من حديدٍ لا يكاد يُرخيها.",
   "يمضي هو بعد نصف ساعة، ويبقى أثر قبضته يومًا ويومين؛ وعلى ساعته الرملية: 48.",
   "وثانيهم جنديٌّ صغير من صفيح، مصنوعٌ لا مولود: لا يمسّ إلا الكفّ المقصودة، يلمسها ثم يُفلتها في لمح البصر.",
   "وعلى ساعته الرملية: 8.",
   "وثالثتهم صبيّةٌ ترمي على الأيدي سوارًا من سبع خرزاتٍ في حلقة؛ وساعتها أصغر الساعات: 4.",
   "سأل الصبيّ جدَّه: «أيُّهم أشدّ؟» قال: «العملاق أطولهم أثرًا وأعماهم قبضة؛ والجنديّ أخفّهم يدًا وأسرعهم رجوعًا».",
   "ثم رفع الشيخ عينيه إلى قصر الحاكم، وتمتم بكلام شيخٍ رهيبٍ قرأه في كتاب:",
  ],
  quote=("«ثمّة ثلاث قوى، ثلاثٌ لا غير على هذه الأرض، قادرةٌ على أن تقهر ضمير هؤلاء المتمرّدين الضعفاء وتأسره إلى الأبد: المعجزة، والسرّ، والسلطة.»",
         "الإخوة كارامازوف"),
  explanation=[
   "Three glycoprotein IIb/IIIa inhibitors are in clinical use: abciximab, tirofiban and eptifibatide.",
   "Abciximab is the large molecule, a monoclonal antibody fragment; it binds with high affinity, is slowly reversible and its receptor binding is nonspecific.",
   "Its platelet inhibition lasts 24-48 hours, although its elimination half-life is only 30 minutes.",
   "Tirofiban is a synthetic nonpeptide with low affinity that binds in a rapidly reversible manner and is highly specific for the GP IIb/IIIa receptor: the correct statement.",
   "Its platelet inhibition lasts 4-8 hours, with a half-life of 2 hours.",
   "Eptifibatide is a cyclic heptapeptide, also low affinity, rapidly reversible and highly specific; its effect lasts 2-4 hours, with a half-life of 2.5 hours.",
   "So eptifibatide and tirofiban have a shorter, not longer, duration of effect than abciximab.",
  ]),

 dict(n=35, title="The mitral valve: a D behind the aortic valve", titleAr="الصمام التاجي: حرف D خلف الصمام الأبهري",
  tags=["cardiac/mitral-tricuspid", "d:anatomy", "q:recall", "l:2"],
  palette="warm golden sandstone in noon sun, vermilion doors, vivid emerald ivy, blue sky",
  words="",
  scene=("the gate of an island monastery in bright noon light, seen from slightly above. The gateway is not round: it has the "
         "exact shape of a capital letter D, and it is the focal point. Its straight side is pressed against the back of a "
         "perfectly round stone tower that stands in front of it, so the gate lies behind the tower and a little askew. The gate "
         "has two vermilion door-leaves: a large front leaf hung on the straight side, at the foot of the round tower, and a "
         "smaller rear leaf on the curved side. Thick emerald ivy creeps down from the roof over the base of the smaller rear leaf "
         "only; the large front leaf is bare, sheltered by the tower. The frame of the gate is not iron but a thick rope of "
         "fibre that does not go all the way round, drawn tight like the drawstring of a purse. From the free edges of both "
         "leaves thin cords run down to two sturdy monks planted in the ground like posts, who hold them taut. A visiting engineer "
         "with a pair of compasses frowns at the gate; an old abbot laughs."),
  story=[
   "لدير الجزيرة بوّابةٌ لا تشبه البوّابات: ليست دائرةً تامّة، بل قوسٌ مشدودٌ إلى وتر.",
   "ووترُها المستقيم ملتصقٌ بظهر البرج المستدير؛ فالبرج أمامها، وهي من خلفه مائلةٌ قليلًا.",
   "ولها مصراعان: أماميٌّ كبير معلّقٌ إلى أصل البرج، وخلفيٌّ أصغر منه.",
   "وقد زحف لبلاب السطح على أصل المصراع الخلفيّ وحده؛ فإذا تمدّد السطح جذبه معه، فانفرجت في البوّابة فُرجة.",
   "أمّا الأماميّ فلا يبلغه اللبلاب؛ البرج يحميه.",
   "وإطار البوّابة لا حديدٌ ولا عضل، بل حبلٌ من ليف، لا يكتمل طوقًا.",
   "وعند كل إغلاقٍ ينقبض الحبل انقباضَ فم الصُّرّة، فيلتقي المصراعان شفةً على شفة.",
   "ومن حافّتيهما حبالٌ رفيعة يقبض عليها راهبان مغروسان في الأرض كالوتدين، لئلّا ينقلب المصراعان إلى الوراء.",
   "سخر المهندس الزائر وفرجاره في يده: «بوّابةٌ عرجاء! سأجعلها دائرةً كاملة كما يقضي العقل».",
   "فقهقه الأب تيخون وقال:",
  ],
  quote=("«العقل وغد، أمّا الحماقة فمستقيمةٌ وشريفة.»", "الإخوة كارامازوف"),
  explanation=[
   "The mitral apparatus is the left atrial wall, the annulus, the leaflets, the chordae tendineae, the papillary muscles and the left ventricular wall.",
   "The valve lies obliquely behind the aortic valve, not in front of it; the aortic valve sits between the ventricular septum and the mitral valve.",
   "The left atrial myocardium extends over the proximal portion of the posterior leaflet, so left atrial enlargement can cause mitral regurgitation through that leaflet; the anterior leaflet is spared because it is attached to the root of the aorta.",
   "The annulus is a fibrous ring, not a fibromuscular one, that connects with the leaflets.",
   "It is not a continuous ring around the orifice, and it is D-shaped rather than circular as prosthetic valves are.",
   "The straight border of the D is posterior to the aortic valve: the correct statement.",
   "The annulus works as a sphincter that reduces the area of the valve in systole, so annular dilatation gives poor leaflet apposition and mitral regurgitation.",
  ]),

 dict(n=36, title="A normal thromboelastogram in a bleeding patient", titleAr="مخطّط تخثّرٍ طبيعي ومريضٌ ينزف",
  tags=["cardiac/perioperative", "d:postoperative", "d:diagnosis", "q:vignette", "l:2", "x:high-yield"],
  palette="a dark cellar lit by one lantern: crimson wine, five bright emerald-green cards, warm oak",
  words='"6", "2", "58", "60", "5"',
  scene=("a wine cellar lit by a single lantern. On the right a large new oak barrel is leaking: a thin, steady jet of crimson "
         "wine spurts from one small hole where a nail has fallen out of a hoop, and a boy tugs a man's sleeve and points straight "
         "at the hole: this is the focal point. On the left, at a long table, sit five solemn judges in a row behind a pot of "
         "cooper's glue. Each holds up a large emerald-green card with one number: 6, 2, 58, 60 and 5. The fourth judge is also "
         "pulling a thick cord of set glue hard with both hands and it does not break. The owner of the cellar, red in the face, "
         "stands between the judges and the barrel waving his arms at a wall of glue pots he wants poured on."),
  story=[
   "في قبو النبيذ برميلٌ جديد أُصلح صباحَ اليوم، والنبيذ ما زال ينزّ منه أحمرَ على البلاط.",
   "زأر صاحب القبو: «الغراء فاسد! عليَّ بالمحكّمين».",
   "جلس خمسة محكّمين إلى الطاولة، أمامهم قِدر الغراء، وفي يد كلٍّ منهم بطاقة.",
   "قال الأول: «بدأ ينعقد في 6 دقائق: في أوانه». ورفع بطاقته خضراء.",
   "قال الثاني: «واشتدّ في 2». وقال الثالث: «وصعد على زاوية 58».",
   "وشدّ الرابع الخيط بكلتا يديه فما انقطع: «متانته 60». وقال الخامس: «وما ذاب منه بعد نصف ساعةٍ إلا 5 في المئة».",
   "خمسُ بطاقاتٍ خضراء. والنبيذ يسيل.",
   "«إذن زيدوا الغراء!» صاح صاحب القبو. «صبّوا كل ما في المخزن!»",
   "فجذب الصبيُّ كمَّه وأشار إلى مسمارٍ سقط من طوق البرميل: «الغراء بريءٌ يا سيدي. الثقب ها هنا».",
   "احمرّ الرجل حتى أذنيه، ثم انفجر ضاحكًا، وقال ما كان يصيح به الطالب رازوميخين:",
  ],
  quote=("«بالخطأ تبلغ الحقيقة! إنما أنا إنسانٌ لأنني أخطئ.»", "الجريمة والعقاب"),
  explanation=[
   "Thromboelastography is a point-of-care test that follows the whole of clotting, from the first fibrin through clot strengthening to lysis.",
   "The R time, from the start to initial fibrin formation, is normally 5-10 minutes and is prolonged by clotting factor deficiency or anticoagulation; here it is 6.",
   "The K time, normally 1-3 minutes, and the alpha angle, normally 53-72 degrees, show how fast the clot builds and fall short with low fibrinogen or platelets; here they are 2 and 58.",
   "The maximum amplitude, normally 50-70 mm, is the ultimate strength of the clot and reflects platelets and their interaction with fibrin; here it is 60.",
   "LY30, normally 0-8%, rises with fibrinolysis; here it is 5%.",
   "Every value is normal, so this patient has normal coagulation: no factor deficiency, no lack of fibrinogen needing cryoprecipitate, no excessive fibrinolysis.",
   "With normal coagulation, continuing chest drain losses suggest a surgical source of bleeding.",
  ]),

 dict(n=37, title="Pulmonary artery pressure from the tricuspid jet", titleAr="ضغط الشريان الرئوي من نافورة الصمام ثلاثي الشرف",
  tags=["cardiac/foundations", "cardiac/mitral-tricuspid", "d:diagnosis", "q:numbers", "l:2", "x:high-yield"],
  palette="turquoise water and white foam against red brick in bright morning light",
  words='"3", "7", "43"',
  scene=("a red brick dam on a river in bright morning light. In the dam is a gate with three door-leaves that do not close "
         "completely, and through the crack between them a fierce white jet of water shoots BACKWARDS into the calm pool in front: "
         "this jet is the focal point, and the number 3 is chalked beside it. Behind the dam the reservoir is swollen and bulging, "
         "and on its brick wall a single huge number is chalked: 43. In the calm pool in front stands a measuring staff with the "
         "number 7 at the waterline. The old sluice-keeper stands on the bank holding up four fingers of one hand and a piece of "
         "chalk in the other. Beside him a worried widow of about sixty in a dark shawl looks up at the bulging reservoir; far "
         "behind it stands her mill."),
  story=[
   "على النهر سدٌّ من الآجرّ الأحمر، وفيه بابٌ بثلاثة مصاريع لا ينطبق انطباقًا تامًّا.",
   "كلّما امتلأ الخزّان من ورائه ارتدّت من شقّ الباب نافورةٌ غاضبة إلى الحوض الهادئ.",
   "جاءت الأرملة صاحبة الطاحون تسأل: «كم يحمل خزّاني من الضغط؟ أخشى أن ينفجر».",
   "لم يصعد الحارس ياكوف إلى الخزّان ولم يغُص فيه؛ نظر إلى النافورة وحدها.",
   "«سرعتُها 3»، قال. «أضربها في نفسها فتصير تسعة».",
   "ثم رفع أصابعه الأربع: «وأضرب التسعة في أربعة، وذلك قانون الماء مذ كان الماء: ستةٌ وثلاثون».",
   "وغرس مسطرته في الحوض الهادئ: «وهنا 7 من قبلُ ومن بعد. فأزيدها».",
   "وخطّ بالطبشور على جدار الخزّان رقمًا واحدًا عظيمًا: 43.",
   "شحبت الأرملة: «أبهذه السهولة يُعرف ما في جوف الحجر؟»",
   "فهزّ ياكوف كتفيه، وردّد كلمةَ طالبٍ سمعها في حانة، وما زال يرتعد منها:",
  ],
  quote=("«موتٌ واحد، ومئةُ حياةٍ في مقابله: إنها مسألة حسابٍ بسيطة!»", "الجريمة والعقاب"),
  explanation=[
   "Pulmonary artery systolic pressure can be estimated at echocardiography from the maximal velocity of tricuspid regurgitation.",
   "The modified Bernoulli equation turns the velocity into a pressure gradient: 4 x Vmax squared.",
   "The estimated right atrial pressure, here the CVP, must be added to that gradient.",
   "So 4 x 3 x 3 = 36, and 36 + 7 = 43 mmHg.",
   "The answer 36 mmHg is the gradient alone, with the right atrial pressure forgotten.",
  ]),

 dict(n=38, title="The normal distribution: 68, 95, 99.7", titleAr="التوزيع الطبيعي: 68 و95 و99.7",
  tags=["cardiac/foundations", "d:evidence", "q:numbers", "l:1", "x:high-yield"],
  palette="deep green baize and glittering gold under chandeliers, with ribbons in three blues",
  words='"68", "95", "99.7"',
  scene=("a gambling hall under chandeliers. On a long table covered in green baize lies one enormous heap of gold coins in the "
         "exact, perfectly symmetrical shape of a bell: high in the middle, thinning out to almost nothing at both ends. It is "
         "the focal point and fills the width of the picture. Three pairs of ribbons are laid across the heap, symmetrically on "
         "either side of the peak: an inner pair of dark blue ribbons close to the peak, with the number 68 between them; a second "
         "pair of medium blue ribbons further out, with the number 95; and a third pair of pale blue ribbons near the ends, with "
         "the number 99.7. Beyond the last ribbon, at the far thin end of the heap, lies a single coin that a feverish young "
         "gambler has just thrown there, his arm still stretched out. An old cashier with a rake watches him calmly."),
  story=[
   "في قاعة القمار برولتنبورغ صبّ الصرّاف الشيخ ذهبَ الليلة كلَّه على المائدة الخضراء، فاستوى كومةً على هيئة جرس.",
   "قال: «هكذا يسقط الذهب أبدًا: يتزاحم عند الوسط، ويرقّ عند الطرفين».",
   "ثم مدّ شريطين أزرقين داكنين، خطوةً عن يمين القمّة وخطوةً عن يسارها: «بين هذين 68 من كل مئة».",
   "ومدّ شريطين آخرين على خطوتين: «وبين هذين 95».",
   "ثم شريطين باهتين على ثلاث خطوات: «وبين هذين 99.7. لا يكاد يفلت شيء».",
   "وكان ألكسي واقفًا يرتجف، وفي كفّه آخرُ قطعةٍ يملكها.",
   "«وما وراء الشريط الثالث؟» سأل. قال الشيخ: «قطعةٌ أو قطعتان من ألف. هناك يراهن المجانين».",
   "«فهناك إذن أراهن!» وقذف بقطعته إلى أقصى الطرف.",
   "ضجّت القاعة بالضحك. أمّا هو فلم يسمع أحدًا، وهتف:",
  ],
  quote=("«ما أنا الآن؟ صفر. وما عساي أكون غدًا؟ غدًا قد أُبعث من بين الأموات وأبدأ الحياة من جديد!»", "المقامر"),
  explanation=[
   "The 68-95-99.7 rule, also called the three-sigma or empirical rule, describes a normal distribution.",
   "68.27% of the values lie within one standard deviation of the mean.",
   "95.45% lie within two standard deviations: the answer.",
   "99.73%, nearly all, lie within three standard deviations.",
   "The option 34.1% is the share between the mean and one standard deviation on one side only.",
  ]),

 dict(n=39, title="The balloon pump: supply up, demand down", titleAr="مضخّة البالون الأبهري: إمدادٌ يزيد وطلبٌ ينقص",
  tags=["cardiac/heart-failure", "d:physiology", "q:recall", "l:3"],
  palette="golden wheat and clear blue water under a high summer sky, with one red waistcoat",
  words="",
  scene=("the head of a great stone water channel in summer. An exhausted giant in a red waistcoat stands at a hand pump, "
         "resting between two strokes with his head bowed. In the channel in front of him lies a long leather bladder that a "
         "barefoot boy has just blown up tight: this swollen bladder is the focal point. Because it fills the channel, the water "
         "is pushed BACK towards the giant and pours over a small patch of golden wheat growing at his feet. Beside the pump "
         "stands a village elder holding a large balance: in one pan a round loaf of bread, in the other a heavy grey stone, and "
         "the pan with the bread hangs clearly lower. On the ground a clay jug of sour milk lies tipped over and nearly empty."),
  story=[
   "على رأس الساقية الكبرى مضخّةٌ يديرها العملاق إيليا بصدريّته الحمراء، من مطلع الشمس إلى مغيبها، ولا بديل له.",
   "وقد هدّه التعب حتى صار يدفع الماء دفعةً ويلهث دهرًا.",
   "فأرسلوا إليه الصبيَّ فانيا ومعه قِربةٌ طويلة، أرقدها في مجرى الساقية.",
   "كلّما استراح إيليا بين دفعتين نفخ الصبيُّ القِربة؛ فارتدّ الماء إلى الوراء وسقى حقل القمح الصغير عند قدمَي العملاق: قوتَه الذي به يحيا.",
   "وقُبيل أن يدفع إيليا دفعته أفرغ الصبيُّ القِربة بغتة؛ فلم يجد العملاق أمامه إلا فراغًا، ومضت دفعته هيّنةً ليّنة.",
   "وجاء المختار بميزانه: في كفّةٍ خبزُ إيليا، وفي كفّةٍ حجرُ عنائه.",
   "«رجحت كفّة الخبز!» صاح. «ما دام الخبز أرجحَ من الحجر فالصبيّ يُحسن صنعته».",
   "وجرّة اللبن الحامض، التي كانت تمتلئ من كدّ العملاق، أخذت تفرغ.",
   "قال الصبيّ: «لكن إن أسرع إيليا وقصرت استراحته، لم أجد وقتًا أملأ فيه قِربتي».",
   "ونظر إيليا إلى يدَي الصبيّ المتشقّقتين، وقال بصوتٍ أجشّ:",
  ],
  quote=("«الحبّ الفعّال شيءٌ قاسٍ ومخيف إذا قيس بالحبّ في الأحلام.»", "الإخوة كارامازوف"),
  explanation=[
   "An intra-aortic balloon pump inflates at the onset of diastole, raising diastolic pressure, and deflates just before systole, reducing left ventricular afterload.",
   "Inflation increases the pressure difference between the aorta and the left ventricle, the diastolic pressure time index (DPTI), and with it coronary blood flow and myocardial oxygen supply.",
   "Myocardial oxygen demand is related to the area under the left ventricular systolic pressure curve, the tension time index (TTI); deflation lowers afterload and so decreases the TTI.",
   "The ratio of supply to demand, DPTI to TTI, is the endocardial viability ratio, and it should increase if the balloon pump is working optimally.",
   "A fall, not a rise, in coronary sinus lactate is the evidence of that.",
   "The effect depends on balloon volume, on heart rate (a shorter diastolic time gives less augmentation) and on aortic compliance.",
  ]),

 dict(n=40, title="Interrupted aortic arch: types A, B and C", titleAr="انقطاع قوس الأبهر: الأنماط A و B و C",
  tags=["cardiac/congenital", "d:anatomy", "q:recall", "l:2"],
  palette="burning orange sunset sky over a violet river, with a white stone bridge",
  words='"A", "B", "C"',
  scene=("a great white stone bridge that crosses a violet river in a single high arch at sunset; it starts at the lower left, "
         "rises over the top and comes down on the right. On the top of the arch stand three towers in a row, counted from the "
         "left: first, second, third, and from each tower a road climbs away upwards. Three large signboards hang on the arch: C "
         "on the span between the first and second towers, B on the span between the second and third towers, and A on the span "
         "just beyond the third tower. The span at B has fallen out completely, leaving a wide gap with the river far below: this "
         "gap is the focal point. From a lower, neighbouring stone road a swaying rope footbridge has been strung across to the "
         "far side of the gap, and a mother carrying a child is stepping onto it. At the lower left, in the wall between the two "
         "roads, there is a wide ragged opening through which travellers from both roads mingle."),
  story=[
   "على النهر جسرٌ من حجرٍ أبيض، قوسٌ واحدٌ عظيم، تقوم على ظهره ثلاثة أبراج، ومن كل برجٍ طريقٌ يصعد إلى المدينة العليا.",
   "وُلد الجسر مقطوعًا. هكذا وجده الناس، وما عرفوه يومًا تامًّا.",
   "وقد علّق الأقدمون على مواضع القطع ثلاثة ألواحٍ لا رابع لها: A و B و C.",
   "اللوح A بعد البرج الثالث، حيث تنتهي الأبراج كلّها؛ ومن كل ثلاثة جسورٍ مقطوعة ينقطع هنا واحد.",
   "واللوح B بين البرج الثاني والثالث؛ وهنا ينقطع اثنان من كل ثلاثة، وهنا انقطع جسرنا.",
   "واللوح C بين الأول والثاني؛ ولا يكاد يراه أحدٌ في عمره كلّه.",
   "فكيف يبلغ الناس الضفّة السفلى؟ على جسرٍ من حبالٍ مُدَّ من الطريق المجاور، وُضع إلى حين، وقد يُرفع في أيّ ليلة.",
   "ومن ثغرةٍ واسعةٍ في الجدار الفاصل بين الطريقين يختلط السائرون.",
   "وقفت المرأة بطفلها عند حافّة القطع، وجسر الحبال يرتجّ تحت الريح.",
   "ثم خطت عليه، وهي تردّد ما قاله محكومٌ واقفٌ على صخرةٍ فوق الهاوية:",
  ],
  quote=("«أن يحيا، أن يحيا، أن يحيا! كيفما كانت الحياة، المهمّ أن يحيا!»", "الجريمة والعقاب"),
  explanation=[
   "Interrupted aortic arch usually occurs with a nonrestrictive ventricular septal defect and a ductus arteriosus, less commonly with a large aortopulmonary window or truncus arteriosus.",
   "It is classified into three types, A, B and C, by the site of the interruption.",
   "In type A the interruption is distal to the origin of the left subclavian artery: the correct statement.",
   "In type B it is distal to the origin of the left common carotid artery, between it and the left subclavian.",
   "In type C it is proximal to the origin of the left common carotid artery.",
   "Type B accounts for about two thirds of cases, type A for about one third and type C for less than 1%.",
   "Blood reaches the descending aorta through the ductus arteriosus.",
  ]),
]

if __name__ == "__main__":
    mcqs, chapter_path, out = sys.argv[1:4]
    questions = {q["n"]: q for q in json.load(open(mcqs, encoding="utf-8"))["questions"]}
    chapter = json.load(open(chapter_path, encoding="utf-8"))
    new = {l["id"]: l for l in (lesson(b, questions[b["n"]]) for b in BATCH)}
    kept = [l for l in chapter["lessons"] if l["id"] not in new]
    used = {l["tracks"][0]["segments"][-1]["text"] for l in kept}
    quotes = [b["quote"][0] for b in BATCH]
    assert len(set(quotes)) == len(quotes) and not used & set(quotes), "a quotation is repeated"
    chapter["lessons"] = sorted(kept + list(new.values()), key=lambda l: l["id"])
    json.dump(chapter, open(chapter_path, "w", encoding="utf-8"), ensure_ascii=False)
    prompts = {"items": [{"info": prompt(b["n"], b["palette"], b["scene"], b["words"])} for b in BATCH]}
    json.dump(prompts, open(f"{out}/image-prompts-31-40.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    for b in BATCH:
        print(b["n"], len(b["story"]) + 1, "story segments,", len(b["explanation"]), "explanation sentences")
