# Oral Presentation & Prototype Demo Guide  
# 口頭報告與原型示範指南

**Tonbo (瞳伴)** — Assistive app for visually impaired users  
**Slot**: 20 minutes total | **Presentation**: max 12 minutes, max 20 slides | **Demo**: 3 minutes per student  
**Language**: English | **Dress**: Formal  

**Grading** (equal weight): Material selection · Preparation · Delivery / Handling questions  

---

## Part 1: Presentation Outline (≤20 slides, ≤12 minutes)

Use this as the script for your slides. Aim for **~30–40 seconds per slide** to stay within 12 minutes.

---

### Slide 1 — Title
- **Tonbo (瞳伴): A Comprehensive Assistive Platform for the Visually Impaired**
- Team of 4 | Android application
- Subtitle (optional): *Making technology a visual companion for blind and low-vision users*

---

### Slide 2 — Agenda
- Project objectives & problem background  
- Main features & high-level design  
- System architecture & key diagrams  
- Project schedule & progress  
- Summary & demo preview  

---

### Slide 3 — Project objectives
- **Primary goal**: Build an **all-in-one assistive app** that helps visually impaired users live more independently.
- **Scope**: Environment awareness, document/currency reading, **travel assistance (navigation + walkable path)**, emergency help, voice control, instant volunteer assistance.
- **Success criteria**: Voice-first, minimal touch; works in Cantonese, English, Mandarin; usable without sight (TTS, vibration, large buttons).

---

### Slide 4 — Problem background (1/2)
- **Mobility**: Hard to recognise road signs, traffic lights, zebra crossings → risk of getting lost or colliding.
- **Environment**: Hard to know what is around (people, vehicles, obstacles, light level).
- **Information**: Cannot read documents, receipts, or banknotes → reliance on others for daily tasks.

---

### Slide 5 — Problem background (2/2)
- **Emergency**: Difficult to trigger one-tap help, share location, or contact family/999 quickly.
- **Usability**: Most apps rely on visual UI → blind users cannot operate them independently.
- **Our response**: One app that addresses **seeing (CV), reading (OCR), going (navigation), and asking (voice + LLM)**.

---

### Slide 6 — Solution overview: What Tonbo provides
- **Environment recognition**: Real-time object detection (80 COCO classes) + TTS (e.g. “Person on the left”, “Car ahead”) + night mode.
- **Document & currency**: OCR + Hong Kong currency recognition + read-aloud.
- **Travel assistance**: Voice destination → walking route (Amap) → step-by-step TTS navigation + **walkable path analysis** (obstacles, crosswalk, traffic light).
- **Emergency**: Long-press 3 s → choose contacts → send SMS + location + call (e.g. 999).
- **Voice & AI**: Voice commands + continuous dialogue + LLM (DeepSeek/GLM-4-Flash) with fallback.

---

### Slide 7 — High-level: Use case (main actors)
- **Actor**: Visually impaired user.  
- **Use cases (examples)**:  
  - Recognise environment (objects, position, light).  
  - Read document / recognise currency.  
  - Plan route and navigate by voice.  
  - Get walkable-path hints (clear / move left or right / crosswalk / traffic light).  
  - Trigger emergency (SMS + location + call).  
  - Control app by voice (e.g. “Open environment”, “Go to X”).  
  - Contact volunteer (message / video call).  
- **Optional**: Simple use case diagram (User — use cases above).

---

### Slide 8 — High-level: System architecture (block diagram)
- **Presentation layer**: MainActivity, TravelAssistantActivity, NavigationActivity, Settings, etc.; BaseAccessibleActivity (TTS, vibration, language).
- **Business / control layer**: NavigationController, EmergencyManager, VoiceCommandManager, TravelDetectionController.
- **Capability layer**: ObjectDetectorHelper, YoloDetector, TTSManager, RoutePlanner, WalkAssistManager, LLMClient.
- **Data**: SharedPreferences, UserManager, local storage.  
- **One diagram**: 3–4 horizontal layers with 2–3 components per layer (no code).

---

### Slide 9 — Key design: Travel flow (simplified)
- User says “I want to go to [place]” or types destination.  
- **RoutePlanner** (Amap Web API: geocode + walking) → steps + polyline.  
- **NavigationController**: GPS, step progress, off-route detection → re-plan.  
- **NavigationActivity**: Map (Amap SDK), TTS (“Walk 50 m”, “Turn right”), ~20 m turn reminder.  
- **WalkAssistManager**: Camera + YOLO → left/centre/right occupancy → TTS (“Path clear”, “Move left”, “Crosswalk ahead”).

---

### Slide 10 — Key design: Environment & voice
- **Environment**: CameraX → SSD MobileNet / YOLO → multi-frame stability (3 frames) → priority (person/vehicle first) → TTS + overlay. Night mode: auto when low light inferred from confidence.
- **Voice**: SpeechRecognizer (Cantonese/English/Mandarin) → fuzzy match + “I want to go to X” parser → command or travel destination → execute or LLM → TTS.

---

### Slide 11 — Class diagram (simplified, 5–7 classes)
- **Suggested boxes**:  
  - `TravelAssistantActivity` → `StartTravelActivity`, `NavigationActivity`.  
  - `NavigationController` → `RoutePlanner`, `LocationService`.  
  - `NavigationActivity` → `WalkAssistManager`, `TravelDetectionController`.  
  - `VoiceCommandManager` → `TravelParseResult`, `ExtendedVoiceCommandListener`.  
- One slide: only names and “uses”/“calls” arrows; no attributes.

---

### Slide 12 — Technology stack (short)
- **Platform**: Android (minSdk 21), Java 11.  
- **Vision**: CameraX, TensorFlow Lite (SSD + YOLO), Google ML Kit (OCR).  
- **Maps**: Amap Web (geocode, walking), Amap Android 3D SDK.  
- **Voice**: Android TTS, SpeechRecognizer.  
- **Network / AI**: OkHttp, Gson; DeepSeek/GLM-4-Flash (LLM); Agora RTC (video).  

---

### Slide 13 — Project schedule & progress (example)
- **Phase 1**: Requirements, architecture, UI skeleton.  
- **Phase 2**: Environment recognition, OCR, emergency, settings.  
- **Phase 3**: Voice commands, travel assistance (route + navigation + walkable path).  
- **Phase 4**: LLM integration, instant assistance, testing & refinement.  
- **Current**: Core features implemented; focus on stability and demo readiness.  
- *Replace with your real milestones and a simple Gantt or timeline if available.*

---

### Slide 14 — Innovations & highlights (1/2)
- **End-to-end “hearable” travel**: Voice destination → Amap walking route → step-by-step TTS + turn reminder + off-route re-plan + **walkable path analysis** (obstacles, crosswalk, traffic light) so users can walk without looking at a map.
- **Multi-frame fusion + priority**: Environment only announced after 3 consecutive detections; person/vehicle first; left/centre/right position → less false alarm, less overload.

---

### Slide 15 — Innovations & highlights (2/2)
- **Night mode**: Auto low-light detection (from confidence), auto threshold and TTS “Night mode on”.  
- **Voice–travel integration**: “I want to go to [X]” directly fills destination and starts navigation.  
- **Accessibility throughout**: Large buttons, high contrast, vibration, digit-by-digit phone read-out, three languages, TalkBack-friendly.

---

### Slide 16 — User flow (one slide)
- **Environment**: Home → Environment recognition → point camera → hear objects & light.  
- **Travel**: Home → Travel assistance → Start travel → say/type destination → confirm → Navigation (route TTS + walkable-path hints).  
- **Emergency**: Home → long-press emergency 3 s → select contacts → SMS + location + call.  
- **Voice**: Home → Voice assistant → single command or continuous dialogue (e.g. “Go to X”).

---

### Slide 17 — Demo preview (what you will show)
- **Person 1**: Home → Environment recognition → object + position + night mode (if time).  
- **Person 2**: Home → Travel assistance → “I want to go to [place]” → navigation + walkable-path TTS.  
- **Person 3**: Home → Document/currency reading OR emergency (long-press, contact, SMS/location).  
- **Person 4**: Home → Voice assistant (commands + “Go to X”) OR gesture shortcut.  
- *Adjust to your actual 3-minute-per-person plan.*

---

### Slide 18 — Challenges & mitigations (optional)
- **Challenge**: Accuracy vs. latency in object detection. **Mitigation**: Multi-frame fusion, limit to 2 objects, tune confidence.  
- **Challenge**: Off-route in navigation. **Mitigation**: Distance threshold + re-plan from current location.  
- **Challenge**: LLM unavailable. **Mitigation**: Fallback to keyword matching for commands.

---

### Slide 19 — Summary
- Tonbo = **one platform**: see (CV), read (OCR), go (navigation + walkable path), ask (voice + LLM), emergency, volunteer.  
- **Design**: Voice-first, accessibility-first, three languages.  
- **Impact**: Independence, safety, and information access for visually impaired users.

---

### Slide 20 — Thank you & Q&A
- **Thank you.**  
- **Next**: Short prototype demo (3 minutes per student).  
- **We are happy to take questions.**

---

## Part 2: Prototype Demo (3 minutes per student)

**Rules**: Demo after the presentation; 3 minutes per student; **set up and test the system before the session** (device, network, accounts, Amap/API keys if needed).

### Before the session (checklist)
- [ ] Device charged; screen/volume suitable for venue.  
- [ ] App installed (debug/release); permissions granted (camera, mic, location, SMS/phone if demoing emergency).  
- [ ] Language set (e.g. English for the audience).  
- [ ] Amap Web key valid (for travel demo).  
- [ ] Test: Environment recognition, travel (say destination → navigation + walkable path), OCR or emergency, voice command (“Open environment”, “Go to [place]”).  
- [ ] Decide who demos what so there is no overlap or long switching.

### Suggested split (4 people × ~3 min)
- **Student 1**: Home → Environment recognition → show object + position TTS (and night mode if easy to show).  
- **Student 2**: Home → Travel assistance → Start travel → voice “I want to go to [prepared place]” → navigation page → play one or two TTS instructions + one walkable-path hint.  
- **Student 3**: Home → Document/currency (scan text or note) OR Emergency (long-press, select contact, show SMS/location/call flow briefly).  
- **Student 4**: Home → Voice assistant → “Open environment recognition” or “Go to [place]” → show it opens the right screen; optionally one gesture shortcut.

### During the demo
- Speak in **English**, briefly say what you are doing (“I’m opening Travel assistance… Now I’ll say the destination…”).  
- Keep to **3 minutes**; skip optional steps if short on time.  
- If something fails: state it briefly and explain the intended behaviour (e.g. “Normally it would speak the next turn”).

---

## Part 3: Preparation & Q&A

### Material selection (for grading)
- **Slides**: Clear titles; minimal text; one main idea per slide; diagrams (architecture, use case, class) readable from the back.  
- **Diagrams**: System architecture, main use cases, simplified class diagram for travel/voice; optional: user flow or sequence for one scenario.  
- **Evidence**: One or two slides on schedule/progress; innovations and challenges in one place.

### Preparation
- Rehearse with a timer (aim for 10–11 minutes so you have buffer).  
- Assign slides to speakers if more than one person presents.  
- Prepare a **short backup** (e.g. video or screenshots) if live demo is risky.  
- Bring adapters/cables for the venue’s display.  
- **Formal dress** as required.

### Delivery
- Speak clearly in **English**; avoid reading slides word-for-word.  
- Point to the diagram when explaining architecture or flow.  
- Transition clearly: “Now I’ll show…” before demo.

### Handling questions (examples)
- **“Why Amap and not Google Maps?”** → Explain region (e.g. Hong Kong / China), walking API, and availability.  
- **“How do you handle low light?”** → Night mode: confidence-based detection, lower threshold, TTS announcement.  
- **“What if the user goes off route?”** → NavigationController checks distance; re-plans from current location and announces.  
- **“How accurate is object detection?”** → Multi-frame fusion, 85–90% in testing; we limit to 2 objects and prioritise person/vehicle.  
- **“Can it work offline?”** → Environment and navigation TTS work offline once route is fetched; route planning needs network; LLM needs network with keyword fallback.

---

## Quick reference

| Item | Requirement |
|------|-------------|
| Total slot | 20 minutes |
| Presentation | ≤12 minutes, ≤20 slides |
| Demo | 3 minutes per student |
| Language | English |
| Dress | Formal |
| Grading | Material selection, Preparation, Delivery / Q&A (equal weight) |

---

*Use this guide together with your existing Chinese project summary and PPT outline. Replace placeholders (e.g. schedule, demo split) with your actual plan.*
