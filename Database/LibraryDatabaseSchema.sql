CREATE DATABASE library;
USE library;

-- Create the main books table
CREATE TABLE books (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    is_issued BOOLEAN DEFAULT FALSE,
    issued_to VARCHAR(255),
    return_date DATE,
    issued_on DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Create book_categories table
CREATE TABLE book_categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Add foreign key for categories
ALTER TABLE books 
ADD COLUMN category_id INT,
ADD CONSTRAINT fk_books_category 
FOREIGN KEY (category_id) REFERENCES book_categories(id);

-- Create fine_history table for tracking fines
CREATE TABLE fine_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    book_id INT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    fine_amount DECIMAL(10,2) NOT NULL,
    days_overdue INT NOT NULL,
    paid BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB;

-- Add indexes for better performance
CREATE INDEX idx_books_name_author ON books(name, author);
CREATE INDEX idx_books_issued ON books(is_issued);
CREATE INDEX idx_books_category ON books(category_id);

-- Insert default categories
INSERT INTO book_categories (category_name) VALUES 
('Fiction'),
('Non-Fiction'),
('Science'),
('Technology'),
('History'),
('Biography'),
('Romance'),
('Mystery'),
('Fantasy'),
('Science Fiction');