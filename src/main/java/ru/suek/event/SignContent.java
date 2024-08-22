package ru.suek.event;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.*;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonObject;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SignContent {

    public static void signContent(String rootPath, String stampedPath, JsonValue oInfo) {
        StringBuilder stampText = getStampTextBuilder(oInfo);
        try (FileOutputStream fos = new FileOutputStream(stampedPath)) {
            PdfReader reader = new PdfReader(rootPath);
//                                    PdfStamper stamper = PdfStamper.createSignature(reader, fos, '\0');
            PdfStamper stamper = new PdfStamper(reader, fos);

            String[] lines = stampText.toString().split("\n");
            System.out.println("lines: " + lines.length);
            BaseFont bf = BaseFont.createFont("./resources/fonts/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(bf, 10);

            PdfContentByte content = stamper.getOverContent(1);

            float padding = 10;
            //Определение координат для рамки
            //Получение ширины страницы
            float pageWidth = reader.getPageSize(1).getWidth();
            float width = 300; //Ширина рамки
            float x = pageWidth - width - padding * 2; //Положение по X
            float y = 50; //Положение по Y
            float height = lines.length * (font.getSize() + 2); //Высота рамки

            content.setLineWidth(1);
            content.rectangle(x - padding, y - padding, width + padding * 2, height + padding * 2);
            content.stroke();

            //Установка текста
            content.beginText();
            content.setTextMatrix(x, y - padding); //Начальная позиция текста

            //Разбиение текста на строки
            y = y + height - padding;
            for (String line : lines) {
                ColumnText.showTextAligned(content, Element.ALIGN_LEFT, new Phrase(line, font), x, y, 0);
                y -= font.getSize() + 2;
            }

            stamper.close();
            reader.close();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static StringBuilder getStampTextBuilder(JsonValue oInfo){
        StringBuilder stampText = new StringBuilder();
        JreJsonObject jsonObject = (JreJsonObject) oInfo;
        System.out.println("oInfo: " + oInfo);
        JSONObject mainObject = new JSONObject(jsonObject);
        System.out.println("main object: " + mainObject);
        JSONObject object = mainObject.getJSONObject("object");
        JSONObject issuer = object.optJSONObject("Issuer");
        System.out.println("issuer: " + issuer);
        JSONObject subject = object.optJSONObject("Subject");
        System.out.println("subject: " + subject);
        String thumbprint = object.getString("Thumbprint");
        System.out.println("thumbprint: " + thumbprint);
        String algorithm = object.getString("Algorithm");
        System.out.println("algorithm: " + algorithm);
        String validFromDate = object.getString("ValidFromDate");
        System.out.println("validFromDate: " + validFromDate);
        String validToDate = object.getString("ValidToDate");
        System.out.println("validToDate: " + validToDate);

        //Извлечение даты
        LocalDate fromDate = LocalDate.parse(validFromDate.substring(0, 10));
        LocalDate toDate = LocalDate.parse(validToDate.substring(0, 10));

        //Форматирование даты в нужном формате
        String formattedFromDate = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String formattedToDate = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Рисование рамки штампа (ГОСТ Р 7.0.97-2016)
        stampText
                .append("Документ подписан электронной подписью").append("\n")
                .append("Сертификат: ").append(thumbprint).append("\n")
                .append("Владелец: ").append(subject.get("CN")).append("\n")
                .append("Действителен: ")
                .append("c ").append(formattedFromDate)
                .append(" по ").append(formattedToDate).append("\n");
        System.out.println("stamp text: \n\t" + stampText);

        return stampText;
    }
}
