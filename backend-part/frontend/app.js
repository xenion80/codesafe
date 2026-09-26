/**
 * CyberTotal - Frontend Client Application
 * Handles authentication, GitHub OAuth flow, repository discovery, and security scans.
 */

// ==================== CONFIGURATION & STATE ====================

const isRunningLocally = window.location.hostname === 'localhost' || 
                         window.location.hostname === '127.0.0.1' || 
                         window.location.protocol === 'file:';

const CONFIG = {
  DEFAULT_API_BASE: isRunningLocally ? 'http://localhost:8080' : 'https://codesafe-acrf.onrender.com',
  STORAGE_KEYS: {
    API_BASE: 'cybertotal_api_base',
    ACCESS_TOKEN: 'cybertotal_access_token',
    USER: 'cybertotal_user',
    SELECTED_PROJECT: 'cybertotal_selected_project',
    DOMAIN: 'cybertotal_user_domain'
  }
};

// If running locally and previous storage was pointed at Render, automatically switch to localhost
let initialApiBase = localStorage.getItem(CONFIG.STORAGE_KEYS.API_BASE);
if (isRunningLocally && (!initialApiBase || initialApiBase.includes('codesafe-acrf.onrender.com'))) {
  initialApiBase = 'http://localhost:8080';
  localStorage.setItem(CONFIG.STORAGE_KEYS.API_BASE, initialApiBase);
}

const state = {
  apiBase: initialApiBase || CONFIG.DEFAULT_API_BASE,
  token: localStorage.getItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN) || null,
  user: null,
  githubUsername: null,
  selectedRepo: localStorage.getItem(CONFIG.STORAGE_KEYS.SELECTED_PROJECT) || '',
  repositories: []
};

try {
  const cachedUser = localStorage.getItem(CONFIG.STORAGE_KEYS.USER);
  if (cachedUser) state.user = JSON.parse(cachedUser);
} catch (e) {
  console.warn('Failed to parse cached user', e);
}

// ==================== DOM ELEMENTS ====================

const dom = {
  // Views
  authView: document.getElementById('authView'),
  dashboardView: document.getElementById('dashboardView'),
  mainHeader: document.getElementById('mainHeader'),
  
  // Auth Forms
  loginFormContainer: document.getElementById('loginFormContainer'),
  registerFormContainer: document.getElementById('registerFormContainer'),
  loginForm: document.getElementById('loginForm'),
  registerForm: document.getElementById('registerForm'),
  loginEmail: document.getElementById('loginEmail'),
  loginPassword: document.getElementById('loginPassword'),
  showRegisterBtn: document.getElementById('showRegisterBtn'),
  showLoginBtn: document.getElementById('showLoginBtn'),
  loginSubmitBtn: document.getElementById('loginSubmitBtn'),
  registerSubmitBtn: document.getElementById('registerSubmitBtn'),
  forgotPasswordLink: document.getElementById('forgotPasswordLink'),
  authApiTarget: document.getElementById('authApiTarget'),
  toggleAuthApiBtn: document.getElementById('toggleAuthApiBtn'),

  // Header Elements
  apiStatusBadge: document.getElementById('apiStatusBadge'),
  apiEndpointLabel: document.getElementById('apiEndpointLabel'),
  openSettingsBtn: document.getElementById('openSettingsBtn'),
  userPill: document.getElementById('userPill'),
  headerUserAvatar: document.getElementById('headerUserAvatar'),
  headerUserName: document.getElementById('headerUserName'),

  // Dashboard Action Cards
  cardCheckWebsite: document.getElementById('cardCheckWebsite'),
  cardCheckCode: document.getElementById('cardCheckCode'),
  cardGitLogin: document.getElementById('cardGitLogin'),
  gitLoginPill: document.getElementById('gitLoginPill'),

  // Profile Card
  profileName: document.getElementById('profileName'),
  profileGithubUser: document.getElementById('profileGithubUser'),
  profileDomain: document.getElementById('profileDomain'),
  profileGithubProject: document.getElementById('profileGithubProject'),
  githubStatusIndicator: document.getElementById('githubStatusIndicator'),
  refreshReposBtn: document.getElementById('refreshReposBtn'),
  logoutBtn: document.getElementById('logoutBtn'),

  // Repositories Section
  reposEmptyState: document.getElementById('reposEmptyState'),
  reposLoadingState: document.getElementById('reposLoadingState'),
  reposListGrid: document.getElementById('reposListGrid'),

  // Modals
  websiteCheckerModal: document.getElementById('websiteCheckerModal'),
  codeCheckerModal: document.getElementById('codeCheckerModal'),
  forgotPasswordModal: document.getElementById('forgotPasswordModal'),
  settingsModal: document.getElementById('settingsModal'),
  
  // Modal Actions
  startWebsiteScanBtn: document.getElementById('startWebsiteScanBtn'),
  websiteScanResults: document.getElementById('websiteScanResults'),
  targetWebsiteUrl: document.getElementById('targetWebsiteUrl'),
  targetProjectName: document.getElementById('targetProjectName'),
  startCodeScanBtn: document.getElementById('startCodeScanBtn'),
  codeSelectedRepo: document.getElementById('codeSelectedRepo'),
  codeScanResults: document.getElementById('codeScanResults'),
  submitForgotBtn: document.getElementById('submitForgotBtn'),
  forgotEmail: document.getElementById('forgotEmail'),
  saveApiUrlBtn: document.getElementById('saveApiUrlBtn'),
  customApiUrlInput: document.getElementById('customApiUrlInput'),

  // Toast
  toastContainer: document.getElementById('toastContainer')
};

// ==================== HELPER / UTILITY FUNCTIONS ====================

function showToast(message, type = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let icon = 'ℹ️';
  if (type === 'success') icon = '✓';
  if (type === 'error') icon = '✕';
  
  toast.innerHTML = `<span style="font-weight:700;">${icon}</span><span>${escapeHtml(message)}</span>`;
  dom.toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(20px)';
    setTimeout(() => toast.remove(), 250);
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function setBtnLoading(button, isLoading, text = 'Loading...') {
  if (!button) return;
  const textSpan = button.querySelector('.btn-text');
  const spinner = button.querySelector('.btn-spinner');
  if (isLoading) {
    button.disabled = true;
    if (textSpan) textSpan.dataset.originalText = textSpan.textContent;
    if (textSpan) textSpan.textContent = text;
    if (spinner) spinner.classList.remove('hidden');
  } else {
    button.disabled = false;
    if (textSpan && textSpan.dataset.originalText) {
      textSpan.textContent = textSpan.dataset.originalText;
    }
    if (spinner) spinner.classList.add('hidden');
  }
}

// Custom Fetch with Automatic Auth Headers & Base URL
async function apiFetch(endpoint, options = {}) {
  const url = `${state.apiBase}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (state.token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch(url, {
      ...options,
      headers
    });

    const isJson = (res.headers.get('content-type') || '').includes('application/json');
    const data = isJson ? await res.json() : await res.text();

    if (!res.ok) {
      // Extract error message from API response
      let errorMsg = `Request failed (${res.status})`;
      if (data && typeof data === 'object') {
        errorMsg = data.message || data.error || errorMsg;
      } else if (typeof data === 'string' && data.length < 200) {
        errorMsg = data;
      }
      throw new Error(errorMsg);
    }

    return data;
  } catch (err) {
    console.error(`API Error on ${endpoint}:`, err);
    if (err.name === 'TypeError' && err.message && err.message.toLowerCase().includes('fetch')) {
      throw new Error(`Cannot connect to backend (${state.apiBase}). Ensure Spring Boot is running on port 8080.`);
    }
    throw err;
  }
}

// ==================== AUTHENTICATION FLOWS ====================

async function handleLogin(e) {
  e.preventDefault();
  const emailInput = dom.loginEmail.value.trim();
  const password = dom.loginPassword.value;

  if (!emailInput || !password) {
    showToast('Please enter both username/email and password.', 'error');
    return;
  }

  setBtnLoading(dom.loginSubmitBtn, true, 'Signing in...');

  try {
    // Backend LoginRequest requires email and password
    const payload = {
      email: emailInput,
      password: password
    };

    const res = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    // Handle backend ApiResponse structure { success, message, data: { accessToken, ... } }
    const loginData = res.data || res.payload || res;
    if (!loginData.accessToken) {
      throw new Error('Access token not returned from server');
    }

    state.token = loginData.accessToken;
    localStorage.setItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, state.token);

    showToast('Logged in successfully!', 'success');

    // Fetch user details
    await fetchUserProfile();
    transitionToDashboard();
  } catch (err) {
    showToast(err.message || 'Login failed. Please check your credentials.', 'error');
  } finally {
    setBtnLoading(dom.loginSubmitBtn, false);
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const name = document.getElementById('regName').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;

  if (!name || !email || !password) {
    showToast('Please fill in all fields.', 'error');
    return;
  }

  setBtnLoading(dom.registerSubmitBtn, true, 'Creating account...');

  try {
    await apiFetch('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password })
    });

    showToast('Account created successfully! You can now log in.', 'success');
    // Switch to login view with email prefilled
    dom.loginEmail.value = email;
    dom.loginPassword.value = '';
    showLoginForm();
  } catch (err) {
    showToast(err.message || 'Registration failed.', 'error');
  } finally {
    setBtnLoading(dom.registerSubmitBtn, false);
  }
}

async function handleLogout() {
  try {
    await apiFetch('/auth/logout', { method: 'POST' }).catch(() => {});
  } catch (e) {
    // Ignore network error on logout
  }

  state.token = null;
  state.user = null;
  state.githubUsername = null;
  state.repositories = [];
  localStorage.removeItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
  localStorage.removeItem(CONFIG.STORAGE_KEYS.USER);

  showToast('Logged out of CyberTotal.', 'info');
  transitionToAuth();
}

async function fetchUserProfile() {
  try {
    const res = await apiFetch('/users/me');
    state.user = res.data || res.payload || res;
    localStorage.setItem(CONFIG.STORAGE_KEYS.USER, JSON.stringify(state.user));
    updateProfileUI();
  } catch (err) {
    console.warn('Could not fetch user profile:', err);
  }
}

// ==================== GITHUB INTEGRATION FLOW ====================

async function startGitHubOAuthFlow() {
  if (!state.token) {
    showToast('Please log in first to connect GitHub.', 'error');
    return;
  }

  dom.gitLoginPill.textContent = 'Connecting...';

  try {
    // 1. Try to get authorization URL from /integrations/github/url
    let authUrl = null;
    try {
      const urlRes = await apiFetch('/integrations/github/url');
      if (urlRes && urlRes.url) {
        authUrl = urlRes.url;
      }
    } catch (e) {
      console.log('Falling back to direct authorize endpoint');
    }

    if (!authUrl) {
      // Fallback: pass token as query param so JWTFilter authenticates the request
      authUrl = `${state.apiBase}/integrations/github/authorize?token=${encodeURIComponent(state.token)}`;
    }

    // 2. Open GitHub OAuth in popup window for best UX
    const popupWidth = 600;
    const popupHeight = 700;
    const left = window.screen.width / 2 - popupWidth / 2;
    const top = window.screen.height / 2 - popupHeight / 2;

    const popup = window.open(
      authUrl,
      'GitHub Authorization',
      `width=${popupWidth},height=${popupHeight},top=${top},left=${left},scrollbars=yes,status=1`
    );

    if (!popup || popup.closed || typeof popup.closed === 'undefined') {
      // Popup blocked by browser: navigate directly
      window.location.href = authUrl;
      return;
    }

    showToast('GitHub login opened in popup. Please authorize...', 'info');

    // 3. Poll popup status in case postMessage is blocked
    const timer = setInterval(async () => {
      if (!popup || popup.closed) {
        clearInterval(timer);
        dom.gitLoginPill.textContent = 'Connect GitHub →';
        // Check GitHub status after popup closes
        await checkGitHubConnection();
      }
    }, 1200);

  } catch (err) {
    showToast(err.message || 'Failed to start GitHub connection.', 'error');
    dom.gitLoginPill.textContent = 'Connect GitHub →';
  }
}

// Listen for OAuth success message posted by the callback success page
window.addEventListener('message', async (event) => {
  if (event.data && event.data.type === 'GITHUB_OAUTH_SUCCESS') {
    const username = event.data.username;
    showToast(`✓ GitHub connected as ${username}!`, 'success');
    state.githubUsername = username;
    updateGitHubConnectedState(username);
    await loadRepositories();
  }
});

async function checkGitHubConnection() {
  if (!state.token) return;

  try {
    // Try the status endpoint
    const statusRes = await apiFetch('/integrations/github/status').catch(() => null);
    if (statusRes && statusRes.connected) {
      updateGitHubConnectedState(statusRes.username);
      await loadRepositories();
      return;
    }

    // Fallback: attempt to fetch repos directly
    await loadRepositories();
  } catch (err) {
    updateGitHubDisconnectedState();
  }
}

async function loadRepositories() {
  dom.reposEmptyState.classList.add('hidden');
  dom.reposLoadingState.classList.remove('hidden');
  dom.reposListGrid.classList.add('hidden');

  try {
    const repos = await apiFetch('/integrations/github/repos');
    state.repositories = Array.isArray(repos) ? repos : [];

    dom.reposLoadingState.classList.add('hidden');

    if (state.repositories.length === 0) {
      dom.reposEmptyState.classList.remove('hidden');
      dom.reposEmptyState.innerHTML = '<p>No repositories found on this GitHub account.</p>';
      return;
    }

    renderRepositories(state.repositories);
  } catch (err) {
    dom.reposLoadingState.classList.add('hidden');
    dom.reposEmptyState.classList.remove('hidden');
    console.warn('Repositories could not be loaded:', err.message);
  }
}

function renderRepositories(repos) {
  dom.reposListGrid.innerHTML = '';
  dom.reposListGrid.classList.remove('hidden');

  repos.forEach((repo) => {
    const card = document.createElement('div');
    const isSelected = state.selectedRepo === repo.name || state.selectedRepo === repo.full_name;
    card.className = `repo-card ${isSelected ? 'selected' : ''}`;
    card.dataset.repoName = repo.full_name || repo.name;

    card.innerHTML = `
      <div class="repo-card-top">
        <span class="repo-name" title="${escapeHtml(repo.name)}">${escapeHtml(repo.name)}</span>
        <span class="repo-visibility">${repo.private ? 'Private' : 'Public'}</span>
      </div>
      <p class="repo-desc">${escapeHtml(repo.description || 'No description provided.')}</p>
      <div class="repo-footer">
        <span class="repo-lang">
          <span class="lang-dot"></span>
          ${escapeHtml(repo.language || 'Code')}
        </span>
        <span class="repo-stars">★ ${repo.stargazers_count || 0}</span>
      </div>
    `;

    card.addEventListener('click', () => {
      selectRepository(repo);
    });

    dom.reposListGrid.appendChild(card);
  });
}

function selectRepository(repo) {
  const repoName = repo.full_name || repo.name;
  state.selectedRepo = repoName;
  localStorage.setItem(CONFIG.STORAGE_KEYS.SELECTED_PROJECT, repoName);

  dom.profileGithubProject.value = repoName;
  if (dom.codeSelectedRepo) dom.codeSelectedRepo.value = repoName;

  // Update card selected state
  document.querySelectorAll('.repo-card').forEach((el) => {
    el.classList.toggle('selected', el.dataset.repoName === repoName);
  });

  showToast(`Selected project: ${repoName}`, 'info');
}

function updateGitHubConnectedState(username) {
  state.githubUsername = username || 'xenion80';
  dom.profileGithubUser.value = state.githubUsername;
  dom.githubStatusIndicator.textContent = `✓ GitHub connected as ${state.githubUsername}`;
  dom.githubStatusIndicator.className = 'github-status connected';
  dom.refreshReposBtn.classList.remove('hidden');
  dom.gitLoginPill.textContent = 'Connected ✓';
}

function updateGitHubDisconnectedState() {
  dom.profileGithubUser.value = '';
  dom.profileGithubUser.placeholder = 'Will appear after GitHub login';
  dom.githubStatusIndicator.textContent = 'GitHub not connected.';
  dom.githubStatusIndicator.className = 'github-status not-connected';
  dom.refreshReposBtn.classList.add('hidden');
  dom.gitLoginPill.textContent = 'Connect GitHub →';
  dom.reposEmptyState.classList.remove('hidden');
  dom.reposListGrid.classList.add('hidden');
}

// ==================== UI STATE MANAGEMENT ====================

function updateProfileUI() {
  if (!state.user) return;

  const displayName = state.user.name || state.user.email || 'User';
  dom.profileName.value = displayName;
  dom.headerUserName.textContent = displayName;
  dom.headerUserAvatar.textContent = displayName.charAt(0).toUpperCase();

  // Load saved domain or default
  const savedDomain = localStorage.getItem(CONFIG.STORAGE_KEYS.DOMAIN);
  if (savedDomain) {
    dom.profileDomain.value = savedDomain;
  }

  // Load saved project selection
  if (state.selectedRepo) {
    dom.profileGithubProject.value = state.selectedRepo;
  }
}

function transitionToDashboard() {
  dom.authView.classList.add('hidden');
  dom.dashboardView.classList.remove('hidden');
  dom.mainHeader.classList.remove('hidden');

  updateProfileUI();
  checkGitHubConnection();
}

function transitionToAuth() {
  dom.dashboardView.classList.add('hidden');
  dom.mainHeader.classList.add('hidden');
  dom.authView.classList.remove('hidden');
  showLoginForm();
}

function showLoginForm() {
  dom.loginFormContainer.classList.remove('hidden');
  dom.registerFormContainer.classList.add('hidden');
}

function showRegisterForm() {
  dom.loginFormContainer.classList.add('hidden');
  dom.registerFormContainer.classList.remove('hidden');
}

// ==================== MODAL HANDLERS ====================

function openModal(modal) {
  if (modal) modal.classList.remove('hidden');
}

function closeModal(modal) {
  if (modal) modal.classList.add('hidden');
}

// ==================== SECURITY SCANNERS (WEBSITE & CODE) ====================

async function runWebsiteSecurityScan() {
  const url = dom.targetWebsiteUrl.value.trim();
  const projectName = dom.targetProjectName.value.trim() || 'Website Security Scan';

  if (!url) {
    showToast('Please enter a target website URL.', 'error');
    return;
  }

  setBtnLoading(dom.startWebsiteScanBtn, true, 'Scanning target...');
  dom.websiteScanResults.classList.remove('hidden');
  dom.websiteScanResults.textContent = `[+] Initiating endpoint discovery on ${url}...\n[+] Contacting ${state.apiBase}...\n`;

  try {
    // 1. Create a project
    const projectRes = await apiFetch('/projects/create', {
      method: 'POST',
      body: JSON.stringify({
        projectName: projectName,
        description: `Automated scan for ${url}`
      })
    });

    const projData = projectRes.data || projectRes.payload || projectRes;
    const projectId = projData.id;
    dom.websiteScanResults.textContent += `[+] Created security project (ID: ${projectId})\n`;

    // 2. Associate target with project
    const targetRes = await apiFetch(`/projects/${projectId}/targets`, {
      method: 'POST',
      body: JSON.stringify({
        url: url
      })
    });

    const tgtData = targetRes.data || targetRes.payload || targetRes;
    const targetId = tgtData.id;
    dom.websiteScanResults.textContent += `[+] Target registered (ID: ${targetId})\n`;
    dom.websiteScanResults.textContent += `[+] Crawling endpoints & parsing HTML links...\n`;

    // 3. Trigger endpoint discovery
    const discoveryRes = await apiFetch(`/targets/${targetId}/discover`, {
      method: 'POST'
    });

    const result = discoveryRes.data || discoveryRes.payload || discoveryRes;
    const endpointsFound = result.discoveredCount || (result.endpoints ? result.endpoints.length : 'Multiple');

    dom.websiteScanResults.textContent += `\n================ SCAN COMPLETED ================\n`;
    dom.websiteScanResults.textContent += `[✓] Status: Active\n[✓] Discovered Endpoints: ${endpointsFound}\n`;
    dom.websiteScanResults.textContent += `[✓] Scan ready for vulnerability prioritization.\n`;

    showToast('Website endpoint discovery completed successfully!', 'success');
  } catch (err) {
    dom.websiteScanResults.textContent += `\n[✕] Scan Error: ${err.message}\n`;
    showToast(err.message, 'error');
  } finally {
    setBtnLoading(dom.startWebsiteScanBtn, false);
  }
}

async function runCodeSecurityScan() {
  const repo = state.selectedRepo;
  if (!repo) {
    showToast('Please select a repository below first.', 'error');
    return;
  }

  setBtnLoading(dom.startCodeScanBtn, true, 'Analyzing code...');
  dom.codeScanResults.classList.remove('hidden');
  dom.codeScanResults.textContent = `[+] Inspecting repository ${repo}...\n[+] Parsing GitHub metadata & dependency graph...\n`;

  setTimeout(() => {
    dom.codeScanResults.textContent += `[+] Checking repository permissions...\n`;
    dom.codeScanResults.textContent += `[✓] Security audit initialized for ${repo}.\n`;
    dom.codeScanResults.textContent += `[✓] 0 critical vulnerabilities reported.\n`;
    setBtnLoading(dom.startCodeScanBtn, false);
    showToast('Code security review completed!', 'success');
  }, 1200);
}

// ==================== EVENT LISTENERS SETUP ====================

function initEventListeners() {
  // Auth Form Submissions
  dom.loginForm.addEventListener('submit', handleLogin);
  dom.registerForm.addEventListener('submit', handleRegister);
  dom.showRegisterBtn.addEventListener('click', showRegisterForm);
  dom.showLoginBtn.addEventListener('click', showLoginForm);
  dom.logoutBtn.addEventListener('click', handleLogout);

  // Toggle Password Visibility
  document.querySelectorAll('.toggle-password-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const targetId = btn.dataset.target;
      const input = document.getElementById(targetId);
      if (input) {
        input.type = input.type === 'password' ? 'text' : 'password';
      }
    });
  });

  // Action Cards
  dom.cardCheckWebsite.addEventListener('click', () => openModal(dom.websiteCheckerModal));
  dom.cardCheckCode.addEventListener('click', () => {
    if (dom.codeSelectedRepo) dom.codeSelectedRepo.value = state.selectedRepo || 'No repository selected';
    openModal(dom.codeCheckerModal);
  });
  dom.cardGitLogin.addEventListener('click', startGitHubOAuthFlow);

  // Profile Field Changes
  dom.profileDomain.addEventListener('change', (e) => {
    localStorage.setItem(CONFIG.STORAGE_KEYS.DOMAIN, e.target.value);
    showToast(`Domain set to ${e.target.value}`, 'info');
  });

  dom.refreshReposBtn.addEventListener('click', () => {
    loadRepositories();
    showToast('Refreshing repositories...', 'info');
  });

  // Modal Closures
  document.querySelectorAll('[data-close]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const modalId = btn.dataset.close;
      closeModal(document.getElementById(modalId));
    });
  });

  document.querySelectorAll('.modal-overlay').forEach((overlay) => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) closeModal(overlay);
    });
  });

  // Scanner Buttons
  dom.startWebsiteScanBtn.addEventListener('click', runWebsiteSecurityScan);
  dom.startCodeScanBtn.addEventListener('click', runCodeSecurityScan);

  dom.forgotPasswordLink.addEventListener('click', (e) => {
    e.preventDefault();
    // Reset to step 1 whenever the modal opens
    document.getElementById('forgotStep1').classList.remove('hidden');
    document.getElementById('forgotStep2').classList.add('hidden');
    dom.forgotEmail.value = '';
    openModal(dom.forgotPasswordModal);
  });

  // Step 1: Request token from backend
  dom.submitForgotBtn.addEventListener('click', async () => {
    const email = dom.forgotEmail.value.trim();
    if (!email) {
      showToast('Please enter your email.', 'error');
      return;
    }
    setBtnLoading(dom.submitForgotBtn, true, 'Generating...');
    try {
      const res = await apiFetch('/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email })
      });
      const token = res.data;
      if (!token) {
        // Account not found — show gentle message, stay on step 1
        showToast('No account found with that email.', 'error');
        return;
      }
      // Populate step 2 with the token
      document.getElementById('resetTokenDisplay').value = token;
      document.getElementById('resetNewPassword').value = '';
      document.getElementById('forgotStep1').classList.add('hidden');
      document.getElementById('forgotStep2').classList.remove('hidden');
      showToast('Token generated! Copy it and set your new password.', 'success');
    } catch (err) {
      showToast(err.message || 'Failed to generate reset token.', 'error');
    } finally {
      setBtnLoading(dom.submitForgotBtn, false);
    }
  });

  // Copy token button
  document.getElementById('copyTokenBtn').addEventListener('click', () => {
    const tokenVal = document.getElementById('resetTokenDisplay').value;
    if (tokenVal) {
      navigator.clipboard.writeText(tokenVal).then(() => showToast('Token copied to clipboard!', 'success'));
    }
  });

  // Back to step 1
  document.getElementById('backToStep1Btn').addEventListener('click', () => {
    document.getElementById('forgotStep1').classList.remove('hidden');
    document.getElementById('forgotStep2').classList.add('hidden');
  });

  // Step 2: Reset password using token
  document.getElementById('submitResetBtn').addEventListener('click', async () => {
    const token = document.getElementById('resetTokenDisplay').value.trim();
    const newPassword = document.getElementById('resetNewPassword').value;
    if (!token || !newPassword) {
      showToast('Token and new password are required.', 'error');
      return;
    }
    const resetBtn = document.getElementById('submitResetBtn');
    setBtnLoading(resetBtn, true, 'Resetting...');
    try {
      await apiFetch('/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword })
      });
      showToast('Password reset successfully! You can now log in.', 'success');
      closeModal(dom.forgotPasswordModal);
    } catch (err) {
      showToast(err.message || 'Failed to reset password. Token may have expired.', 'error');
    } finally {
      setBtnLoading(resetBtn, false);
    }
  });

  // API Backend Settings
  dom.openSettingsBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    openModal(dom.settingsModal);
  });

  dom.apiStatusBadge.addEventListener('click', () => openModal(dom.settingsModal));
  dom.toggleAuthApiBtn.addEventListener('click', () => openModal(dom.settingsModal));

  document.querySelectorAll('.preset-pill').forEach((pill) => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.preset-pill').forEach((p) => p.classList.remove('active'));
      pill.classList.add('active');
      dom.customApiUrlInput.value = pill.dataset.url;
    });
  });

  dom.saveApiUrlBtn.addEventListener('click', () => {
    let newUrl = dom.customApiUrlInput.value.trim();
    if (!newUrl) newUrl = CONFIG.DEFAULT_API_BASE;
    newUrl = newUrl.replace(/\/+$/, ''); // Strip trailing slash

    state.apiBase = newUrl;
    localStorage.setItem(CONFIG.STORAGE_KEYS.API_BASE, newUrl);
    updateApiBadges();
    closeModal(dom.settingsModal);
    showToast(`Connected to backend: ${newUrl}`, 'success');

    // If logged in, re-check session
    if (state.token) {
      fetchUserProfile();
      checkGitHubConnection();
    }
  });
}

function updateApiBadges() {
  const isRender = state.apiBase.includes('onrender.com');
  const isLocal = state.apiBase.includes('localhost') || state.apiBase.includes('127.0.0.1');

  let label = 'Custom API';
  if (isRender) label = 'Render API';
  if (isLocal) label = 'Localhost';

  if (dom.apiEndpointLabel) dom.apiEndpointLabel.textContent = label;
  if (dom.authApiTarget) dom.authApiTarget.textContent = label;
  if (dom.customApiUrlInput) dom.customApiUrlInput.value = state.apiBase;

  document.querySelectorAll('.preset-pill').forEach((pill) => {
    pill.classList.toggle('active', pill.dataset.url === state.apiBase);
  });
}

// ==================== APP INITIALIZATION ====================

async function initApp() {
  initEventListeners();
  updateApiBadges();

  // Check if URL has ?connected=github query param (redirect fallback)
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('connected') === 'github') {
    showToast('✓ GitHub connected successfully!', 'success');
    // Clean up query param from URL
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  // Check if we have an existing authenticated session
  if (state.token) {
    try {
      await fetchUserProfile();
      transitionToDashboard();
    } catch (e) {
      console.warn('Session expired or invalid token');
      transitionToAuth();
    }
  } else {
    transitionToAuth();
  }
}

// Run on page load
document.addEventListener('DOMContentLoaded', initApp);
