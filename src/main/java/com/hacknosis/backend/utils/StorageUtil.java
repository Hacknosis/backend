package com.hacknosis.backend.utils;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;

@Log4j2
@Component
public class StorageUtil {
    @Value("${gcp.bucket-name}")
    public String BUCKET_NAME = "medical_report";

    @Value("${gcp.project-id}")
    public String PROJECT_ID = "hacknosis";

    public final Storage storage = StorageOptions.newBuilder()
            .setProjectId(PROJECT_ID)
            .build()
            .getService();

    public void saveContent(byte[] content, String fileName) throws IOException {
        log.info("Saving {} to cloud storage", fileName);
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        try (WriteChannel writer = storage.writer(blobInfo)) {
            writer.write(ByteBuffer.wrap(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readContent(String storageId) {
        BlobId blobId = BlobId.of(BUCKET_NAME, storageId);
        try {
            return storage.readAllBytes(blobId);
        } catch (Exception e) {
            return null;
        }
    }
}
