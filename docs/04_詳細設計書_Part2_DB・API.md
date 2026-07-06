# 詳細設計書 Part 2:DB・API仕様 — 英単語穴埋めクイズWebアプリ

- 版数:v1.5
- 作成日:2026-07-05(v1.1改訂:2026-07-06 / v1.2改訂:2026-07-06 FSRS復習サイクル追加 / v1.3改訂:2026-07-06 次回復習日・件数サマリーAPI追加 / v1.4改訂:2026-07-06 解答結果レスポンスをratingLabel/dueAtに変更 / v1.5改訂:2026-07-06 Anki評価の基準をヒント量に修正・出題優先度を3段階化)
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
    fsrs_state        TEXT,    -- LEARNING / REVIEW / RELEARNING(NULL=未レビュー)
    fsrs_step         INTEGER, -- 学習/再学習ステップ番号(REVIEW中はNULL)
    fsrs_stability    REAL,    -- FSRSの記憶安定度(日数)
    fsrs_difficulty   REAL,    -- FSRSの難易度(1〜10)
    due_at            TEXT,    -- 次回復習予定日時(ISO8601)。NULL=未レビュー(常に出題対象)
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

## 1-3. 復習サイクル(FSRS)

Anki互換の間隔反復スケジューリングとして、公式Javaライブラリ [`io.github.open-spaced-repetition:fsrs`](https://github.com/open-spaced-repetition) をMaven依存として採用する(デフォルトの重みパラメータを使用。自前での数式移植は行わない)。

- 解答のたびに、**今回使ったヒントのペナルティ合計**(累積の習熟度スコアではない)を Part1 §2.6 のルールでAnki評価(Again/Hard/Good/Easy)に変換する
- `user_word_mastery`の`fsrs_state` / `fsrs_step` / `fsrs_stability` / `fsrs_difficulty` / `due_at`をFSRSライブラリの`Card`オブジェクトへ復元し、`Scheduler.reviewCard(card, rating)`を呼び出して更新後の値を保存する(数式自体はライブラリ内部で完結)
- 初回解答時(該当行が存在しない、または`fsrs_state`がNULL)は、ライブラリのデフォルト新規カードとして扱われる
- `GET /api/words`(§2.4)の出題選定では、`due_at`が現在時刻を過ぎている単語(Learning/Review)を最優先し、次に未学習の新規単語、最後に復習期限内の単語で埋める3段階の優先度で選出する
- 実装は`backend/.../service/FsrsScheduler.java`(ヒント量→評価変換、Card⇔DBフィールドの相互変換、New/Learning/Review/期限内の分類)

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
- 認証必須(ログインユーザーの`user_word_mastery`を参照するため)。
- 出題選定は1-3節のとおり3段階の優先度で**合計10件**を選ぶ(1クイズセッション分):①`due_at`を過ぎているLearning/Review状態の単語(復習期限切れ)、②一度も学習していない新規単語、③復習期限内の単語。各段階内はランダム抽出し、上位の段階だけで10件に満たない場合に次の段階から補う。該当レベルの単語が10件未満の場合は存在する分すべてを返す。

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
  "ratingLabel": "HARD",
  "dueAt": "2026-07-07T10:15:00Z"
}
```

備考:
- 習熟度スコア自体(0〜100)はサーバー内部で引き続き計算・保存されるが(Part1 §2.1〜2.5)、レスポンスとしては返さない。フロントに返すのは、**今回の解答で使ったヒント量**から導出したAnki評価(`ratingLabel`:`AGAIN`/`HARD`/`GOOD`/`EASY`、Part1 §2.6)と、それによって更新された次回復習予定日時(`dueAt`)のみ。
- 数値の増減(旧`masteryBefore`/`masteryAfter`/`masteryDelta`)は廃止した。単語ごとの累計スコアはS4の単語一覧(Part2 §2.6)で確認する。

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
      "lastStudied": "2026-07-05T10:15:00Z",
      "dueAt": "2026-07-06T10:15:00Z"
    }
  ]
}
```

`rank` はPart1 §2.5のランク表示ロジックをバックエンドで適用して返す(80〜100:マスター / 40〜79:学習中 / 1〜39:うろ覚え / 0:未学習)。
`dueAt` は1-3節のFSRS次回復習予定日時(未レビューの場合はNULL=常に復習可能)。フロントエンドの単語一覧画面(S4)で「次回復習日」として表示する。

### 2.7 GET /api/review-summary

認証必須。全レベルについて、現在復習可能(=出題対象)な単語数をNew/Learning/Reviewの3区分で集計して返す。ホーム画面(S1)のレベルボタンに表示するバッジ用。

Response(200)
```json
{
  "levels": [
    { "level": 400, "newCount": 49, "learningCount": 2, "reviewCount": 5 },
    { "level": 600, "newCount": 50, "learningCount": 0, "reviewCount": 0 },
    { "level": 800, "newCount": 50, "learningCount": 0, "reviewCount": 0 },
    { "level": 900, "newCount": 50, "learningCount": 0, "reviewCount": 0 }
  ]
}
```

区分の定義(Part1 §2.6・FsrsSchedulerの`DueCategory`と対応):
- `newCount`: 一度もレビューしていない単語(`fsrs_state`がNULL)
- `learningCount`: `fsrs_state`がLEARNING/RELEARNINGで、かつ`due_at`が現在時刻以前(=復習可能)
- `reviewCount`: `fsrs_state`がREVIEWで、かつ`due_at`が現在時刻以前(=復習可能)
- `due_at`が現在時刻より未来の単語(まだ復習期限が来ていない)はどの区分にも含めない

## 3. 決定事項(確認済み)

- パスワードのバリデーションは**8文字以上のみ**(文字種混在は必須としない)
- 1回のクイズセッションの出題数は**10問**固定(該当レベルの単語からランダム抽出。詳細はPart3 §2.3)
