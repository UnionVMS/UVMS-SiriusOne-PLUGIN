package eu.europa.ec.fisheries.uvms.plugins.iridium.siriusone.xml;

import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.jvnet.staxex.Base64EncoderStream;
import com.ctc.wstx.sr.StreamScanner;

public class UnmarshallingTest {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    @Test
    public void unmarshallXmlTest() throws JAXBException {
        Unmarshaller marshaller = JAXBContext.newInstance(Devices.class).createUnmarshaller();
        Devices devices = (Devices) marshaller.unmarshal(getXml());
        
        Device device = devices.getDevice().get(0);
        assertThat(device.getSerial(), CoreMatchers.is("E12345678"));
        Positions positions = device.getPositions();
        Position position = positions.getPosition().get(0);
        assertThat(position.getLatitude(), CoreMatchers.is(57.7152863));
        assertThat(position.getLongitude(), CoreMatchers.is(11.9736080));
        assertThat(position.getGps().toString(), CoreMatchers.is("2020-09-02T14:59:01"));
        assertThat(position.getCourse(), CoreMatchers.is(123.0));
        assertThat(position.getSpeed().getKnots(), CoreMatchers.is(1.2));
    }
    
    private ByteArrayInputStream getXml() {
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
                "<devices>\n" + 
                "    <device>\n" + 
                "        <devicetype>SiriusOne</devicetype>\n" + 
                "        <name>Test</name>\n" + 
                "        <firmware>3.00</firmware>\n" + 
                "        <serial>E12345678</serial>\n" + 
                "        <positions>\n" + 
                "            <position>\n" + 
                "                <serial>350</serial>\n" + 
                "                <csp>2020-09-02 15:00:12</csp>\n" + 
                "                <source>Data Report</source>\n" + 
                "                <type>Interval report</type>\n" + 
                "                <timestamp>2020-09-02 15:00:18</timestamp>\n" + 
                "                <gps>2020-09-02 14:59:01</gps>\n" + 
                "                <speed>\n" + 
                "                    <knots>1.2</knots>\n" + 
                "                    <km>0</km>\n" + 
                "                </speed>\n" + 
                "                <course>123</course>\n" + 
                "                <odometer/>\n" + 
                "                <io/>\n" + 
                "                <zone/>\n" + 
                "                <analog/>\n" + 
                "                <latitude>57.7152863</latitude>\n" + 
                "                <longitude>11.9736080</longitude>\n" + 
                "                <altitude/>\n" + 
                "            </position>\n" + 
                "        </positions>\n" + 
                "    </device>\n" + 
                "</devices>\n" + 
                "";
        return new ByteArrayInputStream(xmlString.getBytes());
    }
    
    @Test
    public void test() throws IOException {
        Path file = Path.of("", "src/test/resources").resolve("27880.sbd");
        byte[] bytes = Files.readAllBytes(file);
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int num = Integer.parseInt(String.format("%02X ", bytes[i]).trim(), 16);
            String tmp = Integer.toBinaryString(num);
            String s = String.format("%8s", tmp).replace(' ', '0');
            buff.append(s);
        }
        System.out.println(buff.toString());
    
        for (int i = 0; i < bytes.length; i++) {
            System.out.println(Byte.toString(bytes[i]));
        }
    }
    
    
    @Test
    public void testtest() throws IOException {
        int reportRequest = 0x04;
        
        Path file = Path.of("", "src/test/resources").resolve("cebb3669-698f-42e3-96e5-48c004c134c9.sbd");
        byte[] bytes2 = Files.readAllBytes(file);
        for (int i = 0; i < bytes2.length; i++) {
            System.out.println(String.format("%02X ", bytes2[i]).trim());
        }
//        
//        String hexString = bytesArrayToHexString(bytes2);
//        System.out.println(hexString);
//        System.out.println(Integer.parseInt(hexString.substring(2,4), 16));
//        System.out.println(Integer.parseInt(hexString.substring(2,6), 16));
//        System.out.println(Integer.parseInt(hexString.substring(2,8), 16));
//        System.out.println(Integer.parseInt(hexString.substring(4,6), 16));
//        System.out.println(Integer.parseInt(hexString.substring(4,8), 16));
//        System.out.println(Integer.parseInt(hexString.substring(6), 16));
//        System.out.println(Integer.parseInt(hexString.substring(8), 16));
//        System.out.println(Integer.parseInt(hexString.substring(10), 16));
//        String hexString = Integer.toHexString(reportRequest);
//        byte[] bytes = hexString.getBytes();
//        byte[] bytes = hexStringToByteArray(hexString);
//        
//        StringBuffer buff = new StringBuffer();
//        for (int i = 0; i < bytes.length; i++) {
//            int num = Integer.parseInt(String.format("%02X ", bytes[i]).trim(), 16);
//            String tmp = Integer.toBinaryString(num);
//            String s = String.format("%8s", tmp).replace(' ', '0');
//            buff.append(s);
//        }
//        System.out.println(buff.toString());
        
    }
    
    @Test
    public void testesetset() {
        int value = 3600;
        System.out.println(Integer.toHexString(value));
    }
    
    @Test
    public void positionRequestTest() {
        int positionRequest = 0x04;
        int positionRequestLength = 0x00;
        
        byte[] bytes2 = {(byte) positionRequest, (byte) positionRequestLength};
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < bytes2.length; i++) {
            int num = Integer.parseInt(String.format("%02X ", bytes2[i]).trim(), 16);
            String tmp = Integer.toBinaryString(num);
            String s = String.format("%8s", tmp).replace(' ', '0');
            buff.append(s);
        }
        System.out.println(buff.toString());
    }
    
    @Test
    public void setIntervalTest() {
        int setInterval = 0x03;
        int setIntevalLength = 0x04;
        int value = 3600;
        
        byte[] test = intToByteArray(value);
        
        byte[] bytes2 = {(byte) setInterval, 
                         (byte) setIntevalLength, 
                         (byte) test[3],
                         (byte) test[2],
                         (byte) test[1],
                         (byte) test[0]};
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < bytes2.length; i++) {
            int num = Integer.parseInt(String.format("%02X ", bytes2[i]).trim(), 16);
            String tmp = Integer.toBinaryString(num);
            String s = String.format("%8s", tmp).replace(' ', '0');
            buff.append(s);
        }
        System.out.println(buff.toString());
    }

    public String intToHex(int... i) {
        String hexString = null;
        for (int j : i) {
            String hexValue = Integer.toHexString(j);
            if (hexValue.length() % 2 != 0) {
                hexValue = '0' + hexValue;
            }
            hexString += hexValue;
        }
        return hexString;
    }
    
    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
    
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    
}