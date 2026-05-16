package huffman;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
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
import java.io.FileInputStream;
import java.io.IOException;

public class HuffmanApp extends Application {
    // Comentario de prueba
    private File currentOriginalFile;
    private File currentCompressedFile;
    private File currentDecompressedFile;

    private TextFlow leftTextFlow;
    private TextFlow rightTextFlow;
    private Label statusBar;

    private static final Font MONOSPACED_FONT = Font.font("Monospaced", 14);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Compresor Huffman");

        // 1. Contenedor Raíz: VBox con padding de 10px.
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // 2. Espaciado en Barras: HBox (topBar)
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button btnLoad = new Button(" CARGAR ARCHIVO ");
        Button btnCompress = new Button(" COMPACTAR ARCHIVO ");
        Button btnDecompress = new Button(" DESCOMPACTAR ARCHIVO ");
        Button btnView = new Button(" VER ARCHIVOS EN PANTALLA ");
        Button btnStats = new Button(" VER ESTADISTICA ");

        topBar.getChildren().addAll(btnLoad, btnCompress, btnDecompress, btnView, btnStats);

        // 3. División Central: SplitPane
        SplitPane splitPane = new SplitPane();
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // Contenedores de texto: ScrollPane
        ScrollPane leftScroll = new ScrollPane();
        leftScroll.setFitToWidth(true);
        leftTextFlow = new TextFlow();
        leftScroll.setContent(leftTextFlow);

        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightTextFlow = new TextFlow();
        rightScroll.setContent(rightTextFlow);

        // Sincronización de scroll
        leftScroll.vvalueProperty().bindBidirectional(rightScroll.vvalueProperty());

        splitPane.getItems().addAll(leftScroll, rightScroll);
        // Dos columnas de igual ancho
        splitPane.setDividerPositions(0.5);

        // 4. Barra de estado inferior
        statusBar = new Label("Estado: Esperando archivo...");
        statusBar.setPadding(new Insets(5));
        statusBar.setTextFill(Color.NAVY);

        root.getChildren().addAll(topBar, splitPane, statusBar);

        // Acciones de botones
        btnLoad.setOnAction(e -> loadFile(primaryStage));
        btnCompress.setOnAction(e -> compressFile());
        btnDecompress.setOnAction(e -> decompressFile());
        btnView.setOnAction(e -> viewFilesOnScreen());
        btnStats.setOnAction(e -> viewStatistics());

        // Configuración de la ventana Principal (Scene): 1100x600 píxeles.
        Scene scene = new Scene(root, 1100, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setStatus(String message, boolean isError) {
        statusBar.setText(message);
        statusBar.setTextFill(isError ? Color.RED : Color.NAVY);
    }

    private void loadFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            currentOriginalFile = file;
            currentCompressedFile = null;
            currentDecompressedFile = null;
            leftTextFlow.getChildren().clear();
            rightTextFlow.getChildren().clear();
            setStatus("Archivo cargado: " + file.getName(), false);
        }
    }

    private void compressFile() {
        if (currentOriginalFile == null) {
            setStatus("Error: No hay archivo cargado para compactar.", true);
            return;
        }

        try {
            String originalName = currentOriginalFile.getName();
            int dotIndex = originalName.lastIndexOf('.');
            String baseName = (dotIndex == -1) ? originalName : originalName.substring(0, dotIndex);
            
            // Generar archivo .huf en el mismo directorio
            File parentDir = currentOriginalFile.getParentFile();
            currentCompressedFile = new File(parentDir, baseName + ".huf");
            
            setStatus("Compactando...", false);
            HuffmanCoder.compress(currentOriginalFile, currentCompressedFile);
            setStatus("Archivo compactado exitosamente: " + currentCompressedFile.getName(), false);
            
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error al compactar: " + e.getMessage(), true);
        }
    }

    private void decompressFile() {
        if (currentCompressedFile == null) {
            // El usuario podría querer cargar directamente un .huf y descomprimirlo.
            // Según los requerimientos: "Solo el boton de CARGAR ARCHIVO va a permitir cargar un archivo"
            if (currentOriginalFile != null && currentOriginalFile.getName().endsWith(".huf")) {
                currentCompressedFile = currentOriginalFile;
            } else {
                setStatus("Error: No hay archivo .huf para descompactar. Compacte primero o cargue un .huf.", true);
                return;
            }
        }

        try {
            String compressedName = currentCompressedFile.getName();
            int dotIndex = compressedName.lastIndexOf('.');
            String baseName = (dotIndex == -1) ? compressedName : compressedName.substring(0, dotIndex);
            
            File parentDir = currentCompressedFile.getParentFile();
            currentDecompressedFile = new File(parentDir, baseName + ".dhu");
            
            setStatus("Descompactando...", false);
            HuffmanCoder.decompress(currentCompressedFile, currentDecompressedFile);
            setStatus("Archivo descompactado exitosamente: " + currentDecompressedFile.getName(), false);
            
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error al descompactar: " + e.getMessage(), true);
        }
    }

    private void viewFilesOnScreen() {
        if (currentOriginalFile == null) {
            setStatus("Error: Debe cargar un archivo primero.", true);
            return;
        }
        
        File fileToCompare = currentDecompressedFile != null ? currentDecompressedFile : currentCompressedFile;
        if (fileToCompare == null) {
            setStatus("Error: Debe compactar o descompactar un archivo primero para comparar.", true);
            return;
        }

        leftTextFlow.getChildren().clear();
        rightTextFlow.getChildren().clear();
        
        try {
            byte[] originalBytes = readAllBytes(currentOriginalFile);
            byte[] comparedBytes = readAllBytes(fileToCompare);

            int maxLength = Math.max(originalBytes.length, comparedBytes.length);

            // Evitar colapsar la UI con archivos gigantes. Solo mostramos hasta 50000 bytes.
            int limit = Math.min(maxLength, 50000);
            
            for (int i = 0; i < limit; i++) {
                char charLeft = i < originalBytes.length ? (char) (originalBytes[i] & 0xFF) : ' ';
                char charRight = i < comparedBytes.length ? (char) (comparedBytes[i] & 0xFF) : ' ';

                Text textLeft = new Text(String.valueOf(charLeft));
                textLeft.setFont(MONOSPACED_FONT);
                textLeft.setFill(Color.NAVY);

                Text textRight = new Text(String.valueOf(charRight));
                textRight.setFont(MONOSPACED_FONT);
                
                if (charLeft != charRight && i < originalBytes.length && i < comparedBytes.length) {
                    // Resaltado de diferencias según guía
                    textRight.setFill(Color.RED);
                    textRight.setStyle("-fx-font-weight: bold;");
                } else {
                    textRight.setFill(Color.NAVY);
                }

                leftTextFlow.getChildren().add(textLeft);
                rightTextFlow.getChildren().add(textRight);
            }
            
            if (maxLength > limit) {
                Text truncateMsg1 = new Text("\n... [Archivo demasiado grande, mostrando los primeros " + limit + " bytes]");
                truncateMsg1.setFont(MONOSPACED_FONT);
                truncateMsg1.setFill(Color.RED);
                leftTextFlow.getChildren().add(truncateMsg1);
                
                Text truncateMsg2 = new Text("\n... [Archivo demasiado grande, mostrando los primeros " + limit + " bytes]");
                truncateMsg2.setFont(MONOSPACED_FONT);
                truncateMsg2.setFill(Color.RED);
                rightTextFlow.getChildren().add(truncateMsg2);
            }

            setStatus("Mostrando " + currentOriginalFile.getName() + " vs " + fileToCompare.getName(), false);

        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Error al leer archivos para mostrar: " + e.getMessage(), true);
        }
    }

    private void viewStatistics() {
        if (currentOriginalFile == null) {
            setStatus("Error: No hay datos suficientes para estadísticas. Cargue y compacte un archivo.", true);
            return;
        }

        long originalSize = currentOriginalFile.length();
        long compressedSize = currentCompressedFile != null ? currentCompressedFile.length() : 0;
        long decompressedSize = currentDecompressedFile != null ? currentDecompressedFile.length() : 0;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Estadísticas de Compresión");
        alert.setHeaderText("Tamaños de los archivos");
        
        StringBuilder content = new StringBuilder();
        content.append("Archivo Original: ").append(originalSize).append(" bytes\n");
        if (currentCompressedFile != null) {
            content.append("Archivo Compactado (.huf): ").append(compressedSize).append(" bytes\n");
            double ratio = (double) compressedSize / originalSize * 100;
            content.append(String.format("Relación de Compresión: %.2f%%\n", ratio));
        } else {
            content.append("Archivo Compactado (.huf): No generado aún\n");
        }
        
        if (currentDecompressedFile != null) {
            content.append("Archivo Descompactado (.dhu): ").append(decompressedSize).append(" bytes\n");
            if (originalSize == decompressedSize) {
                content.append("¡La descompresión fue sin pérdidas (tamaños coinciden)!\n");
            } else {
                content.append("ADVERTENCIA: Los tamaños original y descompactado no coinciden.\n");
            }
        } else {
            content.append("Archivo Descompactado (.dhu): No generado aún\n");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int bytesRead = 0;
            while (bytesRead < data.length) {
                int read = fis.read(data, bytesRead, data.length - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            return data;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
