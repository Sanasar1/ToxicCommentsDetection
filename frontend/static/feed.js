(function () {
  const { Api, Auth, renderPostCard, attachPostHandlers } = window.Civil;

  const composerSection = document.getElementById('composer-section');
  const composerText = document.getElementById('composer-text');
  const composerSubmit = document.getElementById('composer-submit');
  const composerStatus = document.getElementById('composer-status');
  const list = document.getElementById('feed-list');
  const empty = document.getElementById('feed-empty');
  const loadMore = document.getElementById('feed-load-more');

  let oldestEpoch = null;

  if (!Auth.token()) {
    composerSection.classList.add('hidden');
    list.innerHTML = `<p class="muted">You need to <a href="/login.html">log in</a> to see your feed.</p>`;
    return;
  }
  composerSection.classList.remove('hidden');

  async function loadFeed(append = false) {
    try {
      const posts = await Api.feed(20, append ? oldestEpoch : undefined);
      if (!append) list.innerHTML = '';
      if (posts.length === 0) {
        if (!append && list.childElementCount === 0) empty.classList.remove('hidden');
        loadMore.classList.add('hidden');
        return;
      }
      empty.classList.add('hidden');
      list.insertAdjacentHTML('beforeend', posts.map(renderPostCard).join(''));
      attachPostHandlers(list);
      const last = posts[posts.length - 1];
      oldestEpoch = Math.floor(new Date(last.createdAt).getTime() / 1000);
      loadMore.classList.toggle('hidden', posts.length < 20);
    } catch (err) {
      list.innerHTML = `<p class="error">${err.message}</p>`;
    }
  }

  composerSubmit.addEventListener('click', async () => {
    const content = composerText.value.trim();
    if (!content) return;
    composerSubmit.disabled = true;
    composerStatus.className = 'status';
    composerStatus.textContent = '';
    try {
      const post = await Api.createPost(content);
      composerText.value = '';
      list.insertAdjacentHTML('afterbegin', renderPostCard(post));
      attachPostHandlers(list);
      empty.classList.add('hidden');
      composerStatus.className = 'status success';
      composerStatus.textContent = 'Posted.';
    } catch (err) {
      composerStatus.className = 'status error';
      composerStatus.textContent = err.message;
    } finally {
      composerSubmit.disabled = false;
    }
  });

  loadMore.addEventListener('click', () => loadFeed(true));

  loadFeed();
})();
