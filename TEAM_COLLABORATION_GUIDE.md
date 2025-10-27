# 👥 團隊協作指南

## 📋 **核心原則**

### ✅ **永遠：同步 → 修改 → 同步 → 推送**

```bash
同步遠端 → 修改代碼 → 再同步一次 → 推送到遠端
```

---

## 🚀 **標準協作流程**

### **步驟 1: 開始工作前 - 同步遠端代碼**

```bash
# 在 GitHub Desktop：
1. 點擊 "Fetch origin" 獲取最新的遠端代碼
2. 如果有更新，點擊 "Pull origin" 合併到本地
3. 檢查是否有衝突（Conflicts）

# 或使用命令：
git pull origin main
```

### **步驟 2: 工作時 - 只在本地修改**

- ✅ 在本地修改文件
- ✅ 測試你的更改
- ✅ 寫有價值的 commit 信息

### **步驟 3: 推送前 - 再次同步**

```bash
# 在推送前，再次同步遠端代碼
git pull origin main
```

### **步驟 4: 解決合併（如果需要）**

如果 Git 提示需要合併：

**情況 A：無衝突（自動合併）**
```bash
Git 會自動創建一個 "Merge branch 'main'..." commit
✅ 這是正常的，直接推送即可
```

**情況 B：有衝突（需要手動解決）**
```bash
Git 會告訴你哪些文件有衝突：
- 打開有衝突的文件
- 選擇保留你的代碼、保留遠端的代碼、或整合兩者
- 在 GitHub Desktop 點擊 "Mark as resolved"
- Commit 解決方案
```

### **步驟 5: 推送到遠端**

```bash
git push origin main
```

---

## ⚠️ **常見問題和解決方案**

### **問題 1: "Merge branch 'main'" 合併提交**

**原因：**
- 你和組員同時修改了不同文件
- Git 需要將兩個分支合併

**解決：**
```bash
✅ 這是正常的！Git 正在合併你們的工作
- 無衝突：讓 Git 自動合併即可
- 有衝突：手動解決衝突後推送
```

**避免產生合併提交的方法（進階）：**
```bash
# 使用 rebase 而不是 merge
git pull --rebase origin main
git push origin main

# 這樣可以保持提交歷史更乾淨
```

---

### **問題 2: 衝突（Conflicts）**

**症狀：**
```
Auto-merging some-file.java
CONFLICT (content): Merge conflict in some-file.java
```

**解決步驟：**
1. **打開衝突文件**
   ```java
   <<<<<<< HEAD
   // 你的代碼
   =======
   // 組員的代碼
   >>>>>>> branch 'main'
   ```

2. **選擇保留哪些代碼**
   - 如果兩個都要：整合它們
   - 如果只要自己的：保留 `<<<<<<< HEAD` 到 `=======` 之間
   - 如果只要組員的：保留 `=======` 到 `>>>>>>>` 之間

3. **刪除衝突標記**
   - 刪除 `<<<<<<<`、`=======`、`>>>>>>>` 這些標記

4. **標記為已解決**
   - GitHub Desktop：點擊 "Mark as resolved"
   - 命令行：`git add <文件名>`

5. **完成合併**
   - GitHub Desktop：點擊 "Commit merge"
   - 命令行：`git commit`（會自動填寫信息）

---

### **問題 3: 推送到被拒絕**

**錯誤：**
```
! [rejected]        main -> main (fetch first)
error: failed to push some refs
```

**原因：**
- 遠端有你沒有的新 commit

**解決：**
```bash
# 1. 同步遠端代碼
git pull origin main

# 2. 解決可能的衝突
# ...（見上文）

# 3. 再次推送
git push origin main
```

---

## 📊 **理想的工作流程**

```mermaid
A 的電腦                  GitHub                 B 的電腦
---------                                    -----------
   |                                            |
   | 1. Pull                                   |
   |---------------------------------------->  |
   | 2. Pull                                  |
   |<----------------------------------------  |
   |                                            |
   | 3. 修改代碼                               |
   | 4. Commit                                 |
   |                                            |
   | 5. Push                                   |
   |---> 推送到 GitHub                         |
                                            |
                                            | 6. Pull
                                            |<--從 GitHub 拉取
                                            |
                                            | 7. 修改代碼
                                            | 8. Commit
                                            |
                                            | 9. Push
                                            |--> 推送到 GitHub
```

---

## 🎯 **最佳實踐**

### ✅ **推薦做法**

1. **經常同步**
   ```bash
   # 每天開始工作前
   git pull origin main
   
   # 每完成一個功能
   git pull origin main
   git add .
   git commit -m "feat: 完成 XX 功能"
   git push origin main
   ```

2. **使用功能分支（進階）**
   ```bash
   # 創建功能分支
   git checkout -b feature/新功能名稱
   
   # 在分支上工作
   # ... 修改代碼 ...
   
   # 完成後合併到 main
   git checkout main
   git merge feature/新功能名稱
   git push origin main
   ```

3. **寫清楚的 commit 信息**
   ```bash
   # ❌ 不好的
   git commit -m "修改"
   
   # ✅ 好的
   git commit -m "fix: 修復環境識別停止後指示燈不變色的問題"
   
   # 格式：類型: 簡短描述
   # 類型：feat（新功能）、fix（修復）、docs（文檔）、style（格式）、refactor（重構）
   ```

---

## 🚫 **避免的事項**

1. **❌ 不要長時間不推送**
   - 導致工作丟失風險
   - 增加衝突可能性

2. **❌ 不要忽略衝突警告**
   - 必須解決衝突才能推送

3. **❌ 不要強制推送**
   ```bash
   # ❌ 危險！
   git push --force origin main
   
   # 這會覆蓋別人的工作
   ```

4. **❌ 不要推送編譯錯誤**
   - 確保代碼可以編譯
   - 確保測試通過（如果有）

---

## 💡 **GitHub Desktop 使用提示**

### **界面元素說明**

1. **Changes（變更）**
   - 顯示你修改、新增、刪除的文件
   - ✅ 綠色 = 新增或修改
   - ➕ 加號 = 新增文件
   - ➖ 減號 = 刪除文件

2. **History（歷史）**
   - 顯示所有提交記錄
   - "Origin/main" = 遠端代碼
   - "main" = 你的本地代碼

3. **Branch（分支）**
   - 下拉菜單顯示當前分支
   - 可以創建新分支

4. **Pull Request（PR）**
   - 如果要進行代碼審查
   - 創建 PR 而不是直接 push 到 main

---

## 🎓 **總結**

### **核心流程（記住這個！）**

```
1. 同步 (Pull) 
   ↓
2. 修改代碼
   ↓
3. 測試代碼
   ↓
4. Commit（提交）
   ↓
5. 再次同步 (Pull) 
   ↓
6. 推送 (Push)
```

### **GitHub Desktop 快速操作**

```
同步 = 點擊 "Pull origin"
提交 = 填寫信息 → 點擊 "Commit to main"
推送 = 點擊 "Push origin"

有衝突？→ 打開文件解決 → "Mark as resolved" → Commit
```

---

## 📞 **遇到問題時**

1. **查看狀態**
   ```bash
   git status
   ```

2. **查看遠端狀態**
   ```bash
   git log origin/main
   ```

3. **放棄本地更改（如果出錯）**
   ```bash
   # ⚠️ 危險！會丟失本地未提交的更改
   git reset --hard origin/main
   ```

4. **查看詳細幫助**
   - 命令行：`git help <命令>`
   - GitHub Desktop：Help → GitHub Desktop Help

---

## ✨ **最終檢查清單**

推送前檢查：

- [ ] ✅ 代碼可以編譯
- [ ] ✅ 已經同步遠端代碼（Pull）
- [ ] ✅ 沒有衝突，或有衝突但已解決
- [ ] ✅ Commit 信息清晰有意義
- [ ] ✅ 沒有提交不必要的文件（如 `.idea/`）
- [ ] ✅ 測試過你的更改

**然後才推送！** 🚀

