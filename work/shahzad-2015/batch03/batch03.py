"""Questions 21-30 of shahzad-2015: image prompt, Arabic story, quotation, English explanation, tags.

Usage:  python3 batch03.py <mcqs-0001-0100.json> <exported chapter q0001-0100.json> <out folder>
Writes the chapter with lessons 21-30 added (or replaced) and <out folder>/image-prompts-21-30.json.

What the owner asked for in this batch (2026-11-15), and what was changed to meet it:
- Stories in eloquent, forceful classical Arabic (they found the earlier ones plain). Still a real story with
  no medicine in it.
- Stronger quotations: the best known lines of Dostoevsky, each from a named work, none used in lessons 1-20.
- Better pictures: the frame below now asks for one dominant focal point, few large elements each in its own
  area, a bold colour scheme per picture and a different setting each time (the first twenty were all brown,
  snowy and crowded).
The stories were written from the scenes BEFORE the pictures existed: check each against its picture when it
arrives, adjust, and only then set the spots.
"""
import json, sys

LINK = "https://drive.google.com/drive/folders/1Rh8ghw3oe5_5yYzaTvH5iFcpJQTnrRxZ"
MEDIA = "gh:books/shahzad-2015/media/"
AUTHOR = "فيودور دوستويفسكي، "

def prompt(n, palette, scene, words):
    labels = (f"No written text anywhere except exactly these short labels, large and clearly legible, spelled exactly: {words}. "
              if words else "No written text, letters or numbers anywhere in the image. ")
    return (f"{n}. Generate one highly memorable image. Vertical 9:16 aspect ratio. "
            "Style: a masterpiece of 19th-century Russian realist oil painting in the spirit of Fyodor Dostoevsky's novels "
            "(the manner of Repin, Kramskoi and Perov), cinematic and dramatic, with strong directional light and deep shadow. "
            f"Colour scheme: {palette}. "
            "Composition: ONE dominant focal point that the eye finds at once, drawn boldly and larger than life; only the few "
            "elements described below, each of them big, clearly separated from the others and placed in its own area of the "
            "frame, with calm empty space between them. No crowd of small details, no clutter, no extra people, nothing that "
            "is not described. Faces full of feeling. "
            "Scene: " + scene +
            " Every object must be visually meaningful, distinctive and easy to recall, and all of them must be linked by "
            "physical interaction and cause-and-effect inside this one scene. No literal anatomy, no organs, no medical "
            "equipment, no unrelated objects. " + labels +
            f"Save the image with the file name {n} and upload it to: " + LINK)

BATCH = [
 dict(n=21, title="The thoracic inlet: what passes through it", titleAr="مدخل الصدر: ما الذي يعبره",
  tags=["thoracic/foundations", "d:anatomy", "q:recall", "l:1"],
  palette="cold blue dawn with one blaze of gold; scarlet and cobalt accents",
  words="",
  scene=("dawn at the only gate of a small hill fortress. The gate is a narrow, perfectly round ring of bone-white stone, like a "
         "giant finger ring set in the dark wall; it is the centre of the picture. Passing through the ring together: in front, a "
         "chimney-sweep carrying a tall ribbed stovepipe upright; directly behind him a water-carrier dragging a long, soft, limp "
         "leather hose. On one side of them riders in scarlet coats ride out through the ring and up the hill; on the other side "
         "riders in cobalt-blue coats ride back in and down. Between the riders run thin messenger boys trailing long yellow "
         "ribbons; one of them has just come out of the ring and is turning sharply back into it. An old gatekeeper counts them on "
         "his fingers. Far to one side, completely outside the ring, a rope-maker carrying a thick golden braid of exactly five "
         "ropes is climbing over the shoulder of the wall, heading for the long wooden arm of a crane; the rising sun lights his "
         "golden braid."),
  story=[
   "كان لقلعة التلّ بابٌ واحد لا ثانيَ له: طوقٌ من حجرٍ أبيضَ كالعظم، ضيّقٌ كخاتمٍ في إصبع الجبل.",
   "وعند الفجر تزاحم عليه كلُّ مَن لا قِوامَ للمدينة إلا به.",
   "في المقدّمة منظّفُ المداخن، يحمل أنبوبه المضلَّع منتصبًا كالرمح؛ ومن ورائه، لصيقًا بظهره، سقّاءٌ يجرّ خرطومًا من جلدٍ ليّن.",
   "وعن يمينهما فرسانٌ بمعاطفَ قرمزية يخرجون صاعدين، وعن شمالهما فرسانٌ بمعاطف زرقاء يعودون هابطين، لا يزاحم هؤلاءِ أولئك.",
   "وبين الخيل سُعاةٌ نحافٌ تخفق خلفهم أشرطةٌ صفراء؛ ومنهم ساعٍ ما كاد يجاوز الطوق حتى ارتدّ على عقبيه راجعًا من حيث أتى.",
   "والحارس الشيخ غريغوري يعدّهم على أصابعه واحدًا واحدًا، فما أخطأ العدَّ منذ ثلاثين شتاءً.",
   "إلا رجلًا واحدًا لم يعرف الطوقَ قطّ: فتّالُ الحبال، وعلى كتفه ضفيرةٌ من خمسة حبالٍ كأنها الذهب.",
   "رآه الحارس يعتلي كتف السور ويمضي فوق الحجر نحو ذراع الرافعة، فصاح به: «والبابُ يا رجل؟»",
   "فضحك الفتّال ملءَ الفجر: «لكلّ العابرين بابُك؛ أمّا أنا فطريقي إلى الذراع، ولا حاجةَ بي إلى جوف مدينتك».",
   "فأطرق الشيخ، وذكر سكّيرًا بكى على كتفه ذات ليلةٍ وقال:",
  ],
  quote=("«أتدرك يا سيدي الكريم ما معنى ألّا يبقى للمرء مكانٌ يذهب إليه؟ فلا بدّ لكل إنسانٍ من مكانٍ، أيِّ مكانٍ، يذهب إليه!»",
         "الجريمة والعقاب"),
  explanation=[
   "The thoracic inlet, or superior thoracic aperture, is a hole surrounded by a bony ring.",
   "The trachea passes through it, with the oesophagus directly behind.",
   "The arteries leaving the chest pass through it: the common carotid arteries, the brachiocephalic trunk and the subclavian arteries.",
   "So do the veins returning to the chest: the internal jugular, brachiocephalic and subclavian veins.",
   "The nerves that pass through are the phrenic and vagus nerves, the sympathetic trunks and the recurrent laryngeal nerves; the left recurrent laryngeal nerve climbs back up through the inlet from inside the chest.",
   "Lymph nodes and lymphatic vessels pass through as well.",
   "The brachial plexus does not: it runs from the neck over the first rib to the arm, outside the ring.",
  ]),

 dict(n=22, title="Flow-volume loop: obstruction outside the chest", titleAr="منحنى الجريان والحجم: انسدادٌ خارج الصدر",
  tags=["thoracic/foundations", "thoracic/airway", "d:diagnosis", "q:recall", "l:2"],
  figure=True,
  palette="fiery orange forge light inside against a deep violet summer dusk outside",
  words="",
  scene=("a village smithy on the edge of the steppe at dusk, shown cut open so that we see inside the wall and outside it at once. "
         "Inside, glowing orange, a gigantic leather bellows fills the room like a breathing creature, and a blacksmith works its "
         "handle. The bellows has a long neck made of soft canvas that passes through the wall and hangs out into the yard. Outside "
         "the wall, in the violet dusk, a heavy, swollen, round sack hangs on that soft neck; it is the focal point. The bellows "
         "is drawing air IN: under the weight of the sack the soft neck is sucked flat and pinched almost shut, and dust and dry "
         "leaves crowd at its mouth, barely getting in. Inside, on the wall, a boy has drawn in white chalk one simple shape: a "
         "tall sharp mountain peak standing on a horizontal line and, hanging under the line, a wide shallow bowl with a completely "
         "flat bottom. An old woman grips the blacksmith's arm and points through the door at the sack outside."),
  story=[
   "على حافّة السهب حدّادٌ اسمه يفيم، وفي جوف حدّادته كِيرٌ عظيم من جلد، يملأ المكان كأنه رئةُ البيت.",
   "وللكير عنقٌ طويل من قماشٍ ليّن، يخترق الجدار ويطلّ على الفناء.",
   "وعلى ذلك العنق، خارجَ الجدار، جثم كيسٌ منتفخٌ ثقيل لا يذكر أحدٌ مَن علّقه.",
   "كان الكير إذا زفر زمجر زمجرةَ العاصفة، فانتفخ عنقه ودفع الكيسَ عنه، وتأجّجت النار حتى احمرّ السقف.",
   "فإذا شهق انخسف العنق تحت ثِقل الكيس، فلا يدخله من الهواء إلا خيطٌ نحيل، كأنّ يدًا خفيّة تقبض على حلقه.",
   "ورسم الصبيّ بالطبشور على الجدار ما يراه كل مساء: قمّةً شاهقةً فوق الخط، ومن تحت الخط قاعًا مسطّحًا كصحنٍ فارغ.",
   "قال يفيم وقد ضاق صدره: «العلّة في جوف الكير. غدًا أشقّه بسكّيني».",
   "فقبضت العجوز مارفا على ذراعه: «يُعطي ملءَ صدره ولا يستطيع أن يأخذ! دَعِ الجوف يا أحمق، وانظر ما على عنقه في الخارج».",
   "خرج يفيم إلى الفناء، فرأى الكيس جاثمًا حيث لم يخطر له أن ينظر.",
   "فضحك ضحكةً مُرّة، وقال ما كان يقوله سيّدٌ عربيدٌ من أسياد الناحية:",
  ],
  quote=("«ما أوسعَ الإنسان! إنه أوسعُ ممّا ينبغي؛ ولو كان الأمر لي لضيّقتُه.»", "الإخوة كارامازوف"),
  explanation=[
   "A goitre in the neck is a variable obstruction of the airway outside the chest.",
   "During forced expiration the obstruction is pushed outwards by the force of the air, so the expiratory part of the flow-volume loop is normal.",
   "During inspiration the obstruction is sucked into the trachea and partly blocks it, so the inspiratory part of the loop is flattened.",
   "That is the loop shown: a normal peak above the line and a flat plateau below it.",
   "The same loop is seen with vocal cord paralysis and laryngeal tumours.",
   "Tracheal stenosis is a fixed obstruction and flattens both parts of the loop, and asthma gives a concave expiratory curve instead.",
  ]),

 dict(n=23, title="Flow-volume loop: obstruction inside the chest", titleAr="منحنى الجريان والحجم: انسدادٌ داخل الصدر",
  tags=["thoracic/foundations", "thoracic/airway", "d:diagnosis", "q:vignette", "l:2"],
  figure=True,
  palette="warm candle gold and deep wine red, with dark green shadows",
  words="",
  scene=("a wedding feast in a country tavern by candlelight. In the centre stands a bagpiper in a scarlet coat buttoned tightly "
         "up to his chin; he is the focal point, huge in the frame. Under the tight coat, on his chest, a round bulge the size of "
         "a loaf presses against the pipe of his bagpipe, straining the buttons. He is squeezing the bag hard to blow the music OUT: "
         "the coat is stretched tight over the bulge, the pipe is pinched under it, his face is red with effort, and only one thin "
         "straight streak of sound leaves the pipe. Around him the dancers stand frozen in mid-step, waiting. The bride in white "
         "looks at him with pity. At one side the innkeeper's daughter holds up a black slate on which she has drawn in white chalk "
         "one simple shape: a hill with its top cut off perfectly flat standing on a horizontal line and, hanging under the line, "
         "a deep, full, round bowl."),
  story=[
   "في عرسٍ بحانة القرية وقف الزمّار لوكا في معطفه القرمزيّ، وقد زرّره حتى الذقن.",
   "وتحت المعطف، لصيقَ القصبة، انتفخت صُرّةٌ دسّها هناك ولم يُطلع عليها مخلوقًا.",
   "كان يسحب الهواء فيدخل صدرَه طليقًا عميقًا، كما يدخل النهرُ البحر.",
   "فإذا عصر القِربة ليُخرج اللحن شدّ المعطفُ على الصرّة، وجثمت الصرّة على القصبة.",
   "فلا يخرج منها إلا أنينٌ واحدٌ ممدود، مستوٍ كحدّ السكّين، لا يعلو ولا ينخفض.",
   "ووقف الراقصون وأقدامهم معلّقةٌ في الهواء، ينتظرون نغمةً لا تجيء.",
   "ورسمت ابنة صاحب الحانة على لوحها ما سمعته: تلًّا مبتورَ الرأس فوق الخط، ومن تحت الخط قوسًا عميقًا تامًّا.",
   "صاح العريس: «قِربتك مثقوبة يا لوكا!» فهزّ رأسه: «القِربة سليمة. الداء هنا، تحت الأزرار».",
   "ثم مال على العروس وعيناه تلمعان: «آخذُ من الدنيا ما أشاء، ولا أقدر أن أردّ إليها شيئًا. أتعرفين عذابًا فوق هذا؟»",
   "فشحب وجهها، وذكرت كلمة الشيخ الناسك في الدير:",
  ],
  quote=("«ما الجحيم؟ إنه عذابُ مَن لم يعد قادرًا على أن يحبّ.»", "الإخوة كارامازوف"),
  explanation=[
   "A mass compressing the intrathoracic trachea is a variable intrathoracic obstruction, the opposite of an extrathoracic one.",
   "During inspiration the mass is pushed outwards, away from the airway, so the inspiratory part of the loop is normal.",
   "During expiration it is pushed into the trachea and partly blocks it, so the expiratory part is flattened: loop A.",
   "Loop B, flattened in both inspiration and expiration, is a fixed large airway obstruction such as tracheal stenosis or a circular tracheal tumour.",
   "Loop C, flattened only in inspiration, is a variable extrathoracic obstruction such as a goitre, vocal cord paralysis or a laryngeal tumour.",
   "Loop D is concave, the pattern of obstructive lung disease such as asthma and COPD.",
   "Loop E keeps its normal shape with a low volume, which suggests restrictive lung disease.",
  ]),

 dict(n=24, title="The pressure-volume loop: point A", titleAr="حلقة الضغط والحجم: النقطة A",
  tags=["cardiac/foundations", "d:physiology", "q:recall", "l:1", "x:high-yield"],
  figure=True,
  palette="dazzling white snow and clear blue sky, with one vivid red barrel and a cobalt-blue tap",
  words='"A", "B", "C", "D"',
  scene=("a bright frosty morning on a snowy hillside. A square track of deep footprints runs round a square yard that lies on "
         "the slope, so that its upper side is high and its lower side is low. At each corner stands a tall post with one large "
         "letter: A at the lower right, B at the upper right, C at the upper left, D at the lower left. The focal point is at post "
         "A: a huge bright red barrel filled to the very brim with water, with a red tap and a blue tap both tightly shut, and a "
         "powerful old water-carrier with his shoulder braced against it, every muscle tense, in the instant before he pushes it "
         "up the steep right-hand side. At post B, at the top, the red tap of the barrel has burst open and water shoots into a "
         "long stone trough that runs along the upper side to post C, where the barrel is nearly empty and the red tap is shut "
         "again. Down the left-hand side the tracks show the light barrel rolling downhill to post D, where the blue tap opens "
         "and clear water from a cistern quietly refills the barrel along the lower side, back to A. The footprints make it plain "
         "that he goes round anticlockwise."),
  story=[
   "على سفح التلّ فناءٌ مربّع، في كل ركنٍ منه عمودٌ عليه حرف، وحول الأعمدة يدور السقّاء كوزما منذ أربعين شتاءً.",
   "عند العمود A، في الركن الأسفل الأيمن، يقف برميله الأحمر ممتلئًا حتى الحافّة، وقد أُحكم صنبوراه كلاهما.",
   "تلك لحظتُه: لم تبقَ قطرةٌ تدخل، ولم تخرج قطرةٌ بعد. امتلاءٌ تامّ، وسكونٌ يسبق العاصفة.",
   "ثم يسند كتفه إلى البرميل ويدفعه صُعُدًا في المنحدر؛ لا ينقص ماؤه ولا يزيد، وعروق عنقه تكاد تتفجّر.",
   "وعند B في الأعلى ينفتح الصنبور الأحمر قسرًا، فيندفع الماء هادرًا إلى ساقية المدينة.",
   "وعند C يكاد البرميل يفرغ، فينغلق الأحمر.",
   "ثم ينحدر به خفيفًا إلى D، والصنبوران مغلقان؛ وهناك ينفتح الأزرق، ويعود الماء من الصهريج هادئًا حتى يبلغ A من جديد.",
   "هكذا يدور، عكسَ عقارب الساعة، دورةً في إثر دورة، لا يملّ ولا يشكو.",
   "سأله طالبٌ عابر: «أيُّ الأركان أحبُّ إليك أيها الشيخ؟»",
   "فأشار إلى A، إلى برميله الممتلئ قبل الدفعة، وقال بصوتٍ متهدّج:",
  ],
  quote=("«يا إلهي! لحظةٌ كاملةٌ من النعيم! أفَقليلٌ هذا، ولو على عمر إنسانٍ بأسره؟»", "الليالي البيضاء"),
  explanation=[
   "Point A is the end-diastolic point: filling is complete and this is where contraction begins.",
   "Pressure rises above atrial pressure and the mitral valve closes; the aortic valve is still closed, so from A to B the volume is constant: isovolumic contraction.",
   "At B the pressure becomes higher than aortic pressure, the aortic valve opens and ejection begins; from B to C the volume falls.",
   "C is the end-systolic point, where the aortic valve closes.",
   "From C to D both valves are closed again and pressure falls at constant volume: isovolumic relaxation.",
   "At D the pressure falls below atrial pressure and the mitral valve opens; DA is the diastolic filling period, completed by atrial contraction.",
   "Each loop is formed anticlockwise, and its width is the stroke volume.",
  ]),

 dict(n=25, title="Predicted postoperative FEV1", titleAr="وظيفة الرئة المتوقَّعة بعد الاستئصال",
  tags=["thoracic/lung-cancer", "thoracic/foundations", "d:indications", "q:numbers", "l:2", "x:high-yield"],
  palette="white walls and gold bells against a deep blue evening sky, with black smoke as the one dark accent",
  words='"19", "16", "2.6", "2.2"',
  scene=("a white monastery with two tall open bell towers at blue evening, seen from the road. The right tower holds ten golden "
         "bells in three tiers: three at the top, two in the middle, five at the bottom. The left tower holds nine golden bells: "
         "three, two and four. The focal point is the top tier of the right tower: its three bells are blackened and cracked by "
         "the smoke that rises from the long pipe of an old monk sitting just beneath them, and they are being lowered on ropes "
         "by other monks. In the foreground a young novice holds up a slate on which the number 19 is struck through and the "
         "number 16 is written large beside it. A straight road runs from the monastery towards the viewer with two white "
         "milestones on it: the farther one, near the viewer, marked 2.6, and a nearer one to the monastery marked 2.2, beside "
         "which the novice's footprints stop."),
  story=[
   "لدير القدّيس نيقولا برجان، وفيهما تسعة عشر جرسًا: عشرةٌ في البرج الأيمن، وتسعةٌ في الأيسر.",
   "في الأيمن ثلاثةٌ في الذروة، واثنان في الوسط، وخمسةٌ في القاعدة؛ وفي الأيسر ثلاثةٌ واثنان وأربعة.",
   "وكانت إذا قُرعت جميعًا بلغ صوتها الحجرَ المنقوش عليه 2.6 على طريق السهل.",
   "لكنّ الأب بافنوتي دخّن غليونه أربعين سنةً تحت أجراس الذروة اليمنى، حتى اسودّت الثلاثة وتصدّعت.",
   "قال رئيس الدير: «تُنزَل الثلاثة الليلة، قبل أن تُفسد أخواتها».",
   "فضجّ الرهبان بالبكاء: «سيخرس الدير! سينسانا الله والناس!»",
   "أمّا المبتدئ أليوشا فلم يبكِ؛ أخذ لوحه، وشطب التسعة عشر، وكتب تحتها: ستة عشر.",
   "ثم مشى على الطريق حتى وقف عند الحجر 2.2، وقال: «إلى هنا يبلغ صوتنا غدًا، لا أبعد ولا أدنى. وهذا يكفي لنحيا».",
   "حدّق فيه الرئيس طويلًا، ثم قال وفي صوته رهبةٌ من برودة الحساب:",
  ],
  quote=("«اثنان في اثنين يساوي أربعة: هذا لم يعد حياةً يا سادة، بل هو بداية الموت.»", "رسائل من تحت الأرض"),
  explanation=[
   "Predicted postoperative FEV1 is the preoperative FEV1 multiplied by the number of bronchopulmonary segments that will remain and divided by the total number of segments.",
   "There are normally 19 segments: 10 in the right lung (3 upper, 2 middle, 5 lower) and 9 in the left (3, 2 and 4).",
   "A right upper lobectomy removes 3 segments and leaves 16.",
   "So 2.6 litres x 16/19 = 2.19, about 2.2 litres.",
   "The percentage is predicted the same way: 80% x 16/19 = 67% of predicted normal.",
   "The same formula can be used for FVC and DLCO.",
  ]),

 dict(n=26, title="Schatzki ring", titleAr="حلقة شاتسكي",
  tags=["thoracic/oesophagus", "d:diagnosis", "q:vignette", "l:2"],
  palette="summer noon: crimson silk and silver against fresh green and a pale sky, with steam from a copper samovar",
  words="",
  scene=("a summer fair. From a wooden beam hangs a long, narrow sleeve of crimson silk, like a windsock, reaching down into a "
         "wicker basket on the ground; it runs down the centre of the picture. Near its lower end, just above the basket, a thin "
         "bright silver ring is sewn inside it and shows as a tight, shining waist. A whole round meat pie that a fat merchant "
         "has just dropped in is stuck exactly at the silver ring, and the silk bulges above it like the neck of a goose that has "
         "swallowed an apple: this is the focal point. The red-faced merchant shakes the sleeve angrily. A boy standing on a stool "
         "pours a glass of steaming tea from a copper samovar into the top of the sleeve to wash the pie down. Chickpeas thrown by "
         "a child run straight through a second sleeve beside it without stopping. In the background, a row of fourteen more "
         "identical crimson sleeves hangs quietly from the beam, each with its faint silver waist and nothing stuck in any of them."),
  story=[
   "في سوق الصيف نصب الغجريّ ميخاي لعبته: كُمٌّ طويل من حريرٍ قرمزيّ، يتدلّى من العارضة إلى سلّةٍ من خوص.",
   "وفي أسفل الكمّ، قُبيل السلّة، خاط من داخله حلقةً رفيعةً من فضّة؛ ثابتةً لا تتّسع ولا تضيق.",
   "ونادى في الناس: «ألقِ ما شئت! فإن بلغ السلّة فهو لك».",
   "ألقى الأطفال حبّاتِ الحمّص فانسابت انسيابَ الماء، وما وقف منها شيء.",
   "ثم أقبل التاجر السمين بفطيرة لحمٍ كاملة، لم يكسرها ولم يتمهّل، وقذف بها في الكمّ.",
   "هوت الفطيرة حتى بلغت الحلقة، فوقفت؛ وانتفخ الحرير فوقها كعنق إوزّةٍ ابتلعت تفّاحة.",
   "هزّ التاجر الكمّ وسبّ ولعن واحمرّ حتى أذنيه، والفطيرة لا تتزحزح.",
   "فصعد صبيُّ السماور على كرسيّه، وصبّ فوقها كوبًا من الشاي الساخن؛ فانزلقت ومضت إلى السلّة كأن لم يكن شيء.",
   "ضحك الناس: «تَعلَق مرّةً وتمرّ عشرًا!» ومن خلفهم أربعة عشر كمًّا مثله، في كلٍّ منها حلقة، وما اشتكى منها أحد.",
   "أمّا التاجر فرفع كوبه عاليًا، وأقسم أمام السوق كلّه:",
  ],
  quote=("«أيفنى العالم، أم أُحرم أنا من شايي؟ أقول: ليَفنَ العالم، على أن أشرب شايي دائمًا!»", "رسائل من تحت الأرض"),
  explanation=[
   "Intermittent, short-lived dysphagia for solids, in which the food can be forced down with liquids or regurgitated, is typical of a Schatzki ring.",
   "It is a fixed mucosal ring in the distal oesophagus.",
   "Lower oesophageal rings are found in 6-14% of routine barium examinations and the vast majority cause no symptoms; rings that cause symptoms, the true Schatzki rings, are found in only 0.5%.",
   "The diagnosis therefore rests on both the clinical and the radiological findings.",
   "Muscular rings, or A rings, are different: transient, smooth, circumferential indentations.",
   "Diffuse spasm, nutcracker oesophagus and a hypertensive sphincter are disorders of motility rather than a fixed ring, and a carcinoma gives dysphagia that progresses instead of coming and going.",
   "In a large endoscopic series a Schatzki ring was the fourth most common finding in dysphagia.",
  ]),

 dict(n=27, title="Achalasia: what is lost", titleAr="تعذّر ارتخاء المريء: ما الذي يُفقَد",
  tags=["thoracic/oesophagus", "d:pathology", "d:pharmacology", "q:recall", "l:3"],
  palette="dark pine green and wet timber brown under a stormy grey sky, with one bright emerald flask and a red lantern gone dark",
  words='"10"',
  scene=("a long wooden log-slide running down a forested mountainside from the top of the picture to a river at the bottom. At "
         "the bottom a massive timber gate is clamped shut by a scowling, bearded gatekeeper with both fists; behind the gate the "
         "logs are jammed in a huge pile and the slide above it has bulged out wide like a sack: this is the focal point. Along "
         "the slide stand a line of small stools for the men who used to pass the logs down with poles: the lower stools are "
         "empty, each with only an abandoned cap and pole on it, and only at the very top do two men still sit and push. Beside "
         "the gate, on an empty hook under a dark, unlit lantern, hangs a large key whose owner is gone. The gatepost carries ten "
         "deep notches and the number 10. Above, a telegraph wire coming from a distant station has frayed and snapped, its end "
         "dangling. At the gatekeeper's feet stands a bright emerald-green flask, and his fists are clenched white; a little girl "
         "holds up to him a sprig of glossy black berries, and the fingers of his nearer hand are beginning to open."),
  story=[
   "على سفح الغابة مَزلقٌ طويل من خشب، تنحدر فيه الجذوع من القمّة إلى النهر، وفي أسفله بوّابةٌ ثقيلة.",
   "وكان على طول المزلق رجالٌ على مقاعدهم، يدفع كلٌّ منهم الجذعَ إلى جاره بعصاه، فتمضي الجذوع كأنها موجةٌ واحدة.",
   "وعند البوّابة رجلان: فيدور العابس الذي يشدّها، وصاحبُ المفتاح الوديع الذي يهمس له: «أَرخِ يدك»، فتنفتح.",
   "ثم أخذ الرجال يختفون؛ من أسفل المزلق أولًا، ثم الذي فوقه، ثم الذي فوقه.",
   "حفر فيدور في العمود حزًّا لكل سنة؛ فلمّا بلغت الحزوز عشرةً لم يبقَ على المقاعد إلا القبّعات.",
   "وغاب صاحب المفتاح مع الغائبين، وبقي مفتاحه معلّقًا على مسماره تحت فانوسٍ منطفئ.",
   "حتى السلك الآتي من المحطة البعيدة تهرّأ وانقطع، فلم يعد يبلغ البوّابةَ أمرٌ ولا خبر.",
   "فبقي فيدور وحده، قابضًا على البوّابة لا يُرخيها، والجذوع تتراكم خلفها حتى انتفخ المزلق انتفاخَ الغريق.",
   "سقَوه من القارورة الخضراء فازدادت قبضته تشنّجًا؛ وناولته الصبيّة عنقودًا من التوت الأسود فارتخت أصابعه قليلًا.",
   "ووقف الحطّاب الشيخ ينظر إلى الطريق الذي شقّه بيديه وقد صار خرابًا، وقال:",
  ],
  quote=("«الإنسان يحبّ أن يبني وأن يشقّ الطرق، هذا لا جدال فيه. فلماذا إذن يعشق الهدم والفوضى أيضًا، إلى حدّ الهُيام؟»",
         "رسائل من تحت الأرض"),
  explanation=[
   "Achalasia is the best defined primary motility disorder and the only one with an established pathology.",
   "The predominant process is the loss of ganglion cells, not muscle cells, from the wall of the oesophagus, starting at the lower oesophageal sphincter and developing proximally.",
   "The loss parallels the duration of the disease: at 10 years ganglion cells are likely to be completely absent.",
   "At the sphincter the inhibitory nerves are lost, shown by loss of staining for nitric oxide synthase and VIP.",
   "In the oesophageal body achalasia is characterised by a loss of intrinsic acetylcholine-containing nerves, which is the correct statement.",
   "Extrinsic nerves may also be affected, with Wallerian degeneration within the vagus nerve and its dorsal motor nucleus.",
   "Edrophonium increases lower oesophageal sphincter pressure and atropine decreases it, which probably explains why botulinum toxin, an inhibitor of acetylcholine release, can help.",
  ]),

 dict(n=28, title="A rib that grows inwards: osteochondroma", titleAr="ضلعٌ ينمو إلى الداخل: الورم العظمي الغضروفي",
  tags=["thoracic/chest-wall", "d:diagnosis", "q:vignette", "l:3"],
  palette="a single candle: warm amber on pale oak, deep black shadow, and one shocking streak of crimson",
  words='"5"',
  scene=("night in a cooper's workshop. A gigantic oak barrel lies on its side, shown cut open so that we look into it; its "
         "curved staves are like the walls of a small room. Inside it a seventeen-year-old apprentice has been sleeping on straw "
         "and has just woken, clutching his side in pain. Beside him inside the barrel lies a large inflated leather wineskin. "
         "From the fifth stave at the front of the barrel, marked with the number 5, a thick knot of the same oak has grown "
         "INWARDS like a thorn, and its tip wears a smooth, glistening, pearl-white cap: this is the focal point. The thorn has "
         "pierced the wineskin: a jet of air hisses out of it, blowing the straw about, and a thin stream of crimson wine runs "
         "down onto the straw. The master cooper leans in holding a candle in one hand and a saw in the other; the candle throws "
         "the shadow of the thorn onto the inner wall of the barrel as one sharp black triangle."),
  story=[
   "نام الفتى ميتيا، ابنُ السابعة عشرة، في جوف البرميل العظيم الذي يصنعه معلّمه، كما اعتاد أن ينام منذ طفولته.",
   "كان البرميل بيتَه: ألواحُه المقوّسة جدرانُه، وفي حضنه قِربةٌ منتفخة تعلو وتهبط كأنها تتنفّس معه.",
   "ومن اللوح الخامس، في صدر البرميل، نبتت عقدةٌ من الخشب نفسه، على رأسها قلنسوةٌ بيضاء لامعة كاللؤلؤ.",
   "لم تنبت إلى الخارج فتراها عين؛ نبتت إلى الداخل، في العتمة، صامتةً كسرٍّ قديم.",
   "كبرت معه عامًا بعد عام، تطول كلّما طال، ولم توجعه يومًا.",
   "وفي تلك الليلة تقلّب في نومه، فانغرزت في القِربة.",
   "صحا على طعنةٍ في جنبه مع كل نَفَس؛ والقِربة تصفر هواءً، وتقطر أحمرَ قانيًا على القشّ.",
   "رفع المعلّم شمعته، فارتمى ظلّ العقدة على الجدار مثلّثًا أسود.",
   "قال وهو يتناول المنشار: «خفتُ عليك من الريح والبرد والذئاب، وما كان العدوّ إلا من ألواحنا نحن».",
   "فابتسم الفتى في وجعه، وردّد ما قرأه في رسالة سجينٍ إلى أخيه:",
  ],
  quote=("«الحياة حياةٌ في كل مكان؛ الحياة فينا نحن، لا في ما هو خارجٌ عنّا.»", "رسالة إلى أخيه ميخائيل، 22 ديسمبر 1849"),
  explanation=[
   "An osteochondroma is a cartilage-capped outgrowth of bone, a developmental defect of growth, and it stops growing at skeletal maturity.",
   "Primary tumours of the thoracic cage make up 5-8% of primary bone tumours, and osteochondroma of a rib is exceedingly rare.",
   "A rib exostosis is either a solitary costal exostosis or part of hereditary multiple exostosis, an autosomal dominant condition usually seen in the long bones.",
   "One that projects outwards may be felt as a swelling on the chest; one that projects inwards is generally symptomless and found by chance on a chest radiograph.",
   "A large inward one can present with acute chest pain and complications such as haemothorax, haemopneumothorax, pleural or pericardial effusion and diaphragmatic rupture.",
   "A teenager with pleuritic pain, a haemopneumothorax and a mass growing inwards from a rib therefore fits an osteochondroma.",
   "An exostosis that penetrates an intrathoracic structure can be life-threatening and needs to be resected.",
  ]),

 dict(n=29, title="The farmer's cough: hydatid cyst", titleAr="سعال الفلّاح: الكيسة العدارية",
  tags=["thoracic/lung-benign", "d:diagnosis", "q:vignette", "l:2"],
  figure=True,
  palette="golden evening light through a small window, straw yellow and earth brown, with pure white and clear aquamarine in the glass",
  words="",
  scene=("the inside of a poor farmer's hut on the steppe at golden evening, where the man lives together with his animals. In "
         "the lower left corner of the room stands a gigantic, perfectly round, smooth-walled glass carboy, as tall as a man's "
         "waist: this is the focal point, lit by the window. It is exactly half full of clear aquamarine water with a sharp level "
         "line, the upper half is empty air, and on the surface of the water floats a crumpled, folded white membrane like a "
         "white water lily on a pond; a few small glass beads lie at the bottom. In the middle of the room the farmer, bent "
         "double by a fit of coughing, holds a cloth to his mouth and stares at the carboy in alarm. A shaggy sheepdog licks his "
         "free hand; a very long white ribbon is tied to the dog's collar and trails across the floor to three sheep lying in the "
         "straw on the right, winding round their legs."),
  story=[
   "عاش الفلّاح نيكيفور مع غنمه وكلبه تحت سقفٍ واحد؛ يأكلون من قصعةٍ واحدة، وينامون على قشٍّ واحد.",
   "وفي عنق الكلب شريطٌ أبيض طويل، ينسلّ طرفه بين قوائم الغنم، ولا يدري أحدٌ أين ينتهي.",
   "وفي الركن الأسفل الأيسر من الكوخ قارورةٌ مستديرةٌ عظيمة، ملساءُ الجدار، لبثت سنين مملوءةً ماءً صافيًا لا يمسّها أحد.",
   "كانت تكبر في صمت، وهو لا يراها تكبر.",
   "ثم جاءه السعال: نوباتٌ تهزّه هزًّا وتطويه طيًّا، يلفظ في إثرها ماءً مالحًا كماء البحر.",
   "وذات مساءٍ التفت إلى القارورة فارتاع: هبط ماؤها إلى النصف، واحتلّ الهواء نصفها الأعلى.",
   "وعلى وجه الماء طفا غشاءٌ أبيض متجعّد، كزنبقة ماءٍ على بركةٍ ساكنة.",
   "قال جاره: «ما أجملها!» فقال نيكيفور: «ليست زهرة. هذا كفنُ ما كان يسكن جوفها».",
   "ثم نظر إلى الكلب يلعق يده، وإلى الغنم الوادعة، فلم يجد في قلبه عليها ضغينة.",
   "وتمتم بما سمعه يومًا من سيّدٍ شابٍّ محموم العينين:",
  ],
  quote=("«يتحدّثون أحيانًا عن قسوة الإنسان \"الوحشية\"، وفي هذا ظلمٌ فادحٌ للوحوش وإهانةٌ لها: فالوحش لا يستطيع أبدًا أن يكون قاسيًا كالإنسان.»",
         "الإخوة كارامازوف"),
  explanation=[
   "Cystic echinococcosis, or hydatid disease, is the larval cystic stage of the small tapeworm Echinococcus granulosus; its intermediate hosts are herbivorous animals, and people are infected accidentally.",
   "The plain chest radiograph is the most valuable test: an uncomplicated cyst is a homogeneous round or oval mass with smooth borders, from 1 to 20 cm across.",
   "As the cyst grows it erodes bronchioles and air enters between its layers, giving the crescent or meniscus sign, which some consider a sign of impending rupture.",
   "If the cyst ruptures into the bronchial tree its contents are coughed up and an air-fluid level appears.",
   "When the endocyst has collapsed completely, its crumpled membrane floats on the remaining fluid: the water-lily sign, pathognomonic of a collapsed endocyst and seen in this patient's images.",
   "CT shows the cystic nature of the mass, with contents close to the density of water, and localises it for surgery.",
   "A farmer who lives alongside his livestock, with this picture, has cystic echinococcosis rather than an aspergilloma, an abscess or a cavitating carcinoma.",
  ]),

 dict(n=30, title="Torsade de pointes", titleAr="تورساد دي بوانت: الالتواء حول الخط",
  tags=["cardiac/arrhythmia", "d:diagnosis", "d:pharmacology", "q:vignette", "l:2", "x:high-yield"],
  figure=True,
  palette="deep indigo night, a scarlet ribbon, and one blinding burst of pure white light",
  words="",
  scene=("a fairground at night. A perfectly straight tightrope is stretched horizontally across the whole picture between two "
         "posts. A very long scarlet ribbon is wound round the rope: on the left it lies along the rope in one long, flat, "
         "stretched-out length, and then it breaks into wild twisting spindles that swell and shrink and swell again, swinging "
         "above the rope and then below it: this twisting ribbon is the focal point. A young rope-dancer in white, from whose hand "
         "the ribbon has escaped, is fainting and falling backwards off the rope into a net. Below on the left an old drummer "
         "stands frozen with his drumstick held high in a pause that has lasted too long, an empty little medicine flask at his "
         "feet, and a travelling pedlar of potions slinks away behind him. On the ground under the rope lies a hopelessly tangled "
         "heap of scarlet ribbon with no beginning and no end. On the right a photographer beside a wooden box camera fires a tray "
         "of flash powder: a blinding burst of pure white light floods the scene, and where the light reaches it the ribbon is "
         "falling straight and still along the rope again."),
  story=[
   "في سوق المساء شُدَّ حبلٌ مستقيم بين عمودين؛ عليه ترقص كاتيا، ومن تحته يضبط الطبّال الشيخ خطاها.",
   "دقّةٌ فخطوة، دقّةٌ فخطوة؛ هكذا عاشا عشرين عامًا، لا يسبق أحدهما الآخر.",
   "في تلك الليلة شكا الطبّال رجفةً في يده، فدسّ له بائع العقاقير الجوّال قارورةً صغيرة: «جرعةٌ واحدة، ويستقيم كلّ شيء».",
   "شرب. فطالت الوقفة بين الدقّة وأختها؛ طالت حتى حبس الناس أنفاسهم.",
   "وفي تلك الهوّة الطويلة انفلت الشريط القرمزيّ من يد كاتيا.",
   "راح يلتفّ حول الحبل؛ يتّسع ثم يضيق ثم يتّسع، يعلوه مرّةً ويهوي تحته مرّة، كمغزلٍ أصابه الجنون.",
   "دارت بها الدنيا، وهوت مغشيًّا عليها في الشبكة.",
   "وتحت الحبل كومةٌ من شرائط متشابكة، لا رأس لها ولا ذيل: إليها ينتهي كل شريطٍ يُترك لجنونه.",
   "عندئذٍ أشعل المصوّر مسحوقه الأبيض، فغمر الساحةَ وميضٌ كالبرق؛ وسكن الشريط، واستوى على حبله.",
   "فتحت كاتيا عينيها ويداها ترتجفان، وهمست بما يهمس به كلُّ مَن وقف على الحافّة:",
  ],
  quote=("«أقملةٌ أنا كسائر الناس، أم إنسان؟ أمخلوقٌ مرتعشٌ أنا، أم لي الحق؟»", "الجريمة والعقاب"),
  explanation=[
   "Torsade de pointes is a specific form of polymorphic ventricular tachycardia in patients with a long QT interval.",
   "Its rapid, irregular QRS complexes appear to twist around the ECG baseline, their polarity shifting from one side to the other.",
   "Between episodes the ECG shows a long QT interval after correction for heart rate; normal values average about 0.44 seconds.",
   "It most commonly results from a drug, usually a class Ia, Ic or III antiarrhythmic, and flecainide is class Ic; tricyclic antidepressants, phenothiazines and certain antivirals and antifungals can also induce it.",
   "The rate of 200 to 250 beats per minute does not perfuse, so patients present with syncope, and palpitations if conscious.",
   "It may cease spontaneously or degenerate into ventricular fibrillation.",
   "Treatment is intravenous magnesium, measures to shorten the QT interval, and direct-current defibrillation when ventricular fibrillation is precipitated.",
  ]),
]

# Obvious printing errors in stems and options, fixed as the guide allows (the master file keeps the printed text).
TYPOS = {"acetylcholinecontaining": "acetylcholine-containing", "Wolff-Parkison-White": "Wolff-Parkinson-White"}

def lesson(b, q):
    n = b["n"]
    assert q["n"] == n
    fix = lambda s: [s := s.replace(a, c) for a, c in TYPOS.items()][-1]
    story = [{"text": t} for t in b["story"]] + [{"text": b["quote"][0], "quote": AUTHOR + b["quote"][1]}]
    quiz = {"q": fix(q["stem"]), "options": [fix(o) for o in q["options"]], "answer": q["answer"],
            "explain": " ".join(b["explanation"]), "lang": "en", "source": f"Shahzad 2015, Q{n}"}
    if b.get("figure"):
        quiz["image"] = f"{MEDIA}figure-q{n:04d}.png"
    return {"id": f"shahzad-q{n:04d}", "title": b["title"], "titleAr": b["titleAr"], "tags": b["tags"],
            "tracks": [{"kind": "story", "lang": "ar", "segments": story},
                       {"kind": "explanation", "lang": "en", "segments": [{"text": t} for t in b["explanation"]]}],
            "quiz": [quiz]}

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
    json.dump(prompts, open(f"{out}/image-prompts-21-30.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    for b in BATCH:
        print(b["n"], len(b["story"]) + 1, "story segments,", len(b["explanation"]), "explanation sentences")
