(function () {
  const API_BASE = (window.__API_BASE__ || '/api/v1');
  const TOKEN_KEY = 'civil.token';
  const USER_KEY = 'civil.user';

  const Auth = {
    token() { return localStorage.getItem(TOKEN_KEY); },
    user() {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    },
    set(token, user) {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    },
    clear() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    },
    require(redirect = '/login.html') {
      if (!this.token()) {
        window.location.href = redirect;
        return false;
      }
      return true;
    }
  };

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = Auth.token();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
      const err = new Error((data && data.error) || `HTTP ${res.status}`);
      err.status = res.status;
      err.payload = data;
      throw err;
    }
    return data;
  }

  const Api = {
    register: (body) => request('POST', '/auth/register', body),
    login: (body) => request('POST', '/auth/login', body),
    me: () => request('GET', '/auth/me'),

    feed: (limit = 20, before) => {
      const qs = new URLSearchParams({ limit });
      if (before) qs.set('before', before);
      return request('GET', `/feed?${qs}`);
    },

    createPost: (content) => request('POST', '/posts', { content }),
    getPost: (id) => request('GET', `/posts/${id}`),
    deletePost: (id) => request('DELETE', `/posts/${id}`),
    likePost: (id) => request('POST', `/posts/${id}/like`),
    unlikePost: (id) => request('DELETE', `/posts/${id}/like`),
    listComments: (id) => request('GET', `/posts/${id}/comments`),
    addComment: (id, content) => request('POST', `/posts/${id}/comments`, { content }),

    profile: (username) => request('GET', `/users/${username}`),
    userPosts: (username, limit = 20, offset = 0) =>
      request('GET', `/users/${username}/posts?limit=${limit}&offset=${offset}`),
    updateMe: (body) => request('PUT', '/users/me', body),
    follow: (username) => request('POST', `/users/${username}/follow`),
    unfollow: (username) => request('DELETE', `/users/${username}/follow`),
  };

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  function timeAgo(iso) {
    const t = new Date(iso).getTime();
    const diff = Math.max(0, Date.now() - t) / 1000;
    if (diff < 60) return 'just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    if (diff < 604800) return `${Math.floor(diff / 86400)}d`;
    return new Date(iso).toLocaleDateString();
  }

  function renderPostCard(post, opts = {}) {
    const me = Auth.user();
    const ownsPost = me && me.id === post.author.id;
    const linkAuthor = `/profile.html?u=${encodeURIComponent(post.author.username)}`;
    const linkPost = `/post.html?id=${post.id}`;
    return `
      <article class="post" data-post-id="${post.id}">
        <div class="header">
          <a class="author" href="${linkAuthor}">${escapeHtml(post.author.displayName)}</a>
          <span class="username">@${escapeHtml(post.author.username)}</span>
          <a class="time" href="${linkPost}">${timeAgo(post.createdAt)}</a>
        </div>
        <a href="${linkPost}" style="color:inherit;text-decoration:none;">
          <div class="content">${escapeHtml(post.content)}</div>
        </a>
        <div class="actions">
          <button class="like-btn ${post.likedByMe ? 'liked' : ''}" data-action="toggle-like">
            ${post.likedByMe ? '♥' : '♡'} <span class="like-count">${post.likesCount}</span>
          </button>
          <a class="comment-link" href="${linkPost}">
            <button type="button">💬 <span>${post.commentsCount}</span></button>
          </a>
          ${ownsPost ? `<button class="delete-btn" data-action="delete">Delete</button>` : ''}
        </div>
      </article>
    `;
  }

  function attachPostHandlers(root) {
    root.querySelectorAll('.post').forEach(node => {
      const postId = Number(node.dataset.postId);
      node.querySelector('[data-action="toggle-like"]')?.addEventListener('click', async (e) => {
        if (!Auth.require()) return;
        const btn = e.currentTarget;
        const liked = btn.classList.contains('liked');
        try {
          const res = liked ? await Api.unlikePost(postId) : await Api.likePost(postId);
          btn.classList.toggle('liked', res.likedByMe);
          btn.querySelector('.like-count').textContent = res.likesCount;
          btn.firstChild.textContent = res.likedByMe ? '♥ ' : '♡ ';
        } catch (err) {
          alert(err.message);
        }
      });
      node.querySelector('[data-action="delete"]')?.addEventListener('click', async () => {
        if (!confirm('Delete this post?')) return;
        try {
          await Api.deletePost(postId);
          node.remove();
        } catch (err) {
          alert(err.message);
        }
      });
    });
  }

  function renderNav() {
    const nav = document.getElementById('nav');
    if (!nav) return;
    const me = Auth.user();
    const right = me
      ? `<a class="me-link" href="/profile.html?u=${encodeURIComponent(me.username)}">@${escapeHtml(me.username)}</a>
         <a href="#" id="nav-logout">Log out</a>`
      : `<a href="/login.html">Log in</a> <a href="/register.html">Register</a>`;
    nav.innerHTML = `
      <a class="brand" href="/">Civil</a>
      <a href="/">Feed</a>
      <span class="spacer"></span>
      ${right}
    `;
    document.getElementById('nav-logout')?.addEventListener('click', (e) => {
      e.preventDefault();
      Auth.clear();
      window.location.href = '/login.html';
    });
  }

  document.addEventListener('DOMContentLoaded', renderNav);

  window.Civil = { Api, Auth, escapeHtml, timeAgo, renderPostCard, attachPostHandlers, renderNav };
})();
