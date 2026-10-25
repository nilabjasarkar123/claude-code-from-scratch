import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonClassDescription("Read and return the contents of a file")
public class ReadFileTool {

    @JsonPropertyDescription("The path to the file to read")
    @JsonProperty("file_path")
    public String filePath;

    // Tool name: Read

    //Purpose: read and return a file's contents
    //Required argument: file_path (a string)

    public String execute() throws IOException {
        Path path = Paths.get(filePath)
                .toAbsolutePath()
                .normalize();
        if (!Files.isRegularFile(path)) {
            throw new IOException("File not found: " + filePath);
        }
        //System.out.println("Reading file: " + Files.readString(path));
        return Files.readString(path);
    }

}