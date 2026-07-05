// 詳細設計書 Part1 §1: 重み付きダメラウ・レーベンシュタイン距離による類似度計算
// バックエンド(SimilarityCalculator.java)と同一ロジック。フロントでは即時表示のみに使用し、
// 正誤・習熟度の確定はサーバー側の再計算を正とする。
(function (global) {
  const INSERT_DELETE_COST = 1.0;
  const SUBSTITUTION_COST = 1.0;
  const RL_SUBSTITUTION_COST = 0.3;
  const TRANSPOSITION_COST = 0.5;

  function substitutionCost(a, b) {
    if ((a === 'r' && b === 'l') || (a === 'l' && b === 'r')) return RL_SUBSTITUTION_COST;
    return SUBSTITUTION_COST;
  }

  function weightedDamerauLevenshtein(a, b) {
    const n = a.length, m = b.length;
    const d = Array.from({ length: n + 1 }, () => new Array(m + 1).fill(0));
    for (let i = 0; i <= n; i++) d[i][0] = i * INSERT_DELETE_COST;
    for (let j = 0; j <= m; j++) d[0][j] = j * INSERT_DELETE_COST;

    for (let i = 1; i <= n; i++) {
      const ca = a[i - 1];
      for (let j = 1; j <= m; j++) {
        const cb = b[j - 1];
        const subCost = ca === cb ? 0 : substitutionCost(ca, cb);
        let value = Math.min(
          d[i - 1][j] + INSERT_DELETE_COST,
          d[i][j - 1] + INSERT_DELETE_COST,
          d[i - 1][j - 1] + subCost
        );
        if (i > 1 && j > 1 && ca !== cb && ca === b[j - 2] && a[i - 2] === cb) {
          value = Math.min(value, d[i - 2][j - 2] + TRANSPOSITION_COST);
        }
        d[i][j] = value;
      }
    }
    return d[n][m];
  }

  function calculateSimilarity(input, correct) {
    const a = (input || '').toLowerCase();
    const b = correct.toLowerCase();
    const distance = weightedDamerauLevenshtein(a, b);
    const ratio = 1 - distance / b.length;
    return Math.round(Math.max(0, ratio) * 100);
  }

  global.calculateSimilarity = calculateSimilarity;
})(window);
