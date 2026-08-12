package com.tripass.ocr.client;

import com.tripass.ocr.dto.internal.VisionOcrResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Google Cloud 실제 연결 확인이 필요할 때만 실행")
class GoogleCloudConnectionTest {

    /**
     * Google Cloud Translation 연결 확인
     */
    @Test
    void translateToKorean() {
        String projectId =
                System.getenv("GOOGLE_CLOUD_PROJECT");

        GoogleTranslationClient client =
                new GoogleTranslationClient(projectId);

        List<String> translatedTexts =
                client.translateToKorean(
                        List.of(
                                "TOKYO MART",
                                "COFFEE"
                        )
                );

        assertNotNull(translatedTexts);
        assertFalse(translatedTexts.isEmpty());

        translatedTexts.forEach(System.out::println);
    }

    /**
     * Google Cloud Vision 연결 확인
     */
    @Test
    void analyzeReceiptImage() throws Exception {
        VisionOcrClient client =
                new VisionOcrClient();

        byte[] imageBytes =
                createTestReceiptImage();

        VisionOcrResult result =
                client.analyze(imageBytes);

        assertNotNull(result);
        assertNotNull(result.getRawText());
        assertFalse(result.getRawText().isBlank());

        System.out.println(result.getRawText());
    }

    /**
     * 외부 이미지 없이 테스트용 영수증 이미지를 생성한다.
     */
    private byte[] createTestReceiptImage()
            throws Exception {

        BufferedImage image =
                new BufferedImage(
                        800,
                        600,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                image.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(
                    0,
                    0,
                    image.getWidth(),
                    image.getHeight()
            );

            graphics.setColor(Color.BLACK);
            graphics.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.PLAIN,
                            32
                    )
            );

            graphics.drawString(
                    "TOKYO MART",
                    50,
                    80
            );
            graphics.drawString(
                    "COFFEE 450",
                    50,
                    150
            );
            graphics.drawString(
                    "SANDWICH 780",
                    50,
                    220
            );
            graphics.drawString(
                    "TOTAL 1230 JPY",
                    50,
                    300
            );
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        ImageIO.write(
                image,
                "png",
                outputStream
        );

        return outputStream.toByteArray();
    }
}