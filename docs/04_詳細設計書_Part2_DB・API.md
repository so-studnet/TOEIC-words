# 詳細設計書 Part 2:DB・API仕様 — 英単語穴埋めクイズWebアプリ

- 版数:v1.1
- 作成日:2026-07-05(v1.1改訂:2026-07-06)
- 前提ドキュメント:要件定義書 v1.3 / 基本設計書 v1.0 / 詳細設計書 Part1 v1.0

---

## 1. テーブルDDL(SQLite)

```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE words (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    word       TEXT NOT NULL UNIQUE,
    level      INTEGER NOT NULL CHECK (level IN (400, 600, 800, 900)),
    definition TEXT NOT NULL,
    example    TEXT NOT NULL,   -- 対象単語部分は "_____" で保持
    synonyms   TEXT,            -- カンマ区切り(初期版)
    image_url  TEXT
);
CREATE INDEX idx_words_level ON words(level);

CREATE TABLE user_word_mastery (
    user_id           INTEGER NOT NULL,
    word_id           INTEGER NOT NULL,
    mastery           INTEGER NOT NULL DEFAULT 0 CHECK (mastery BETWEEN 0 AND 100),
    attempts          INTEGER NOT NULL DEFAULT 0,
    correct_count     INTEGER NOT NULL DEFAULT 0,
    hints_used_total  INTEGER NOT NULL DEFAULT 0,
    last_studied      TEXT,
    PRIMARY KEY (user_id, word_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE
);
```

補足:
- `words.word` はフロントエンドにも返却する(発音ヒントはブラウザの Web Speech API に単語テキストを渡して読み上げるため、フロント側で単語文字列を保持する必要があり、隠しても意味がないため)。
- 正誤判定・習熟度計算は基本設計書どおりバックエンドで再計算し、フロントの申告を信用しない。
- `word` にUNIQUE制約を付与し、単語マスタ投入処理(後述)を冪等にする。

## 1-2. 単語マスタデータの投入方式

単語マスタ(200語)はアプリケーション本体と切り離したJSONファイルではなく、`backend/src/main/resources/data.sql` にSQL(`INSERT OR IGNORE INTO words (...) VALUES (...);`)として同梱し、Spring Bootの初期化データ機構(`spring.jpa.defer-datasource-initialization=true` + `data.sql`自動実行)によってアプリ起動のたびにSQLiteへ直接投入する。

- Hibernateが`words`テーブルを作成した**後**に`data.sql`が実行されるよう`spring.jpa.defer-datasource-initialization=true`を設定する。
- SQLiteは組み込みDB(H2等)として自動検出されないため、`spring.sql.init.mode=always`を明示して`data.sql`を毎回実行させる。
- `word`列のUNIQUE制約と`INSERT OR IGNORE`の組み合わせにより、起動の都度実行しても既存行は上書きされず、未投入の新規語のみが追加される(冪等)。
- 単語マスタの追加・修正は`data.sql`を直接編集し、Gitでレビュー・履歴管理する。JSON経由の読み込み処理(旧`WordDataSeeder`)は廃止した。

## 2. API リクエスト/レスポンス仕様

共通事項:
- 認証はセッションCookie(Spring Security)。認証必須APIは未ログイン時に `401` を返す。
- エラーレスポンス共通形式:`{ "error": { "code": "STRING", "message": "STRING" } }`

### 2.1 POST /api/auth/register

Request
```json
{ "email": "user@example.com", "password": "password123" }
```

Response(201)
```json
{ "id": 1, "email": "user@example.com" }
```

エラー:`400 EMAIL_INVALID` / `400 PASSWORD_TOO_SHORT` / `409 EMAIL_ALREADY_EXISTS`

### 2.2 POST /api/auth/login

Request
```json
{ "email": "user@example.com", "password": "password123" }
```

Response(200) — セッションCookieを発行
```json
{ "id": 1, "email": "user@example.com" }
```

エラー:`401 INVALID_CREDENTIALS`

### 2.3 POST /api/auth/logout

Request:なし(Cookieのセッションを破棄)
Response(204):本文なし

### 2.4 GET /api/words?level=600

Response(200)
```json
{
  "level": 600,
  "words": [
    {
      "id": 101,
      "word": "comfortable",
      "definition": "giving a pleasant feeling of relaxation",
      "example": "He felt _____ sitting in the new office chair.",
      "synonyms": ["cozy", "relaxed", "at ease"],
      "imageUrl": "/images/words/comfortable.png"
    }
  ]
}
```

備考:
- `synonyms` はDBではカンマ区切り文字列だが、APIレスポンスでは配列に変換して返す。
- 1回の呼び出しで**該当レベルの単語からランダムに10件抽出**して返す(1クイズセッション分)。該当レベルの単語が10件未満の場合は存在する分すべてを返す。

### 2.5 POST /api/quiz/answer

Request
```json
{
  "wordId": 101,
  "answer": "comfortabre",
  "hintsUsed": ["definition", "shuffle"]
}
```

`hintsUsed` の値は `"definition" | "synonyms" | "pronunciation" | "shuffle" | "image"` のいずれか(使用した種類のみ、順不同・重複なし)。

Response(200)
```json
{
  "correct": false,
  "similarityPercent": 95,
  "correctWord": "comfortable",
  "masteryBefore": 32,
  "masteryAfter": 27,
  "masteryDelta": -5
}
```

エラー:`404 WORD_NOT_FOUND` / `400 INVALID_HINT_KEY`

### 2.6 GET /api/mastery?level=600(levelは任意フィルタ)

Response(200)
```json
{
  "words": [
    {
      "wordId": 101,
      "word": "comfortable",
      "level": 600,
      "mastery": 27,
      "rank": "うろ覚え",
      "attempts": 3,
      "correctCount": 1,
      "hintsUsedTotal": 4,
      "lastStudied": "2026-07-05T10:15:00Z"
    }
  ]
}
```

`rank` はPart1 §2.5のランク表示ロジックをバックエンドで適用して返す(80〜100:マスター / 40〜79:学習中 / 1〜39:うろ覚え / 0:未学習)。

## 3. 決定事項(確認済み)

- パスワードのバリデーションは**8文字以上のみ**(文字種混在は必須としない)
- 1回のクイズセッションの出題数は**10問**固定(該当レベルの単語からランダム抽出。詳細はPart3 §2.3)
