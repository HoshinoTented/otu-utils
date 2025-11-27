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