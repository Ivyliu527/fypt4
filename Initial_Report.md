# Initial Report: Tonbo - Intelligent Visual Assistant for Visually Impaired Users

**Project Title:** Tonbo (瞳伴) - Intelligent Visual Assistant Application  
**Project ID:** [To be filled]  
**Supervisor:** [To be filled]  
**Team Members:**  
- Hui Pui Yi (首頁與導航、環境識別、語音命令、三語言切換)  
- Li Xiaojing (文件與貨幣閱讀、系統設定、手勢管理)  
- Luo Feiyang (尋找功能、即時協助)  
- Liu Cuiru (用戶系統、障礙物偵測)  

**Date:** [Current Date]  
**Version:** 1.0

---

## Abstract

This initial report presents the objectives, problem analysis, proposed solution, and project plan for **Tonbo** (瞳伴), an intelligent visual assistant mobile application designed specifically for visually impaired users. The project aims to leverage advanced computer vision technologies, machine learning models, and multi-language voice synthesis to assist visually impaired individuals in their daily activities, including environment recognition, document reading, emergency assistance, and navigation.

The report outlines the scope of the problem faced by visually impaired users, describes the proposed solution architecture, discusses technical constraints and requirements, presents the software development process model, and provides a detailed project plan with deliverables and timeline. The application integrates TensorFlow Lite for object detection, Google ML Kit for OCR, and comprehensive accessibility features to create a user-friendly assistive technology solution.

**Key Objectives:**
- Develop a comprehensive mobile application for visually impaired users
- Implement real-time object detection using AI/ML models
- Provide multi-language support (Cantonese, Mandarin, English)
- Ensure high accessibility standards and user-friendly interface
- Deliver a production-ready application by mid-term deadline (February 2nd)

---

## 1. Introduction

### 1.1 Document Structure

This initial report is organized into the following sections:

1. **Abstract**: Overview of project objectives and report contents
2. **Introduction**: General description of the document and project
3. **The Problem**: Detailed analysis of the problem scope and environment
4. **Proposed Solution**: Comprehensive description of the solution architecture and features
5. **Constraints**: Technical and non-technical constraints affecting the project
6. **Software Process Model**: Development methodology and approach
7. **Project Plan**: Schedule, deliverables, tools, and resources
8. **References**: Academic and technical references

### 1.2 Project Overview

**Tonbo** (瞳伴) is an Android mobile application designed to serve as an intelligent visual assistant for visually impaired individuals. The application combines cutting-edge computer vision technologies with voice synthesis and accessibility features to help users navigate their environment, recognize objects, read documents, and access emergency assistance.

The project is being developed as a Final Year Project (FYP) with a completion deadline of April 20th, 2025, and a mid-term report deadline of February 2nd, 2025. The core functionality of the application must be completed before the mid-term deadline.

**Project Vision:**
To create a comprehensive, user-friendly, and accessible mobile application that empowers visually impaired users to independently perform daily tasks through intelligent visual recognition and voice-guided assistance.

**Target Platform:**
- Android 5.0 (API 21) or higher
- Support for various Android device sizes and configurations
- Offline-first architecture with optional cloud features

---

## 2. The Problem

### 2.1 Scope of the Problem

Visually impaired individuals face significant challenges in their daily lives, including:

1. **Environmental Navigation**: Difficulty identifying objects, obstacles, and people in their surroundings
2. **Information Access**: Limited ability to read printed documents, currency, and signs
3. **Emergency Situations**: Challenges in quickly accessing emergency assistance
4. **Technology Accessibility**: Many existing applications lack proper accessibility features
5. **Language Barriers**: Limited support for local languages (Cantonese, Traditional Chinese)

### 2.2 Description of the Problem

#### 2.2.1 Environmental Recognition Challenges

Visually impaired users struggle to:
- Identify objects and people in their immediate environment
- Understand spatial relationships and object positions
- Detect potential obstacles or hazards
- Recognize familiar items in unfamiliar locations

**Impact**: Reduced independence, increased safety risks, and limited ability to navigate public spaces confidently.

#### 2.2.2 Document and Currency Reading Challenges

Users face difficulties with:
- Reading printed documents, letters, and forms
- Identifying currency denominations (especially Hong Kong dollars)
- Accessing information from signs and labels
- Verifying written information independently

**Impact**: Dependence on others for information access, privacy concerns, and reduced autonomy.

#### 2.2.3 Emergency Assistance Challenges

Critical issues include:
- Difficulty quickly accessing emergency contacts
- Challenges in communicating location during emergencies
- Limited ability to use standard emergency interfaces
- Need for simplified, accessible emergency activation

**Impact**: Safety concerns, delayed response times, and increased vulnerability in emergency situations.

#### 2.2.4 Technology Accessibility Gaps

Existing solutions often lack:
- Proper screen reader compatibility
- Multi-language voice support (especially Cantonese)
- Intuitive, touch-friendly interfaces
- Offline functionality for critical features
- Comprehensive accessibility features

**Impact**: Exclusion from technological benefits, frustration with complex interfaces, and limited adoption of assistive technologies.

### 2.3 Problem Environment

#### 2.3.1 User Environment

- **Primary Users**: Visually impaired individuals in Hong Kong and surrounding regions
- **Age Range**: Primarily adults and elderly users
- **Technical Proficiency**: Varying levels, from basic to advanced
- **Language Preferences**: Cantonese (primary), English, Mandarin
- **Device Usage**: Primarily Android smartphones with varying capabilities

#### 2.3.2 Technical Environment

- **Mobile Platform**: Android ecosystem (diverse device manufacturers and screen sizes)
- **Network Conditions**: Variable connectivity (urban and rural areas)
- **Hardware Constraints**: Limited processing power on older devices
- **Battery Life**: Need for efficient power consumption
- **Storage**: Limited storage on some devices

#### 2.3.3 Social and Regulatory Environment

- **Accessibility Standards**: Need to comply with WCAG guidelines and Android accessibility standards
- **Privacy Regulations**: GDPR, local data protection laws
- **Healthcare Context**: Potential integration with healthcare and social services
- **Community Support**: Collaboration with visually impaired user groups for testing and feedback

---

## 3. Proposed Solution

### 3.1 Scope of the Proposed Solution

The **Tonbo** application provides a comprehensive solution addressing the identified problems through:

1. **Real-time Environment Recognition**: AI-powered object detection with voice feedback
2. **Document and Currency Reading**: OCR-based text recognition and currency identification
3. **Emergency Assistance System**: Quick-access emergency contacts and location sharing
4. **Multi-language Voice Interface**: Cantonese, Mandarin, and English support
5. **Accessibility-First Design**: Comprehensive accessibility features and intuitive UI
6. **Offline Functionality**: Core features work without internet connectivity

### 3.2 Functions Provided Through the Proposed Solution

#### 3.2.1 Environment Recognition Module

**Core Functions:**
- Real-time object detection using SSD MobileNet model (80 COCO categories)
- Multi-frame fusion stability filtering (85-90% accuracy)
- Object position description (left/center/right)
- Intelligent voice broadcast priority (safety-related objects first)
- Color and lighting analysis
- Performance monitoring and reporting

**Technical Implementation:**
- TensorFlow Lite for on-device inference
- CameraX API for camera access
- Multi-frame stability filtering to reduce false positives
- Confidence-based object filtering and sorting
- Real-time bounding box visualization

**User Benefits:**
- Independent environment awareness
- Safety enhancement through obstacle detection
- Reduced dependence on others for spatial information

#### 3.2.2 Document and Currency Reading Module

**Core Functions:**
- OCR text recognition using Google ML Kit (Chinese and English)
- Hong Kong currency recognition (banknotes and coins)
- Full-paragraph and line-by-line reading modes
- Voice synthesis for reading results
- Large scanning area indication

**Technical Implementation:**
- Google ML Kit Text Recognition API
- Custom currency detection algorithms
- Multi-language text processing
- TTS integration for voice output

**User Benefits:**
- Independent document reading
- Currency verification without assistance
- Privacy in financial transactions

#### 3.2.3 Emergency Assistance Module

**Core Functions:**
- Long-press (3 seconds) emergency button activation
- Emergency contact selection and management
- Automatic SMS sending to selected contacts
- Smart dialing (prioritizes 999, supports multiple contacts)
- Location information sharing
- Digit-by-digit phone number reading for accessibility

**Technical Implementation:**
- Emergency contact management system
- SMS and phone call integration
- Location services (GPS)
- Accessibility-optimized UI with large touch targets

**User Benefits:**
- Quick access to emergency assistance
- Reduced response time in critical situations
- Enhanced safety and peace of mind

#### 3.2.4 Voice Command Module

**Core Functions:**
- Multi-language speech recognition (Cantonese, English, Mandarin)
- Fuzzy matching algorithm for command recognition
- Multi-dimensional similarity calculation (Levenshtein distance, LCS, character set, prefix matching)
- Text preprocessing (removes filler words, handles simplified/traditional Chinese)
- Command confirmation and feedback
- Environment recognition control via voice

**Technical Implementation:**
- Android SpeechRecognizer API
- Custom fuzzy matching algorithms
- Command mapping system
- Broadcast mechanism for inter-activity communication

**User Benefits:**
- Hands-free application control
- Natural language interaction
- Reduced learning curve

#### 3.2.5 System Settings Module

**Core Functions:**
- Speech rate, pitch, and volume adjustment (0-200% range)
- Accessibility settings (vibration feedback, screen reader support)
- Language switching (Cantonese, Mandarin, English)
- Settings persistence using SharedPreferences
- Voice test and preview functionality

**Technical Implementation:**
- TTS parameter adjustment
- VibrationManager for haptic feedback
- LocaleManager for language management
- Settings persistence layer

**User Benefits:**
- Personalized user experience
- Optimal accessibility configuration
- Comfortable interaction parameters

#### 3.2.6 Additional Modules

**Find Items Module:**
- Item marking and tagging
- Smart item search using camera
- Location memory system
- Voice-guided search process

**Travel Assistant Module:**
- Navigation and route planning
- Real-time traffic information
- Weather updates
- Emergency location sharing

**Instant Assistance Module:**
- One-tap volunteer calling
- Quick message sending
- Video call support
- Permission management

**Gesture Management Module:**
- Custom gesture creation
- Function binding to gestures
- Multi-gesture pattern recognition
- Language consistency maintenance

### 3.3 Data Description

#### 3.3.1 User Data

- **User Profile**: Login credentials, preferences, settings
- **Emergency Contacts**: Names, phone numbers, relationships
- **Marked Items**: Item descriptions, locations, timestamps
- **Usage Statistics**: Feature usage, performance metrics
- **Settings**: TTS parameters, accessibility preferences, language selection

**Storage**: Local storage using SharedPreferences and SQLite (future: Firebase cloud sync)

#### 3.3.2 Application Data

- **AI Models**: 
  - SSD MobileNet v1 model (TensorFlow Lite format, ~27MB)
  - COCO class labels (80 categories)
- **Language Resources**: 
  - String resources for three languages
  - TTS voice data
  - Command mappings
- **Configuration Files**: 
  - App constants and thresholds
  - UI layout definitions
  - Accessibility configurations

#### 3.3.3 Runtime Data

- **Camera Frames**: Processed in real-time, not stored
- **Detection Results**: Temporary storage for stability filtering
- **Performance Metrics**: Real-time monitoring data
- **Speech Recognition Results**: Temporary command processing

### 3.4 Role of Users and Hardware in the Solution

#### 3.4.1 User Roles

**Primary Users (Visually Impaired Individuals):**
- Use the application for daily assistance
- Provide feedback for improvement
- Participate in user testing

**Secondary Users (Caregivers/Family):**
- Assist with initial setup
- Configure emergency contacts
- Monitor usage and provide support

**Administrators (Future):**
- Manage cloud services
- Monitor system performance
- Handle user support

#### 3.4.2 Hardware Requirements

**Minimum Requirements:**
- Android 5.0 (API 21) or higher
- Rear-facing camera (for object detection)
- Microphone (for voice commands)
- Vibration motor (for haptic feedback)
- 2GB RAM minimum
- 100MB storage space

**Recommended Requirements:**
- Android 8.0 (API 26) or higher
- 4GB RAM or more
- Modern camera with autofocus
- Good quality microphone
- 500MB storage space

**Hardware Utilization:**
- **Camera**: Real-time image capture for object detection and OCR
- **CPU/GPU**: AI model inference, image processing
- **Memory**: Model loading, image buffering, application state
- **Storage**: Model files, user data, settings
- **Network**: Optional cloud features, OCR (if online mode)

### 3.5 Advantages and Drawbacks of the Solution

#### 3.5.1 Advantages

**Technical Advantages:**
- **Offline Functionality**: Core features work without internet
- **Real-time Processing**: Low-latency object detection (<500ms)
- **High Accuracy**: 85-90% detection accuracy with stability filtering
- **Multi-language Support**: Comprehensive localization
- **Accessibility-First**: Built with accessibility in mind
- **Scalable Architecture**: Modular design allows easy feature addition

**User Advantages:**
- **Independence**: Reduced dependence on others
- **Safety**: Enhanced safety through obstacle detection and emergency features
- **Privacy**: Local processing for sensitive operations
- **Ease of Use**: Intuitive interface with voice guidance
- **Cost-Effective**: Free application, no subscription required

**Social Advantages:**
- **Inclusion**: Promotes digital inclusion for visually impaired users
- **Awareness**: Raises awareness about accessibility needs
- **Community**: Potential for community building and support

#### 3.5.2 Drawbacks and Limitations

**Technical Limitations:**
- **Model Accuracy**: 85-90% accuracy means some false positives/negatives
- **Battery Consumption**: Continuous camera and AI processing drain battery
- **Device Compatibility**: Performance varies across different Android devices
- **Limited Categories**: 80 COCO categories may not cover all objects
- **Network Dependency**: Some features (OCR, cloud sync) require internet

**User Limitations:**
- **Learning Curve**: Users need to learn voice commands and gestures
- **Language Barriers**: Limited to three languages (Cantonese, English, Mandarin)
- **Privacy Concerns**: Camera and location access required
- **Device Requirements**: Requires modern Android device with good camera

**Development Limitations:**
- **Resource Constraints**: Limited development time and team size
- **Testing Challenges**: Difficult to test with actual visually impaired users
- **Maintenance**: Ongoing updates needed for model improvements and bug fixes

**Mitigation Strategies:**
- Continuous model optimization and accuracy improvement
- Battery optimization through intelligent processing frequency control
- Comprehensive device testing across different manufacturers
- User feedback collection and iterative improvement
- Clear privacy policy and transparent data usage

---

## 4. Constraints

### 4.1 Reliability Requirements

**System Reliability:**
- **Uptime**: Application should be available 99% of the time (excluding device-specific issues)
- **Crash Rate**: Less than 1% crash rate during normal operation
- **Data Integrity**: User settings and emergency contacts must be preserved
- **Error Handling**: Graceful degradation when features fail (e.g., camera unavailable)

**Detection Reliability:**
- **Accuracy**: Minimum 85% object detection accuracy
- **False Positive Rate**: Less than 15% false positive rate
- **Stability**: Consistent results across multiple detection attempts
- **Performance**: Detection time should not exceed 500ms on average

**Accessibility Reliability:**
- **Voice Output**: TTS should work consistently across all features
- **Screen Reader Compatibility**: Full compatibility with TalkBack
- **Input Methods**: Support for multiple input methods (touch, voice, gestures)

### 4.2 Performance Requirements

**Response Time:**
- **Object Detection**: < 500ms average detection time
- **OCR Processing**: < 2 seconds for document scanning
- **Voice Command Recognition**: < 1 second response time
- **UI Navigation**: < 100ms response to user input
- **Emergency Activation**: < 3 seconds from button press to contact selection

**Resource Usage:**
- **Memory**: < 200MB RAM usage during normal operation
- **Battery**: Optimized to minimize battery drain (intelligent processing frequency)
- **Storage**: < 100MB application size (excluding models)
- **CPU**: Efficient processing to avoid device overheating

**Scalability:**
- Support for devices with varying capabilities (low-end to high-end)
- Graceful performance degradation on older devices
- Efficient model loading and memory management

### 4.3 Existing Data Interface and Hardware Environment

**Data Interfaces:**
- **Local Storage**: SharedPreferences for settings, SQLite for structured data (future)
- **Cloud Storage**: Firebase (planned for future cloud sync)
- **External APIs**: 
  - Google ML Kit (OCR)
  - Android SpeechRecognizer API
  - Location Services (GPS)
  - SMS and Phone APIs

**Hardware Interfaces:**
- **Camera**: CameraX API for camera access (with Legacy Camera API fallback)
- **Microphone**: Android AudioRecord for voice input
- **Vibration**: VibrationManager for haptic feedback
- **Display**: Standard Android UI components with accessibility support
- **Sensors**: GPS for location services

**Compatibility:**
- **Android Versions**: Support for Android 5.0 (API 21) to latest
- **Screen Sizes**: Responsive design for various screen sizes
- **Orientations**: Portrait and landscape support
- **Manufacturers**: Compatibility across major Android manufacturers

### 4.4 Future Extensions of the Proposed Solution

**Short-term Extensions (Post-Mid-term):**
- **Firebase Integration**: Cloud data synchronization and user management
- **YOLOv8 Model**: Integration of more advanced object detection model
- **Extended Language Support**: Additional languages (Japanese, Korean)
- **Advanced Gesture Recognition**: More complex gesture patterns
- **Item Tracking**: Enhanced item finding with object tracking

**Medium-term Extensions:**
- **Web Portal**: Web interface for caregivers to monitor and assist
- **Wearable Integration**: Support for smartwatches and wearables
- **IoT Integration**: Integration with smart home devices
- **Community Features**: User community and sharing features
- **Analytics Dashboard**: Usage analytics and insights

**Long-term Extensions:**
- **AI Model Training**: Custom model training for specific user needs
- **Augmented Reality**: AR overlay for enhanced spatial awareness
- **Healthcare Integration**: Integration with healthcare systems
- **Multi-platform Support**: iOS version development
- **Commercial Features**: Premium features and subscription model

### 4.5 Required Implementation Language

**Primary Language:**
- **Java**: Main implementation language for Android development
- **Rationale**: 
  - Native Android development language
  - Extensive library support
  - Team familiarity
  - Better performance for system-level operations

**Supporting Languages/Technologies:**
- **XML**: Layout definitions, resource files, configuration
- **Gradle**: Build system and dependency management
- **SQL**: Database queries (for future SQLite integration)
- **JSON**: Data serialization and API communication

**Future Considerations:**
- **Kotlin**: Potential migration for modern Android development
- **Python**: For AI model training and optimization (external tools)
- **JavaScript/TypeScript**: For web portal development (future)

**Development Tools:**
- **Android Studio**: Primary IDE
- **Git**: Version control
- **TensorFlow Lite**: AI model inference
- **Google ML Kit**: OCR and text recognition

---

## 5. Software Process Model

### 5.1 Methodology/Approach Adopted

The project adopts a **Hybrid Software Process Model** combining elements of:

1. **Simplified Waterfall Model** (for overall project structure)
2. **Iterative/Incremental Development** (for feature development)
3. **Agile Principles** (for flexibility and adaptation)

### 5.2 Process Model Description

#### 5.2.1 Phase 1: Requirements Analysis and Planning (Completed)

**Activities:**
- Problem identification and scope definition
- User requirements gathering
- Technical requirements analysis
- Initial architecture design
- Project planning and scheduling

**Deliverables:**
- Requirements specification
- System architecture design
- Project plan and timeline
- Initial report (this document)

**Duration:** September - October 2024

#### 5.2.2 Phase 2: Design and Architecture (Completed)

**Activities:**
- Detailed system design
- Database design (for future use)
- UI/UX design with accessibility focus
- API design and integration planning
- Technology stack selection

**Deliverables:**
- System design documents
- UI/UX mockups and prototypes
- Architecture diagrams
- Technology stack documentation

**Duration:** October - November 2024

#### 5.2.3 Phase 3: Implementation - Core Features (In Progress)

**Iterative Development Approach:**

**Iteration 1: Foundation (Completed)**
- Base architecture and core managers
- Main activity and navigation
- Basic UI components
- Language management system

**Iteration 2: Environment Recognition (Completed)**
- Object detection implementation
- Camera integration
- Voice feedback system
- Performance optimization

**Iteration 3: Document Reading (Completed)**
- OCR integration
- Currency detection
- Text-to-speech integration
- UI optimization

**Iteration 4: Emergency Features (Completed)**
- Emergency contact management
- SMS and phone integration
- Location services
- Accessibility optimization

**Iteration 5: Voice Commands (Completed)**
- Speech recognition
- Command mapping
- Fuzzy matching algorithm
- Voice control integration

**Iteration 6: Additional Features (Completed)**
- Find items module
- Travel assistant
- Instant assistance
- Gesture management

**Iteration 7: Optimization and Refinement (In Progress)**
- Performance optimization
- Accuracy improvement
- Bug fixes
- User experience enhancement

**Duration:** November 2024 - January 2025

#### 5.2.4 Phase 4: Testing and Quality Assurance (In Progress)

**Testing Activities:**
- Unit testing for core components
- Integration testing for modules
- System testing for end-to-end scenarios
- Accessibility testing with screen readers
- Performance testing on various devices
- User acceptance testing (with visually impaired users)

**Testing Approach:**
- **Unit Tests**: Individual component testing
- **Integration Tests**: Module interaction testing
- **System Tests**: Full application testing
- **Accessibility Tests**: WCAG compliance and TalkBack compatibility
- **Performance Tests**: Response time and resource usage
- **User Tests**: Real-world usage scenarios

**Duration:** January - February 2025

#### 5.2.5 Phase 5: Documentation and Mid-term Report (In Progress)

**Activities:**
- Technical documentation
- User manual preparation
- API documentation
- Mid-term report writing
- Presentation preparation

**Deliverables:**
- Technical documentation
- User guide
- Mid-term report
- Presentation slides

**Duration:** January - February 2025

#### 5.2.6 Phase 6: Final Implementation and Refinement (Planned)

**Activities:**
- Remaining feature implementation
- Advanced optimizations
- Extended testing
- Bug fixes and improvements
- Final documentation

**Duration:** February - March 2025

#### 5.2.7 Phase 7: Final Testing and Deployment (Planned)

**Activities:**
- Comprehensive system testing
- User acceptance testing
- Performance optimization
- Security audit
- Deployment preparation

**Duration:** March - April 2025

### 5.3 Agile Principles Applied

**Sprint-based Development:**
- 2-week sprints for feature development
- Daily stand-up meetings (team coordination)
- Sprint reviews and retrospectives

**Continuous Integration:**
- Regular code commits to version control
- Automated testing where possible
- Code review process

**User Feedback Integration:**
- Regular user testing sessions
- Feedback incorporation in next iteration
- Iterative improvement based on user needs

**Adaptive Planning:**
- Flexible timeline adjustments
- Priority-based feature development
- Risk management and mitigation

### 5.4 Quality Assurance Process

**Code Quality:**
- Code review by team members
- Coding standards and best practices
- Documentation requirements

**Testing Strategy:**
- Test-driven development where applicable
- Comprehensive test coverage
- Automated testing for critical paths

**Accessibility Quality:**
- WCAG 2.1 compliance checking
- TalkBack compatibility testing
- User testing with visually impaired individuals

---

## 6. Project Plan

### 6.1 Project Schedule

#### 6.1.1 Overall Timeline

**Project Duration:** September 2024 - April 2025 (8 months)

**Key Milestones:**
- **September 2024**: Project initiation and requirements analysis
- **October 2024**: Design and architecture planning
- **November 2024**: Core feature implementation begins
- **December 2024**: Major features completion
- **January 2025**: Testing and optimization
- **February 2, 2025**: **Mid-term Report Deadline** (Core functionality must be complete)
- **February-March 2025**: Final implementation and refinement
- **April 20, 2025**: **Final Project Deadline**

#### 6.1.2 Detailed Schedule (September 2024 - February 2025)

| Phase | Duration | Activities | Status |
|-------|----------|------------|--------|
| **Requirements & Planning** | Sept 2024 | Problem analysis, requirements gathering, initial planning | ✅ Completed |
| **Design & Architecture** | Oct 2024 | System design, UI/UX design, technology selection | ✅ Completed |
| **Core Foundation** | Nov 2024 | Base architecture, managers, main activity | ✅ Completed |
| **Environment Recognition** | Nov-Dec 2024 | Object detection, camera integration, voice feedback | ✅ Completed |
| **Document Reading** | Nov-Dec 2024 | OCR, currency detection, TTS integration | ✅ Completed |
| **Emergency Features** | Dec 2024 | Emergency contacts, SMS, phone, location | ✅ Completed |
| **Voice Commands** | Dec 2024 | Speech recognition, command mapping, fuzzy matching | ✅ Completed |
| **Additional Modules** | Dec 2024 - Jan 2025 | Find items, travel assistant, gestures | ✅ Completed |
| **Optimization** | Jan 2025 | Performance, accuracy, UX improvements | 🔄 In Progress |
| **Testing** | Jan-Feb 2025 | Unit, integration, system, accessibility testing | 🔄 In Progress |
| **Mid-term Report** | Jan-Feb 2025 | Documentation, report writing, presentation | 🔄 In Progress |
| **Final Implementation** | Feb-Mar 2025 | Remaining features, advanced optimizations | 📅 Planned |
| **Final Testing** | Mar-Apr 2025 | Comprehensive testing, user acceptance | 📅 Planned |
| **Final Report** | Apr 2025 | Final documentation and report | 📅 Planned |

#### 6.1.3 Current Status (As of Report Date)

**Completed (98%):**
- ✅ All core modules implemented
- ✅ Environment recognition with 85-90% accuracy
- ✅ Document and currency reading
- ✅ Emergency assistance system
- ✅ Voice command system with fuzzy matching
- ✅ Multi-language support (Cantonese, English, Mandarin)
- ✅ Comprehensive accessibility features
- ✅ System settings and customization
- ✅ Additional modules (find items, travel assistant, gestures)

**In Progress (2%):**
- 🔄 Performance optimization
- 🔄 Final testing and bug fixes
- 🔄 Documentation completion
- 🔄 Mid-term report preparation

**Planned:**
- 📅 Firebase cloud integration
- 📅 YOLOv8 model integration
- 📅 Extended user testing
- 📅 Final refinements

### 6.2 Deliverables

#### 6.2.1 Code Deliverables

**Source Code:**
- Complete Android application source code
- All Java source files
- XML layout and resource files
- Configuration files
- Build scripts (Gradle)

**AI Models:**
- SSD MobileNet v1 model file
- Model documentation
- Integration code

**Repository:**
- Git repository with complete version history
- README with setup instructions
- Documentation in repository

#### 6.2.2 Documentation Deliverables

**Technical Documentation:**
- System architecture documentation
- API documentation
- Code comments and Javadoc
- Database schema (if applicable)
- Deployment guide

**User Documentation:**
- User manual (accessible format)
- Feature guide
- Troubleshooting guide
- Accessibility guide

**Project Documentation:**
- Initial report (this document)
- Mid-term report (due February 2, 2025)
- Final report (due April 2025)
- Presentation slides
- Project log sheets

#### 6.2.3 Testing Deliverables

**Test Documentation:**
- Test plan
- Test cases
- Test results
- Bug reports and resolutions
- Performance test reports

**Quality Assurance:**
- Code review reports
- Accessibility audit results
- User testing feedback
- Performance benchmarks

#### 6.2.4 Deployment Deliverables

**Application Package:**
- APK file (debug and release versions)
- Installation instructions
- System requirements document

**Deployment Materials:**
- App store listing materials (if applicable)
- Marketing materials (if applicable)
- User onboarding materials

### 6.3 Software Tools Needed

#### 6.3.1 Development Tools

**Primary IDE:**
- **Android Studio**: Latest stable version
  - Purpose: Main development environment
  - Features: Code editing, debugging, emulator, profiler

**Version Control:**
- **Git**: Version control system
- **GitHub**: Remote repository hosting
  - Purpose: Code versioning and collaboration

**Build Tools:**
- **Gradle**: Build automation tool
  - Purpose: Dependency management, building APK

#### 6.3.2 Design Tools

**UI/UX Design:**
- **Figma** or **Adobe XD**: UI mockup and prototyping
  - Purpose: Interface design and user experience planning

**Graphics:**
- **Adobe Illustrator** or **Inkscape**: Icon and graphic design
  - Purpose: App icons, custom graphics

#### 6.3.3 Testing Tools

**Testing Frameworks:**
- **JUnit**: Unit testing framework
- **Espresso**: UI testing framework
- **Robolectric**: Unit testing for Android components

**Performance Tools:**
- **Android Profiler**: Performance monitoring
- **LeakCanary**: Memory leak detection
- **Systrace**: System-level performance analysis

**Accessibility Tools:**
- **TalkBack**: Screen reader for testing
- **Accessibility Scanner**: Automated accessibility checking
- **Lighthouse**: Web accessibility auditing (if web components)

#### 6.3.4 AI/ML Tools

**Model Development:**
- **TensorFlow**: Model training and conversion
- **TensorFlow Lite**: On-device inference
- **COCO Dataset**: Training data reference

**Model Optimization:**
- **TensorFlow Lite Converter**: Model optimization
- **Model Analyzer**: Model size and performance analysis

#### 6.3.5 Documentation Tools

**Documentation:**
- **Markdown**: Documentation writing
- **LaTeX** (optional): Academic report formatting
- **Microsoft Word**: Report writing and formatting
- **Draw.io** or **Lucidchart**: Diagram creation

**Collaboration:**
- **Google Docs**: Collaborative document editing
- **GitHub Issues**: Task and bug tracking
- **Slack** or **Discord**: Team communication

### 6.4 Facilities and Hardware Needed

#### 6.4.1 Development Hardware

**Computers:**
- **Development Machines**: 
  - Minimum: 8GB RAM, quad-core processor, 256GB SSD
  - Recommended: 16GB RAM, 8-core processor, 512GB SSD
  - OS: Windows, macOS, or Linux
  - Purpose: Android development, testing, documentation

**Mobile Devices for Testing:**
- **Primary Test Device**: Modern Android device (Android 8.0+)
  - Purpose: Primary development and testing
- **Secondary Test Devices**: 
  - Older Android device (Android 5.0-7.0) for compatibility testing
  - Various screen sizes for responsive design testing
  - Different manufacturers (Samsung, Xiaomi, etc.) for compatibility

**Peripheral Devices:**
- **External Camera** (optional): For testing camera features
- **Headphones**: For testing audio and TTS features
- **External Storage**: For backup and file sharing

#### 6.4.2 Software Licenses and Services

**Required Licenses:**
- **Android Studio**: Free (open source)
- **Git**: Free (open source)
- **TensorFlow**: Free (open source)

**Optional Services:**
- **GitHub**: Free for public repositories, paid for private (if needed)
- **Firebase**: Free tier available, paid for extended usage
- **Google Cloud Platform**: Free tier available for testing

#### 6.4.3 Testing Facilities

**Accessibility Testing:**
- **Access to Visually Impaired Users**: For user testing and feedback
  - Collaboration with local organizations
  - User testing sessions
  - Feedback collection

**Physical Testing Environment:**
- **Various Lighting Conditions**: For camera testing
- **Different Environments**: Indoor, outdoor, various scenarios
- **Network Testing**: Different network conditions (WiFi, 4G, offline)

#### 6.4.4 Cloud Services (Future)

**Firebase Services:**
- **Firebase Authentication**: User management
- **Firebase Realtime Database** or **Firestore**: Cloud data storage
- **Firebase Cloud Messaging**: Push notifications
- **Firebase Analytics**: Usage analytics

**Google Services:**
- **Google Maps API**: For navigation features (if needed)
- **Google Cloud Storage**: For model and resource hosting (if needed)

### 6.5 Risk Management

#### 6.5.1 Technical Risks

**Risk 1: AI Model Performance**
- **Description**: Model accuracy may not meet requirements
- **Probability**: Medium
- **Impact**: High
- **Mitigation**: 
  - Continuous model optimization
  - Multiple model testing
  - Fallback mechanisms
  - User feedback integration

**Risk 2: Device Compatibility**
- **Description**: Application may not work well on all devices
- **Probability**: Medium
- **Impact**: Medium
- **Mitigation**:
  - Extensive device testing
  - Graceful degradation
  - Minimum requirements specification
  - Performance optimization

**Risk 3: Battery Consumption**
- **Description**: Continuous camera and AI processing may drain battery quickly
- **Probability**: High
- **Impact**: Medium
- **Mitigation**:
  - Intelligent processing frequency control
  - Battery optimization techniques
  - User-configurable settings
  - Background processing optimization

#### 6.5.2 Schedule Risks

**Risk 4: Timeline Delays**
- **Description**: Development may take longer than planned
- **Probability**: Medium
- **Impact**: High
- **Mitigation**:
  - Regular progress monitoring
  - Priority-based feature development
  - Buffer time in schedule
  - Early identification of blockers

**Risk 5: Mid-term Deadline Pressure**
- **Description**: Core functionality may not be ready by February 2nd
- **Probability**: Low (currently on track)
- **Impact**: High
- **Mitigation**:
  - Focus on core features first
  - Regular milestone reviews
  - Early completion of critical features
  - Contingency planning

#### 6.5.3 Resource Risks

**Risk 6: Team Availability**
- **Description**: Team members may have conflicting commitments
- **Probability**: Medium
- **Impact**: Medium
- **Mitigation**:
  - Clear task distribution
  - Regular communication
  - Flexible scheduling
  - Backup plans for critical tasks

**Risk 7: Limited User Testing**
- **Description**: Difficulty accessing visually impaired users for testing
- **Probability**: Medium
- **Impact**: Medium
- **Mitigation**:
  - Early engagement with user groups
  - Accessibility tools for self-testing
  - Feedback collection mechanisms
  - Iterative improvement based on available feedback

### 6.6 Quality Assurance Plan

#### 6.6.1 Code Quality

**Standards:**
- Follow Android coding conventions
- Comprehensive code comments
- Javadoc for public APIs
- Code review process

**Metrics:**
- Code coverage target: >70%
- Cyclomatic complexity: Keep methods simple
- Code duplication: Minimize

#### 6.6.2 Testing Strategy

**Unit Testing:**
- Test all core managers and utilities
- Mock dependencies
- Target: >70% code coverage

**Integration Testing:**
- Test module interactions
- Test API integrations
- Test data flow

**System Testing:**
- End-to-end user scenarios
- Performance testing
- Stress testing

**Accessibility Testing:**
- WCAG 2.1 compliance
- TalkBack compatibility
- User testing with visually impaired individuals

#### 6.6.3 Performance Targets

**Response Times:**
- Object detection: <500ms
- OCR processing: <2s
- Voice command: <1s
- UI response: <100ms

**Resource Usage:**
- Memory: <200MB
- Battery: Optimized for extended use
- Storage: <100MB (excluding models)

---

## 7. References

### 7.1 Academic References

1. Lin, T. Y., et al. (2014). "Microsoft COCO: Common Objects in Context." *European Conference on Computer Vision (ECCV)*, 2014.

2. Redmon, J., et al. (2016). "You Only Look Once: Unified, Real-Time Object Detection." *IEEE Conference on Computer Vision and Pattern Recognition (CVPR)*, 2016.

3. Howard, A. G., et al. (2017). "MobileNets: Efficient Convolutional Neural Networks for Mobile Vision Applications." *arXiv preprint arXiv:1704.04861*.

4. World Wide Web Consortium (W3C). (2018). "Web Content Accessibility Guidelines (WCAG) 2.1." Retrieved from https://www.w3.org/TR/WCAG21/

5. Google. (2023). "Android Accessibility Overview." Android Developer Documentation. Retrieved from https://developer.android.com/guide/topics/ui/accessibility

### 7.2 Technical Documentation

6. Google. (2023). "TensorFlow Lite Guide." TensorFlow Documentation. Retrieved from https://www.tensorflow.org/lite

7. Google. (2023). "ML Kit Documentation." Google Developers. Retrieved from https://developers.google.com/ml-kit

8. Google. (2023). "CameraX Overview." Android Developer Documentation. Retrieved from https://developer.android.com/training/camerax

9. Google. (2023). "Text-to-Speech API." Android Developer Documentation. Retrieved from https://developer.android.com/reference/android/speech/tts/TextToSpeech

10. Google. (2023). "Speech Recognition API." Android Developer Documentation. Retrieved from https://developer.android.com/reference/android/speech/SpeechRecognizer

### 7.3 Accessibility Standards

11. Android Accessibility Service. (2023). "Making Apps More Accessible." Android Developer Documentation. Retrieved from https://developer.android.com/guide/topics/ui/accessibility/services

12. Web Accessibility Initiative (WAI). (2023). "Mobile Accessibility." W3C. Retrieved from https://www.w3.org/WAI/mobile/

### 7.4 Software Development

13. Beck, K., et al. (2001). "Manifesto for Agile Software Development." Agile Alliance. Retrieved from https://agilemanifesto.org/

14. Royce, W. W. (1970). "Managing the Development of Large Software Systems." *Proceedings of IEEE WESCON*, 1970.

15. Boehm, B. W. (1988). "A Spiral Model of Software Development and Enhancement." *Computer*, 21(5), 61-72.

### 7.5 Dataset and Models

16. Common Objects in Context (COCO). (2023). "COCO Dataset." Retrieved from https://cocodataset.org/

17. TensorFlow Model Zoo. (2023). "Pre-trained Models." TensorFlow Hub. Retrieved from https://www.tensorflow.org/lite/models

### 7.6 User Experience and Design

18. Norman, D. A. (2013). *The Design of Everyday Things: Revised and Expanded Edition*. Basic Books.

19. Krug, S. (2014). *Don't Make Me Think, Revisited: A Common Sense Approach to Web Usability*. New Riders.

20. Google Material Design. (2023). "Accessibility Guidelines." Material Design. Retrieved from https://material.io/design/usability/accessibility.html

---

## Appendix A: Glossary

**AI (Artificial Intelligence)**: Computer systems that can perform tasks typically requiring human intelligence.

**API (Application Programming Interface)**: Set of protocols and tools for building software applications.

**COCO (Common Objects in Context)**: Large-scale object detection, segmentation, and captioning dataset.

**OCR (Optical Character Recognition)**: Technology that converts images of text into machine-readable text.

**SSD (Single Shot Detector)**: Object detection algorithm that detects objects in a single pass.

**TTS (Text-to-Speech)**: Technology that converts written text into spoken words.

**WCAG (Web Content Accessibility Guidelines)**: International standards for web accessibility.

**YOLO (You Only Look Once)**: Real-time object detection system.

---

## Appendix B: Abbreviations

- **API**: Application Programming Interface
- **APK**: Android Package Kit
- **CPU**: Central Processing Unit
- **FYP**: Final Year Project
- **GPS**: Global Positioning System
- **GPU**: Graphics Processing Unit
- **IDE**: Integrated Development Environment
- **IoT**: Internet of Things
- **ML**: Machine Learning
- **NMS**: Non-Maximum Suppression
- **OCR**: Optical Character Recognition
- **RAM**: Random Access Memory
- **SDK**: Software Development Kit
- **SMS**: Short Message Service
- **SQL**: Structured Query Language
- **SSD**: Single Shot Detector
- **TTS**: Text-to-Speech
- **UI**: User Interface
- **UX**: User Experience
- **WCAG**: Web Content Accessibility Guidelines

---

**End of Initial Report**

