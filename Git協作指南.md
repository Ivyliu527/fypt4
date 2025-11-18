# Git 協作指南 - 如何合併同學的更改

## 步驟 1：拉取最新的更改

在開始工作前，先從 GitHub 拉取最新的更改：

```bash
# 1. 查看當前狀態
git status

# 2. 如果有未提交的更改，先提交或暫存
git add .
git commit -m "我的更改描述"

# 3. 拉取遠程倉庫的最新更改
git pull origin main
```

## 步驟 2：處理合併衝突

如果 Git 提示有衝突（conflict），需要手動解決：

### 情況 A：沒有衝突（自動合併成功）
- Git 會自動合併更改
- 直接繼續工作即可

### 情況 B：有衝突（需要手動解決）

1. **查看衝突文件**
```bash
git status
# 會顯示哪些文件有衝突，例如：
# both modified: app/src/main/java/com/example/tonbo_app/EnvironmentActivity.java
```

2. **打開衝突文件**
- 在 Android Studio 中打開有衝突的文件
- 會看到類似這樣的標記：
```
<<<<<<< HEAD
你的更改
=======
同學的更改
>>>>>>> origin/main
```

3. **解決衝突**
- 選擇保留哪個版本，或合併兩個版本
- 刪除衝突標記（`<<<<<<<`, `=======`, `>>>>>>>`）
- 確保代碼邏輯正確

4. **標記衝突已解決**
```bash
# 添加解決後的文件
git add 文件名

# 完成合併
git commit -m "合併同學的更改"
```

## 步驟 3：推送到 GitHub

```bash
git push origin main
```

### ⚠️ 重要：Git 的推送保護機制

**好消息：** Git 有內建保護機制，**不會讓同學直接覆蓋你的更改**！

#### 如果同學直接推送會發生什麼？

如果同學在你推送之後直接執行 `git push origin main`，Git 會**拒絕推送**並提示：

```
! [rejected]        main -> main (fetch first)
error: failed to push some refs to 'https://github.com/...'
hint: Updates were rejected because the remote contains work that you do
hint: not have locally. This is usually caused by another repository pushing
hint: to the same ref. You may want to first integrate the remote changes
hint: (e.g., 'git pull ...') before pushing again.
```

**這意味著：**
- ✅ Git 保護了你的更改，不會被直接覆蓋
- ✅ 同學必須先拉取你的更改，然後才能推送

#### 正確的流程（Git 會強制執行）

同學**必須**按照以下順序操作：

```bash
# 1. 先拉取你的最新更改（必須！）
git pull origin main

# 2. 如果有衝突，解決衝突

# 3. 然後才能推送
git push origin main
```

**如果同學跳過步驟 1，Git 會拒絕推送！**

## 最佳實踐建議

### 1. 工作前先拉取
```bash
# 每天開始工作前
git pull origin main
```

### 2. 頻繁提交和推送
```bash
# 完成一個功能後立即提交
git add .
git commit -m "完成功能描述"
git push origin main
```

### 3. 分工明確
- 不同的人負責不同的文件或模組
- 減少同時修改同一個文件的機會

### 4. 使用分支（進階）
```bash
# 創建自己的分支
git checkout -b 你的名字-功能名稱

# 在自己的分支上工作
# 完成後合併到 main
git checkout main
git merge 你的名字-功能名稱
git push origin main
```

## 常見問題解決

### 問題 1：拉取時提示本地有未提交的更改
**解決方法：**
```bash
# 選項 A：先提交你的更改
git add .
git commit -m "我的更改"
git pull origin main

# 選項 B：暫存你的更改（暫時保存）
git stash
git pull origin main
git stash pop  # 恢復你的更改
```

### 問題 2：推送被拒絕（這是正常的保護機制！）

**這是 Git 的保護機制，防止覆蓋別人的更改！**

當你看到這個錯誤：
```
! [rejected]        main -> main (fetch first)
error: failed to push some refs
```

**解決方法：**
```bash
# 1. 先拉取最新更改（必須！）
git pull origin main

# 2. 如果有衝突，解決衝突（見上方"處理合併衝突"）

# 3. 解決衝突後，標記為已解決
git add 衝突的文件

# 4. 完成合併
git commit -m "合併更改"

# 5. 然後才能推送
git push origin main
```

**記住：** 推送被拒絕是**好事**，說明 Git 正在保護別人的工作！

### 問題 3：不知道如何解決衝突
**解決方法：**
1. 在 Android Studio 中，衝突文件會有提示
2. 點擊 "Accept Yours"（保留你的）或 "Accept Theirs"（保留同學的）
3. 或手動編輯合併兩個版本

## 示例流程

假設同學修改了 `EnvironmentActivity.java`，你也修改了同一個文件：

```bash
# 1. 拉取最新更改
git pull origin main

# 2. 如果有衝突，Android Studio 會提示
# 3. 在 Android Studio 中解決衝突
# 4. 標記為已解決
git add app/src/main/java/com/example/tonbo_app/EnvironmentActivity.java

# 5. 完成合併
git commit -m "合併同學的 EnvironmentActivity 更改"

# 6. 推送
git push origin main
```

## 注意事項

⚠️ **重要：**
- 永遠不要強制推送（`git push --force`），除非你確定要覆蓋別人的更改
- 合併前先備份你的工作
- 如果衝突太多，可以聯繫同學協商分工

## ⚠️ 關於 Cursor Account 和 GitHub Account

### 重要區別

**Cursor Account（編輯器帳戶）** 和 **GitHub Account（代碼倉庫帳戶）** 是**完全分開**的：

- ✅ **Cursor Account**：用於編輯器功能（AI 助手、設置同步等）
- ✅ **GitHub Account**：用於代碼版本控制和協作
- ✅ **Git 配置**：是**本地配置**，每個人在自己的電腦上設置

### 即使使用同一個 Cursor Account

即使你和同學使用同一個 Cursor account，**Git 和 GitHub 的配置仍然是獨立的**：

1. **Git 用戶信息是本地配置**：
   - 每個人在自己的電腦上設置
   - 不會因為 Cursor account 而共享
   - 每個人的 commit 會顯示不同的作者

2. **GitHub 帳戶可以不同**：
   - 每個人可以使用自己的 GitHub 帳戶
   - 或者使用同一個 GitHub 帳戶（不推薦，見下方）

## ⚠️ 使用同一個 GitHub 帳戶的問題

### 問題 1：無法區分誰做了什麼
- 所有 commit 都會顯示同一個作者
- 無法追蹤是誰做了哪些更改
- 出問題時難以追責

### 問題 2：容易互相覆蓋更改
- 如果兩個人同時推送，後推送的會覆蓋先推送的
- 容易丟失工作成果

### 問題 3：無法使用 Pull Request 審查
- 無法進行代碼審查
- 無法討論更改

## ✅ 推薦解決方案

### 方案 1：每個人都創建自己的 GitHub 帳戶（強烈推薦）

**優點：**
- ✅ 可以清楚看到誰做了什麼
- ✅ 可以使用 Pull Request 進行代碼審查
- ✅ 可以設置不同的權限
- ✅ 更專業的協作方式

**步驟：**
1. 每個同學創建自己的 GitHub 帳戶（免費）
2. 你將同學添加為協作者（Collaborator）：
   - 進入 GitHub 倉庫頁面
   - 點擊 "Settings" → "Collaborators"
   - 添加同學的 GitHub 用戶名
3. 同學在自己的電腦上設置 Git：
```bash
git config --global user.name "同學的名字"
git config --global user.email "同學的GitHub郵箱"
```

### 方案 2：如果必須使用同一個帳戶

**注意事項：**
1. **嚴格遵守流程：**
   ```bash
   # 工作前必須先拉取
   git pull origin main
   
   # 完成後立即推送
   git add .
   git commit -m "你的名字: 更改描述"
   git push origin main
   ```

2. **在 commit 訊息中標註名字：**
   ```bash
   git commit -m "張三: 修復環境識別問題"
   git commit -m "李四: 添加新功能"
   ```

3. **使用分支（推薦）：**
   ```bash
   # 每個人創建自己的分支
   git checkout -b 張三-功能名稱
   # 在自己的分支上工作
   git push origin 張三-功能名稱
   # 完成後合併到 main
   ```

4. **頻繁溝通：**
   - 修改前先通知其他人
   - 避免同時修改同一個文件
   - 完成後立即推送

### 方案 3：使用組織帳戶（適合團隊項目）

1. 創建 GitHub 組織（Organization）
2. 每個同學加入組織
3. 在組織下創建倉庫
4. 每個人都用自己的帳戶協作

## 設置 Git 用戶信息

**重要：** 即使使用同一個 Cursor account，每個人都需要在自己的電腦上設置 Git 用戶信息。

### 每個人在自己的電腦上設置：

```bash
# 設置你的名字（會顯示在 commit 中）
git config --global user.name "你的名字"

# 設置你的郵箱（使用你自己的郵箱，可以是 GitHub 郵箱）
git config --global user.email "your-email@example.com"

# 查看當前設置
git config --global --list
```

### 示例：

**同學 A 的電腦：**
```bash
git config --global user.name "張三"
git config --global user.email "zhangsan@example.com"
```

**同學 B 的電腦：**
```bash
git config --global user.name "李四"
git config --global user.email "lisi@example.com"
```

這樣，即使使用同一個 Cursor account，每個人的 commit 都會顯示不同的作者信息。

### 驗證設置：

```bash
# 查看當前 Git 配置
git config --global user.name
git config --global user.email

# 查看所有配置
git config --global --list
```

## 最佳實踐總結

✅ **推薦做法：**
1. 每個人都使用自己的 GitHub 帳戶
2. 使用分支進行開發
3. 使用 Pull Request 進行代碼審查
4. 頻繁提交和推送
5. **推送前先拉取**（`git pull origin main`）

❌ **不推薦：**
1. 多人共用一個 GitHub 帳戶
2. 直接在主分支上工作
3. 長時間不推送更改
4. **強制推送覆蓋別人的更改**（`git push --force`）
5. 跳過拉取直接推送

## 🔒 Git 的保護機制說明

### Git 如何防止覆蓋？

1. **推送前檢查：**
   - Git 會檢查遠程倉庫是否有你本地沒有的新 commit
   - 如果有，Git 會拒絕推送

2. **必須先合併：**
   - 你必須先拉取（`git pull`）合併遠程的更改
   - 解決衝突後才能推送

3. **強制推送的危險：**
   - `git push --force` 可以強制覆蓋（非常危險！）
   - 只有在確定要丟棄遠程更改時才使用
   - **團隊協作時永遠不要使用！**

## ⚠️ 如果文件被覆蓋了怎麼辦？

### 情況：同學使用了 `git push --force` 覆蓋了你的更改

**好消息：** Git 有歷史記錄，可以恢復！

### 方法 1：從本地恢復（如果你還沒拉取）

```bash
# 1. 查看你的本地 commit 歷史
git log --oneline

# 2. 找到被覆蓋前的 commit（記下 commit ID）
# 例如：abc1234 你的更改描述

# 3. 恢復到那個 commit
git reset --hard abc1234

# 4. 強制推送恢復（只有在你確定要恢復時才用）
git push --force origin main
```

### 方法 2：從 GitHub 恢復（推薦）

1. **在 GitHub 網頁上：**
   - 進入倉庫：`https://github.com/Charlieppy2/Tonbo_App`
   - 點擊 "Commits" 查看所有 commit 歷史
   - 找到被覆蓋前的 commit
   - 點擊那個 commit
   - 點擊 "Browse files" 查看文件
   - 可以手動複製文件內容恢復

2. **或使用 Git 命令恢復：**
```bash
# 1. 查看遠程的所有 commit（包括被覆蓋的）
git fetch origin

# 2. 查看 reflog（引用日誌，記錄所有操作）
git reflog

# 3. 找到被覆蓋前的 commit ID
# 4. 恢復到那個 commit
git reset --hard <commit-id>

# 5. 強制推送恢復
git push --force origin main
```

### 方法 3：創建新分支保存你的工作

```bash
# 1. 創建一個備份分支（在拉取前）
git branch backup-你的名字-日期

# 2. 這樣即使 main 被覆蓋，你的工作還在備份分支中
# 3. 之後可以從備份分支恢復
```

## 🛡️ 如何防止被覆蓋？

### 1. 設置分支保護規則（GitHub）

在 GitHub 倉庫設置中：
1. 進入 "Settings" → "Branches"
2. 添加 "Branch protection rule" 保護 `main` 分支
3. 勾選：
   - ✅ "Require pull request reviews before merging"
   - ✅ "Require status checks to pass before merging"
   - ✅ **"Do not allow force pushes"**（最重要！）
   - ✅ "Do not allow deletions"

這樣即使同學嘗試 `git push --force`，GitHub 也會拒絕！

### 2. 使用分支工作流程

```bash
# 每個人創建自己的分支
git checkout -b 你的名字-功能名稱

# 在自己的分支上工作
git add .
git commit -m "你的更改"
git push origin 你的名字-功能名稱

# 完成後創建 Pull Request 合併到 main
# 這樣可以審查更改，避免直接覆蓋
```

### 3. 頻繁備份

```bash
# 每次推送前創建備份分支
git branch backup-$(date +%Y%m%d-%H%M%S)
git push origin backup-$(date +%Y%m%d-%H%M%S)
```

### 4. 定期拉取和推送

```bash
# 每天開始工作前
git pull origin main

# 完成工作後立即推送
git push origin main
```

## 📋 團隊協作規則（重要！）

### 必須遵守的規則：

1. ❌ **永遠不要使用 `git push --force`**
2. ✅ **推送前必須先拉取**：`git pull origin main`
3. ✅ **使用分支工作**：不要直接在 main 分支上工作
4. ✅ **頻繁溝通**：修改前通知其他人
5. ✅ **設置分支保護**：在 GitHub 上保護 main 分支

### 實際場景示例

**場景：你推送了更改，同學也想推送**

1. **你推送成功：**
   ```bash
   git push origin main
   # ✅ 成功推送到 GitHub
   ```

2. **同學嘗試推送（沒有先拉取）：**
   ```bash
   git push origin main
   # ❌ 被拒絕！Git 提示需要先拉取
   ```

3. **同學必須先拉取：**
   ```bash
   git pull origin main
   # ✅ 拉取你的更改
   # 如果有衝突，解決衝突
   git push origin main
   # ✅ 現在可以推送了
   ```

**結果：** 你的更改不會被覆蓋，兩個人的更改都會保留！

