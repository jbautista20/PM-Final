package org.hamming;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class CryptoTimeHelper {

    public static byte[] packWithTimeLock(byte[] data, LocalDateTime targetTime) throws Exception {
        String timeStr = targetTime.toString();
        byte[] timeBytes = timeStr.getBytes(StandardCharsets.UTF_8);
        byte[] key = timeBytes; // La KEY es un arreglo de bytes extraidos de la fecha y hora

        // Encriptación con XOR
        byte[] encryptedPayload = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encryptedPayload[i] = (byte) (data[i] ^ key[i % key.length]);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(timeBytes.length); // 1 byte para indicar la longitud de string
        baos.write(timeBytes); // string con la fecha y hora
        baos.write(encryptedPayload); //guarda los datos encriptados

        return baos.toByteArray();
    }

    public static byte[] unpackTimeLock(byte[] fileData) throws Exception {
        int timeLen = fileData[0] & 0xFF;
        String targetTimeStr = new String(fileData, 1, timeLen, StandardCharsets.UTF_8);
        LocalDateTime targetTime = LocalDateTime.parse(targetTimeStr);

        if (LocalDateTime.now().isBefore(targetTime)) {
            throw new Exception("ACCESO DENEGADO: El archivo no puede abrirse antes de: " + targetTimeStr);
        }

        byte[] key = targetTimeStr.getBytes(StandardCharsets.UTF_8);
        int payloadOffset = 1 + timeLen; // 1 byte guardaba tam. + longitud de string = offset
        int payloadLen = fileData.length - payloadOffset;
        byte[] originalData = new byte[payloadLen];

        for (int i = 0; i < payloadLen; i++) {
            originalData[i] = (byte) (fileData[payloadOffset + i] ^ key[i % key.length]);  //vuelve a aplicar XOR con la
                                                                                            //key para desencriptar
        }

        return originalData;
    }
}
