(function () {
  const { Api, Auth } = window.Civil;

  if (Auth.token() && (location.pathname.endsWith('/login.html') || location.pathname.endsWith('/register.html'))) {
    window.location.href = '/';
    return;
  }

  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    const errorEl = document.getElementById('login-error');
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      errorEl.textContent = '';
      const data = Object.fromEntries(new FormData(loginForm));
      try {
        const res = await Api.login(data);
        Auth.set(res.token, res.user);
        window.location.href = '/';
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  const registerForm = document.getElementById('register-form');
  if (registerForm) {
    const errorEl = document.getElementById('register-error');
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      errorEl.textContent = '';
      const data = Object.fromEntries(new FormData(registerForm));
      try {
        const res = await Api.register(data);
        Auth.set(res.token, res.user);
        window.location.href = '/';
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }
})();
