package kakao.login.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path rootLocation;

    @Autowired
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }

    @PostConstruct
    public void init() throws IOException {
        System.out.println("▶ rootLocation = " + rootLocation.toAbsolutePath());
        Files.createDirectories(rootLocation);
    }

    /** 파일을 디스크에 저장하고 public URL 경로를 반환 */
    public String store(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String filename = UUID.randomUUID() + extension;

        Path dest = rootLocation.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
        // 예를 들어, "http://서버주소/uploads/uuid_파일명.ext" 혹은 "/uploads/uuid_파일명.ext"
        return "/uploads/" + filename;
    }

    public Resource loadAsResource(String filePathOrName) {
        try {
            // Extract just the filename if a full path is provided
            String filename = filePathOrName;
            if (filePathOrName.contains("/")) {
                filename = filePathOrName.substring(filePathOrName.lastIndexOf('/') + 1);
            }

            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filePathOrName, e);
        }
    }

    public void deleteFile(String filePathOrName) {
        if (filePathOrName == null || filePathOrName.isEmpty()) {
            System.out.println("deleteFile: 파일 경로가 null이거나 비어 있음");
            return;
        }
        try {
            String filename = filePathOrName;
            if (filePathOrName.contains("/")) {
                filename = filePathOrName.substring(filePathOrName.lastIndexOf('/') + 1);
            } else if (filePathOrName.contains("uploads/")) {
                filename = filePathOrName.substring(filePathOrName.indexOf("uploads/") + 8);
            }
            System.out.println("deleteFile: 파일명 추출 - " + filename + ", rootLocation=" + rootLocation.toAbsolutePath());
            Path file = rootLocation.resolve(filename).normalize();
            System.out.println("deleteFile: 삭제 대상 파일 경로 - " + file.toAbsolutePath());
            if (Files.exists(file)) {
                Files.delete(file);
                System.out.println("deleteFile: 파일 삭제 성공 - " + filename);
            } else {
                System.out.println("deleteFile: 파일을 찾을 수 없음 - " + filename);
            }
        } catch (IOException e) {
            System.err.println("deleteFile: 파일 삭제 실패 - " + filePathOrName + ", 이유: " + e.getMessage());
        }
    }
}
