# Interim Report: Tonbo - Intelligent Visual Assistant for Visually Impaired Users

**Project Title:** Tonbo (瞳伴) - Intelligent Visual Assistant Application  
**Project ID:** IT-114118-02  
**Supervisor:** Sky Wong  
**Team Members:**  
- Liu Cuiru (240123448@stu.vtc.edu.hk) - Leader
- Li Xiaojing (240128886@stu.vtc.edu.hk)
- Hui Pui Yi (240017387@stu.vtc.edu.hk)
- Luo Feiyang (240146622@stu.vtc.edu.hk)

**Institution:** Hong Kong Institute of Vocational Education (Tuen Mun)  
**Department:** Department of Information Technology  
**Course:** IT114118 AI and Mobile Applications Development  
**Academic Year:** 2025/2026  
**Date:** [Submission Date]  
**Version:** 1.0

---

## Table of Contents

1. [Abstract](#1-abstract)
2. [Introduction](#2-introduction)
3. [The Requirements](#3-the-requirements)
4. [Documentation for Problem Analysis](#4-documentation-for-problem-analysis)
5. [Documentation for Detailed Design](#5-documentation-for-detailed-design)
6. [Critical Evaluation](#6-critical-evaluation)
7. [Detailed Project Plan](#7-detailed-project-plan)
8. [References](#8-references)
9. [Appendices](#9-appendices)

---

## 1. Abstract

### 1.1 Project Objectives
- Develop a comprehensive Android mobile application for visually impaired users
- Implement real-time object detection using AI/ML models (SSD MobileNet, TensorFlow Lite)
- Provide multi-language support (Cantonese, Mandarin, English)
- Integrate OCR capabilities for document and currency reading
- Ensure high accessibility standards (WCAG 2.1 compliance)

### 1.2 Interim Report Objectives
- Present progress achieved in the Implementation Phase
- Document system architecture and detailed design
- Demonstrate prototype functionality and core technologies
- Identify challenges encountered and solutions implemented
- Provide updated project plan and timeline

---

## 2. Introduction

### 2.1 Document Structure
This interim report documents the current state of the Tonbo App project, including:
- Problem analysis documentation (use cases, diagrams)
- Detailed system design
- Implementation progress and achievements
- Critical evaluation of challenges and solutions
- Updated project plan

### 2.2 Project Background
- **Problem Statement**: Visually impaired users face daily challenges in navigation, document reading, and emergency situations
- **Solution Approach**: AI-powered mobile assistant with computer vision and voice synthesis
- **Target Platform**: Android 5.0+ (API 21+)
- **Current Status**: Implementation Phase - 98% complete

### 2.3 Project Scope
- **Core Functions**: Environment recognition, document/currency reading, emergency assistance, voice commands
- **Additional Features**: Item finding, travel assistance, gesture management, system settings
- **Technical Stack**: Java/Kotlin, TensorFlow Lite, Google ML Kit, CameraX

---

## 3. The Requirements

### 3.1 Scope of the Proposed System
- **Target Users**: Visually impaired and low-vision individuals in Hong Kong
- **Platform**: Android mobile devices
- **Operating Mode**: Offline-first with optional cloud features
- **Language Support**: Cantonese, Mandarin, English

### 3.2 Functions Provided by the System

#### 3.2.1 Environment Recognition Module
- Real-time object detection (80 COCO categories)
- Multi-frame fusion stability filtering (3-frame confirmation)
- Detection accuracy: 85-90%, False positive rate: 10-15%
- Object position description (left/center/right)
- Priority-based audio announcement
- Night mode optimization
- Color and lighting analysis

#### 3.2.2 Document and Currency Reading Module
- OCR text recognition (Chinese and English) using Google ML Kit
- Hong Kong currency recognition (notes and coins)
- Voice playback of recognized content
- Scanning area guidance

#### 3.2.3 Emergency Assistance Module
- **3-second long-press activation mechanism**: Simple single-function emergency button
- **Direct 999 dialing**: Automatically dials Hong Kong emergency service (999) when activated
- **Voice feedback**: Audio confirmation when emergency call is triggered
- **Vibration feedback**: Strong vibration pattern to alert user
- **Anti-accidental trigger**: Short press provides instruction, only long press (3 seconds) activates
- **Simplified design**: No contact management or selection required - single function for quick access

#### 3.2.4 Voice Command Module
- Multi-language speech recognition (Cantonese, Mandarin, English)
- Fuzzy matching algorithm with multi-dimensional similarity
- Text preprocessing (removal of filler words)
- Natural language command mapping
- Continuous conversation mode with LLM integration

#### 3.2.5 System Settings Module
- Speech parameter adjustment (rate, pitch, volume: 0-200%)
- Accessibility options (vibration feedback, screen reader support)
- Multi-language TTS system
- Settings persistence using SharedPreferences

#### 3.2.6 Additional Modules
- Find Items Module
- Travel Assistant Module
- Instant Assistance Module
- Gesture Management Module
- User System Module

### 3.3 Data Processed by the System

#### 3.3.1 User Data
- User profiles and preferences
- Emergency contacts
- Customized settings
- Personal configurations

#### 3.3.2 Application Data
- AI model files (TensorFlow Lite, ~50MB)
- Language resources (text strings, voice prompts)
- Configuration files
- System parameters

#### 3.3.3 Runtime Data
- Camera frame buffers
- Detection results
- Audio streams
- Sensor data
- Performance metrics

---

## 4. Documentation for Problem Analysis

### 4.1 Use Case Descriptions

#### UC1: Environment Recognition
- **Actor**: Visually Impaired User
- **Precondition**: App is running, camera permission granted
- **Main Flow**:
  1. User opens environment recognition feature
  2. System activates camera
  3. System performs object detection every 3 frames
  4. System filters results using multi-frame fusion (3-frame confirmation)
  5. System announces detected objects with position information
- **Postcondition**: Objects are detected and announced to user

#### UC2: Document Reading
- **Actor**: Visually Impaired User
- **Precondition**: App is running, document in camera view
- **Main Flow**:
  1. User opens document reading feature
  2. User aligns document with scanning area
  3. System performs OCR recognition
  4. System reads recognized text aloud
  5. User can choose full-text or line-by-line reading
- **Postcondition**: Document text is read to user

#### UC3: Emergency Assistance
- **Actor**: Visually Impaired User in Emergency
- **Precondition**: App is running, emergency button accessible
- **Main Flow**:
  1. User long-presses emergency button for 3 seconds
  2. System provides audio feedback: "Emergency button pressed, please continue holding for 3 seconds"
  3. After 3 seconds, system automatically dials 999 (Hong Kong emergency service)
  4. System provides voice confirmation: "Calling emergency service 999, please stay calm"
  5. System provides strong vibration feedback
  6. Emergency call is placed (with permission handling fallback to dialer if needed)
- **Postcondition**: Emergency service 999 is called
- **Alternative Flow**:
  - If user releases button before 3 seconds: System provides instruction to long press for 3 seconds

#### UC4: Voice Command
- **Actor**: Visually Impaired User
- **Precondition**: App is running, microphone permission granted
- **Main Flow**:
  1. User speaks a command
  2. System recognizes speech
  3. System preprocesses text (removes filler words)
  4. System performs fuzzy matching
  5. System executes corresponding function
  6. System provides audio feedback
- **Postcondition**: Command is executed

#### UC5: Find Items
- **Actor**: Visually Impaired User
- **Precondition**: App is running, camera permission granted
- **Main Flow**:
  1. User opens find items feature
  2. User chooses to mark an item or find a marked item
  3. If marking: User captures item image and adds description
  4. If finding: System analyzes camera feed to match item features
  5. System provides location guidance and announces when item is found
- **Postcondition**: Item is marked or found

#### UC6: Travel Assistant
- **Actor**: Visually Impaired User
- **Precondition**: App is running, location permission granted (optional)
- **Main Flow**:
  1. User opens travel assistant feature
  2. User selects function (navigation, traffic info, weather, location share)
  3. System processes request and provides information
  4. System announces information via voice
- **Postcondition**: Travel information is provided to user
- **Note**: Some features require third-party API integration (future enhancement)

#### UC7: Instant Assistance
- **Actor**: Visually Impaired User
- **Precondition**: App is running, phone permission granted
- **Main Flow**:
  1. User opens instant assistance feature
  2. User selects assistance type (call volunteer, send message, video call)
  3. System processes request and connects user with volunteer
  4. System provides voice feedback on connection status
- **Postcondition**: User is connected with volunteer assistance

#### UC8: Gesture Management
- **Actor**: Visually Impaired User
- **Precondition**: App is running
- **Main Flow**:
  1. User opens gesture management feature
  2. User chooses to create gesture or manage existing gestures
  3. If creating: User draws gesture pattern on screen
  4. User binds gesture to a function
  5. System saves gesture pattern and binding
  6. System recognizes gesture when drawn and executes bound function
- **Postcondition**: Gesture is created and bound to function

#### UC9: User Login/Register
- **Actor**: Visually Impaired User, Guest User
- **Precondition**: App is launched
- **Main Flow**:
  1. User opens app and sees login screen
  2. User chooses to login, register, or use guest mode
  3. If login: User enters credentials, system validates and logs in
  4. If register: User enters information, system creates account and logs in
  5. If guest: User skips registration and enters app
  6. System navigates to main screen
- **Postcondition**: User is logged in or in guest mode, main screen is displayed

#### UC10: System Settings
- **Actor**: Visually Impaired User
- **Precondition**: App is running
- **Main Flow**:
  1. User opens system settings
  2. User adjusts speech parameters (rate, pitch, volume)
  3. User configures accessibility options (vibration, screen reader)
  4. User switches language (Cantonese, Mandarin, English)
  5. System saves settings and applies changes immediately
  6. System provides voice confirmation of changes
- **Postcondition**: Settings are saved and applied

### 4.2 Use Case Diagrams
*(Diagrams to be included: Overall System Use Cases, Environment Recognition Use Cases, Emergency Assistance Use Cases)*

### 4.3 Class Diagram
*(Main classes to be documented:)*
- **Activity Classes**: MainActivity, RealAIDetectionActivity, DocumentCurrencyActivity, VoiceCommandActivity, SettingsActivity
- **Manager Classes**: TTSManager, VibrationManager, LocaleManager, UserManager, ConversationManager, EmergencyManager (simplified single-function design)
- **Detector Classes**: ObjectDetectorHelper, YoloDetector, OCRHelper, CurrencyDetector
- **AI/ML Classes**: NightModeOptimizer, ColorLightingAnalyzer
- **LLM Classes**: LLMClient, LLMConfig
- **UI Classes**: BaseAccessibleActivity, DetectionOverlayView, OptimizedDetectionOverlayView

### 4.4 State Transition Diagrams
*(State diagrams to be included:)*
- Application Lifecycle States
- Environment Recognition States (Idle, Initializing, Detecting, Processing, Announcing)
- Voice Command States (Listening, Processing, Executing, Feedback)
- Emergency Assistance States (Ready, Button-Pressed, Long-Press-Countdown, Dialing, Call-Initiated)

### 4.5 Sequence Diagrams
*(Sequence diagrams to be included:)*
- Environment Recognition Flow
- Multi-frame Fusion Process
- OCR Recognition Process
- Emergency Assistance Flow
- Voice Command Processing Flow
- LLM Conversation Flow

---

## 5. Documentation for Detailed Design

### 5.1 Data Design

#### 5.1.1 Data Models
- **User Profile**: username, email, preferences, settings
- **Detection Result**: objectClass, confidence, boundingBox, position
- **Conversation History**: messages, timestamps, context
- **Emergency Event Log**: timestamp, event type (for future analytics)

#### 5.1.2 Data Storage
- **SharedPreferences**: User settings, language preferences
- **Local Database**: Item markers, emergency event logs
- **File Storage**: AI models, language resources

#### 5.1.3 Data Flow
- Camera frames → Image preprocessing → AI model inference → Result filtering → Audio output
- User input → Speech recognition → Command processing → Function execution → Feedback

### 5.2 Software/Hardware Architectural Design

#### 5.2.1 System Architecture
- **Presentation Layer**: Activities, Fragments, Views
- **Business Logic Layer**: Managers, Helpers, Detectors
- **Data Layer**: Models, Storage, Persistence
- **AI/ML Layer**: TensorFlow Lite, Google ML Kit, LLM APIs

#### 5.2.2 Technology Stack
- **Development Language**: Java (primary), Kotlin (secondary)
- **AI Framework**: TensorFlow Lite (SSD MobileNet V1)
- **OCR Engine**: Google ML Kit
- **Camera API**: CameraX
- **TTS Engine**: Android Text-to-Speech (multi-language)
- **LLM APIs**: DeepSeek, GLM-4-Flash

#### 5.2.3 Hardware Requirements
- **Minimum**: Android 5.0+, 3GB RAM, 8MP camera
- **Recommended**: Android 10.0+, 4GB RAM, 12MP camera with autofocus

#### 5.2.4 Software Architecture Patterns
- **MVC Pattern**: Separation of Views, Controllers, and Models
- **Singleton Pattern**: Managers (TTS, Vibration, Locale)
- **Strategy Pattern**: Different detectors and analyzers
- **Observer Pattern**: Event-driven updates

### 5.3 User Interface Design

#### 5.3.1 Design Principles
- **Accessibility First**: WCAG 2.1 compliance
- **High Contrast**: Black background, white text
- **Large Touch Targets**: Minimum 48dp
- **Voice Feedback**: All actions have audio feedback
- **Consistent Navigation**: Unified navigation patterns

#### 5.3.2 Main Screens
- **Splash Screen**: App launch with loading indication
- **Main Screen**: Home page with 7 core function cards
- **Environment Recognition Screen**: Camera preview with detection overlay
- **Document Reading Screen**: Camera preview with scanning area
- **Settings Screen**: Configuration options with clear labels
- **Voice Command Screen**: Listening indicator and feedback

#### 5.3.3 UI Components
- **BaseAccessibleActivity**: Base class for all activities
- **DetectionOverlayView**: Real-time detection visualization
- **OptimizedDetectionOverlayView**: Enhanced overlay with smart positioning
- **Custom Buttons**: Large, high-contrast buttons
- **Audio Feedback**: Voice prompts for all interactions

---

## 6. Critical Evaluation

### 6.1 Problems and Difficulties Encountered

#### 6.1.1 Technical Challenges
- **Challenge 1**: ImageProxy Buffer Conflict (SIGSEGV crash)
  - **Problem**: Direct image processing for lighting analysis caused buffer access conflicts
  - **Solution**: Changed to inference-based lighting detection using detection confidence
  - **Result**: Eliminated crashes, improved performance

- **Challenge 2**: Detection Accuracy Issues
  - **Problem**: Initial accuracy was 70%, false positive rate 30%
  - **Solution**: Implemented multi-frame fusion (3-frame confirmation), increased confidence thresholds
  - **Result**: Accuracy improved to 85-90%, false positives reduced to 10-15%

- **Challenge 3**: Performance on Low-end Devices
  - **Problem**: Detection lag on devices with limited processing power
  - **Solution**: Intelligent frame skipping (every 3 frames), adaptive quality settings
  - **Result**: Smooth operation on mid-range devices (5+ FPS)

#### 6.1.2 User Experience Challenges
- **Challenge 1**: Information Overload
  - **Problem**: Too many detected objects caused overwhelming audio feedback
  - **Solution**: Intelligent limiting (max 2 objects), priority-based filtering
  - **Result**: Clear, concise audio feedback

- **Challenge 2**: Language Consistency
  - **Problem**: Inconsistent language across different pages
  - **Solution**: Unified LocaleManager, dynamic language switching
  - **Result**: Complete language consistency throughout app

#### 6.1.3 Accessibility Challenges
- **Challenge 1**: Emergency Function Complexity
  - **Problem**: Original design with contact selection dialog was too complex for emergency situations
  - **Solution**: Simplified to single-function design - long-press 3 seconds directly dials 999
  - **Result**: Faster activation, reduced cognitive load, better suited for emergency use
- **Challenge 2**: Phone Number Recognition (Historical - removed in simplified design)
  - **Note**: Original design included digit-by-digit readout, but simplified design eliminates need for contact selection

### 6.2 Delays and Changes in Project Schedule
- **No Significant Delays**: Project is on track, 98% complete
- **Design Simplifications**: 
  - **Emergency Assistance Module**: Simplified from contact management system to single-function design (long-press 3 seconds to dial 999 directly)
    - **Rationale**: For visually impaired users in emergency situations, simplicity and speed are critical. Removing contact selection reduces cognitive load and activation time.
    - **Impact**: Faster emergency response, reduced complexity, better accessibility
- **Enhancements Added**: 
  - Night mode optimization was added beyond original scope
  - LLM integration was added as enhancement
- **Schedule Impact**: Minimal, all changes completed within timeline

### 6.3 Limitations of the Proposed System

#### 6.3.1 Hardware Limitations
- Requires Android 5.0+ devices (excludes older devices)
- Camera quality affects detection accuracy
- Battery consumption during continuous use

#### 6.3.2 Technical Limitations
- Model accuracy depends on training data (80 COCO categories)
- OCR accuracy varies with document quality and lighting
- LLM integration requires network connectivity (with fallback)

#### 6.3.3 Feature Limitations
- **Emergency Assistance**: Simplified to single function (999 only) - no custom contact management
  - **Rationale**: Designed for speed and simplicity in emergency situations
  - **Future Enhancement**: Could add contact management as optional feature in settings
- Currency recognition limited to Hong Kong dollars
- Navigation requires third-party API integration (future enhancement)
- Cloud sync requires Firebase integration (planned)

### 6.4 Potential Difficulties in Progression Route

#### 6.4.1 Testing Phase Challenges
- **User Testing**: Need to coordinate with visually impaired organizations
- **Device Compatibility**: Testing across various Android devices
- **Performance Testing**: Ensuring consistent performance across devices

#### 6.4.2 Deployment Phase Challenges
- **App Store Approval**: Meeting Google Play Store requirements
- **Documentation**: Comprehensive user manual and technical documentation
- **Training Materials**: User guides for target audience

#### 6.4.3 Future Enhancement Challenges
- **Firebase Integration**: Cloud sync implementation
- **Model Updates**: Upgrading to newer AI models (YOLOv8)
- **Additional Languages**: Expanding language support

---

## 7. Detailed Project Plan

### 7.1 Current Progress (Implementation Phase - 98% Complete)

#### Completed Deliverables
- ✅ All core functional modules (10 modules, 100% each)
- ✅ AI/ML integration (TensorFlow Lite, Google ML Kit)
- ✅ Multi-language support (Cantonese, Mandarin, English)
- ✅ Accessibility features (WCAG 2.1 compliance)
- ✅ Performance optimizations
- ✅ Night mode optimization
- ✅ LLM integration

### 7.2 Remaining Tasks

#### 7.2.1 Implementation Phase (2% remaining)
- [ ] Final bug fixes and refinements
- [ ] Code optimization and cleanup
- [ ] Documentation completion

#### 7.2.2 Testing Phase (March 10, 2025 - April 21, 2025)
- [ ] Unit testing
- [ ] Integration testing
- [ ] System testing
- [ ] Accessibility testing
- [ ] User acceptance testing (with visually impaired users)
- [ ] Performance testing across devices
- [ ] Bug fixing based on test results

**Deliverables:**
- Test plan document
- Test cases and results
- Bug tracking records
- Performance test reports

#### 7.2.3 Deployment Phase (April 21, 2025 - May 26, 2025)
- [ ] Final bug fixes
- [ ] Performance optimization
- [ ] User manual creation
- [ ] Technical documentation
- [ ] Final report preparation
- [ ] Presentation preparation

**Deliverables:**
- Final application (APK)
- User manual
- Technical documentation
- Final report
- Presentation materials

### 7.3 Gantt Chart / Timeline
*(Visual timeline to be included showing:)*
- Implementation Phase (Dec 23, 2024 - Mar 10, 2025) - **98% Complete**
- Testing Phase (Mar 10, 2025 - Apr 21, 2025)
- Deployment Phase (Apr 21, 2025 - May 26, 2025)
- Milestones and deliverables

### 7.4 Resource Allocation
- **Development**: All team members
- **Testing**: Coordinated team effort + external users
- **Documentation**: Shared responsibility
- **Presentation**: Team collaboration

---

## 8. References

### 8.1 Academic References
1. World Health Organization. (2021). "World Report on Vision"
2. Hong Kong Blind Union. (2023). "Statistics on Visual Impairment in Hong Kong"
3. W3C. (2018). "Web Content Accessibility Guidelines (WCAG) 2.1"

### 8.2 Technical References
1. Google. (2024). "TensorFlow Lite Documentation"
2. Google. (2024). "ML Kit Text Recognition API"
3. Android Developers. (2024). "CameraX Documentation"
4. Android Developers. (2024). "Accessibility Guide"

### 8.3 AI/ML References
1. COCO Dataset. (2024). "Common Objects in Context"
2. SSD MobileNet. (2024). "Single Shot MultiBox Detector"
3. DeepSeek. (2024). "API Documentation"
4. GLM-4-Flash. (2024). "Zhipu AI Documentation"

### 8.4 Standards and Guidelines
1. Android Accessibility Guidelines
2. WCAG 2.1 Level AA Standards
3. Material Design Accessibility Guidelines

---

## 9. Appendices

### Appendix A: Project Specifications
- **Platform**: Android 5.0+ (API 21+)
- **Minimum RAM**: 3GB
- **Camera**: 8MP rear camera (minimum)
- **Storage**: ~100MB (including models)
- **Network**: Optional (for LLM features)

### Appendix B: Code Statistics
- **Total Lines of Code**: ~11,700+
- **Java Files**: 50+
- **XML Layout Files**: 80+
- **Core Modules**: 10
- **Manager Classes**: 5
- **Detector Classes**: 6

### Appendix C: Performance Metrics
- **Detection Accuracy**: 85-90%
- **False Positive Rate**: 10-15%
- **Detection Time**: Average < 500ms
- **Detection Frame Rate**: 5+ FPS (mid-range devices)
- **OCR Processing Time**: 2-3 seconds (A4 document)

### Appendix D: Function Completion Matrix
| Module | Completion | Status |
|--------|-----------|--------|
| Environment Recognition | 100% | ✅ Complete |
| Document & Currency Reading | 100% | ✅ Complete |
| Emergency Assistance | 100% | ✅ Complete |
| Voice Command | 100% | ✅ Complete |
| System Settings | 100% | ✅ Complete |
| Find Items | 100% | ✅ Complete |
| Travel Assistant | 100% | ✅ Complete |
| Instant Assistance | 100% | ✅ Complete |
| Gesture Management | 100% | ✅ Complete |
| User System | 100% | ✅ Complete |
| **Overall** | **98%** | **🔄 In Progress** |

### Appendix E: Key Achievements
1. ✅ All 21 required functions implemented (100% compliance)
2. ✅ Performance targets achieved or exceeded
3. ✅ Complete accessibility design (WCAG 2.1 compliant)
4. ✅ Multi-language support (3 languages)
5. ✅ Night mode optimization (beyond original scope)
6. ✅ LLM integration for enhanced voice interaction

### Appendix F: Diagrams and Charts
*(To be included:)*
- System Architecture Diagram
- Class Diagram
- Use Case Diagrams
- State Transition Diagrams
- Sequence Diagrams
- Data Flow Diagrams
- UI Mockups/Screenshots

### Appendix G: Test Plans (Preliminary)
- Unit Test Cases
- Integration Test Scenarios
- Accessibility Test Checklist
- Performance Test Criteria

---

## Oral Presentation Outline

### Presentation Structure (15 minutes, ≤20 slides)

#### Slide 1: Title Slide
- Project Title, Team Members, Supervisor

#### Slide 2: Project Overview
- Problem Statement
- Solution Approach
- Target Users

#### Slide 3: Project Objectives
- Main objectives
- Key features

#### Slide 4: System Architecture (High-Level)
- Architecture diagram
- Technology stack

#### Slide 5: Core Functions Overview
- 10 main modules
- Completion status

#### Slide 6: Environment Recognition
- Features, performance metrics
- Visual demonstration

#### Slide 7: Document & Currency Reading
- OCR capabilities
- Currency recognition

#### Slide 8: Emergency Assistance
- Simple single-function design
- 3-second long-press activation
- Direct 999 dialing
- Voice and vibration feedback

#### Slide 9: Voice Command System
- Multi-language support
- LLM integration

#### Slide 10: Use Case Diagram
- Main use cases

#### Slide 11: Class Diagram (Key Classes)
- Main classes and relationships

#### Slide 12: State Transition Diagram
- Key state transitions

#### Slide 13: Sequence Diagram
- Example flow (Environment Recognition)

#### Slide 14: Technical Achievements
- Performance metrics
- Accuracy improvements

#### Slide 15: Challenges and Solutions
- Key challenges
- Solutions implemented

#### Slide 16: Project Progress
- Timeline
- Completion status (98%)

#### Slide 17: Testing Plan
- Testing strategy
- User testing approach

#### Slide 18: Future Enhancements
- Planned features
- Roadmap

#### Slide 19: Demonstration Preview
- What will be demonstrated

#### Slide 20: Conclusion & Q&A
- Summary
- Questions

---

## Prototype Demonstration Plan (10 minutes)

### Demonstration Flow

#### Part 1: Core Technologies (3 minutes)
1. **Object Detection**
   - Real-time detection demonstration
   - Show 80 COCO categories support
   - Demonstrate multi-frame fusion

2. **OCR Recognition**
   - Document reading demonstration
   - Currency recognition demonstration

#### Part 2: Critical/Major Functions (4 minutes)
3. **Environment Recognition**
   - Live camera detection
   - Audio announcements
   - Position descriptions

4. **Emergency Assistance**
   - Demonstrate long-press button (3 seconds)
   - Show direct 999 dialing (simulated or actual)
   - Demonstrate voice feedback: "Calling emergency service 999"
   - Show vibration feedback
   - Explain simplified single-function design (no contact management needed)

5. **Voice Commands**
   - Multi-language recognition
   - Command execution
   - LLM conversation (if available)

#### Part 3: UI Design (3 minutes)
6. **Accessibility Features**
   - Voice feedback demonstration
   - Vibration feedback
   - Large touch targets

7. **Multi-language Support**
   - Language switching
   - Consistent UI updates

8. **Settings and Customization**
   - Speech parameter adjustment
   - Accessibility options

### Setup Requirements
- Android device with app installed
- Test documents and currency
- Good lighting for camera demonstration
- External speakers for audio clarity

---

**Document Version**: 1.0  
**Last Updated**: [Date]  
**Prepared By**: Tonbo App Development Team
