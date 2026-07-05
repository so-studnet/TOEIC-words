(function (global) {
  async function request(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'same-origin'
    });
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      const code = data && data.error ? data.error.code : 'UNKNOWN_ERROR';
      const message = data && data.error ? data.error.message : 'エラーが発生しました';
      const err = new Error(message);
      err.code = code;
      throw err;
    }
    return data;
  }

  global.api = {
    register: (email, password) => request('POST', '/api/auth/register', { email, password }),
    login: (email, password) => request('POST', '/api/auth/login', { email, password }),
    logout: () => request('POST', '/api/auth/logout'),
    getWords: (level) => request('GET', `/api/words?level=${level}`),
    submitAnswer: (wordId, answer, hintsUsed) =>
      request('POST', '/api/quiz/answer', { wordId, answer, hintsUsed: Array.from(hintsUsed) }),
    getMastery: (level) => request('GET', `/api/mastery${level ? `?level=${level}` : ''}`)
  };
})(window);
