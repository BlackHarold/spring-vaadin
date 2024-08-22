package ru.suek.model;

public class FileDTO {
   private String name;
   private String description;
   private String path;
   private String size;

    public FileDTO(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public FileDTO(String name, String description, String path) {
        this.name = name;
        this.description = description;
        this.path = path;
    }

    public FileDTO(String name, String description, String path, String size) {
        this.name = name;
        this.description = description;
        this.path = path;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "FileDTO{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", path='" + path + '\'' +
                ", size='" + size + '\'' +
                '}';
    }
}
