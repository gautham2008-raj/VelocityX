const AuthEngine = {
  currentUser: null,

  async checkSession() {
    try {
      const res = await fetch('/api/auth/me');
      const data = await res.json();
      if (data.authenticated) {
        this.currentUser = data;
        this.updateNavUi();
        return data;
      }
    } catch (e) {
      console.warn('[AuthEngine] Session check failed:', e);
    }
    this.currentUser = null;
    this.updateNavUi();
    return null;
  },

  async login(email, password, isAdmin = false) {
    const endpoint = isAdmin ? '/api/auth/admin-login' : '/api/auth/login';
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.error || 'Authentication failed');
    }
    await this.checkSession();
    return data;
  },

  async register(fullName, email, password, phone, role = 'CUSTOMER', adminSecret = '') {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName,
        email,
        password,
        phone,
        role,
        adminSecret
      })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.error || 'Registration failed');
    }
    await this.checkSession();
    return data;
  },

  async logout() {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      this.currentUser = null;
      window.location.href = 'index.html';
    }
  },

  updateNavUi() {
    const navLinks = document.getElementById('navLinks');
    const authNavRegister = document.getElementById('authNavRegister');
    const authNavLogin = document.getElementById('authNavLogin');
    const myRentalsNav = document.getElementById('myRentalsNavItem');

    if (!navLinks) return;

    if (this.currentUser) {
      if (myRentalsNav) {
        myRentalsNav.style.display = 'inline-block';
      }
      if (authNavRegister) {
        authNavRegister.style.display = 'none';
      }
      if (authNavLogin) {
        const isAdmin = this.currentUser.role === 'ADMIN';
        authNavLogin.innerHTML = `
          <div style="display:flex; align-items:center; gap:0.75rem;">
            ${isAdmin ? `
              <a href="admin-dashboard.html" class="btn btn-outline" style="border-color:var(--accent-magenta); color:var(--accent-magenta); padding:0.35rem 0.75rem; font-size:0.8rem;">
                Admin Center
              </a>
            ` : ''}
            <span style="color:var(--accent-cyan); font-weight:600; font-size:0.9rem;">
              ${this.escapeHtml(this.currentUser.name)}
            </span>
            <button onclick="AuthEngine.logout()" class="btn btn-outline" style="padding:0.35rem 0.75rem; font-size:0.8rem;">
              Sign Out
            </button>
          </div>
        `;
      }
    } else {
      if (myRentalsNav) {
        myRentalsNav.style.display = 'none';
      }
      if (authNavRegister) {
        authNavRegister.style.display = 'inline-block';
      }
      if (authNavLogin) {
        authNavLogin.innerHTML = `<a href="login.html" class="btn btn-outline">Sign In</a>`;
      }
    }
  },

  escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag));
  }
};

document.addEventListener('DOMContentLoaded', () => {
  AuthEngine.checkSession();
});