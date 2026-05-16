package org.hamming;

import huffman.HuffmanCoder;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FinalApp extends Application {
    private Stage mainStage;
    
    // UI
    private BorderPane root;
    private TextFlow leftTextFlow = new TextFlow();
    private TextFlow rightTextFlow = new TextFlow();
    private Label statusLabel = new Label("Listo.");
    private VBox toolbarContainer;
    
    // variables de archivos
    private File activeFile;
    private byte[] activeFileBytes;
    private byte[] originalFileBytes; // for comparison when decoding without correction
    
    // Huffman specific state
    private File currentCompressedFile;
    private File currentDecompressedFile;
    
    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        primaryStage.setTitle("Aplicación Integrada Huffman - Hamming - Encriptación");

        root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Mode Selection Toolbar
        Button btnHuffmanMode = new Button("Compactar/Descompactar");
        Button btnHammingMode = new Button("Codificar/Decodificar");
        
        HBox topMenu = new HBox(10, new Label("Modo:"), btnHuffmanMode, btnHammingMode);
        topMenu.setAlignment(Pos.CENTER_LEFT);
        topMenu.setPadding(new Insets(0, 0, 10, 0));
        
        toolbarContainer = new VBox(10);
        
        VBox topContainer = new VBox(10, topMenu, toolbarContainer);
        root.setTop(topContainer);
        
        // Text areas for comparison
        ScrollPane leftScroll = new ScrollPane(leftTextFlow);
        leftScroll.setFitToWidth(true);
        VBox leftBox = new VBox(new Label("Archivo Original / Referencia"), leftScroll);
        VBox.setVgrow(leftScroll, Priority.ALWAYS);

        ScrollPane rightScroll = new ScrollPane(rightTextFlow);
        rightScroll.setFitToWidth(true);
        VBox rightBox = new VBox(new Label("Archivo Procesado / Visualización"), rightScroll);
        VBox.setVgrow(rightScroll, Priority.ALWAYS);

        leftScroll.vvalueProperty().bindBidirectional(rightScroll.vvalueProperty());
        SplitPane splitPane = new SplitPane(leftBox, rightBox);
        
        root.setCenter(splitPane);
        
        statusLabel.setPadding(new Insets(5));
        statusLabel.setTextFill(Color.NAVY);
        root.setBottom(statusLabel);
        
        // Actions
        btnHuffmanMode.setOnAction(e -> setupHuffmanToolbar());
        btnHammingMode.setOnAction(e -> promptHammingMode());
        
        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Default mode
        setupHuffmanToolbar();
    }
    
    // --- Huffman Toolbar Setup ---
    private void setupHuffmanToolbar() {
        toolbarContainer.getChildren().clear();
        Button btnLoad = new Button("Subir Archivo");
        Button btnCompress = new Button("Compactar Archivo");
        Button btnDecompress = new Button("Descompactar Archivo");
        
        btnLoad.setOnAction(e -> loadFileHuffman());
        btnCompress.setOnAction(e -> compressFile());
        btnDecompress.setOnAction(e -> decompressFile());
        
        HBox huffmanToolbar = new HBox(10, btnLoad, btnCompress, btnDecompress);
        huffmanToolbar.setAlignment(Pos.CENTER_LEFT);
        toolbarContainer.getChildren().add(huffmanToolbar);
        showStatus("Modo Compactar/Descompactar seleccionado.", false);
        clearPanels();
    }
    
    // --- Hamming Toolbar Prompts ---
    private void promptHammingMode() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Selección de Operación");
        alert.setHeaderText("Codificar / Decodificar");
        alert.setContentText("¿Desea Codificar o Decodificar un archivo?");
        
        ButtonType btnCodificar = new ButtonType("Codificar");
        ButtonType btnDecodificar = new ButtonType("Decodificar");
        alert.getButtonTypes().setAll(btnCodificar, btnDecodificar, ButtonType.CANCEL);
        
        alert.showAndWait().ifPresent(type -> {
            if (type == btnCodificar) {
                setupHammingEncodeToolbar();
            } else if (type == btnDecodificar) {
                setupHammingDecodeToolbar();
            }
        });
    }
    
    private void setupHammingEncodeToolbar() {
        toolbarContainer.getChildren().clear();
        Button btnLoad = new Button("Subir Archivo");
        
        ComboBox<String> blockSizeBox = new ComboBox<>();
        blockSizeBox.getItems().addAll("8 bits", "1024 bits", "16384 bits");
        blockSizeBox.setValue("8 bits");
        
        ComboBox<String> errorBox = new ComboBox<>();
        errorBox.getItems().addAll("Sin error", "1 error", "2 errores");
        errorBox.setValue("Sin error");
        
        Button btnProtect = new Button("Proteger");
        
        btnLoad.setOnAction(e -> loadFileHamming(false));
        btnProtect.setOnAction(e -> encodeAndProtectFile(blockSizeBox.getValue(), errorBox.getValue()));
        
        HBox encodeToolbar = new HBox(10, btnLoad, new Label("Bloque:"), blockSizeBox, new Label("Tipo de error:"), errorBox, btnProtect);
        encodeToolbar.setAlignment(Pos.CENTER_LEFT);
        toolbarContainer.getChildren().add(encodeToolbar);
        showStatus("Modo Codificar seleccionado.", false);
        clearPanels();
    }
    
    private void setupHammingDecodeToolbar() {
        toolbarContainer.getChildren().clear();
        Button btnLoad = new Button("Subir Archivo");
        
        CheckBox chkCorrect = new CheckBox("Corregir errores");
        chkCorrect.setSelected(true);
        
        Button btnUnprotect = new Button("Desproteger");
        
        btnLoad.setOnAction(e -> loadFileHamming(true));
        btnUnprotect.setOnAction(e -> decodeAndUnprotectFile(chkCorrect.isSelected()));
        
        HBox decodeToolbar = new HBox(10, btnLoad, chkCorrect, btnUnprotect);
        decodeToolbar.setAlignment(Pos.CENTER_LEFT);
        toolbarContainer.getChildren().add(decodeToolbar);
        showStatus("Modo Decodificar seleccionado.", false);
        clearPanels();
    }
    
    // --- File Loading Logic ---
    private void loadFileHuffman() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(mainStage);
        if (f != null) {
            activeFile = f;
            currentCompressedFile = null;
            currentDecompressedFile = null;
            try {
                activeFileBytes = Files.readAllBytes(f.toPath());
                updatePanel(leftTextFlow, activeFileBytes, null);
                rightTextFlow.getChildren().clear();
                showStatus("Archivo cargado: " + f.getName(), false);
            } catch (Exception ex) {
                showStatus("Error al cargar: " + ex.getMessage(), true);
            }
        }
    }
    
    private void loadFileHamming(boolean isDecoding) {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(mainStage);
        if (f != null) {
            try {
                byte[] tempBytes = Files.readAllBytes(f.toPath());
                
                // If decoding and it's encrypted, try to decrypt first
                if (isDecoding && f.getName().endsWith(".enc")) {
                    try {
                        tempBytes = CryptoTimeHelper.unpackTimeLock(tempBytes);
                        showStatus("Archivo desencriptado automáticamente por fecha válida.", false);
                    } catch (Exception ex) {
                        showStatus(ex.getMessage(), true);
                        return; // Stop loading if decryption fails (date not reached)
                    }
                } else if (f.getName().endsWith(".enc")) {
                    showStatus("Advertencia: Ha cargado un archivo encriptado en modo Codificar.", false);
                }
                
                activeFile = f;
                activeFileBytes = tempBytes;
                
                if (f.getName().endsWith(".txt")) {
                    originalFileBytes = activeFileBytes.clone();
                }
                
                updatePanel(leftTextFlow, activeFileBytes, null);
                rightTextFlow.getChildren().clear();
                if (!isDecoding || !f.getName().endsWith(".enc")) {
                     showStatus("Archivo " + f.getName() + " cargado exitosamente.", false);
                }
            } catch (Exception ex) {
                showStatus("No se pudo cargar: " + ex.getMessage(), true);
            }
        }
    }
    
    // --- Huffman Operations ---
    private void compressFile() {
        if (activeFile == null) {
            showStatus("Cargue un archivo primero.", true); return;
        }
        try {
            String originalName = activeFile.getName();
            String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
            currentCompressedFile = new File(activeFile.getParentFile(), baseName + ".huf");
            
            long startTime = System.currentTimeMillis();
            HuffmanCoder.compress(activeFile, currentCompressedFile);
            long endTime = System.currentTimeMillis();
            
            byte[] compressedBytes = Files.readAllBytes(currentCompressedFile.toPath());
            updatePanel(rightTextFlow, compressedBytes, null);
            
            showStatus("Compactado en " + (endTime - startTime) + "ms.", false);
            showHuffmanStats();
            
        } catch (Exception e) {
            showStatus("Error al compactar: " + e.getMessage(), true);
        }
    }
    
    private void decompressFile() {
        if (currentCompressedFile == null && activeFile != null && activeFile.getName().endsWith(".huf")) {
            currentCompressedFile = activeFile;
        }
        if (currentCompressedFile == null) {
            showStatus("No hay archivo compactado.", true); return;
        }
        try {
            String originalName = currentCompressedFile.getName();
            String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
            currentDecompressedFile = new File(currentCompressedFile.getParentFile(), baseName + ".dhu");
            
            HuffmanCoder.decompress(currentCompressedFile, currentDecompressedFile);
            
            byte[] decompressedBytes = Files.readAllBytes(currentDecompressedFile.toPath());
            byte[] originalBytesToCompare = null;
            try {
                if (activeFile != null && activeFile.exists() && activeFile.getName().endsWith(".txt")) {
                    originalBytesToCompare = Files.readAllBytes(activeFile.toPath());
                } else {
                    File potentialOriginal = new File(currentCompressedFile.getParentFile(), baseName + ".txt");
                    if (potentialOriginal.exists()) {
                         originalBytesToCompare = Files.readAllBytes(potentialOriginal.toPath());
                    }
                }
            } catch (Exception ignore) {}
            
            updatePanel(rightTextFlow, decompressedBytes, originalBytesToCompare);
            
            showStatus("Descompactado exitosamente.", false);
        } catch (Exception e) {
            showStatus("Error al descompactar: " + e.getMessage(), true);
        }
    }
    
    private void showHuffmanStats() {
        if (activeFile == null || currentCompressedFile == null) return;
        long origSize = activeFile.length();
        long compSize = currentCompressedFile.length();
        double ratio = (double) compSize / origSize * 100;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de Compactación");
        alert.setHeaderText(null);
        alert.setContentText(String.format("Tamaño Original: %d bytes\nTamaño Compactado: %d bytes\nPorcentaje de compactación: %.2f%%", origSize, compSize, 100 - ratio));
        alert.showAndWait();
    }
    
    // --- Hamming Operations ---
    private int extractN(String choice) {
        if (choice.startsWith("8")) return 8;
        if (choice.startsWith("1024")) return 1024;
        if (choice.startsWith("16384")) return 16384;
        return 8;
    }
    
    private int extractErrors(String choice) {
        if (choice.startsWith("1")) return 1;
        if (choice.startsWith("2")) return 2;
        return 0;
    }
    
    private void encodeAndProtectFile(String blockStr, String errorStr) {
        if (activeFileBytes == null || !activeFile.getName().endsWith(".txt")) {
            showStatus("Debe cargar un archivo original (.txt) para protegerlo.", true);
            return;
        }
        try {
            int N = extractN(blockStr);
            int maxErrs = extractErrors(errorStr);
            
            // Protect
            byte[] protectedBytes = HammingCodec.protect(activeFileBytes, N);
            
            // Inject Errors
            byte[] finalBytes = HammingCodec.introduceErrors(protectedBytes, N, maxErrs);
            
            int infoBits = activeFileBytes.length * 8;
            int numBlocks = (BitManipulator.unpackBits(protectedBytes, protectedBytes.length * 8).length) / N;
            
            // Ask for Encryption
            Alert encAlert = new Alert(Alert.AlertType.CONFIRMATION);
            encAlert.setTitle("Encriptación");
            encAlert.setHeaderText("¿Desea encriptar el archivo por fecha?");
            ButtonType btnYes = new ButtonType("Sí");
            ButtonType btnNo = new ButtonType("No, guardar sin encriptar");
            encAlert.getButtonTypes().setAll(btnYes, btnNo);
            
            boolean isEncrypted = false;
            
            if (encAlert.showAndWait().orElse(btnNo) == btnYes) {
                TextInputDialog dateDialog = new TextInputDialog(LocalDate.now().plusDays(1).toString());
                dateDialog.setHeaderText("Fecha (AAAA-MM-DD):");
                String dateStr = dateDialog.showAndWait().orElse(null);
                
                if (dateStr != null) {
                    TextInputDialog timeDialog = new TextInputDialog("12:00");
                    timeDialog.setHeaderText("Hora (HH:MM):");
                    String timeStr = timeDialog.showAndWait().orElse(null);
                    
                    if (timeStr != null) {
                        try {
                            LocalDateTime targetTime = LocalDateTime.parse(dateStr + "T" + timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                            if (targetTime.isAfter(LocalDateTime.now())) {
                                finalBytes = CryptoTimeHelper.packWithTimeLock(finalBytes, targetTime);
                                isEncrypted = true;
                                showStatus("Archivo protegido y encriptado hasta: " + targetTime, false);
                            } else {
                                showStatus("Fecha en el pasado. Guardando sin encriptar.", true);
                            }
                        } catch(DateTimeParseException dtpe) {
                             showStatus("Formato de fecha inválido. Guardando sin encriptar.", true);
                        }
                    }
                }
            }
            
            String ext = isEncrypted ? ".enc" : (N == 8 ? ".HA1" : (N == 1024 ? ".HA2" : ".HA3"));
            File outFile = new File(activeFile.getParent(), activeFile.getName().replace(".txt", "") + ext);
            Files.write(outFile.toPath(), finalBytes);
            
            updatePanel(rightTextFlow, finalBytes, null);
            showHammingStats(activeFileBytes.length, finalBytes.length, numBlocks, infoBits, maxErrs, N);
            
            if (!isEncrypted) showStatus("Archivo protegido creado: " + outFile.getName(), false);
            
        } catch (Exception e) {
            showStatus("Error en protección: " + e.getMessage(), true);
        }
    }
    
    private void showHammingStats(int origSize, int finalSize, int numBlocks, int infoBits, int maxErrs, int N) {
        int m = (int) (Math.log(N) / Math.log(2));
        int addedBitsPerBlock = m + 1;
        int totalAddedBits = numBlocks * addedBitsPerBlock + 32; // 32 bits of header
        double incRatio = ((double) finalSize - origSize) / origSize * 100;
        double redundancy = (double) totalAddedBits / (infoBits + totalAddedBits);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de Protección (Hamming)");
        alert.setHeaderText(null);
        String msg = String.format(
            "Tamaño Original: %d bytes\nTamaño Protegido: %d bytes\nPorcentaje de Incremento: %.2f%%\nCantidad de Bloques: %d\nBits de Información: %d\nErrores introducidos (Max por bloque): %d\nBits Agregados: %d\nRedundancia: %.4f",
            origSize, finalSize, incRatio, numBlocks, infoBits, maxErrs, totalAddedBits, redundancy
        );
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    private void decodeAndUnprotectFile(boolean correctErrors) {
        if (activeFileBytes == null) {
            showStatus("No hay archivo para desproteger.", true); return;
        }
        try {
            int N = 8;
            if (activeFile.getName().contains("2")) N = 1024;
            if (activeFile.getName().contains("3")) N = 16384;
            
            byte[] resultBytes = HammingCodec.unprotect(activeFileBytes, N, correctErrors);
            
            if (HammingCodec.doubleErrorDetected) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Advertencia Crítica");
                alert.setHeaderText("Error Doble Detectado");
                alert.setContentText("Se ha detectado un error doble en al menos un módulo. No es posible corregirlo con total seguridad. Se mostrará el mejor intento de recuperación.");
                alert.showAndWait();
            }
            
            String extOriginal = correctErrors ? "_DC.txt" : "_DE.txt";
            File outFile = new File(activeFile.getParent(), activeFile.getName().substring(0, activeFile.getName().lastIndexOf(".")) + extOriginal);
            Files.write(outFile.toPath(), resultBytes);
            
            if (!correctErrors && originalFileBytes != null) {
                updatePanel(rightTextFlow, resultBytes, originalFileBytes);
            } else {
                updatePanel(rightTextFlow, resultBytes, null);
            }
            
            showStatus("Archivo desprotegido generado: " + outFile.getName(), false);
        } catch (Exception e) {
            showStatus("Error al desproteger: " + e.getMessage(), true);
        }
    }
    
    // --- Utilities ---
    private void clearPanels() {
        leftTextFlow.getChildren().clear();
        rightTextFlow.getChildren().clear();
        activeFile = null;
        activeFileBytes = null;
        originalFileBytes = null;
    }
    
    private void updatePanel(TextFlow panel, byte[] data, byte[] reference) {
        panel.getChildren().clear();
        int limit = Math.min(data.length, 50000); // Prevent UI freeze
        
        for (int i = 0; i < limit; i++) {
            char curr = (char) (data[i] & 0xFF);
            char ref = (reference != null && i < reference.length) ? (char) (reference[i] & 0xFF) : curr;
            
            Text t = new Text(String.valueOf(curr));
            t.setFont(Font.font("Monospaced", 14));
            if (reference != null && curr != ref) {
                t.setFill(Color.RED);
                t.setStyle("-fx-font-weight: bold;");
            } else {
                t.setFill(Color.NAVY);
            }
            panel.getChildren().add(t);
        }
        
        if (data.length > limit) {
            Text truncateMsg = new Text("\n... [Archivo demasiado grande, mostrando los primeros " + limit + " bytes]");
            truncateMsg.setFont(Font.font("Monospaced", 14));
            truncateMsg.setFill(Color.RED);
            panel.getChildren().add(truncateMsg);
        }
    }
    
    private void showStatus(String msg, boolean isError) {
        statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);
        statusLabel.setText(msg);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
