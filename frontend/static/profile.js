(function () {
  const { Api, Auth, escapeHtml, renderPostCard, attachPostHandlers } = window.Civil;

  const params = new URLSearchParams(location.search);
  const username = params.get('u');
  if (!username) { document.body.innerHTML = '<p class="error">Missing username</p>'; return; }

  const headerEl = document.getElementById('profile-header');
  const statsEl = document.getElementById('profile-stats');
  const bioEl = document.getElementById('profile-bio');
  const followBtn = document.getElementById('follow-btn');
  const editBtn = document.getElementById('edit-btn');
  const editForm = document.getElementById('edit-form');
  const editCancel = document.getElementById('edit-cancel');
  const userPosts = document.getElementById('user-posts');
  const postsEmpty = document.getElementById('user-posts-empty');

  let profile = null;

  function renderProfile() {
    headerEl.innerHTML = `
      <h2 style="margin-bottom:0.25rem;">${escapeHtml(profile.user.displayName)}</h2>
      <div class="muted">@${escapeHtml(profile.user.username)}</div>
    `;
    statsEl.innerHTML = `
      <div><strong>${profile.postsCount}</strong> posts</div>
      <div><strong>${profile.followersCount}</strong> followers</div>
      <div><strong>${profile.followingCount}</strong> following</div>
    `;
    bioEl.textContent = profile.user.bio || '';

    if (profile.isMe) {
      editBtn.classList.remove('hidden');
      followBtn.classList.add('hidden');
    } else if (Auth.token()) {
      followBtn.classList.remove('hidden');
      followBtn.textContent = profile.isFollowing ? 'Unfollow' : 'Follow';
      followBtn.classList.toggle('secondary', profile.isFollowing);
    }
  }

  followBtn.addEventListener('click', async () => {
    if (!Auth.require()) return;
    try {
      if (profile.isFollowing) {
        await Api.unfollow(username);
        profile.isFollowing = false;
        profile.followersCount = Math.max(0, profile.followersCount - 1);
      } else {
        await Api.follow(username);
        profile.isFollowing = true;
        profile.followersCount += 1;
      }
      renderProfile();
    } catch (err) { alert(err.message); }
  });

  editBtn.addEventListener('click', () => {
    editForm.classList.remove('hidden');
    editForm.elements.displayName.value = profile.user.displayName;
    editForm.elements.bio.value = profile.user.bio || '';
  });
  editCancel.addEventListener('click', () => editForm.classList.add('hidden'));
  editForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(editForm));
    try {
      const updated = await Api.updateMe(data);
      profile.user = updated;
      const me = Auth.user();
      Auth.set(Auth.token(), { ...me, displayName: updated.displayName, bio: updated.bio });
      editForm.classList.add('hidden');
      renderProfile();
    } catch (err) { alert(err.message); }
  });

  async function loadPosts() {
    try {
      const posts = await Api.userPosts(username, 50, 0);
      if (posts.length === 0) {
        postsEmpty.classList.remove('hidden');
        return;
      }
      userPosts.innerHTML = posts.map(renderPostCard).join('');
      attachPostHandlers(userPosts);
    } catch (err) {
      userPosts.innerHTML = `<p class="error">${err.message}</p>`;
    }
  }

  (async function init() {
    try {
      profile = await Api.profile(username);
      renderProfile();
      await loadPosts();
    } catch (err) {
      document.querySelector('main').innerHTML = `<p class="error">${err.message}</p>`;
    }
  })();
})();
