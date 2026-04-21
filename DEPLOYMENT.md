# GitHub Pages 部署说明

## 🚀 自动部署

文档网站已配置 GitHub Actions 自动部署到 GitHub Pages。

### 部署流程

1. **推送代码到 master 分支**
   ```bash
   cd light-framework
   git add .
   git commit -m "docs: update documentation"
   git push origin master
   ```

2. **GitHub Actions 自动触发**
   - 工作流文件：`.github/workflows/github-pages.yml`
   - 自动构建并部署 `show` 目录到 GitHub Pages

3. **访问在线文档**
   - GitHub Pages URL: https://RONGX563647.github.io/LightSSM/show/
   - 或者配置自定义域名

## 📋 手动部署 (可选)

如果自动部署失败，可以手动部署:

### 方法 1: 使用 GitHub Pages 设置

1. 进入 GitHub 仓库 Settings → Pages
2. Source 选择 "Deploy from a branch"
3. Branch 选择 `master`，文件夹选择 `/show`
4. 点击 Save 保存

### 方法 2: 使用 gh-pages 分支

```bash
# 安装 gh-pages
npm install -g gh-pages

# 部署 show 目录
cd light-framework
gh-pages -d show
```

## 🔍 检查部署状态

1. 进入 GitHub 仓库 → Actions 标签
2. 查看 "Deploy to GitHub Pages" 工作流
3. 点击最近的运行记录查看详情
4. 部署成功后会显示 Pages URL

## ⚠️ 注意事项

- 确保 `show` 目录包含所有必要的 HTML、CSS、JS 文件
- 所有相对路径引用需要正确
- 首次部署可能需要 2-5 分钟
- 更新后 GitHub Pages 可能有几分钟缓存延迟

## 📁 目录结构

```
light-framework/
├── docs/                    # Markdown 源文档
├── show/                    # 生成的 HTML 文档网站
│   ├── index.html          # 首页
│   ├── 01-architecture.html
│   ├── 02-ioc-container.html
│   ├── ... (其他文档页面)
│   ├── template.html       # 页面模板
│   └── README.md           # 文档说明
└── .github/workflows/
    └── github-pages.yml    # 部署工作流
```

## 🌐 访问地址

- **GitHub Pages**: https://RONGX563647.github.io/LightSSM/show/
- **本地测试**: `python -m http.server 8000` (在 show 目录)
- **自定义域名**: 可在 GitHub Pages 设置中配置

## 📝 更新文档

1. 编辑 `docs/` 目录下的 Markdown 文件
2. 运行转换脚本生成 HTML:
   ```bash
   python convert_all_docs_v2.py
   ```
3. 提交并推送更改
4. GitHub Pages 自动更新

## 🔧 故障排查

### 部署失败

1. 检查 GitHub Actions 日志
2. 确认 `show` 目录结构正确
3. 验证工作流文件语法

### 页面显示异常

1. 清除浏览器缓存
2. 检查文件路径引用
3. 确认所有依赖文件已上传 (如 mermaid.min.js)

### 404 错误

1. 等待 2-5 分钟让 GitHub Pages 完成部署
2. 检查 URL 是否正确 (区分大小写)
3. 确认 GitHub Pages 设置中选择了正确的分支和目录
