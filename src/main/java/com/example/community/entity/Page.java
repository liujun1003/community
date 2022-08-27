package com.example.community.entity;

public class Page {

    // 当前页码
    private int current = 1;
    // 每页显示条数
    private int limit = 10;
    // 数据库总条数（计算总页数）
    private int rows;
    // 请求路径（分页查询）
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // 总页数
    public int getTotal() {
        return rows % limit == 0 ? rows / limit : (rows / limit) + 1;
    }

    // 当前页起始数据行
    public int getOffset() {
        return (current - 1) * limit;
    }

    // 当前分页条显示起始页
    public int getFrom() {
        int total = this.getTotal();
        int extraSub = Math.max(current - (total - 4), 0);
        return Math.max(current - 5 - extraSub, 1);
    }

    // 当前分页条显示结尾页
    public int getTo() {
        int total = this.getTotal();
        int extraAdd = Math.max(6 - current, 0);
        return Math.min(current + 4 + extraAdd, total);
    }
}
