-- First, create a temporary table for bulk insert
CREATE TEMPORARY TABLE temp_books (
    name VARCHAR(255),
    author VARCHAR(255),
    category VARCHAR(255)
);

-- Insert all books data into temporary table
INSERT INTO temp_books (name, author, category) VALUES
-- data records to be put here; enter some VALUES HERE
-- eg.('The Diary of a Young Girl', 'Anne Frank', 'Biography'),........


-- Insert books from temporary table to actual table
INSERT INTO books (name, author, category_id)
SELECT 
    t.name, 
    t.author, 
    bc.id
FROM temp_books t
JOIN book_categories bc ON LOWER(bc.category_name) = LOWER(t.category);

-- Clean up
DROP TEMPORARY TABLE temp_books;