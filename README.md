# Campus Alert - Campus Safety Mobile App

**PBDV Project 2 - Native Mobile App**

Campus Alert is an Android application built to improve campus safety by providing students, staff, security personnel, and administrators with a centralized platform for emergency response, incident reporting, and real-time communication. The app delivers role-specific dashboards and features, ensuring each user type sees only what's relevant to them.

## Features

### For Students & Staff
- **Panic Alert System** - Trigger an emergency alert that instantly sends your live GPS location and emergency contacts to campus security.
- **Incident Reporting** - Submit detailed reports (theft, harassment, medical emergencies, etc.) with optional photo/video attachments.
- **View Alerts** - Receive and browse campus-wide safety announcements with real-time search and filtering.
- **Safety Tips** - Access categorized emergency procedures (fire, lockdown, bomb threat, medical, etc.).
- **Emergency Contacts** - Add, edit, and manage personal emergency contacts that get attached to panic alerts.
- **Profile Management** - Update personal info and profile photo with built-in image cropping.

### For Security Personnel
- Receive panic alerts and incident reports in real time.
- View live GPS locations of users and responding security members on Google Maps.
- Resolve alerts and reports, with automatic reverse geocoding to log readable addresses.
- Send campus-wide alerts and safety announcements to all users.
- Access a searchable archive of resolved items.

### For Administrators
- Analytics dashboard with three charts (MPAndroidChart):
  - Line chart: Resolved panic alerts vs. resolved reports (7-day trend)
  - Pie chart: Today's active vs. resolved panic alerts
  - Bar chart: User distribution by role
- Quick access to active panic alerts, active reports, and resolved items.
- Full oversight of campus safety operations.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Platform | Android (minSdk 24, targetSdk 35) |
| Language | Java |
| Authentication | Firebase Authentication |
| Database | Firebase Firestore |
| Media Storage | Firebase Storage |
| Push Notifications | OneSignal |
| Maps & Location | Google Maps SDK, FusedLocationProviderClient, Geocoder API, Places API |
| Charts | MPAndroidChart |
| Image Loading | Glide |
| Image Cropping | uCrop |
| HTTP Client | OkHttp |

## Architecture

The app follows a role-based architecture:
1. **Splash Activity** - Checks authentication state and routes users based on their Firestore-stored role.
2. **Authentication** - Firebase Auth handles login/registration; roles are assigned at sign-up.
3. **Dashboards** - Each role gets a tailored dashboard (Student/Staff, Security, Admin).
4. **Core Modules** - Panic alerts, incident reports, alerts viewing, safety tips, emergency contacts, profile management, and admin analytics all communicate with Firebase services and OneSignal for notifications.

## Testing

- **Unit tests** - Form validation, role redirection logic.
- **Integration tests** - Firebase Auth, Firestore, Storage, and OneSignal workflows.
- **System tests** - End-to-end scenarios across all user roles.
- **Performance** - Panic alerts delivered in under 3 seconds; dashboards load in under 3 seconds.
- **Security** - Firestore rules and Storage rules verified to block unauthorized access.

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Connect your own Firebase project (add `google-services.json`).
4. Set up OneSignal and add your App ID.
5. Enable Google Maps SDK and add your API key.
6. Build and run on an Android device or emulator (API 24+).

## Notes

This project was developed as part of a university module (PBDV Project 2) to demonstrate native Android development, cloud backend integration, real-time notifications, and role-based access control.
