# CloudGuard - CLOUDMEN

CloudGuard is a comprehensive internal management and authorization portal developed for **CLOUDMEN**. It manages user roles, access requests, and system statuses, and seamlessly integrates with external services like Google Workspace, Teamleader, Auth0, and Supabase.

## 🚀 Tech Stack

**Frontend:**
* Angular (v17+)
* Tailwind CSS (Styling)
* Lucide Angular (Icons)
* Transloco (Internationalization / i18n)

**Backend**
* Java (v21)
* Spring Boot (v3+)
* Maven

**Infrastructure & Services:**
* **Main Database:** MySQL (Containerized)
* **Token Storage:** Supabase (Used exclusively for Teamleader tokens)
* **Authentication:** Auth0 (Single Page Application)
* **Integrations:** Teamleader API, Google Admin SDK (Domain-wide Delegation)
* **Deployment:** Docker, Docker Compose, Traefik (Reverse Proxy)
* **CI/CD:** Gitlab Pipelines

## ✨ Key Features

* **User & Role Management:** Comprehensive system to manage user accounts, assign roles, and control platform permissions.

* **Access Request Workflow:** Streamlined process for users to request access or specific roles, with an admin approval interface.

* **Google Workspace Integration:** Deep integration with Google Admin SDK to monitor and manage Workspace users, groups, and drive data.

* **Teamleader Synchronization:** Automated connectivity with the Teamleader API, including secure OAuth token management.

* **Advanced Authorization:** Secure Auth0-based authentication flow using custom post-login actions for email verification.

* **Security Dashboards:** Visual representation of system security statuses and account activities using interactive charts.

* **Multi-language Support:** Fully localized interface with support for multiple languages (Dutch and English).

* **Automated Deployment:** Robust CI/CD pipeline and containerized infrastructure for seamless updates and high availability.

## 👥 User Roles & Permissions
CloudGuard utilizes a role-based access control (RBAC) system to ensure secure and appropriate access to its features. The following roles are currently supported:

* **Super Admin:** Has broad access to the platform's data and can view all security dashboards.

* **CLOUDMEN Super Admin:** A specialized, top-level administrator. Inherits all standard Super Admin functionalities, with the exclusive ability to manage all users, approve or deny access requests, assign roles, and allocate organizations to users.

* **Security Dashboard Viewers:** Instead of an all-or-nothing approach, read-only access is divided into specific viewer roles. Each of these roles corresponds directly to a specific security module within the system. Users can be assigned one or multiple of the following:
    * `USERS_GROUPS_VIEWER`
    * `ORG_UNITS_VIEWER`
    * `SHARED_DRIVES_VIEWER`
    * `DEVICES_VIEWER`
    * `APP_ACCESS_VIEWER`
    * `APP_PASSWORDS_VIEWER`
    * `PASSWORD_SETTINGS_VIEWER`
    * `DOMAIN_DNS_VIEWER`
    * `LICENSES_VIEWER`
    * `SECURITY_PREFERENCES_VIEWER`

* **Unassigned:** The default state for newly registered users. These users have successfully authenticated via Auth0 but have not yet been granted access to any specific dashboards by an administrator. Users with no roles will automatically been assigned this status and will not have any access to the platform's content.

## ⚙️ Prerequisites

Before you begin, ensure you have met the following requirements:
* Git installed
* Node.js and npm (for local frontend development)
* Java 21 and Maven (for local backend development)
* Docker & Docker Compose (for containerized deployment)
* Access to the project's external services (Auth0, Supabase, Teamleader, Google Cloud)

## 🛠️ Configuration & Environment Variables

To connect the backend to the required services, you must create an `.env` file in the backend directory based on the provided `env-vars` template.

```env
# Google Configuration
GOOGLE_CLIENT_EMAIL=your-service-account-email
GOOGLE_PRIVATE_KEY=your-private-key

# Teamleader Configuration
TEAMLEADER_CLIENT_ID=your-client-id
TEAMLEADER_CLIENT_SECRET=your-client-secret

# Supabase Configuration (For Teamleader tokens only)
SUPABASE_URL=[https://your-project.supabase.co](https://your-project.supabase.co)
SUPABASE_KEY=your-service-role-secret-key

# Auth0 Configuration
AUTH0_DOMAIN=your-tenant.eu.auth0.com
AUTH0_CLIENT_ID=your-client-id
AUTH0_CLIENT_SECRET=your-client-secret
AUTH0_AUDIENCE=[https://your-tenant.eu.auth0.com/api/v2/](https://your-tenant.eu.auth0.com/api/v2/)

# Mail & System
MAIL_USERNAME=your-mail-account
MAIL_PASSWORD=your-mail-password
BACKEND_PORT=8080
CLOUDMEN_ADMIN_EMAILS=admin1@example.com,admin2@example.com
```

## 🔐 Auth0 Post-Login Action Setup

To securely pass the user's Google Workspace email to the backend, you must configure a Custom Action in your Auth0 dashboard.

1. Navigate to **Actions** &rarr; **Triggers** &rarr; **Post Login** in Auth0.

2. Create a new "Custom Action" (e.g., `Check Google Admin Status`).

3. Replace the default code with the following script:
```js
exports.onExecutePostLogin = async (event, api) => {
  if (event.connection.strategy !== 'google-oauth2') return;

  const userEmail = event.user.email;

  // We sent only the email as secure claim
  // The backend will decide the roles later (Admin or User).
  api.idToken.setCustomClaim('https://cloudguard.com/workspace_email', userEmail);
  api.accessToken.setCustomClaim('https://cloudguard.com/workspace_email', userEmail);

  console.log(`Successful login via Google Workspace for: ${userEmail}`);
};
```

4. Click **Deploy** to save the script.

5. Drag and drop this new action between "Start" and "Complete" in the Post-Login flow diagram.

6. Click **Apply** in the top right corner to make the flow live. <br>
⚠️ **IMPORTANT:** For a complete, step-by-step guide with screenshots on how to configure Auth0 and other external platforms, please refer to the [Releaseplan_CloudGuard_en.pdf](./docs/Releaseplan_CloudGuard_en.pdf) or the [Releaseplan_CloudGuard_nl.pdf](./docs/Releaseplan_CloudGuard_nl.pdf).

## 💻 Local Development

**Frontend**

1. Navigate to the frontend directory: `cd frontend`

2. Install dependencies: `npm install`

3. Start the development server: `npm start`

4. The application will be available at `http://localhost:4200`.

**Backend**

1. Navigate to the backend directory: `cd api`

2. Ensure your `.env` file is properly configured.

3. Build the project: `mvn clean install`

4. Run the Spring Boot Application: `mvn spring-boot:run`

5. The backend API will be available at `http://localhost:8080` (the port here is determined by what you have set in the `.env` for `${BACKEND_PORT}`).

## 🐳 Docker Deployment (Production / Staging)

The application is fully containerized and uses **Traefik** as a reverse proxy for routing and automatic SSL (HTTPS) certificate generation.

1. **Install Docker:** Ensure Docker and Docker Compose are installed on your server.

2. **Create the Traefik Network:**
```bash
docker network create traefik
```

3. **Configure Environment Variables:** Ensure the `.env` files in both the Traefik directory and the project root directory are filled out.

4. **Start Traefik:**
``` bash
cd traefik
docker compose up -d --build
```

5. **Start CloudGuard (Frontend, Backend, MySQL):**
``` bash
cd ..
docker compose up -d --build
```

## 🔄 CI/CD Pipeline

This project uses **Gitlab CI/CD** for automated testing, building, and deployment.
- **Test Stage:** Automatically runs Maven tests on the backend.

- **Build Stage:** Compiles the Spring Boot `.jar` and the Angular `dist` folder as pipeline artifacts.

- **Deploy Stage:** Upon merging to the `main` branch, the pipeline connects to the server via SSH, pulls the latest code, and restarts the Docker containers.

## 📝 License & Authors
- **Authors:**
    - [Anna Yang](https://github.com/03ayv)
    - [Arno Massart](https://github.com/ArnoMassart)

- Developed as an internship project for **CLOUDMEN**.
