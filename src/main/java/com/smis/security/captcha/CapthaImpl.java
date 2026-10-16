package com.smis.security.captcha;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.CubicCurve2D;
import java.io.*;
import java.security.SecureRandom;
public class CapthaImpl implements Captcha {
    private static final int COUNT_NUM = 6;
    private static final int WIDTH_IMG = 150;
    private static final int HEIGHT_IMG = 40;
    private String genStr;
    private final String charsInImg = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom random = new SecureRandom();

    private void getRandomString() {
        char[] chars = charsInImg.toCharArray();
        StringBuilder tmpStr = new StringBuilder();
        for (int i = 0; i < COUNT_NUM; i++) {
            tmpStr.append(chars[random.nextInt(chars.length)]);
        }
        genStr = tmpStr.toString();
    }

    @Override
    public BufferedImage getCaptchaBufferedImage() {
        getRandomString();
        BufferedImage bufferedImage = new BufferedImage(WIDTH_IMG, HEIGHT_IMG, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.addRenderingHints(hints);
        graphics.setPaint(new GradientPaint(0, 0, backgroundColor(),
                WIDTH_IMG, HEIGHT_IMG, backgroundColor()));
        graphics.fillRect(0,0, WIDTH_IMG, HEIGHT_IMG);
        for (int i = 0; i < 90; i++) {
            graphics.setColor(new Color(130 + random.nextInt(80),
                    130 + random.nextInt(80), 130 + random.nextInt(80)));
            graphics.fillOval(random.nextInt(WIDTH_IMG), random.nextInt(HEIGHT_IMG), 2, 2);
        }
        BufferedImage textLayer = new BufferedImage(WIDTH_IMG, HEIGHT_IMG, BufferedImage.TYPE_INT_ARGB);
        Graphics2D textGraphics = textLayer.createGraphics();
        textGraphics.addRenderingHints(hints);
        for (int i = 0; i < genStr.length(); i++) {
            Graphics2D glyph = (Graphics2D) textGraphics.create();
            try {
                glyph.setFont(new Font(i % 2 == 0 ? Font.SANS_SERIF : Font.SERIF,
                        Font.BOLD, 24 + random.nextInt(4)));
                glyph.setColor(new Color(25 + random.nextInt(65),
                        25 + random.nextInt(65), 25 + random.nextInt(65)));
                glyph.translate(16 + i * 23 + random.nextInt(3), 21 + random.nextInt(5));
                glyph.rotate(Math.toRadians(random.nextInt(35) - 17));
                glyph.shear((random.nextDouble() - 0.5) * 0.25, 0);
                glyph.drawString(String.valueOf(genStr.charAt(i)), -8, 7);
            } finally {
                glyph.dispose();
            }
        }
        textGraphics.dispose();
        // Warp the text across columns so glyph edges do not share a uniform baseline.
        double phase = random.nextDouble() * Math.PI * 2;
        for (int x = 0; x < WIDTH_IMG; x++) {
            int offset = (int) Math.round(2 * Math.sin(x / 13.0 + phase));
            graphics.drawImage(textLayer, x, offset, x + 1, HEIGHT_IMG + offset,
                    x, 0, x + 1, HEIGHT_IMG, null);
        }
        graphics.setStroke(new BasicStroke(1.1f));
        for (int i = 0; i < 3; i++) {
            graphics.setColor(new Color(100 + random.nextInt(60),
                    100 + random.nextInt(60), 100 + random.nextInt(60)));
            graphics.draw(new CubicCurve2D.Double(0, random.nextInt(HEIGHT_IMG),
                    45, random.nextInt(HEIGHT_IMG), 105, random.nextInt(HEIGHT_IMG),
                    WIDTH_IMG, random.nextInt(HEIGHT_IMG)));
        }
        graphics.dispose();
        return bufferedImage;
    }

    private Color backgroundColor() {
        return Color.getHSBColor(random.nextFloat(), 0.16f + random.nextFloat() * 0.14f, 0.98f);
    }

    @Override
    public boolean checkUserAnswer(String userAnswer) {
        String expected = genStr;
        genStr = null;
        return expected != null && expected.equals(userAnswer);
    }

    @Override
    public Image getCaptchaImg(){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(getCaptchaBufferedImage(), "png", bos);
            StreamResource streamResource = new StreamResource("img.png", () -> new ByteArrayInputStream(bos.toByteArray()));
            return new Image(streamResource, "CAPTCHA challenge");
        }catch (IOException e){
            throw new IllegalStateException("Unable to generate CAPTCHA", e);
        }
    }
}
