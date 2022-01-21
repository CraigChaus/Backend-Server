package fileHandler;

public class MyFile {
    private int id;
    private byte[] content;
    private String fileExtensions;

    public MyFile(int id, byte[] content, String fileExtensions) {
        this.id = id;
        this.content = content;
        this.fileExtensions = fileExtensions;
    }

    public int getId() {
        return id;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileExtensions() {
        return fileExtensions;
    }
}
