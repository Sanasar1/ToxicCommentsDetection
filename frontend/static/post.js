(function () {
  const { Api, Auth, escapeHtml, timeAgo, renderPostCard, attachPostHandlers } = window.Civil;

  const params = new URLSearchParams(location.search);
  const postId = Number(params.get('id'));
  if (!postId) { document.body.innerHTML = '<p class="error">Missing post id</p>'; return; }

  const postContainer = document.getElementById('post-container');
  const commentForm = document.getElementById('comment-form');
  const commentStatus = document.getElementById('comment-status');
  const commentsList = document.getElementById('comments-list');
  const commentsEmpty = document.getElementById('comments-empty');

  if (Auth.token()) commentForm.classList.remove('hidden');

  function renderComment(c) {
    return `
      <div class="comment">
        <div class="header">
          <a class="author" href="/profile.html?u=${encodeURIComponent(c.author.username)}">${escapeHtml(c.author.displayName)}</a>
          <span class="muted">@${escapeHtml(c.author.username)}</span>
          <span class="time">${timeAgo(c.createdAt)}</span>
        </div>
        <div class="content">${escapeHtml(c.content)}</div>
      </div>
    `;
  }

  async function loadPost() {
    try {
      const post = await Api.getPost(postId);
      postContainer.innerHTML = renderPostCard(post);
      attachPostHandlers(postContainer);
    } catch (err) {
      postContainer.innerHTML = `<p class="error">${err.message}</p>`;
    }
  }

  async function loadComments() {
    try {
      const items = await Api.listComments(postId);
      if (items.length === 0) {
        commentsEmpty.classList.remove('hidden');
        commentsList.innerHTML = '';
        return;
      }
      commentsEmpty.classList.add('hidden');
      commentsList.innerHTML = items.map(renderComment).join('');
    } catch (err) {
      commentsList.innerHTML = `<p class="error">${err.message}</p>`;
    }
  }

  commentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const content = commentForm.elements.content.value.trim();
    if (!content) return;
    commentStatus.className = 'status';
    commentStatus.textContent = '';
    try {
      const c = await Api.addComment(postId, content);
      commentForm.elements.content.value = '';
      commentsEmpty.classList.add('hidden');
      commentsList.insertAdjacentHTML('beforeend', renderComment(c));
      commentStatus.className = 'status success';
      commentStatus.textContent = 'Posted.';
    } catch (err) {
      commentStatus.className = 'status error';
      commentStatus.textContent = err.message;
    }
  });

  loadPost();
  loadComments();
})();
