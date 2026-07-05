(function () {
  const ERROR_MESSAGES = {
    EMAIL_INVALID: 'メールアドレスの形式が正しくありません。',
    PASSWORD_TOO_SHORT: 'パスワードは8文字以上で入力してください。',
    EMAIL_ALREADY_EXISTS: 'このメールアドレスは既に登録されています。',
    INVALID_CREDENTIALS: 'メールアドレスまたはパスワードが正しくありません。',
    WORD_NOT_FOUND: '単語が見つかりませんでした。',
    INVALID_HINT_KEY: 'ヒントの指定が不正です。',
    VALIDATION_ERROR: '入力内容を確認してください。',
    UNAUTHENTICATED: 'ログインが必要です。',
    UNKNOWN_ERROR: '通信エラーが発生しました。もう一度お試しください。'
  };

  function errorMessage(err) {
    return ERROR_MESSAGES[err && err.code] || ERROR_MESSAGES.UNKNOWN_ERROR;
  }

  function showScreen(id) {
    document.querySelectorAll('.screen').forEach((el) => el.classList.remove('active'));
    document.getElementById(id).classList.add('active');
  }

  // ---- S0: ログイン / 新規登録 ----
  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach((b) => b.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach((p) => p.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById(`${btn.dataset.tab}-form`).classList.add('active');
    });
  });

  document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    errorEl.textContent = '';
    try {
      await api.login(email, password);
      showScreen('screen-home');
    } catch (err) {
      errorEl.textContent = errorMessage(err);
    }
  });

  document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const confirm = document.getElementById('register-password-confirm').value;
    const errorEl = document.getElementById('register-error');
    errorEl.textContent = '';
    if (password !== confirm) {
      errorEl.textContent = 'パスワードが一致しません。';
      return;
    }
    try {
      await api.register(email, password);
      await api.login(email, password);
      showScreen('screen-home');
    } catch (err) {
      errorEl.textContent = errorMessage(err);
    }
  });

  document.getElementById('logout-btn').addEventListener('click', async () => {
    await api.logout();
    showScreen('screen-login');
  });

  // ---- S1: ホーム / レベル選択 ----
  let sessionWords = [];
  let currentIndex = 0;
  let currentWord = null;
  let hintsUsed = new Set();

  document.querySelectorAll('.level-btn').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const level = btn.dataset.level;
      try {
        const data = await api.getWords(level);
        if (!data.words || data.words.length === 0) {
          alert('このレベルの単語はまだ準備中です。');
          return;
        }
        sessionWords = data.words;
        currentIndex = 0;
        showScreen('screen-quiz');
        startQuestion();
      } catch (err) {
        alert(errorMessage(err));
      }
    });
  });

  document.getElementById('goto-mastery-btn').addEventListener('click', () => {
    showScreen('screen-mastery');
    loadMasteryList();
  });

  document.getElementById('back-home-btn').addEventListener('click', () => {
    showScreen('screen-home');
  });

  // ---- S2: クイズ画面 ----
  function startQuestion() {
    currentWord = sessionWords[currentIndex];
    hintsUsed = new Set();
    document.getElementById('quiz-progress').textContent = `${currentIndex + 1}問目/${sessionWords.length}問`;
    document.getElementById('quiz-hint-count').textContent = `ヒント使用:0/5`;
    document.getElementById('quiz-example').textContent = currentWord.example;
    document.getElementById('quiz-answer').value = '';
    document.getElementById('quiz-error').textContent = '';
    document.getElementById('hint-content').innerHTML = '';
    document.querySelectorAll('.hint-btn').forEach((b) => b.classList.remove('used'));
    document.getElementById('result-overlay').classList.add('hidden');
  }

  document.querySelectorAll('.hint-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const key = btn.dataset.hint;
      hintsUsed.add(key);
      btn.classList.add('used');
      document.getElementById('quiz-hint-count').textContent = `ヒント使用:${hintsUsed.size}/5`;
      renderHint(key);
    });
  });

  function renderHint(key) {
    const container = document.getElementById('hint-content');
    const box = document.createElement('div');
    box.className = 'hint-item';
    switch (key) {
      case 'definition':
        box.textContent = `定義: ${currentWord.definition}`;
        break;
      case 'synonyms':
        box.textContent = `類義語: ${currentWord.synonyms.join(', ')}`;
        break;
      case 'pronunciation':
        box.textContent = '発音を再生しました。';
        if ('speechSynthesis' in window) {
          const utter = new SpeechSynthesisUtterance(currentWord.word);
          utter.lang = 'en-US';
          window.speechSynthesis.speak(utter);
        }
        break;
      case 'shuffle': {
        const letters = currentWord.word.split('');
        for (let i = letters.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [letters[i], letters[j]] = [letters[j], letters[i]];
        }
        box.textContent = `シャッフル文字: ${letters.join(' ').toUpperCase()}`;
        break;
      }
      case 'image':
        box.innerHTML = '';
        if (currentWord.imageUrl) {
          const img = document.createElement('img');
          img.src = currentWord.imageUrl;
          img.alt = 'イメージイラスト';
          box.appendChild(img);
        } else {
          box.textContent = 'イラストは準備中です。';
        }
        break;
    }
    // 同じ種類のヒントを再度開いたときは表示を差し替える
    const existing = container.querySelector(`[data-hint-box="${key}"]`);
    box.setAttribute('data-hint-box', key);
    if (existing) {
      existing.replaceWith(box);
    } else {
      container.appendChild(box);
    }
  }

  document.getElementById('quiz-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const answerInput = document.getElementById('quiz-answer');
    const answer = answerInput.value.trim();
    const errorEl = document.getElementById('quiz-error');
    errorEl.textContent = '';
    if (!answer) {
      errorEl.textContent = '解答を入力してください。';
      return;
    }

    const localSimilarity = calculateSimilarity(answer, currentWord.word);
    showResult({
      correct: localSimilarity === 100,
      similarityPercent: localSimilarity,
      correctWord: currentWord.word,
      masteryBefore: null,
      masteryAfter: null,
      pending: true
    });

    try {
      const res = await api.submitAnswer(currentWord.id, answer, hintsUsed);
      showResult({ ...res, pending: false });
    } catch (err) {
      document.getElementById('result-mastery').textContent = errorMessage(err);
    }
  });

  function similarityLabel(percent) {
    if (percent === 100) return '正解!';
    if (percent >= 90) return 'おしい!';
    if (percent >= 70) return '惜しい';
    if (percent >= 40) return 'もう少し';
    return 'ちがいます';
  }

  function showResult(result) {
    document.getElementById('result-overlay').classList.remove('hidden');
    document.getElementById('result-title').textContent = result.correct ? '正解!' : '不正解';
    document.getElementById('result-input').textContent = `入力: ${document.getElementById('quiz-answer').value}`;
    document.getElementById('result-correct-word').textContent = result.correct ? '' : `正解: ${result.correctWord}`;
    document.getElementById('result-similarity').textContent =
      `類似度: ${result.similarityPercent}%(${similarityLabel(result.similarityPercent)})`;
    document.getElementById('result-mastery').textContent = result.pending
      ? '習熟度を更新中...'
      : `習熟度 ${result.masteryBefore} → ${result.masteryAfter}(${result.masteryDelta >= 0 ? '+' : ''}${result.masteryDelta})`;
  }

  document.getElementById('next-btn').addEventListener('click', () => {
    currentIndex += 1;
    if (currentIndex < sessionWords.length) {
      startQuestion();
    } else {
      showScreen('screen-home');
    }
  });

  // ---- S4: 単語一覧 / 習熟度 ----
  const rankClass = {
    'マスター': 'rank-master',
    '学習中': 'rank-learning',
    'うろ覚え': 'rank-vague',
    '未学習': 'rank-new'
  };

  async function loadMasteryList() {
    const level = document.getElementById('mastery-level-filter').value;
    const sort = document.getElementById('mastery-sort').value;
    const listEl = document.getElementById('mastery-list');
    listEl.textContent = '読み込み中...';
    try {
      const data = await api.getMastery(level);
      let words = data.words;
      if (sort === 'mastery-asc') words = words.slice().sort((a, b) => a.mastery - b.mastery);
      else if (sort === 'alpha') words = words.slice().sort((a, b) => a.word.localeCompare(b.word));
      else if (sort === 'recent') words = words.slice().sort((a, b) => (b.lastStudied || '').localeCompare(a.lastStudied || ''));

      listEl.innerHTML = '';
      words.forEach((w) => {
        const row = document.createElement('div');
        row.className = 'mastery-row';
        row.innerHTML = `
          <span>${w.word}(Lv.${w.level})</span>
          <span class="mastery-rank ${rankClass[w.rank] || ''}">${w.rank} ${w.mastery}</span>
        `;
        const detail = document.createElement('div');
        detail.className = 'mastery-detail hidden';
        detail.innerHTML = `
          <p>定義: ${w.definition}</p>
          <p>類義語: ${w.synonyms.join(', ')}</p>
          <p>例文: ${w.example}</p>
          <p>挑戦回数: ${w.attempts} / 正解回数: ${w.correctCount} / ヒント使用累計: ${w.hintsUsedTotal}</p>
        `;
        row.addEventListener('click', () => detail.classList.toggle('hidden'));
        listEl.appendChild(row);
        listEl.appendChild(detail);
      });
    } catch (err) {
      listEl.textContent = errorMessage(err);
    }
  }

  document.getElementById('mastery-level-filter').addEventListener('change', loadMasteryList);
  document.getElementById('mastery-sort').addEventListener('change', loadMasteryList);

  showScreen('screen-login');
})();
