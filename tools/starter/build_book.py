"""Writes book.json for the starter book. Edit the text here, then run:  python3 tools/starter/build_book.py"""
import json, pathlib

def spot(x, y, r): return {"x": x, "y": y, "r": r}
def seg(text, s=None): return {"text": text, **({"spot": s} if s else {})}
def q(question, options, answer, explain): return {"q": question, "options": options, "answer": answer, "explain": explain, "lang": "en"}

# ---- How Light Travels ----------------------------------------------------
L_SUN, L_BEAM, L_MIRROR = spot(0.213, 0.146, 0.16), spot(0.53, 0.245, 0.17), spot(0.796, 0.323, 0.14)
L_PRISM, L_RAINBOW = spot(0.315, 0.49, 0.13), spot(0.63, 0.545, 0.21)
L_SHADOW, L_GLASS = spot(0.36, 0.76, 0.26), spot(0.76, 0.715, 0.16)

light = {
    "id": "how-light-travels", "title": "How Light Travels", "titleAr": "كيف يسافر الضوء", "image": "asset:seed/light.webp",
    "tracks": [
        {"kind": "story", "lang": "ar", "segments": [
            seg("في صباحٍ مشرق، سألت ليلى جدَّها: يا جدي، كيف يصل ضوء الشمس إلينا؟"),
            seg("ابتسم الجد وقال: الضوء يا ليلى مسافرٌ لا يتعب. يخرج من الشمس ويقطع نحو مئةٍ وخمسين مليون كيلومتر في ثماني دقائق فقط.", L_SUN),
            seg("وهو يسافر دائمًا في خطوطٍ مستقيمة، لا ينحني ولا يلتفّ في طريقه.", L_BEAM),
            seg("وإذا اصطدم بسطحٍ لامعٍ كالمرآة ارتدّ عنه، كما ترتدّ الكرة عن الجدار. نسمّي ذلك الانعكاس.", L_MIRROR),
            seg("وحين يمرّ الضوء الأبيض عبر منشورٍ زجاجي، يكشف لنا سرَّه.", L_PRISM),
            seg("فهو في الحقيقة مزيجٌ من ألوانٍ كثيرة، تنفصل كلها لتظهر كقوس قزح.", L_RAINBOW),
            seg("وإذا وقف في طريقه شيءٌ لا يسمح له بالمرور، كهذه الشجرة، تكوَّن خلفه الظل.", L_SHADOW),
            seg("أما حين ينتقل من الهواء إلى الماء فإنه يبطئ قليلًا وينحرف، فتبدو المصّاصة في الكوب كأنها مكسورة. هذا هو الانكسار.", L_GLASS),
            seg("ضحكت ليلى وقالت: إذن الضوء مسافرٌ سريع، مستقيم الطريق، وله حيلٌ جميلة!"),
        ]},
        {"kind": "explanation", "lang": "en", "segments": [
            seg("Light is a form of energy that travels outward from a source, such as the Sun. In empty space it moves at about 300,000 kilometres per second.", L_SUN),
            seg("Light travels in straight lines. This is called rectilinear propagation.", L_BEAM),
            seg("When light hits a smooth, shiny surface it bounces off. This is reflection: the angle at which it arrives equals the angle at which it leaves.", L_MIRROR),
            seg("White light is a mixture of colours. A prism bends each colour by a slightly different amount.", L_PRISM),
            seg("So the colours spread out into a spectrum, from red to violet. This is called dispersion.", L_RAINBOW),
            seg("An opaque object blocks light. Because light does not bend around it, a shadow forms on the far side.", L_SHADOW),
            seg("When light passes from air into water it slows down and changes direction. This bending is refraction, and it makes the straw look broken.", L_GLASS),
        ]},
    ],
    "quiz": [
        q("How does light travel through a uniform material such as air?", ["In straight lines", "In circles", "In zigzags", "Only downward"], 0,
          "Light travels in straight lines (rectilinear propagation). That is why shadows have clear shapes."),
        q("What is it called when light bounces off a mirror?", ["Refraction", "Reflection", "Dispersion", "Absorption"], 1,
          "Reflection is light bouncing off a surface. The angle of incidence equals the angle of reflection."),
        q("Why does a straw look broken in a glass of water?", ["The water magnifies the straw", "Light changes speed and direction between water and air", "The straw absorbs water", "The glass is curved"], 1,
          "This is refraction: light slows down in water and bends at the surface, so the underwater part appears shifted."),
        q("About how long does sunlight take to reach Earth?", ["8 seconds", "8 minutes", "8 hours", "8 days"], 1,
          "The Sun is about 150 million km away. At 300,000 km per second, light needs roughly 8 minutes."),
    ],
}

# ---- Why We Remember Stories ----------------------------------------------
M_FIRE, M_BRAIN, M_HIPPO = spot(0.5, 0.83, 0.26), spot(0.5, 0.215, 0.27), spot(0.556, 0.245, 0.10)
M_FACTS, M_CHAIN, M_HEART = spot(0.231, 0.505, 0.16), spot(0.694, 0.513, 0.19), spot(0.5, 0.645, 0.12)

memory = {
    "id": "why-we-remember-stories", "title": "Why We Remember Stories", "titleAr": "لماذا نتذكّر القصص؟", "image": "asset:seed/memory.webp",
    "tracks": [
        {"kind": "story", "lang": "ar", "segments": [
            seg("منذ آلاف السنين، قبل الكتب والمدارس، كان الناس يجتمعون حول النار ليحكوا القصص.", M_FIRE),
            seg("بهذه الطريقة حفظوا أين يوجد الماء، وأيّ النباتات سامّ، وكيف ينجون من الخطر.", M_FIRE),
            seg("واليوم يعرف العلماء السبب: دماغنا يحبّ القصص أكثر مما يحبّ المعلومات المتفرقة.", M_BRAIN),
            seg("جرّب أن تحفظ قائمةً من الحقائق الجافة، وستنساها غالبًا بعد أيام.", M_FACTS),
            seg("أما القصة فتربط الأحداث ببعضها: هذا حدث، فأدّى إلى ذاك. والدماغ يتذكر السلاسل المترابطة بسهولة.", M_CHAIN),
            seg("والقصة تحرّك مشاعرنا؛ نخاف ونفرح ونتشوّق. والمشاعر تقول للدماغ: هذا مهم، احتفظ به!", M_HEART),
            seg("عندها ينشط الحُصين، وهو جزءٌ صغير في الدماغ يشبه فرس البحر، فيحوّل ما سمعناه إلى ذكرى طويلة الأمد.", M_HIPPO),
            seg("لذلك، إذا أردت أن تتذكر درسًا، فاجعله قصة."),
        ]},
        {"kind": "explanation", "lang": "en", "segments": [
            seg("Memory works in three steps: encoding information, storing it, and retrieving it later.", M_BRAIN),
            seg("Isolated facts are hard to encode because they have few connections to what we already know.", M_FACTS),
            seg("A story organises information into causes and effects. Each event becomes a cue for recalling the next one.", M_CHAIN),
            seg("Stories also trigger emotion. The amygdala, the brain's emotion centre, marks emotional events as important.", M_HEART),
            seg("That signal strengthens the work of the hippocampus, which consolidates new experiences into long-term memory.", M_HIPPO),
            seg("Stories make us imagine sights, sounds and feelings, so the same memory is stored through several routes.", M_FIRE),
            seg("To study better: link facts into a narrative, add vivid images, and connect them to things you care about."),
        ]},
    ],
    "quiz": [
        q("Which brain structure is most important for forming new long-term memories?", ["Cerebellum", "Hippocampus", "Brainstem", "Optic nerve"], 1,
          "The hippocampus consolidates new experiences into long-term memory."),
        q("Why are stories easier to remember than lists of facts?", ["They are longer", "They link events through cause and effect", "They use difficult words", "They are always true"], 1,
          "Connected events act as cues for one another, so recalling one part brings back the next."),
        q("What role does emotion play in memory?", ["It erases memories", "It marks an event as important, strengthening the memory", "It has no effect", "It only affects short-term memory"], 1,
          "Emotional arousal activates the amygdala, which boosts consolidation by the hippocampus."),
        q("What are the three stages of memory, in order?", ["Storage, retrieval, encoding", "Encoding, storage, retrieval", "Retrieval, encoding, storage", "Encoding, retrieval, storage"], 1,
          "Information is first encoded, then stored, and finally retrieved when needed."),
    ],
}

# ---- Journey of a Water Drop ----------------------------------------------
W_DROP, W_SUN, W_VAPOUR = spot(0.648, 0.872, 0.13), spot(0.796, 0.125, 0.15), spot(0.796, 0.655, 0.17)
W_CLOUD, W_RAIN, W_MOUNTAIN = spot(0.565, 0.232, 0.18), spot(0.25, 0.345, 0.17), spot(0.255, 0.55, 0.25)
W_RIVER, W_SEA = spot(0.38, 0.72, 0.16), spot(0.40, 0.89, 0.28)

water = {
    "id": "journey-of-a-water-drop", "title": "Journey of a Water Drop", "titleAr": "رحلة قطرة ماء", "image": "asset:seed/water.webp",
    "tracks": [
        {"kind": "story", "lang": "ar", "segments": [
            seg("أنا قَطْرة، قطرةُ ماءٍ صغيرة أعيش في البحر الواسع مع ملايين من أخواتي.", W_DROP),
            seg("في يومٍ حارّ، سخّنتني أشعة الشمس حتى صرت خفيفةً جدًا.", W_SUN),
            seg("فتحوّلتُ إلى بخارٍ لا يُرى، وارتفعت في الهواء عاليًا عاليًا. هذا هو التبخّر.", W_VAPOUR),
            seg("في الأعلى كان الجو باردًا، فتجمّعتُ مع قطراتٍ أخرى حول ذرات الغبار، وصنعنا معًا سحابة. هذا هو التكاثف.", W_CLOUD),
            seg("حملتنا الرياح فوق اليابسة، وكبرنا وثقُلنا حتى لم تعد السحابة تقدر على حملنا، فسقطنا مطرًا.", W_RAIN),
            seg("هبطتُ على قمة جبلٍ عالٍ، وانزلقتُ بين الصخور مع قطراتٍ كثيرة.", W_MOUNTAIN),
            seg("صرنا جدولًا، ثم نهرًا يجري نحو الأسفل، يسقي الأشجار والحقول في طريقه.", W_RIVER),
            seg("وأخيرًا عدتُ إلى البحر، بيتي القديم.", W_SEA),
            seg("لكنّ رحلتي لا تنتهي أبدًا. فغدًا تشرق الشمس، وأبدأ من جديد."),
        ]},
        {"kind": "explanation", "lang": "en", "segments": [
            seg("The water cycle is the continuous movement of water between the oceans, the air and the land. About 97 percent of Earth's water is in the oceans.", W_SEA),
            seg("The Sun provides the energy that drives the whole cycle.", W_SUN),
            seg("Evaporation: heat turns liquid water at the surface into water vapour, an invisible gas that rises with warm air.", W_VAPOUR),
            seg("Condensation: higher up the air is colder, so the vapour turns back into tiny droplets around dust particles. Billions of droplets form a cloud.", W_CLOUD),
            seg("Precipitation: droplets merge and grow. When they become too heavy to stay up, they fall as rain, snow or hail.", W_RAIN),
            seg("Collection: water runs downhill in streams and rivers, and some soaks into the ground as groundwater.", W_RIVER),
            seg("Eventually it returns to the sea and the cycle begins again. No new water is made: the same water is used over and over.", W_DROP),
        ]},
    ],
    "quiz": [
        q("What provides the energy for the water cycle?", ["Wind", "The Sun", "The Moon", "Ocean currents"], 1,
          "Heat from the Sun evaporates water and keeps the whole cycle moving."),
        q("The change from liquid water to water vapour is called…", ["Condensation", "Precipitation", "Evaporation", "Collection"], 2,
          "Evaporation turns liquid water into an invisible gas, water vapour."),
        q("How do clouds form?", ["Water vapour condenses into tiny droplets", "Rain evaporates as it falls", "Smoke rises from the ground", "Snow melts on mountains"], 0,
          "As moist air rises and cools, vapour condenses on dust particles into droplets that make up a cloud."),
        q("Which of these is NOT a form of precipitation?", ["Rain", "Snow", "Hail", "Water vapour"], 3,
          "Precipitation is water falling from clouds as liquid or ice. Water vapour is a gas rising into the air."),
    ],
}

book = {
    "id": "starter", "title": "YouTop Starter", "titleAr": "البداية مع YouTop",
    "description": "Three example lessons with placeholder pictures.",
    "chapters": [{"id": "examples", "title": "Examples", "titleAr": "أمثلة", "lessons": [light, memory, water]}],
}
out = pathlib.Path(__file__).with_name("book.json")
out.write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("wrote", out)
