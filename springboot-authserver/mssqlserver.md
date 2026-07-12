
## MSSQL-Server Docker Install and Run

```shell
brew install --cask docker

docker pull mcr.microsoft.com/mssql/server:2022-latest

docker run -e "ACCEPT_EULA=Y" \
-e "MSSQL_SA_PASSWORD=YourStrong@Passw0rd" \
-p 1433:1433 \
--name sqlserver \
-d mcr.microsoft.com/mssql/server:2022-latest


```

## 在 VS Code 中使用 `mssql`

以下是详细步骤：

### 第一步：确认 Docker 中的 SQL Server 正在运行
在终端输入以下命令，确认容器状态是 `Up`：
```bash
docker ps
```
如果看到 `sqlserver` (或你起的名字) 在列表中且状态为 `Up`，说明数据库已经准备好，可以连接了。

### 第二步：在 VS Code 中安装 mssql 扩展
1. 打开 VS Code。
2. 按下快捷键 `Cmd + Shift + X` 打开扩展市场。
3. 搜索 `mssql`。
4. 找到由 **Microsoft** 发布的名为 **SQL Server (mssql)** 的扩展，点击 **Install (安装)**。

### 第三步：调出 SQL Server 连接面板
1. 安装完成后，按 `Cmd + Shift + P` 打开命令面板。
2. 输入 `mssql`，在下拉菜单中选择 **MS SQL: Connect** （或者直接点击左侧边栏活动栏中多出来的那个数据库图标）。

### 第四步：按提示输入连接信息
点击连接后，VS Code 顶部会弹出一个下拉菜单/输入框，按顺序操作：

1. **Create Connection Profile** (创建连接配置) —— 选择这项，按回车。
2. **Server name** (服务器名称) —— 输入 `localhost`，按回车。
3. **Database** (数据库名) —— 输入 `master`（这是系统默认数据库），按回车。
4. **Authentication Type** (身份验证类型) —— 选择 `SQL Login`，按回车。
5. **User name** (用户名) —— 输入 `sa`，按回车。
6. **Password** (密码) —— 输入你启动 Docker 时设置的密码（例如 `YourStrong@Passw0rd`），按回车。
7. **Save Password?** (保存密码) —— 选择 `Yes`，按回车，这样下次就不用重输了。
8. **Profile Name** (配置文件名称) —— 可以直接按回车跳过，或者输入一个好记的名字如 `LocalDockerSQL`。

### 第五步：开始写 SQL 并执行
连接成功后，左侧边栏的 SQL SERVER 面板下就会出现你的 `localhost` 连接，你可以展开看到数据库的表、视图等。

**如何执行 SQL 语句：**
1. 点击 VS Code 顶部菜单 `File` -> `New File` 新建一个文件。
2. 点击右下角的语言模式（可能显示 Plain Text），在弹出的列表中选择 **SQL**。
3. 在文件中输入测试语句：
   ```sql
   SELECT @@VERSION;
   ```
4. 选中这行代码（如果不选中，默认执行整个文件），然后按下快捷键 `Cmd + Shift + E`（或者右键选择 **Execute Query**）。
5. 稍等片刻，VS Code 右侧就会弹出结果面板，显示你的 SQL Server 版本信息，说明连接和执行完全成功！

## 数据库库操作
新建查询文件
```
-- 如果存在同名数据库先删除（可选，注意会丢失数据）
-- DROP DATABASE IF EXISTS MyTestDB;

-- 创建新数据库
CREATE DATABASE MyTestDB;


CREATE TABLE Users (
    Id INT PRIMARY KEY IDENTITY(1,1),
    UserName NVARCHAR(50) NOT NULL,
    Age INT
);


-- Delete rows from table 'TableName'
DELETE FROM Users;
	/* add search conditions here */


-- 插入 10 条数据到 Users 表
INSERT INTO Users (UserName, Age) 
VALUES 
    (N'张三', 25),
    (N'李四', 30),
    (N'王五', 28),
    (N'赵六', 22),
    (N'孙七', 35),
    (N'周八', 27),
    (N'吴九', 31),
    (N'郑十', 29),
    (N'钱十一', 24),
    (N'冯十二', 33);


select * from Users;

```