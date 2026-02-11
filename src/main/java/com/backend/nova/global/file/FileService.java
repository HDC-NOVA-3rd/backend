package com.backend.nova.global.file;

import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${file.dir}")
    private String fileDir;

    @Value("${file.prefix}")
    private String urlPrefix;

    /**
     * 단일 파일 저장
     * @param file 업로드된 파일
     * @return DB에 저장할 접근 URL (예: /images/uuid_filename.jpg)
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        // 저장할 파일명 생성 (UUID + 확장자)
        String storeFilename = createStoreFileName(originalFilename);

        // 전체 물리 경로 (C:/Users/.../uuid.jpg)
        String fullPath = getFullPath(storeFilename);

        try {
            // 디렉토리가 없으면 생성
            File directory = new File(fileDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    log.error("디렉토리 생성 실패: {}", fileDir);
                }
            }

            // 파일 저장
            file.transferTo(new File(fullPath));

            // DB에 저장할 URL 반환 (/images/uuid.jpg)
            return urlPrefix + storeFilename;

        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 물리적 전체 경로 반환
     */
    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    /**
     * UUID를 이용한 고유 파일명 생성
     * (예: image.png -> 550e8400-e29b...png)
     */
    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    /**
     * 확장자 추출
     */
    private String extractExt(String originalFilename) {
        if (StringUtils.hasText(originalFilename)) {
            int pos = originalFilename.lastIndexOf(".");
            return originalFilename.substring(pos + 1);
        }
        return "";
    }
}