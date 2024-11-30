package com.example;

public class IssuedBook {
    private int id;
    private String name;
    private String author;
    private String category;
    private String issuedTo;
    private String returnDate;
    private String issuedOn;

    public IssuedBook(int id, String name, String author, String category, String issuedTo, String returnDate,
            String issuedOn) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.category = category;
        this.issuedTo = issuedTo;
        this.returnDate = returnDate;
        this.issuedOn = issuedOn;
    }

    // Getters and setters for each field
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

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(String issuedOn) {
        this.issuedOn = issuedOn;
    }
}