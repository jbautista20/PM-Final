package huffman;

import java.io.*;
import java.util.*;

public class HuffmanCoder {

    // Comprime el archivo input y lo guarda en output (.huf)
    public static void compress(File input, File output) throws IOException {
        // 1. Contar frecuencias
        Map<Byte, Integer> frequencies = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(input)) {
            byte[] buffer = new byte[8192];     // Trae 8kb del disco a RAM para agilizar la lectura
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];
                    frequencies.put(b, frequencies.getOrDefault(b, 0) + 1); //busca al caracter y le suma 1
                                                                                        //a la frecuencia. Si no estaba
                                                                                        //comienza su frecuenci en 0+1
                }
            }
        }

        if (frequencies.isEmpty()) {
            // Archivo vacío
            try (FileOutputStream fos = new FileOutputStream(output);
                 DataOutputStream dos = new DataOutputStream(fos)) {
                dos.writeInt(0);
                dos.writeLong(0);
            }
            return;
        }

        // 2. Construir árbol
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();      //cola de prioridad, con .poll traeré el mas bajo
        for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));      // Poblar la cola con nodos.
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();           // Saca el mas chico
            HuffmanNode right = pq.poll();          // Saca el segundo mas chico
            pq.add(new HuffmanNode(left, right));   // Los une y agrega al padre de nuevo a la pq
        }

        HuffmanNode root = pq.poll();       // Cuando solo queda uno en la pq, entonces es la raiz

        // 3. Generar códigos
        Map<Byte, String> huffmanCodes = new HashMap<>();       // { cod: CHAR, ... , 001: B, ..., }
        generateCodes(root, "", huffmanCodes);              // retorna el map con los códigos

        // 4. Escribir archivo comprimido
        try (FileOutputStream fos = new FileOutputStream(output);
             DataOutputStream dos = new DataOutputStream(fos)) {
             
            // Cabecera: Tamaño de tabla de frecuencias
            dos.writeInt(frequencies.size());
            
            // Entradas de tabla de frecuencias
            for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
                dos.writeByte(entry.getKey());
                dos.writeInt(entry.getValue());
            }

            // Longitud original del archivo
            long originalLength = input.length();
            dos.writeLong(originalLength);

            // Escribir datos codificados
            try (FileInputStream fis = new FileInputStream(input)) {
                int currentByte = 0;
                int numBits = 0;

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    for (int i = 0; i < bytesRead; i++) {
                        String code = huffmanCodes.get(buffer[i]);      // Código en string ej: "101"
                        for (char c : code.toCharArray()) {             //itero sacando cada numm del código
                            currentByte = (currentByte << 1) | (c - '0');   // " c - '0' " me da el valor del num
                            numBits++;                                      // '1' = 49, '0' = 48 --> 49-48 = 1
                            if (numBits == 8) {
                                dos.write(currentByte);         // Cuando complete el Byte, lo escribo en el archivo
                                currentByte = 0;            //ej: 0110 0101 --> referencia a caracteres: 01,100, 101
                                numBits = 0;                //luego paso a otro bloque de 1 byte para seguir guardando.
                            }
                        }
                    }
                }
                
                // Escribir bits restantes (padding)
                if (numBits > 0) {
                    currentByte = currentByte << (8 - numBits);
                    dos.write(currentByte);
                }
            }
        }
    }

    private static void generateCodes(HuffmanNode node, String code, Map<Byte, String> huffmanCodes) {
        if (node == null) return;
        if (node.isLeaf()) {
            // Caso especial de un solo caracter en todo el archivo
            huffmanCodes.put(node.data, code.isEmpty() ? "0" : code);
        }
        generateCodes(node.left, code + "0", huffmanCodes);     // Por la izquierda codifico ccon 0
        generateCodes(node.right, code + "1", huffmanCodes);    // derecha codifico con 1
    }

    // Descomprime el archivo input (.huf) y lo guarda en output (.dhu)
    public static void decompress(File input, File output) throws IOException {
        try (FileInputStream fis = new FileInputStream(input);
             DataInputStream dis = new DataInputStream(fis)) {
             
            // 1. Leer tamaño de tabla de frecuencias
            int tableSize;
            try {
                tableSize = dis.readInt();
            } catch (EOFException e) {
                // Archivo vacío
                try (FileOutputStream fos = new FileOutputStream(output)) {}
                return;
            }

            if (tableSize == 0) {
                // Archivo vacío
                try (FileOutputStream fos = new FileOutputStream(output)) {}
                return;
            }

            // 2. Reconstruir tabla de frecuencias
            Map<Byte, Integer> frequencies = new HashMap<>();
            for (int i = 0; i < tableSize; i++) {
                byte b = dis.readByte();
                int freq = dis.readInt();
                frequencies.put(b, freq);
            }

            // 3. Reconstruir árbol
            PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
            for (Map.Entry<Byte, Integer> entry : frequencies.entrySet()) {
                pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));
            }

            while (pq.size() > 1) {
                HuffmanNode left = pq.poll();
                HuffmanNode right = pq.poll();
                pq.add(new HuffmanNode(left, right));
            }

            HuffmanNode root = pq.poll();

            // 4. Leer longitud original
            long originalLength = dis.readLong();

            // 5. Descomprimir datos
            try (FileOutputStream fos = new FileOutputStream(output);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                
                HuffmanNode current = root;
                long bytesWritten = 0;
                
                // Caso especial de un solo caracter en todo el archivo
                if (root.isLeaf()) {
                    while (bytesWritten < originalLength) {
                        bos.write(root.data);
                        bytesWritten++;
                    }
                    return;
                }

                int byteRead;
                while (bytesWritten < originalLength && (byteRead = dis.read()) != -1) {
                    for (int i = 7; i >= 0; i--) {      //saca los bits de cada Byte leido
                        int bit = (byteRead >> i) & 1;
                        if (bit == 0) {
                            current = current.left;
                        } else {
                            current = current.right;
                        }

                        if (current.isLeaf()) {
                            bos.write(current.data);
                            bytesWritten++;
                            current = root;
                            if (bytesWritten == originalLength) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
