# CyberTotal - Minimal Static Frontend

A lightweight, high-performance static website for **CyberTotal** (AI-based Vulnerability Prioritizer & Security Scanner). Built with pure HTML5, vanilla CSS, and modern JavaScript — zero build steps, zero node_modules required.

---

## 📁 Folder Structure

```
frontend/
├── index.html       # Semantic HTML layout (Login & Dashboard views, Modals)
├── styles.css       # Obsidian dark cybersecurity aesthetic & micro-animations
├── app.js           # API integration, GitHub OAuth popup flow, & scanners
└── README.md        # Deployment & setup documentation
```

---

## 🚀 Running Locally

You can run this frontend immediately using any of these simple methods:

### Method 1: Just Double-Click
Open [`index.html`](index.html) directly in Google Chrome, Brave, Edge, or Firefox.

### Method 2: VS Code Live Server
1. Open this folder in VS Code.
2. Right click [`index.html`](index.html) → **Open with Live Server**.

### Method 3: Node / Python local server
```bash
# Using Node
npx serve .

# Or using Python 3
python -m http.server 3000
```
Then visit `http://localhost:3000`.

---

## 🌐 Deploying to Production (Free)

Because this is a 100% static folder, you can deploy it in less than 60 seconds:

### Option 1: Vercel (Recommended)
1. Install Vercel CLI: `npm i -g vercel`
2. Run inside the `frontend/` folder:
   ```bash
   vercel --prod
   ```
   *Or drag & drop the `frontend` folder at [vercel.com/new](https://vercel.com/new).*

### Option 2: Netlify (Drag & Drop)
1. Go to [app.netlify.com/drop](https://app.netlify.com/drop).
2. Drag and drop the `frontend` folder directly into your browser window.
3. Your site will instantly go live with an HTTPS URL.

### Option 3: Render Static Site
1. Go to your **Render Dashboard** → **New +** → **Static Site**.
2. Connect your Git repository.
3. Configure:
   - **Root Directory**: `frontend`
   - **Build Command**: *(leave empty)*
   - **Publish Directory**: `.`
4. Click **Create Static Site**.

### Option 4: GitHub Pages
1. Push this folder to your repository.
2. Go to **Repository Settings → Pages**.
3. Under **Branch**, select `main` and folder `/frontend` (or `/docs`).
4. Click **Save**.

---

## 🔗 Backend API Integration

The frontend automatically connects to your Render backend:
- **Default Cloud Endpoint**: `https://codesafe-acrf.onrender.com`
- **Localhost Endpoint**: `http://localhost:8080`

### Switching API Endpoints
You can switch between Render and Localhost at any time:
1. Click the **API Badge** (or the ⚙ gear icon) in the top-right corner.
2. Select **Render** or **Localhost**, or type a custom API URL.
3. Click **Save & Reconnect**. Your choice is automatically persisted in `localStorage`.

---

## 🐙 GitHub OAuth Flow

1. Log into your CyberTotal account on the frontend.
2. Click the **Git Login** card in the dashboard.
3. A secure popup opens GitHub authorization.
4. Once you approve, GitHub redirects to:
   ```
   https://codesafe-acrf.onrender.com/integrations/github/callback
   ```
5. The backend records your token, securely communicates back to the frontend window via `window.opener.postMessage`, and closes the popup.
6. The profile card instantly updates with your **GitHub Username** (e.g. `xenion80`) and displays your **GitHub Repositories**.
7. Clicking any repository assigns it to **GitHub Project** for automated vulnerability scans.
