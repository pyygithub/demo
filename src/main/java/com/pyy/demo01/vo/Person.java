package com.pyy.demo01.vo;

public class Person {
    private String id;
    private String username;
    private String tel;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }
    public Person(String id, String username, String tel) {
        this.id = id;
        this.username = username;
        this.tel = tel;
    }

    public Person() {
    }
}
