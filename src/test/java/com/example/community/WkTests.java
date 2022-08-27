package com.example.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class WkTests {

    @Test
    public void testWk() {

        String cmdPdf = "D:/DevSoftware/wkhtmltopdf/bin/wkhtmltopdf https://www.nowcoder.com d:/DevData/community/1.pdf";
        String cmdImage = "D:/DevSoftware/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com d:/DevData/community/wkImage/1.png";

        try {
            Runtime.getRuntime().exec(cmdPdf);
            Runtime.getRuntime().exec(cmdImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
