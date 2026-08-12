package com.tripass.ocr.service.storage;

import com.tripass.ocr.dto.internal.StoredReceiptFile;
import com.tripass.ocr.dto.internal.ValidatedReceiptImage;

// 영수증 이미지 파일 저장·조회·삭제 기능
public interface ReceiptFileStorage {

    // 검증이 완료된 영수증 이미지를 저장한다.
    StoredReceiptFile store(
            ValidatedReceiptImage receiptImage
    );

    // 저장된 영수증 이미지 데이터를 읽는다.
    byte[] load(String storedPath);

    // 저장된 영수증 이미지 파일을 삭제한다.
    void delete(String storedPath);
}