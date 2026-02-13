# otu-utils

> Oops, typo

osu 小工具

## Project Structure

* `api`: osu api
* `data`: 小工具的数据结构
* `database`: 数据库接口，但实际上并不是数据库！
* `providers`: `database` 和 `api` 的包装

## Tips

如果用 idea 跑 Main 的时候乱码了，试试在 vm options 里面加上 `-Duser.country=US`, see
also [this](https://intellij-support.jetbrains.com/hc/en-us/community/posts/16875638446482-Issue-with-IntelliJ-run-output-console-not-displaying-utf-8-characters).

## Usage

```plain
Usage: otu-utils [-v] [--dry-run] [--no-refresh] [--prefer-local]
                 [-p=DIRECTORY] [--user-profile=DIRECTORY] [COMMAND]
      --dry-run             不更新绝大部分数据到数据库，除了 user token
      --no-refresh          不刷新 user token，通常与 --dry-run 一同使用
  -p, --profile=DIRECTORY   存放应用数据的目录
      --prefer-local        优先从本地 osu! 获取数据，
                            必须填写 application.json 中的 'local_osu_path'。
                            注意部分来自本地 osu! 的数据不可用，比如在线用户分数 id。
      --user-profile=DIRECTORY
                            存放用户数据的目录
  -v, --verbose
Commands:
  auth
  me                       输出当前用户信息
  analyze                  分析最近的分数
  list-analyze             列出所有正在追踪的谱面
  rollback                 回滚一次分析
  render-scores            根据特定谱面的分数渲染图表
  info-collection          输出谱面包信息
  export-collection-score  从本地 osu! 中导出谱面包中谱面的对应最高分数，分数必须至少使用 V2 才会被计入
```

本地 osu! 的数据不会立刻更新（比如分数），你可能需要关闭正在运行的 osu! 来更新本地数据。