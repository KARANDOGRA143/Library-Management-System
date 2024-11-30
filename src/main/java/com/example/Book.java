package com.example;

public class Book {
    private int id;
    private String name;
    private String author;
    private String category;

    public Book(int id, String name, String author, String category) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.category = category;
    }

    // Add constructor without category for backward compatibility
    public Book(int id, String name, String author) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.category = null;
    }

    // Existing getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}