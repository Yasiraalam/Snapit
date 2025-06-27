# Snapit App

## Description
**Snapit** is a native Android social media application built using Kotlin and Jetpack Compose. It enables users to engage in discussions, share posts with images, follow other users, view profiles, and interact with content through likes and comments — all within a sleek and intuitive interface.

## Features
- **User Authentication:** Secure sign up, login, and logout functionality using Firebase Authentication.
- **Post Creation:** Users can create and share image-based posts.
- **User Profiles:** View your own or other users’ profiles.
- **Follow System:** Follow or unfollow other users to stay updated on their activity.
- **Likes and Comments:** Like posts and participate in discussions via comments.
- **Search:** Easily find users by searching their names or usernames.

## Upcoming Features
- **Real-time Notifications**
- **Post Sharing**
- **Hashtags and Mentions Support**

## Technologies Used

- **Frontend:**
  - Kotlin with Jetpack Compose for a modern, reactive UI
  - Coil for efficient image loading and caching

- **Backend:**
  - Firebase Authentication for secure user management
  - Firebase Firestore to store user data, posts, likes, comments
  - Firebase Storage to manage and serve uploaded images

## Getting Started

To get a local copy of Snapit up and running, follow these simple steps.

### Prerequisites

You will need to have Java Development Kit (JDK) 17 installed on your machine. This project requires it to build.

### Installation

1. **Clone the repo**
    ```sh
    git clone https://github.com/Yasiraalam/snapit.git
    ```

2. **Install JDK 17**

    **macOS (using Homebrew)**
    ```sh
    brew install openjdk@17
    ```
    Then add the following to your shell configuration (e.g., `~/.zshrc` or `~/.bash_profile`):
    ```sh
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    export PATH="$JAVA_HOME/bin:$PATH"
    ```

    **Other Systems:**
    Download JDK 17 from [Adoptium](https://adoptium.net/temurin/releases/?version=17) and set your `JAVA_HOME` accordingly.

3. **Build the project**
    ```sh
    chmod +x gradlew
    ./gradlew build
    ```

## Demo Pics

<img src="https://github.com/user-attachments/assets/3993ce40-8a48-48f2-aae6-ab7746abe31b" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/0c21e40b-9cab-4295-9a8d-5cff599a75a2" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/29ae1398-8e05-4a95-b211-d69b0e468219" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/4d512e3e-faf9-4ee9-9bcf-57de369cdefb" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/43be7e23-6e8e-4271-8254-39aa82c03f75" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/753d7a2c-5fc7-4d70-870f-5279faabc837" width="220" height="450"/>
<img src="https://github.com/user-attachments/assets/753d7a2c-5fc7-4d70-870f-5279faabc837" width="220" height="450"/>




